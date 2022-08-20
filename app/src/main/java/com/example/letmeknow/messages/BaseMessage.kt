package com.example.letmeknow.messages

import com.example.letmeknow.util.MyStruct
import java.util.*
import kotlin.collections.ArrayList

public  abstract class BaseMessage(public var from:Int=0 , public var to: Int=0) {
    public var time:Long=Calendar.getInstance().timeInMillis;
   public abstract fun getFields():Array<Any>;
    public abstract fun applyFields(objects:Array<Any>);
    public abstract   fun getFormatString():String;
    companion object{
        val BaseMessageFormatString=">ihiiq";
    }

    public fun serialize():ByteArray{
        var formatString=BaseMessageFormatString;
         var objects= ArrayList<Any>();
        formatString+=getFormatString()
        objects.add(MyStruct.getDataSize(formatString))
        objects.add(MessageClassManager.getId(this::class));
        objects.add(from)
        objects.add(to)
        objects.add(time)
        var addObjects=getFields();

        objects.addAll(addObjects)


        return MyStruct.pack(formatString,*objects.toArray())

    }

    /**
     * Deserializes the byte array to the instance, the byte array should not contain the total length
     * @param the byte array given that should be unpacked
     */

    public fun deserialize(bytes:ByteArray){
        val objects=MyStruct.unpack(BaseMessageFormatString+getFormatString(),bytes)
        from=objects[0] as Int
        to=objects[1] as Int
        time=objects[2] as Long
        val otherFieldsObjects=objects.drop(BaseMessageFormatString.length-1).toTypedArray()
        applyFields(otherFieldsObjects)
    }

}