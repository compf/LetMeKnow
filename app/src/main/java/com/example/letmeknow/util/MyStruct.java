package com.example.letmeknow.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

import java.util.ArrayList;
import java.util.List;

public class MyStruct {
    private static class FormatString {
        private static final String ENDIAN_PREFIX_REGEX = "^[!@<>=]";
        private static final char DEFAULT_ENDIAN_PREFIX = '@';
        private static final String FORMAT_CHARACTER_REGEX = "(\\d*[cbBhHiIlLqQfds])";
        private boolean isLittleEndian = true;
        private boolean isNative;


        public boolean isLittleEndian() {

            return isLittleEndian;
        }

        public boolean isNative() {
            return isNative;
        }

        public final List<FormatStringPart> parts = new ArrayList<>();

        private Class<?> getTypeRep(char formatChar) {
            switch (formatChar) {
                case 'c':
                    return char.class;
                case 'b':
                case 'B':
                    return Byte.class;
                case '?':
                    return boolean.class;
                case 'h':
                case 'H':
                    return Short.class;
                case 'i':

                case 'I':
                    return Integer.class;
                case 'l':
                case 'L':
                    return isNative() ? Long.class : Integer.class;
                case 'q':

                case 'Q':
                    return Long.class;
                case 's':
                    return String.class;
                case 'f':
                    return Float.class;
                case 'd':
                    return Double.class;
                default:
                    throw new IllegalArgumentException("Format char not valid");

            }
        }

        public static FormatString parse(String frmString) {
            FormatString formatString = new FormatString();
            char prefix;
            if (frmString == null || frmString.length() == 0) {
                throw new IllegalArgumentException("Empty string not valid");
            }
            if (frmString.length() == 1) {
                prefix = DEFAULT_ENDIAN_PREFIX;
            } else {
                prefix = frmString.charAt(0);
                if (ENDIAN_PREFIX_REGEX.contains(prefix + "")) {
                    // there is a prefix, cut if off and deal with the rest of the string
                    frmString = frmString.substring(1);
                } else {
                    // There is no endianess prefix, set it to default
                    prefix = DEFAULT_ENDIAN_PREFIX;
                }
            }
            formatString.parseEndianness(prefix);
            Pattern pattern = Pattern.compile(FORMAT_CHARACTER_REGEX);
            Matcher matcher = pattern.matcher(frmString);

            if (matcher.groupCount() == 0) {
                throw new IllegalArgumentException("Not a valid format string");
            }
            while (matcher.find()) {


                String m = matcher.group();

                char formatCharacter = m.charAt(m.length() - 1);
                int length = 1;
                if (m.length() > 1) {
                    length = Integer.parseInt(m.substring(0, m.length() - 1));
                }

                addFormatStringPart(formatString, formatCharacter, length);


            }
            return formatString;

        }

        public int getSize() {
            int sum = 0;
            for (FormatStringPart p : parts) {

                sum += p.getSize();
            }
            return sum;

        }

        private static void addFormatStringPart(FormatString frmString, char formatCharacter, int length) {
            if (formatCharacter == 's') {
                frmString.parts.add(new LengthPrefixFormatStringPart(length));
            } else {
                for (int i = 0; i < length; i++) {
                    frmString.parts.add(new FormatStringPart(frmString.getTypeRep(formatCharacter)));
                }
            }
        }


        private void parseEndianness(char endianness) {

            switch (endianness) {
                case '@':
                    isNative = true;
                    isLittleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
                    return;
                case '=':
                    isLittleEndian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
                    return;
                case '<':
                    isLittleEndian = true;
                    return;
                case '>':
                case '!':
                    isLittleEndian = false;
                    return;

            }
            throw new IllegalArgumentException("Not a valid endianness");
        }
    }

    private static class FormatStringPart {
        private final Class<?> dataType;
        private static final int SHORT_BYTES=2;
        private static final int INT_BYTES=4;
        private static final int LONG_BYTES=8;
        private static final int FLOAT_BYTES=INT_BYTES;
        private static final int DOUBLE_BYTES=LONG_BYTES;
        public FormatStringPart(Class<?> type) {
            dataType = type;
        }

        public Class<?> getDataType() {
            return dataType;
        }

