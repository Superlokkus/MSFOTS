/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package msfots;

import java.io.*;
import static java.lang.Integer.min;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.*;
import java.nio.Buffer.*;
import java.nio.ByteBuffer;
import java.lang.*;

/**
 *
 * @author MarkusKlemm.net
 */

public class Client implements AutoCloseable
{
    public Client (int port, InetAddress addr) throws SocketException
    {
        dataFieldOctets = 1024; //NetworkInterface.getByInetAddress sucks
        ds = new DatagramSocket();
        ds.connect(addr,port);
        random = new Random();
    }
    
    public void sendFile(Path pathToFile) throws FileNotFoundException, IOException
    {
        p = pathToFile;
        
        cfis = new CheckedInputStream (new FileInputStream(p.toString()),new CRC32());
        finishedReading = false;
        
        sessionId = (short) random.nextInt();
        packetId = 0;
        
        DatagramPacket outGoingPacket = generateStartPacket();
        
        class RTO
        {
            public int getValue()
            {
                return (min(1000,eRTT + 4 * dRTT));
            }
            
            public void sampleRTT(int sRTT)
            {
                eRTT = (int) ((1-alpha)*eRTT + alpha*sRTT);
                dRTT = (int) ((1-beta)*dRTT + beta*(sRTT-eRTT)); 
            }
            
            public RTO()
            {
                eRTT = 3000; dRTT = 1500;
                alpha = 0.125; beta = 0.125;
            }
            
            private int eRTT,dRTT;
            private final double alpha,beta;
        }
        
        RTO rto = new RTO();
        
        do
        {
            //As long as there are packets to send
            for (int sendTry = 0; ;sendTry++)
            {
                //As long as not recieved the correct ackn
                final long pre = System.nanoTime();
                ds.send(outGoingPacket);
                
                ByteBuffer inComing = ByteBuffer.allocate(3);
                assert(inComing.hasArray());
                DatagramPacket inComingPacket = new DatagramPacket(inComing.array(),inComing.capacity());
                
                try {
                    ds.setSoTimeout(rto.getValue());
                    ds.receive(inComingPacket);
                } catch (java.net.SocketTimeoutException e)
                {
                    rto.sampleRTT((int) (System.nanoTime() - pre) / 1000);
    
                    if (sendTry == 10)
                    {
                        throw new HostTimeOutException("Cancel resend after 10th try");
                    }
                    continue; //Retry
                }
                rto.sampleRTT((int) (System.nanoTime() - pre) / 1000);
                if (inComing.getShort() == sessionId && inComing.get() == packetId % 2)
                {
                    //TODO behavoir maybe resend and break IFF correctSessionId and Timeout
                    break; //Succesfully sent
                }
                
            }
            
        } while ((outGoingPacket = generateNextPacket()) != null);
    }
    
    @Override
    public void close () throws IOException
    {
        ds.close();
        if (cfis != null)
            cfis.close();
    }
    
    private DatagramSocket ds;
    private Path p;
    private CheckedInputStream cfis;
    private boolean finishedReading;
    
    private Random random;
    private short sessionId;
    private byte packetId;
    private final short dataFieldOctets;
    
    private DatagramPacket generateStartPacket() throws UnsupportedEncodingException
    {
        ByteBuffer data = ByteBuffer.allocate(512); //Initial byte order always bigendian
        
        data.putShort(sessionId);
        assert(packetId == 0); data.put(packetId);
        
        final byte[] startSymbol = ("Start").getBytes("US-ASCII");
        byte[] testSymbol = {0x53,0x74,0x61,0x72,0x74};
        assert(testSymbol ==  startSymbol);
        data.put(startSymbol);
        
        data.putLong(p.toFile().length());
        
        data.putShort((short) min(255, p.getFileName().toString().length()));
        data.put(p.getFileName().toString().substring(0,min(255, p.getFileName().toString().length())).getBytes("UTF-8") );
        
        CRC32 firstPacketCRC = new CRC32();
        assert(data.hasArray());
        firstPacketCRC.update(data.array(),0,data.position());//High off by 1 risk
        
        data.putInt((int) firstPacketCRC.getValue());//Hope it takes the 32 LSBs
        
        DatagramPacket dp = new DatagramPacket(data.array(),0,data.position());
        return dp;
    }
    
    private DatagramPacket generateNextPacket() throws IOException
    {
        if (finishedReading)
        {
            return null;
        }
        
        final byte headerlength = 3;
        ByteBuffer data = ByteBuffer.allocate(dataFieldOctets+headerlength);
        assert(data.hasArray());
        
        packetId++;
        data.putShort(sessionId); data.put((byte)(packetId % 2));
        
        int read = cfis.read(data.array(),headerlength,dataFieldOctets);//Todo catch IOException and retry
        int toBeUsed = headerlength;

        if (read == -1)
        {
            finishedReading = true;
            data.putInt((int) cfis.getChecksum().getValue());
            toBeUsed += 4;
        } else if (read < dataFieldOctets - 4)
        {
            finishedReading = true;
            data.position(headerlength + read);
            data.putInt((int) cfis.getChecksum().getValue());
            toBeUsed += read + 4;
        } else 
        {
            assert (read <= dataFieldOctets);
            toBeUsed += read;
        }
        
        return new DatagramPacket(data.array(),0,toBeUsed);
    }
    
}

