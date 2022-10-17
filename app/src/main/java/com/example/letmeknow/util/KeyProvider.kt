package com.example.letmeknow.util

import java.security.Key;
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec

interface KeyProvider {
    public fun getKey(keyId:String):Key;
     fun initCipher(cipher: Cipher, encryptMode: Int, keyId: String,iv: IvParameterSpec){
         cipher.init(encryptMode,getKey(keyId),iv)
    }
}