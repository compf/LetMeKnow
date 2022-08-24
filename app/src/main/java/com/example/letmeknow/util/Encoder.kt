package com.example.letmeknow.util

import android.content.Context
import com.example.letmeknow.messages.BaseMessage
import com.example.letmeknow.messages.MessageClassManager
import org.json.JSONObject
import org.json.JSONTokener
import org.w3c.dom.Element
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.xml.parsers.DocumentBuilderFactory


class Encoder() {
    public val TRANSFOMRMATION_STRING = "AES/CBC/PKCS5Padding";

    private class TempValueMapper:KeyValueMapper{
        public val map= mutableMapOf<String,Any>()
        override fun getValue(name: String): Any {
            return map.get(name)!!
        }

        override fun setValue(name: String, value: Any) {
            map.set(name,value)
        }
    }
    public fun convertToBytes(msg:BaseMessage, context: Context, className: String,keyProvider: KeyProvider): ByteArray {
      val builderFactory=DocumentBuilderFactory.newInstance()
        val builder=builderFactory.newDocumentBuilder()
        val doc=context.assets.open(className+".xml").use { builder.parse(it) }
        return convertToBytesRec(msg,keyProvider,doc.documentElement)
    }
    public fun convertToMessage(bytes:ByteArray,context:Context,className: String,keyProvider: KeyProvider):BaseMessage{
        val builderFactory=DocumentBuilderFactory.newInstance()
        val builder=builderFactory.newDocumentBuilder()
        val doc=context.assets.open(className+".xml").use { builder.parse(it) }
        val mapper=TempValueMapper()

         convertToMessageRec(bytes,doc.documentElement,0,mapper,keyProvider)
        val msg=MessageClassManager.newInstance(mapper.getValue("messageType") as Short)
        for(key in mapper.map.keys){
            msg.setValue(key,mapper.getValue(key))
        }
        return msg
    }
    private fun getBlockSize():Int{
        return 16
    }
    private fun getHashSize(hashType:String):Int{
        return hashType.split("-")[1].toInt()/8
    }
    private fun convertToMessageRec(bytes:ByteArray,root:Element,offset:Int,mapper:KeyValueMapper,keyProvider:KeyProvider){
        var currOffset=offset
        var result=ByteArray(0)
        var formatString=""
        for(i in 0..root.childNodes.length){
            if(!(root.childNodes.item(i) is  Element))continue
            val ele=root.childNodes.item(i) as Element
            if(ele.tagName=="single"){
                formatString+=ele.getAttribute("type")
            }
        }
        val objects=MyStruct.unpack(formatString,bytes,offset)
        var counter=0
        for(i in 0..root.childNodes.length){
            if(!(root.childNodes.item(i) is  Element))continue
            val ele=root.childNodes.item(i) as Element
            if(ele.tagName=="single"){
                mapper.setValue(ele.getAttribute("id"),objects[counter])
                counter+=1
            }

        }
        currOffset=offset+MyStruct.getDataSize(formatString)
        if(root.getElementsByTagName("block").length>0) {
            val ele = root.getElementsByTagName("block").item(0) as Element
            val iv=ByteArray(getBlockSize())

            val sizeType=ele.getAttribute("sizeType")
            if(ele.getAttribute("mode")!="plain"){
                bytes.copyInto(iv,0,currOffset,currOffset+iv.size)
                currOffset+=iv.size
            }
            val size= MyStruct.castPrimitive( MyStruct.unpack(sizeType,bytes,currOffset).get(0),Int.javaClass) as Int
            var blockBytes=ByteArray(size)
            currOffset+=MyStruct.getDataSize(sizeType)
            bytes.copyInto(blockBytes,0,currOffset,currOffset+size)
            currOffset+=size
            if(root.hasAttribute("hashType")){
                val hashBytes=ByteArray(getHashSize(root.getAttribute("hashType")))
                bytes.copyInto(hashBytes,0,currOffset,currOffset+hashBytes.size)
                val processedBytesCount=currOffset-offset
                val compareBytes=ByteArray(processedBytesCount)
                bytes.copyInto(compareBytes,0,offset,offset+processedBytesCount)
                val hashed=hash(compareBytes,root.getAttribute("hashType"))
                if(!Arrays.equals(hashBytes,hashed)){
                    throw IllegalStateException("Hash invalid")
                }
                //currOffset+=hashBytes.size

            }

1
            if(ele.getAttribute("mode")!="plain"){
                val encryptedData=EncryptedData(IvParameterSpec(iv),blockBytes)
                blockBytes=decrypt(encryptedData,keyProvider,ele.getAttribute("mode"),ele.getAttribute("keyId"),mapper )
                blockBytes.copyInto(bytes,currOffset-blockBytes.size,0,blockBytes.size)
                currOffset-=blockBytes.size
            }
            convertToMessageRec(bytes,ele,currOffset,mapper,keyProvider)


        }
    }
    fun generateIv(): IvParameterSpec {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        return IvParameterSpec(iv)
    }
    private class EncryptedData(public val IV: IvParameterSpec,public val data: ByteArray){

    }
    private fun encrypt(bytes:ByteArray,keyProvider:KeyProvider,mode:String,keyId:String,mapper: KeyValueMapper):EncryptedData{
        val cipher = Cipher.getInstance(mode)
        val key=keyProvider.getKey(keyId,mapper )
        val iv=generateIv()
        cipher.init(Cipher.ENCRYPT_MODE, key,iv)
        val encrypted = cipher.doFinal(bytes)
        return EncryptedData(iv,encrypted)
    }
    private  fun decrypt(encryptedData: EncryptedData,keyProvider: KeyProvider,mode:String,keyId:String,mapper: KeyValueMapper):ByteArray{
        val cipher = Cipher.getInstance(mode)
        val key=keyProvider.getKey(keyId,mapper )
        val iv=encryptedData.IV
        cipher.init(Cipher.DECRYPT_MODE,key,iv)
        return cipher.doFinal(encryptedData.data)
    }
    private fun hash(bytes:ByteArray,hashType:String):ByteArray{
        return MessageDigest.getInstance(hashType).digest(bytes)
    }
    private fun convertToBytesRec(mapper:KeyValueMapper,keyProvider:KeyProvider,root:Element):ByteArray{
        val objectList= mutableListOf<Any>()
        var result=ByteArray(0)
        var formatString=""
        for(i in 0..root.childNodes.length){
            if(!(root.childNodes.item(i) is  Element))continue
            val ele=root.childNodes.item(i) as Element
            if(ele.tagName=="single"){
                formatString+=ele.getAttribute("type")
                objectList.add(mapper.getValue(ele.getAttribute("id")))
            }

        }
        if(formatString!=""){
            result+=MyStruct.pack(formatString,*objectList.toTypedArray())
        }
        if(root.getElementsByTagName("block").length>0) {
            val ele = root.getElementsByTagName("block").item(0) as Element
            var blockBytes = convertToBytesRec(mapper, keyProvider, ele)
            val sizeType=ele.getAttribute("sizeType")
            if(ele.getAttribute("mode")!="plain"){
                val encryptedData=encrypt(blockBytes,keyProvider,ele.getAttribute("mode"),ele.getAttribute("keyId"),mapper)
                blockBytes=encryptedData.data
                result+=encryptedData.IV.iv
            }
            result+=MyStruct.pack(sizeType,blockBytes.size)
            result+=blockBytes
            if(root.hasAttribute("hashType")){
                result+=hash(result,root.getAttribute("hashType"));
            }

        }

        return result
    }



}