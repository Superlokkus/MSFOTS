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
        
        sessionId = (short) random.nextInt();
        packetId = 0;
        
        DatagramPacket lastPacket = generateStartPacket();
        
        
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
        data.put(p.getFileName().toString().substring(0,min(255-1, p.getFileName().toString().length()-1)).getBytes("UTF-8") );
        
        CRC32 firstPacketCRC = new CRC32();
        firstPacketCRC.update(data.array(),0,data.position());//High off by 1 risk
        
        data.putInt((int) firstPacketCRC.getValue());//Hope it takes the 32 LSBs
        
        DatagramPacket dp = new DatagramPacket(data.array(),0,data.position());
        return dp;
    }
    
    private DatagramPacket generateNextPacket()
    {
        
        return null;
    }
    
}

