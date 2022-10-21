package com.example.letmeknow.util

import android.content.Context
import com.example.letmeknow.messages.BaseMessage
import com.example.letmeknow.messages.MessageClassManager
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Text
import org.xml.sax.InputSource
import java.io.StringReader
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory


class Encoder {
    private fun elementToString(element: Element): String {
        var result = "<" + element.tagName
        if (element.attributes.length > 0) {
            val map = HashMap<String, String>()
            result += " "
            for (i in 0 until element.attributes.length) {
                val attr = element.attributes.item(i) as Attr
                map[attr.name] = "'" + attr.value + "'"
            }
            result += map.map { it.key + "=" + it.value }.joinToString(" ")

        }
        result += ">"
        for (i in 0 until element.childNodes.length) {
            if (element.childNodes.item(i) is Element) {
                result += elementToString(element.childNodes.item(i) as Element)
            } else if (element.childNodes.item(i) is Text) {
                val txt = element.childNodes.item(i) as Text
                result += txt.wholeText.trim()
            }
        }
        result += "</" + element.tagName + ">"
        return result
    }

    fun convertToXml(
        msg: BaseMessage,
        context: Context,
        className: String,
        keyProvider: KeyProvider
    ): String {
        val builderFactory = DocumentBuilderFactory.newInstance()
        val builder = builderFactory.newDocumentBuilder()
        val doc = context.assets.open(className + ".xml").use { builder.parse(it) }
        convertToXmlRec(msg, doc.documentElement, keyProvider)
        doc.documentElement.setAttribute("hash", "")

        return elementToString(doc.documentElement)
    }

    private fun isCryptographyKey(id: String): Boolean {
        return id == "DECRYPTION_KEY" || id == "USER_PASSWORD"
    }

