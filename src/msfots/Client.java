/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package msfots;

import java.io.*;
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
        
        data.put(p.getFileName().toString().getBytes("UTF-8"));
        
        DatagramPacket dp = new DatagramPacket(data.array(),data.capacity());
        return dp;
    }
    
    private DatagramPacket generateNextPacket()
    {
        
        return null;
    }
    
}

