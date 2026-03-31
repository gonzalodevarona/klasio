package com.klasio.shared.infrastructure.exception;

public class NegativeBalanceException extends RuntimeException {
    public NegativeBalanceException(String message) { super(message); }
}
