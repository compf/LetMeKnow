package com.example.letmeknow.util

interface KeyValueMapper {
    public fun getValue(name:String):Any;
    public fun setValue(name:String,value:Any):Unit;
}