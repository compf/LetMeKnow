package com.example.letmeknow

import android.widget.Toast
import com.example.letmeknow.messages.AcknowledgementMessage
import com.example.letmeknow.messages.BaseMessage
import com.example.letmeknow.messages.MessageClassManager
import com.example.letmeknow.util.Encoder
import com.example.letmeknow.util.MyStruct
import org.jetbrains.annotations.NotNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.reflect.KFunction2

class MessageHandler(val success: (msg:BaseMessage)->Unit, val failure: (msg:BaseMessage)->Unit): Runnable {
    private var socket:DatagramSocket= DatagramSocket()
    private var thread:Thread=Thread(this)
    private var shallContinue=true
    private val messageQueue=LinkedBlockingQueue<BaseMessage>()
    private val HOST_IP="37.221.197.246"
    private val PORT=1997
    public var Sucess=false
    //datagramSocket.send(DatagramPacket(data,data.size,InetSocketAddress(",1997)))

    fun start(){
        thread.start()
    }
    fun offerMessage(msg:BaseMessage){
        messageQueue.offer(msg)
    }
    override fun run() {

        while(shallContinue){
            val msg=messageQueue.poll(5,TimeUnit.SECONDS)
            if(msg==null){
                continue;
            }
            var successfullySent=false
            while(!successfullySent){
                //sendMessage(msg)
                successfullySent= waitReply(msg)
            }
            Sucess=true



        }
    }
    /*private fun sendMessage(@NotNull msg:BaseMessage){
        val data=Encoder().convertToBytes(msg,)
        val datagramm=DatagramPacket(data,data.size,InetSocketAddress(HOST_IP,PORT))
        socket.send(datagramm)
    }*/
    private fun receiveMessage():BaseMessage?{
        var buffer_size=4096

        var buffer=ByteArray(buffer_size)
        var packet=DatagramPacket(buffer,buffer.size)
        socket.soTimeout=500
        try {
            socket.receive(packet)
            var bitConverter=ByteBuffer.wrap(packet.data)
            buffer_size=bitConverter.int
            val id=bitConverter.short

            return MessageClassManager.newInstance(id)
        }catch (ex: SocketTimeoutException){
            return null;
        }

    }
    private fun waitReply(@NotNull sentMessage:BaseMessage):Boolean{
        var shallContinue=true
        var counter=0
        val MaxRepeatCount=3
        while(shallContinue && counter<MaxRepeatCount){
            val msg=receiveMessage()
            if(msg==null){
                counter++
            }
            else if(msg is AcknowledgementMessage ){
               shallContinue=false
            }
            else{
                counter++
            }
        }
        if(shallContinue){
            this.failure(sentMessage);
        }
        else{
            this.success(sentMessage);
        }

        return !shallContinue // True if ack received

    }
    fun stop(){
        shallContinue=false;
    }


}