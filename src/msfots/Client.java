/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package msfots;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.zip.CheckedInputStream;

/**
 *
 * @author MarkusKlemm.net
 */

public class Client implements AutoCloseable
{
    public Client (int port, InetAddress addr) throws SocketException
    {
        ds = new DatagramSocket(port,addr);
    }
    
    public void sendFile(Path pathToFile)
    {
        p = pathToFile;
        
        cfis = new CheckedInputStream (new FileInputStream(p.toString()));
        
    }
    
    @Override
    public void close ()
    {
        ds.close();
    }
    
    private DatagramSocket ds;
    private Path p;
    private CheckedInputStream cfis;
}

