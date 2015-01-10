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

/**
 *
 * @author MarkusKlemm.net
 */

public class Client implements AutoCloseable
{
    public Client (int port, InetAddress addr) throws SocketException
    {
        ds = new DatagramSocket(port,addr);
        random = new Random();
    }
    
    public void sendFile(Path pathToFile) throws FileNotFoundException, IOException
    {
        p = pathToFile;
        
        cfis = new CheckedInputStream (new FileInputStream(p.toString()),new CRC32());
        
        short sessionId = (short) random.nextInt();
        byte packetId = 0;
        
        System.out.print("My Session ID is:"); System.out.println(sessionId);
        System.out.print("First byte of file is:"); System.out.println(cfis.read());
        
        
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
}

