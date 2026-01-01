package com.example.flashsale.exception;

public class OutOfStockException extends RuntimeException {
    public OutOfStockException(String msg) { super(msg); }
}
