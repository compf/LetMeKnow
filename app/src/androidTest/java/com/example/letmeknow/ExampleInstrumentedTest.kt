package com.example.letmeknow

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.letmeknow.messages.SignUpMessage
import com.example.letmeknow.messages.SignUpResponseMessage
import com.example.letmeknow.messages.UserMessage
import com.example.letmeknow.util.ClientKeyProvider
import com.example.letmeknow.util.Encoder
import com.example.letmeknow.util.KeyProvider
import com.example.letmeknow.util.SSHKeyConverter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.security.Key
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import javax.crypto.spec.SecretKeySpec

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    open class StubKeyProvider : KeyProvider {
        override fun getKey(keyId: String): Key {
            return SecretKeySpec(
                arrayOf<Byte>(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15).toByteArray(),
                "AES"
            )
        }

    }

    class StubServer(capacity: Int) {
        val queue: BlockingQueue<ByteArray> = ArrayBlockingQueue(capacity)
        fun receive(): ByteArray {
            return queue.poll(5, TimeUnit.SECONDS)
        }

        fun send(array: ByteArray) {
            otherSide!!.queue.offer(array)
        }

        var otherSide: StubServer? = null
    }

    @Test
    fun testSignUp() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val url = URL("http://37.221.197.246:1998/")
        val client = url.openConnection() as HttpURLConnection
        client.doOutput = true
        client.setRequestProperty("Testing", "true")
        client.setRequestProperty("Accept-Encoding", "identity")

        val output = client.outputStream
        val userID = UUID(1, 1)
        val publicKey = appContext.assets.open("id_rsa.pub").use { SSHKeyConverter().parsePublicKey(it) }
        val stubKeyProvider = ClientKeyProvider(publicKey)
        val signUpSent = SignUpMessage("compf".padEnd(16, 0.toChar()), "test123".padEnd(16, 0.toChar()))
        val encoder = Encoder()
        val signUpXml = encoder.convertToXml(signUpSent, appContext, "SignUpMessage", stubKeyProvider)

        output.write(ByteBuffer.allocate(4).putInt(signUpXml.length).array())
        output.write(signUpXml.toString().encodeToByteArray())
        output.flush()
        val input = client.inputStream
        val lengthBuffer = ByteArray(4)
        input.read(lengthBuffer)
        val length = ByteBuffer.wrap(lengthBuffer).int
        val responseBuffer = ByteArray(length)
        input.read(responseBuffer)
        val signUpResponseXml = responseBuffer.decodeToString()
        val responseMessage = encoder.convertXmlToMessage(signUpResponseXml, stubKeyProvider)
        val signUpResponseReceived = responseMessage.first as SignUpResponseMessage

        assertEquals(signUpResponseReceived.userId, userID)
        assertFalse(signUpResponseReceived.userId === userID)
    }

    @Test
    fun testMessageConversion() {

        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val encoder = Encoder()
        val msg = UserMessage(UUID(1, 1), UUID(10, 18), 2)
        msg.time = 5112
        val keyProvider = StubKeyProvider()
        val xml = encoder.convertToXml(msg, appContext, "UserMessage", keyProvider)

        val convMessage = encoder.convertXmlToMessage(xml, keyProvider).first as UserMessage
        assertEquals(convMessage.from, msg.from)
        assertEquals(convMessage.to, msg.to)
        assertEquals(convMessage.time, msg.time)
        assertEquals(convMessage.msgId, msg.msgId)
        assertNotEquals(convMessage, msg)


    }
}