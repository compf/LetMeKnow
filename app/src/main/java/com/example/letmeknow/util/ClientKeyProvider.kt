package com.example.letmeknow.util

import java.security.Key
import java.security.PublicKey
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ClientKeyProvider(val publicKey: PublicKey) : KeyProvider {


    private var password = "123"
    override fun getKey(keyId: String): Key {
        if (keyId == "SERVER_PUBLIC") {
            return publicKey
        } else if (keyId == "SERVER_PRIVATE") {
            throw SecurityException("Cannot use server private key")
        } else {
            val pw = password.toByteArray()
            val spec = SecretKeySpec(Arrays.copyOf(pw, 16), "AES")
            return spec
        }
    }

    fun setPassword(password: String) {
        this.password = password
    }

    override fun initCipher(cipher: Cipher, encryptMode: Int, keyId: String, iv: IvParameterSpec) {
        if (keyId == "SERVER_PUBLIC" || keyId == "SERVER_PRIVATE") {
            cipher.init(encryptMode, getKey(keyId))
        } else {
            super.initCipher(cipher, encryptMode, keyId, iv)
        }

    }

}