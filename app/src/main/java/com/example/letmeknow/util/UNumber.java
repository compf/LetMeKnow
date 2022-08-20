package com.example.letmeknow.util;

public abstract class UNumber {
    private byte[] byteRepresentation;
    protected UNumber(int size){
        byteRepresentation=new byte[size];
    }
    protected UNumber(int size,long value){
        this(size);
    }
    public int size(){
        return byteRepresentation.length;
    }

}
