package com.example.letmeknow

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.letmeknow.messages.BaseMessage
import com.example.letmeknow.messages.SignUpMessage
import com.example.letmeknow.messages.SignUpResponseMessage
import com.example.letmeknow.messages.UserMessage
import com.example.letmeknow.util.Encoder
import com.example.letmeknow.util.KeyProvider
import com.example.letmeknow.util.KeyValueMapper
import com.example.letmeknow.util.SSHKeyConverter
import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.security.Key
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.spec.KeySpec
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
public class ExampleInstrumentedTest {
    open class StubKeyProvider:KeyProvider{
        override fun getKey(keyId: String): Key {
            return SecretKeySpec(arrayOf<Byte>(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15).toByteArray(),"AES")
        }

    }
    class ExtendedStubKeyProvider:StubKeyProvider(){
        private val publicKey=InstrumentationRegistry.getInstrumentation().targetContext.assets.open("id_rsa.pub").use{SSHKeyConverter().parsePublicKey(it)};

        private var password="123"
        override fun getKey(keyId: String): Key {
            if(keyId=="SERVER_PUBLIC"){
               return publicKey
            }
            else if(keyId=="SERVER_PRIVATE"){
                throw SecurityException("Cannot use server private key")
            }
            else{
                val pw=password.toByteArray()
                return SecretKeySpec(Arrays.copyOf(pw,16),"AES")
            }
        }
        public fun setPassword(password:String){
            this.password=password
        }

        override fun initCipher(cipher: Cipher, encryptMode: Int, keyId: String, iv: IvParameterSpec) {
          if(keyId=="SERVER_PUBLIC" || keyId=="SERVER_PRIVATE"){
              cipher.init(encryptMode,getKey(keyId))
          }
            else{
                super.initCipher(cipher, encryptMode, keyId, iv)
          }

        }

    }
    public   class StubServer(capacity:Int){
        public val queue:BlockingQueue<ByteArray> = ArrayBlockingQueue(capacity)
        public  fun receive():ByteArray{
            return queue.poll(5,TimeUnit.SECONDS)
        }
        public fun send(array:ByteArray){
            otherSide!!.queue.offer(array)
        }
        public var otherSide:StubServer?=null
    }
    @Test
    fun testSignUp(){
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val url=URL("http://37.221.197.246:1997/")
        val client= url.openConnection() as HttpURLConnection
        client.doOutput=true
        client.setRequestProperty("Testing","true")
        client.setRequestProperty("Accept-Encoding", "identity");

        val output=client.getOutputStream()
        val userID=UUID(1,1)
        val stubKeyProvider=ExtendedStubKeyProvider()
        val signUpSent=SignUpMessage("compf","test123")
        val encoder=Encoder()
        val signUpXml= encoder.convertToXml(signUpSent,appContext,"SignUpMessage",stubKeyProvider)
        //client.setFixedLengthStreamingMode(signUpXml.length)
        output.write(ByteBuffer.allocate(4).putInt(signUpXml.length).array())
        output.write(signUpXml.toString().encodeToByteArray())
        output.flush()
        val input=client.getInputStream()
        val lengthBuffer=ByteArray(4)
       input.read(lengthBuffer)
        val length=ByteBuffer.wrap(lengthBuffer).getInt()
        val responseBuffer=ByteArray(length)
        input.read(responseBuffer)
        val signUpResponseXml=responseBuffer.decodeToString()
        val responseMessage=encoder.convertXmlToMessage(signUpResponseXml,appContext ,"SignUpMessageResponse",stubKeyProvider)
        val signUpResponseReceived=responseMessage.first as SignUpResponseMessage
        assertEquals(signUpResponseReceived.userId,userID)
        assertFalse(signUpResponseReceived.userId===userID)
    }
    @Test
    fun testMessageConversion() {

        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val encoder=Encoder()
        val msg=UserMessage(UUID(1,1),UUID(10,18),2)
        msg.time=5112
        val keyProvider=StubKeyProvider()
        val xml=encoder.convertToXml(msg,appContext,"UserMessage",keyProvider)

        val convMessage=encoder.convertXmlToMessage(xml,appContext,"UserMessage",keyProvider).first as UserMessage
        assertEquals(convMessage.from,msg.from)
        assertEquals(convMessage.to,msg.to)
        assertEquals(convMessage.time,msg.time)
        assertEquals((convMessage as UserMessage).msgId,(msg as UserMessage).msgId)
        assertNotEquals(convMessage,msg)


    }
}