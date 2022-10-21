package com.example.letmeknow.util

interface KeyValueMapper {
    fun getValue(name: String): Any
    fun setValue(name: String, value: Any): Unit
    fun getKeys(): Set<String>
    fun hasValue(name: String): Boolean {
        return getKeys().contains(name)
    }

}