/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package msfots;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

/**
 *
 * @author MarkusKlemm.net
 */
public class Server implements AutoCloseable {
    
    public Server (int port,Path path) throws SocketException
    {
        this(port,path,0,0);
    }
    
    public Server (int port,Path path,float packetLoss, long delayInMS) throws SocketException
    {
        p = path;
        ds = new DatagramSocket(port);
        ds.setSoTimeout(42 * 1000);
        
        loss = packetLoss; delay = delayInMS;
    }
    
    public void recvLoop () throws IOException
    {
        ByteBuffer data = ByteBuffer.allocate(65536);
        assert(data.hasArray());
        DatagramPacket packet = new DatagramPacket(data.array(),data.capacity());
        
        while (true)
        {
            //Per Session Loop
            try{
                System.out.println("Waiting for Connection");
                startACKRecv = false;
                data.clear();
                ds.receive(packet);
                data.limit(packet.getLength());
                System.out.println("Got an assumed Start Packet");
                if (!processStartPacket(data))
                {
                    continue;
                }
                System.out.println("Start packet evaluated ");
                //System.getProperty("file.separator");
                Path filePath = p.resolve(fileName);
                if (filePath.toFile().exists())
                {
                    System.out.println("File already exists");
                    filePath = p.resolve(fileName + "1");
                }
                System.out.println("File will be stored in: " + filePath.toString());
                clientAddr = packet.getAddress();
                clientPort = packet.getPort();
                
                ds.send(getACK());
                try
                {
                    cfos = new CheckedOutputStream(new FileOutputStream (filePath.toFile()),new CRC32());
                    fileLenRecv = 0;
                    
                    while (true)
                    {
                        //Per (Data)Packet loop
                        data.clear();
                        ds.receive(packet);
                        data.limit(packet.getLength());
                        
                        if (!startACKRecv)
                        {
                            //Maybe Client did not recieved first ACK
                            if (data.getShort() == sessionId && data.get() == 0)
                            {
                                //Client has obviously not recieved first ACK
                                ds.send(getACK());
                                continue;
                            } else{
                                startACKRecv = true;
                                data.rewind();
                            }
                        }
                        
                        if (data.getShort() != sessionId)
                        {
                            break;
                        }
                        final byte PacketId = data.get();
                        if (PacketId == (byte) Math.abs(lastPacketId %2 )) 
                        {
                            System.out.println("Resend Packet");
                            ds.send(getACK());
                            continue;
                        } else if (PacketId != (byte) Math.abs(lastPacketId +1 %2 ))
                        {
                            System.out.println("Illegal Packet Number");
                            continue;
                        }
                        
                        int toRead = data.limit() - data.position();
                        final long shouldLeftToRead = fileLen - fileLenRecv;
                        
                        if (toRead > shouldLeftToRead)
                        {
                            //Last Packet read CRC
                            if (toRead != shouldLeftToRead + 4)
                            {
                                System.out.println("Warning: Last Packet probably malformed");
                            }
                            toRead -= 4;
                        }
                        cfos.write(data.array(), data.position(),(int) Math.min(toRead, shouldLeftToRead));
                        fileLenRecv += Math.min(toRead, shouldLeftToRead);
                        
                        if (shouldLeftToRead == 0)
                        {
                            final int fileCRC = data.getInt();
                            System.out.println("CRC checksum recieved: " + fileCRC);
                            System.out.println("Actual CRC checksum: " + cfos.getChecksum().getValue());
                            if ( fileCRC != (int) cfos.getChecksum().getValue() )
                            {
                                System.out.println("CRC Check failed");
                                continue;
                            }
                        }
                        lastPacketId++;
                        ds.send(getACK());
                    }
            
                    
                    
                }
                finally
                {
                    cfos.close();
                }

                
            } catch (BufferUnderflowException e)
            {
                System.out.println("Error: Malformed Packet");
            } catch (java.net.SocketTimeoutException e)
            {
                System.out.println("Session-Timeout");
            }
            
        }    
        
    }
    
    private short sessionId;
    private byte lastPacketId;
    private int clientPort;
    private InetAddress clientAddr;
    private long fileLen;
    private long fileLenRecv;
    private String fileName;
    private CheckedOutputStream cfos;
    private boolean startACKRecv;
    
    private DatagramPacket getACK()
    {
        ByteBuffer b = ByteBuffer.allocate(3);
        assert(b.hasArray());
       
        b.putShort(sessionId);
        b.put(lastPacketId);
        
        return new DatagramPacket(b.array(),b.capacity(),clientAddr,clientPort);
    }
    
    private boolean processStartPacket(ByteBuffer data) throws UnsupportedEncodingException
    {
        CRC32 actualPacketCRC = new CRC32();
        sessionId = data.getShort();
        System.out.print("SessionNumber: ");
        System.out.println(sessionId);
        lastPacketId = data.get();
        if (lastPacketId != 0) {
            System.out.println("Error: Start PacketId not 0");
            return false;
        }
        byte[] token = new byte[5];
        data.get(token);
        final byte[] start = {0x53, 0x74, 0x61, 0x72, 0x74};
        if (token.equals(start)) {
            System.out.println("Error: Malformed Magic Number");
            return false;
        }
        fileLen = data.getLong();
        final short nameLen = data.getShort();
        
        byte [] name = new byte[nameLen]; data.get(name);
        fileName = new String(name,"UTF-8");
        actualPacketCRC.update(data.array(), 0, data.position());
        
        final int packetCRC = data.getInt();
        
        if ( (int) actualPacketCRC.getValue() != packetCRC)
        {
            System.out.println("Error: Start packet CRC32 test failed");
            System.out.println("Submitted start CRC: " + packetCRC + 
                    " actual: " + actualPacketCRC.getValue());
            return false;
        }
        
        return true;
    }
 
    @Override
    public void close()
    {
        ds.close();
    }
    
    private DatagramSocket ds;
    private Path p;
    private final float loss;
    private final long delay;
}
