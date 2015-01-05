/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package msfots;

import java.io.*;

/**
 *
 * @author Markus Klemm.net
 */
public class MSFOTS {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0)
        {
                System.out.println("Usage:");
                System.out.println("as server (to recive):");
                System.out.println("\t<Program> s <IP> <Port>");
                System.out.println("as client (to send):");
                System.out.println("\t<Program> c <IP> <Port> <File>");
                return;
        }
        switch (args[0].charAt(0))
                {
            case 's':
            case 'S':
                System.out.println("Server-Mode");
                break;
            case 'C':
            case 'c':
                System.out.println("Client-Mode");
                break;
            default:
                break;
        }
    }
    
}
