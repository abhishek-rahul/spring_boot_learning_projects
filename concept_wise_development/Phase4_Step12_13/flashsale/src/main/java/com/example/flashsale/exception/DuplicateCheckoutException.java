package com.example.flashsale.exception;

public class DuplicateCheckoutException extends RuntimeException {
    public DuplicateCheckoutException(String msg) { super(msg); }
}
