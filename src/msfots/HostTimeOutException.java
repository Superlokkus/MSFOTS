/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package msfots;

/**
 *
 * @author markusklemm.net
 */
public class HostTimeOutException extends java.io.IOException {

    /**
     * Creates a new instance of <code>HostTimeOutException</code> without
     * detail message.
     */
    public HostTimeOutException() {
    }

    /**
     * Constructs an instance of <code>HostTimeOutException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public HostTimeOutException(String msg) {
        super(msg);
    }
}