    fun loadXMLFromString(xml: String?): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder: DocumentBuilder = factory.newDocumentBuilder()
        val `is` = InputSource(StringReader(xml))
        return builder.parse(`is`)
    }

    fun convertToXmlRec(mapper: KeyValueMapper, root: Element, keyProvider: KeyProvider) {
        val isEncrypted = root.getAttribute("mode") != "plain"
        for (i in 0..root.childNodes.length) {
            if (root.childNodes.item(i) !is Element) continue
            val ele = root.childNodes.item(i) as Element
            val id = ele.getAttribute("id")
            if (ele.tagName == "single") {
                val isCryptoKey = isCryptographyKey(id)
                val length =
                    if (isCryptoKey) keyProvider.getKey(id).encoded.size else mapper.getValue(id).toString().length
                var value = if (isCryptoKey)
                    Base64.getEncoder().encode(keyProvider.getKey(id).encoded).decodeToString()
                else mapper.getValue(id).toString()
                ele.setAttribute("len", length.toString())


                if (isEncrypted) {
                    android.util.Log.d("Before encrypted", id + " " + value)
                    val encryptId = getEncryptDecryptId(root).first
                    val encrypted = encrypt(
                        value.encodeToByteArray(),
                        keyProvider,
                        root.getAttribute("mode"),
                        encryptId
                    )
                    ele.setAttribute("iv", Base64.getEncoder().encode(encrypted.IV.iv).decodeToString())
                    value = Base64.getEncoder().encode(encrypted.data).decodeToString()
                    android.util.Log.d("After encrypted", id + " " + value)
                }
                ele.appendChild(ele.ownerDocument.createTextNode(value))


            } else if (ele.tagName == "block") {
                convertToXmlRec(mapper, ele, keyProvider)
            }
        }
    }

    fun convertXmlToMessage(
        xmlStr: String,
        keyProvider: KeyProvider
    ): Pair<BaseMessage, KeyValueMapper> {
        val receivedXml = loadXMLFromString(xmlStr)
        val mapper = RecursiveKeyValueMapper(StubKeyValueMapper())

        convertXmlToMessageRec(mapper, receivedXml.documentElement, keyProvider)
        val msg = MessageClassManager.newInstance(mapper.getValue("messageType").toString().toShort())
        val foundSecrets = StubKeyValueMapper()
        for (key in mapper.getKeys()) {
            if (isCryptographyKey(key)) {
                foundSecrets.setValue(key, mapper.getValue(key))
            } else {
                msg.setValue(key, mapper.getValue(key))
            }

        }
        return Pair(msg, foundSecrets)
    }

    private fun convertXmlToMessageRec(mapper: KeyValueMapper, root: Element, keyProvider: KeyProvider) {
        val isEncrypted = root.getAttribute("mode") != "plain"
        for (i in 0..root.childNodes.length) {
            if (root.childNodes.item(i) !is Element) continue
            val ele = root.childNodes.item(i) as Element
            val id = ele.getAttribute("id")
            if (ele.tagName == "single") {
                var value = ele.childNodes.item(0).nodeValue
                if (isEncrypted) {
                    val decoded = Base64.getDecoder().decode(value)
                    val decryptId = getEncryptDecryptId(root).second
                    val iv = Base64.getDecoder().decode(ele.getAttribute("iv"))
                    val encrypted = EncryptedData(IvParameterSpec(iv), decoded)
                    var decrypted = decrypt(encrypted, keyProvider, root.getAttribute("mode"), decryptId)
                    val len = ele.getAttribute("len").toInt()
                    decrypted = decrypted.slice(0 until len).toByteArray()
                    if (isCryptographyKey(id)) {
                        mapper.setValue(id, decrypted)
                    } else {
                        value = decrypted.decodeToString()
                        mapper.setValue(id, value)
                    }

                }
                if (!isCryptographyKey(id)) {
                    mapper.setValue(id, value)
                }


            } else if (ele.tagName == "block") {
                convertXmlToMessageRec(mapper, ele, keyProvider)
            }
        }

    }

    private fun getBlockSize(): Int {
        return 16
    }

    private fun getHashSize(hashType: String): Int {
        return hashType.split("-")[1].toInt() / 8
    }


    fun generateIv(): IvParameterSpec {
        val iv = ByteArray(16)
        //SecureRandom().nextBytes(iv)
        return IvParameterSpec(iv)
    }

    private class EncryptedData(val IV: IvParameterSpec, val data: ByteArray)

    private fun encrypt(bytes: ByteArray, keyProvider: KeyProvider, mode: String, keyId: String): EncryptedData {
        val cipher = Cipher.getInstance(mode)
        val iv = generateIv()
        keyProvider.initCipher(cipher, Cipher.ENCRYPT_MODE, keyId, iv)
        var buffer = bytes
        if (bytes.size % 16 != 0) {
            buffer = Arrays.copyOf(bytes, bytes.size + 16 - bytes.size % 16)
        }
        val encrypted = cipher.doFinal(buffer)
        return EncryptedData(iv, encrypted)
    }

    private fun pad(array: ByteArray): ByteArray {
        val blockSize = 16
        val pad = blockSize - (array.size % blockSize)
        return Arrays.copyOf(array, array.size + pad)
    }

    private fun decrypt(
        encryptedData: EncryptedData,
        keyProvider: KeyProvider,
        mode: String,
        keyId: String
    ): ByteArray {
        val cipher = Cipher.getInstance(mode)
        val key = keyProvider.getKey(keyId)
        val iv = encryptedData.IV
        keyProvider.initCipher(cipher, Cipher.DECRYPT_MODE, keyId, iv)
        return cipher.doFinal(encryptedData.data)
    }

    private fun hash(bytes: ByteArray, hashType: String): ByteArray {
        return MessageDigest.getInstance(hashType).digest(bytes)
    }

    private fun getEncryptDecryptId(ele: Element): Pair<String, String> {
        if (ele.hasAttribute("encryptKeyId") && ele.hasAttribute("decryptKeyId")) {
            return Pair(ele.getAttribute("encryptKeyId"), ele.getAttribute("decryptKeyId"))
        } else if (ele.hasAttribute("encryptKeyId")) {
            return Pair(ele.getAttribute("encryptKeyId"), ele.getAttribute("encryptKeyId"))
        } else if (ele.hasAttribute("decryptKeyId")) {
            return Pair(ele.getAttribute("decryptKeyId"), ele.getAttribute("decryptKeyId"))
        } else {
            throw IllegalStateException("Both encryption and decryption key are missing")
        }
    }

}