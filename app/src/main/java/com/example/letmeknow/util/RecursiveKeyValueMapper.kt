package com.example.letmeknow.util

open class RecursiveKeyValueMapper(val child: KeyValueMapper) : KeyValueMapper {
    private val map = mutableMapOf<String, Any>()
    override fun getValue(name: String): Any {
        if (child.hasValue(name)) return child.getValue(name)
        return map.get(name)!!
    }

    override fun getKeys(): Set<String> {
        return map.keys.union(child.getKeys())
    }

    override fun setValue(name: String, value: Any) {
        if (child.hasValue(name)) {
            child.setValue(name, value)
        } else {
            map.set(name, value)
        }

    }

}