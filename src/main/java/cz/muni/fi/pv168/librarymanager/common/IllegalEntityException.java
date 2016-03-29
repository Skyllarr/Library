/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.pv168.librarymanager.common;

/**
 *
 * @author skylar
 */
public class IllegalEntityException extends RuntimeException {

    public IllegalEntityException() {
    }

    public IllegalEntityException(String msg) {
        super(msg);
    }

    public IllegalEntityException(String message, Throwable cause) {
        super(message, cause);
    }    
}
