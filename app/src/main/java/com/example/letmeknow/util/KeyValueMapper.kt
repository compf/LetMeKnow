package com.example.letmeknow.util

interface KeyValueMapper {
    public fun getValue(name:String):Any;
    public fun setValue(name:String,value:Any):Unit;
    public fun getKeys():Set<String>;
    public fun hasValue(name:String):Boolean{
        return getKeys().contains(name);
    }

}