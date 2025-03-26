package com.nsds.group15.fooddeliveryapplicationbackend.exception;

public class AbortedOrderException extends Exception {
    @Override
    public String toString() {
        return "Shipping address not valid";
    }
}
