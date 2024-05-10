package com.example.mobl_pmd;

public class NotInitializedException extends Exception{
    public NotInitializedException() {
        super("Class is not initialized.");
    }
    public NotInitializedException(String message) {
        super(message);
    }
}
