package com.example.letmeknow.util

import javax.crypto.SecretKey

interface KeyProvider {
    public fun getKey(keyId:String,mapper: KeyValueMapper):SecretKey;
}