package com.example.letmeknow.util

import android.content.Context
import android.util.Log
import android.util.Log.INFO
import com.example.letmeknow.messages.BaseMessage
import com.example.letmeknow.messages.MessageClassManager
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Text
import org.w3c.dom.ls.DOMImplementationLS
import org.w3c.dom.ls.LSSerializer
import org.xml.sax.InputSource
import java.io.StringReader
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.HashMap


class Encoder() {
    public val TRANSFOMRMATION_STRING = "AES/CBC/PKCS5Padding";

    private class AuthenticationInfoMapper : RecursiveKeyValueMapper {
        private val keyProvider: KeyProvider

        constructor(keyProvider: KeyProvider, child: KeyValueMapper) : super(child) {
            this.keyProvider = keyProvider
            this.setValue("authentication", keyProvider.getKey("password").encoded.decodeToString())

        }
    }
    private fun elementToString(element: Element):String{
        var result="<"+element.tagName;
        if(element.attributes.length>0){
            val map=HashMap<String,String>()
            result+=" ";
            for( i in 0 until element.attributes.length){
                val attr=element.attributes.item(i) as Attr
                map[attr.name]="'"+attr.value+"'"
            }
            result+= map.map { it.key+"="+it.value }.joinToString(" ")

        }
        result+=">"
        val a=element.nodeValue
        for(i in 0 until element.childNodes.length){
            if(element.childNodes.item(i) is Element){
                result +=elementToString(element.childNodes.item(i) as Element)
            }
            else if (element.childNodes.item(i) is Text){
                val txt=element.childNodes.item(i) as Text
                result+=txt.wholeText.trim()
            }
        }
        result+="</"+element.tagName+">"
        return result
    }
    public fun convertToXml(
        msg: BaseMessage,
        context: Context,
        className: String,
        keyProvider: KeyProvider
    ): String {
        val builderFactory = DocumentBuilderFactory.newInstance()
        val builder = builderFactory.newDocumentBuilder()
        val doc = context.assets.open(className + ".xml").use { builder.parse(it) }
        val mapper = AuthenticationInfoMapper(keyProvider, msg)
        convertToXmlRec(msg, doc.documentElement, keyProvider)
        doc.documentElement.setAttribute("hash","")

        var str: String = elementToString(doc.documentElement)
        val hash=Base64.getEncoder().encode(this.hash(str.encodeToByteArray(),"SHA-256")).decodeToString()
        doc.documentElement.setAttribute("hash",hash)
        str=elementToString(doc.documentElement)
        return str
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

    public fun convertToXmlRec(mapper: KeyValueMapper, root: Element, keyProvider: KeyProvider) {
        val isEncrypted = root.getAttribute("mode") != "plain"
        for (i in 0..root.childNodes.length) {
            if (root.childNodes.item(i) !is Element) continue
            val ele = root.childNodes.item(i) as Element
            val id = ele.getAttribute("id")
            if (ele.tagName == "single") {
                val isCryptoKey=isCryptographyKey(id)
                val length=if (isCryptoKey ) keyProvider.getKey(id).encoded.size else mapper.getValue(id).toString().length
                var value = if (isCryptoKey)
                    Base64.getEncoder().encode(keyProvider.getKey(id).encoded).decodeToString()
                else mapper.getValue(id).toString();
                ele.setAttribute("len",length.toString())


                if (isEncrypted) {
                    android.util.Log.d("Before encrypted",id +" "+value)
                    val encryptId=getEncryptDecryptId(root).first
                    val encrypted = encrypt(
                        value.encodeToByteArray(),
                        keyProvider,
                        root.getAttribute("mode"),
                        encryptId
                    )
                    ele.setAttribute("iv", Base64.getEncoder().encode(encrypted.IV.iv).decodeToString())
                    value = Base64.getEncoder().encode(encrypted.data).decodeToString()
                    android.util.Log.d("After encrypted",id +" "+value)
                }
                ele.appendChild(ele.ownerDocument.createTextNode(value))


            } else if (ele.tagName == "block") {
                convertToXmlRec(mapper, ele, keyProvider)
            }
        }
    }

    public fun convertXmlToMessage(
        xmlStr: String,
        context: Context,
        className: String,
        keyProvider: KeyProvider
    ): Pair<BaseMessage,KeyValueMapper> {
        val builderFactory = DocumentBuilderFactory.newInstance()
        val builder = builderFactory.newDocumentBuilder()
        val doc = context.assets.open(className + ".xml").use { builder.parse(it) }
        val receivedXml = loadXMLFromString(xmlStr)
        val mapper = RecursiveKeyValueMapper(StubKeyValueMapper())

        convertXmlToMessageRec(mapper, receivedXml.documentElement, keyProvider)
        val msg = MessageClassManager.newInstance(mapper.getValue("messageType").toString().toShort())
        val foundSecrets=StubKeyValueMapper()
        for (key in mapper.getKeys()) {
            if(isCryptographyKey(key)){
                foundSecrets.setValue(key,mapper.getValue(key))
            }
            else{
                msg.setValue(key, mapper.getValue(key))
            }

        }
        return Pair(msg,foundSecrets)
    }

    private fun convertXmlToMessageRec(mapper: KeyValueMapper, root: Element, keyProvider: KeyProvider) {
        val isEncrypted = root.getAttribute("mode") != "plain"
        for (i in 0..root.childNodes.length) {
            if (root.childNodes.item(i) !is Element) continue
            val ele = root.childNodes.item(i) as Element
            val id = ele.getAttribute("id")
            if (ele.tagName == "single") {
                var value=ele.childNodes.item(0).nodeValue;
                if(isEncrypted) {
                    val decoded = Base64.getDecoder().decode(value)
                    val decryptId=getEncryptDecryptId(root).second
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
                if(!isCryptographyKey(id)){
                    mapper.setValue(id, value)
                }



        }
            else if(ele.tagName=="block"){
                convertXmlToMessageRec(mapper,ele,keyProvider)
            }
    }

}

private fun getBlockSize(): Int {
    return 16
}

private fun getHashSize(hashType: String): Int {
    return hashType.split("-")[1].toInt() / 8
}

private fun convertToMessageRec(
    bytes: ByteArray,
    root: Element,
    offset: Int,
    mapper: KeyValueMapper,
    keyProvider: KeyProvider
) {
    var currOffset = offset
    var result = ByteArray(0)
    var formatString = ""
    if (root.hasAttribute("typePrefix")) {
        formatString = root.getAttribute("typePrefix");
    }
    for (i in 0..root.childNodes.length) {
        if (!(root.childNodes.item(i) is Element)) continue
        val ele = root.childNodes.item(i) as Element
        if (ele.tagName == "single") {
            formatString += ele.getAttribute("type")
        }
    }
    val objects = MyStruct.unpack(formatString, bytes, offset)
    var counter = 0
    for (i in 0..root.childNodes.length) {
        if (!(root.childNodes.item(i) is Element)) continue
        val ele = root.childNodes.item(i) as Element
        if (ele.tagName == "single") {
            mapper.setValue(ele.getAttribute("id"), objects[counter])
            counter += 1
        }

    }
    currOffset = offset + MyStruct.getDataSize(formatString)
    if (root.getElementsByTagName("block").length > 0) {
        val ele = root.getElementsByTagName("block").item(0) as Element
        val iv = ByteArray(getBlockSize())

        val sizeType = ele.getAttribute("sizeType")
        if (ele.getAttribute("mode") != "plain") {
            bytes.copyInto(iv, 0, currOffset, currOffset + iv.size)
            currOffset += iv.size
        }
        val size = MyStruct.castPrimitive(MyStruct.unpack(sizeType, bytes, currOffset).get(0), Int.javaClass) as Int
        var blockBytes = ByteArray(size)
        currOffset += MyStruct.getDataSize(sizeType)
        bytes.copyInto(blockBytes, 0, currOffset, currOffset + size)
        currOffset += size
        if (root.hasAttribute("hashType")) {
            val hashBytes = ByteArray(getHashSize(root.getAttribute("hashType")))
            bytes.copyInto(hashBytes, 0, currOffset, currOffset + hashBytes.size)
            val processedBytesCount = currOffset - offset
            val compareBytes = ByteArray(processedBytesCount)
            bytes.copyInto(compareBytes, 0, offset, offset + processedBytesCount)
            val hashed = hash(compareBytes, root.getAttribute("hashType"))
            if (!Arrays.equals(hashBytes, hashed)) {
                throw IllegalStateException("Hash invalid")
            }
            //currOffset+=hashBytes.size

        }

        1
        if (ele.getAttribute("mode") != "plain") {
            val encryptedData = EncryptedData(IvParameterSpec(iv), blockBytes)
            val decryptId = getEncryptDecryptId(ele).second
            blockBytes = decrypt(encryptedData, keyProvider, ele.getAttribute("mode"), decryptId)
            blockBytes.copyInto(bytes, currOffset - blockBytes.size, 0, blockBytes.size)
            currOffset -= blockBytes.size
        }
        convertToMessageRec(bytes, ele, currOffset, mapper, keyProvider)


    }
}

fun generateIv(): IvParameterSpec {
    val iv = ByteArray(16)
    //SecureRandom().nextBytes(iv)
    return IvParameterSpec(iv)
}

private class EncryptedData(public val IV: IvParameterSpec, public val data: ByteArray) {

}

private fun encrypt(bytes: ByteArray, keyProvider: KeyProvider, mode: String, keyId: String): EncryptedData {
    val cipher = Cipher.getInstance(mode)
    val iv = generateIv()
    keyProvider.initCipher(cipher, Cipher.ENCRYPT_MODE, keyId, iv)
    var buffer=bytes
    if(bytes.size % 16!=0){
        buffer=Arrays.copyOf(bytes,bytes.size+16-bytes.size%16)
    }
    val encrypted = cipher.doFinal(buffer)
    return EncryptedData(iv, encrypted)
}
    private fun pad(array: ByteArray):ByteArray{
        val blockSize=16
        val pad=blockSize-(array.size%blockSize)
         return Arrays.copyOf(array,array.size+pad)
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
        return Pair(ele.getAttribute("encryptKeyId"), ele.getAttribute("decryptKeyId"));
    } else if (ele.hasAttribute("encryptKeyId")) {
        return Pair(ele.getAttribute("encryptKeyId"), ele.getAttribute("encryptKeyId"));
    } else if (ele.hasAttribute("decryptKeyId")) {
        return Pair(ele.getAttribute("decryptKeyId"), ele.getAttribute("decryptKeyId"));
    } else {
        throw IllegalStateException("Both encryption and decryption key are missing")
    }
}

private fun convertToBytesRec(mapper: KeyValueMapper, keyProvider: KeyProvider, root: Element): ByteArray {
    val objectList = mutableListOf<Any>()
    var result = ByteArray(0)
    var formatString = ""
    if (root.hasAttribute("typePrefix")) {
        formatString = root.getAttribute("typePrefix");
    }
    for (i in 0..root.childNodes.length) {
        if (!(root.childNodes.item(i) is Element)) continue
        val ele = root.childNodes.item(i) as Element
        if (ele.tagName == "single") {
            formatString += ele.getAttribute("type")
            objectList.add(mapper.getValue(ele.getAttribute("id")))
        }

    }
    if (formatString != "") {
        result += MyStruct.pack(formatString, *objectList.toTypedArray())
    }
    if (root.getElementsByTagName("block").length > 0) {
        val ele = root.getElementsByTagName("block").item(0) as Element
        var blockBytes = convertToBytesRec(mapper, keyProvider, ele)
        val sizeType = ele.getAttribute("sizeType")
        if (ele.getAttribute("mode") != "plain") {
            val encryptId = getEncryptDecryptId(ele).first
            val encryptedData = encrypt(blockBytes, keyProvider, ele.getAttribute("mode"), encryptId)
            blockBytes = encryptedData.data
            result += encryptedData.IV.iv
        }
        result += MyStruct.pack(sizeType, blockBytes.size)
        result += blockBytes
        if (root.hasAttribute("hashType")) {
            result += hash(result, root.getAttribute("hashType"));
        }

    }

    return result
}


}