        public byte[] serialize(Object obj) {
            if (obj.getClass() != getDataType()) {
                obj = getDataType().cast(obj);
            }
            if (obj instanceof Byte) {
                return new byte[]{(byte) obj};
            } else if (obj instanceof Boolean) {
                boolean bo = (boolean) obj;
                return new byte[]{bo ? (byte) 1 : (byte) 0};
            } else if (obj instanceof Short) {
                ByteBuffer buffer = ByteBuffer.allocate(SHORT_BYTES);
                buffer.putShort((short) obj);
                return  buffer.array();
            } else if (obj instanceof Integer) {
                ByteBuffer buffer = ByteBuffer.allocate(INT_BYTES);
                buffer.putInt((int) obj);
                return  buffer.array();
            } else if (obj instanceof Long) {
                ByteBuffer buffer = ByteBuffer.allocate(LONG_BYTES);
                buffer.putLong((long) obj);
                return buffer.array();
            } else if (obj instanceof Float ) {
                ByteBuffer buffer = ByteBuffer.allocate(FLOAT_BYTES);
                buffer.putFloat((float) obj);
                return  buffer.array();
            } else if (obj instanceof Double) {
                ByteBuffer buffer = ByteBuffer.allocate(DOUBLE_BYTES);
                buffer.putDouble((double) obj);
                return  buffer.array();
            } else if (obj instanceof Character) {
                return new byte[]{(byte) (char) obj};
            }
            return null;

        }

        public int getSize() {

            if (getDataType() == Byte.class || getDataType() == boolean.class || getDataType() == char.class) {
                return 1;
            } else if (getDataType() == Short.class) {
                return 2;
            } else if (getDataType() == Integer.class || getDataType() == Float.class) {
                return 4;
            } else if (getDataType() == Long.class || getDataType() == Double.class) {
                return 8;
            }
            return 0;

        }

        public Object deserialize(byte[] array, int offset) {
            if (getDataType() == Byte.class) {
                return array[offset];
            } else if (getDataType() == boolean.class) {
                return array[offset] == 1;
            } else if (getDataType() == Short.class) {
                ByteBuffer buffer = ByteBuffer.wrap(array, offset, SHORT_BYTES);
                return buffer.getShort();
            } else if (getDataType() == Integer.class) {
                ByteBuffer buffer = ByteBuffer.wrap(array, offset, INT_BYTES);
                return buffer.getInt();
            } else if (getDataType() == Long.class)
            {
                ByteBuffer buffer=ByteBuffer.wrap(array,offset,LONG_BYTES);
                return buffer.getLong();
            }
               else if (getDataType()==Float.class)
            {
                ByteBuffer buffer=ByteBuffer.wrap(array,offset,FLOAT_BYTES);
                return buffer.getFloat();
            }
                else if (getDataType()==Double.class)
            {
                ByteBuffer buffer=ByteBuffer.wrap(array,offset,DOUBLE_BYTES);
                return buffer.getDouble();
            }
                else if (getDataType()==char.class)
            {
                return (char)array[offset];
            }
                else
            {
                throw new IllegalArgumentException("Could not process object");
            }
        }


    }

    private static class LengthPrefixFormatStringPart extends FormatStringPart {
        private final int length;

        public LengthPrefixFormatStringPart(int length) {
            super(String.class);


            this.length = length;

        }

        @Override
        public byte[] serialize(Object obj) {

            return obj.toString().getBytes(StandardCharsets.UTF_8);

        }

        @Override
        public Object deserialize(byte[] array, int offset) {

            return new String(array, offset, getLength(), StandardCharsets.UTF_8);

        }

        public int getLength() {
            return length;
        }
        @Override
        public int getSize() {
            return getLength();
        }

    }
    public static  int getDataSize(String formatString){
        return FormatString.parse(formatString).getSize();
    }
    public static byte[] pack(String frmString,  Object... data) {
        FormatString formatStringParsed = FormatString.parse(frmString);
        byte[] result = new byte[formatStringParsed.getSize()];
        int offset = 0;
        for (int i = 0; i < data.length; i++) {
            FormatStringPart part = formatStringParsed.parts.get(i);

            System.arraycopy(part.serialize(data[i]),0,result,offset,part.getSize());
            offset += part.getSize();
        }

        return result;

    }

    public static Object[] unpack(String frmString, byte[] array) {
        FormatString formatStringParsed = FormatString.parse(frmString);
        Object[] result = new Object[formatStringParsed.parts.size()];
        int offset = 0;
        for (int i = 0; i < formatStringParsed.parts.size(); i++) {
            FormatStringPart part = formatStringParsed.parts.get(i);
            Object obj = part.deserialize(array, offset);
            result[i] = obj;
            offset += part.getSize();
        }

        return result;
    }


}

