package com.example.letmeknow.util

class StubKeyValueMapper :KeyValueMapper {
    override fun getValue(name: String): Any {
        throw IllegalArgumentException("Key not found")
    }

    override fun setValue(name: String, value: Any) {
        throw IllegalArgumentException("Key not found")
    }

    override fun getKeys(): Set<String> {
        return HashSet();
    }
}