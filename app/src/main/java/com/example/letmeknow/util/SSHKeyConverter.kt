package com.example.letmeknow.util

import java.io.InputStream
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*


class SSHKeyConverter {
    fun parsePublicKey(stream: InputStream): PublicKey {
        val inputString = stream.readBytes().decodeToString().split(" ")[1]
        val decoded = Base64.getDecoder().decode(inputString)
        // 4 Bytes length
        val buffer = ByteBuffer.wrap(decoded)
        var length = buffer.int
        // String "ssh-rsa"
        var tempArray = ByteArray(length)
        buffer.get(tempArray)
        val ssh_rsa = tempArray.decodeToString()
        assert(ssh_rsa == "ssh-rsa")
        // public exponent length
        length = buffer.int
        // public exponent
        tempArray = ByteArray(length)
        buffer.get(tempArray)
        val exponent = BigInteger(tempArray)
        //length of modulus
        length = buffer.int
        //modulus
        tempArray = ByteArray(length)
        buffer.get(tempArray)
        val modulus = BigInteger(tempArray)
        return KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(modulus, exponent))

    }
}