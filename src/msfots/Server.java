/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package msfots;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
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
        
        loss = packetLoss; delay = delayInMS;
    }
    
    public void recvLoop () throws IOException
    {
        ByteBuffer data = ByteBuffer.allocate(512);
        assert(data.hasArray());
        DatagramPacket packet = new DatagramPacket(data.array(),data.capacity());
        
        while (true)
        {
            try{
                System.out.println("Waiting for Connection");
                data.clear();
                ds.receive(packet);
                data.limit(packet.getLength());
                System.out.println("Got an assumed Start Packet");
                if (!processStartPacket(data))
                {
                    continue;
                }
                System.out.println("Start packet evaluated ");
                p.resolve(fileName);
                System.out.println("Path now: " + p.toString());
                ds.send(getACK());
//                try (cfos = new FileOutputStream(,new CRC32()))
//                {
//                    
//                }

                
            } catch (BufferUnderflowException e)
            {
                System.out.println("Error: Malformed Packet");
            }
        }    
        
    }
    
    private short sessionId;
    private byte lastPacketId;
    private int clientPort;
    private InetAddress clientAddr;
    private long fileLen;
    private String fileName;
    private long fileCRC;
    private CheckedOutputStream cfos;
    
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
        final long packetCRC = data.getInt();
        
        if (actualPacketCRC.getValue() != packetCRC)
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
