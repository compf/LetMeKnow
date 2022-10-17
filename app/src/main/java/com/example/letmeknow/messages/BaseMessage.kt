package com.example.letmeknow.messages

import com.example.letmeknow.util.KeyProvider
import com.example.letmeknow.util.KeyValueMapper
import com.example.letmeknow.util.MyStruct
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.milliseconds

public  abstract class BaseMessage (public var from:UUID=UUID.randomUUID() , public var to: UUID=UUID.randomUUID()):KeyValueMapper {
    public var time:Long=Calendar.getInstance().timeInMillis;
   public abstract fun getFields():Array<Any>;
    public abstract fun applyFields(objects:Array<Any>);
    public abstract   fun getFormatString():String;
    companion object{
        val BaseMessageFormatString=">IHqqqqq";
    }

    public fun serialize():ByteArray{
        var formatString=BaseMessageFormatString;
         var objects= ArrayList<Any>();
        formatString+=getFormatString()
        objects.add(MyStruct.getDataSize(formatString))
        objects.add(MessageClassManager.getId(this::class));
        objects.add(from.mostSignificantBits)
        objects.add(from.leastSignificantBits)
        objects.add(to.mostSignificantBits)
        objects.add(to.leastSignificantBits)
        objects.add(time)
        var addObjects=getFields();

        objects.addAll(addObjects)


        return MyStruct.pack(formatString,*objects.toArray())

    }

    override fun getValue(name: String): Any {
       return when(name){
           "messageType"->MessageClassManager.getId(this::class)
           "fromH"->from.mostSignificantBits
           "fromL"->from.leastSignificantBits
           "toH"->to.mostSignificantBits
           "toL"->to.leastSignificantBits
           "time"->time
           else->0
       }

    }

    override fun getKeys(): Set<String> {
        return setOf("messageType","fromH","fromL","toH","toL","time")
    }

    override fun setValue(name: String,value:Any): Unit {
         when(name){
            "messageType"->{}
            "fromH"->from=UUID(value as Long,from.leastSignificantBits)
            "fromL"->from=UUID(from.mostSignificantBits,value as Long)
             "toH"->to=UUID(value as Long,to.leastSignificantBits)
             "toL"->to=UUID(to.mostSignificantBits,value as Long)
            "time"->time=value as Long
        }

    }

    /**
     * Deserializes the byte array to the instance, the byte array should not contain the total length
     * @param the byte array given that should be unpacked
     */

    public fun deserialize(bytes:ByteArray){
        val objects=MyStruct.unpack(BaseMessageFormatString+getFormatString(),bytes)
        var higherBits=objects[0] as Long
        var lowerBits=objects[1] as Long
        from=UUID(higherBits,lowerBits)
        higherBits=objects[2] as Long
        lowerBits=objects[3] as Long
        to=UUID(higherBits,lowerBits)
        time=objects[4] as Long
        val otherFieldsObjects=objects.drop(BaseMessageFormatString.length-1).toTypedArray()
        applyFields(otherFieldsObjects)
    }

}