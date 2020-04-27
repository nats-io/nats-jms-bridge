package io.nats.bridge.mock;

import javax.jms.JMSException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class JMSBinaryMessage extends JMSMessage implements javax.jms.BytesMessage {

    private final byte[] bytes;
    private final ByteBuffer byteBuffer;

    public JMSBinaryMessage(byte[] body) {
        super(body);
        bytes = body;
        byteBuffer = ByteBuffer.wrap(bytes);
    }

    public JMSBinaryMessage(String body) {
        super(body);
        bytes = body.getBytes(StandardCharsets.UTF_8);
        byteBuffer = ByteBuffer.wrap(bytes);
    }

    @Override
    public long getBodyLength() throws JMSException {
        return bytes.length;
    }

    @Override
    public boolean readBoolean() throws JMSException {
        return byteBuffer.get() != 0;
    }

    @Override
    public byte readByte() throws JMSException {
        return byteBuffer.get();
    }

    @Override
    public int readUnsignedByte() throws JMSException {
        return byteBuffer.getChar();
    }

    @Override
    public short readShort() throws JMSException {
        return byteBuffer.getShort();
    }

    @Override
    public int readUnsignedShort() throws JMSException {
        return byteBuffer.getShort();
    }

    @Override
    public char readChar() throws JMSException {
        return byteBuffer.getChar();
    }

    @Override
    public int readInt() throws JMSException {
        return byteBuffer.getInt();
    }

    @Override
    public long readLong() throws JMSException {
        return byteBuffer.getLong();
    }

    @Override
    public float readFloat() throws JMSException {
        return byteBuffer.getFloat();
    }

    @Override
    public double readDouble() throws JMSException {
        return byteBuffer.getDouble();
    }

    @Override
    public String readUTF() throws JMSException {

        final int length = byteBuffer.getInt();
        byte[] utfBytes = new byte[length];
        byteBuffer.get(utfBytes);
        return new String(utfBytes, StandardCharsets.UTF_8);
    }

    @Override
    public int readBytes(byte[] value) throws JMSException {
        byteBuffer.get(value); //ok this is does not really work
        return value.length;
    }

    @Override
    public int readBytes(byte[] value, int length) throws JMSException {
        byteBuffer.get(bytes, 0, length);
        return value.length;
    }

    @Override
    public void writeBoolean(boolean value) throws JMSException {

        throw new UnsupportedOperationException();
    }

    @Override
    public void writeByte(byte value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeShort(short value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeChar(char value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeInt(int value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeLong(long value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeFloat(float value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeDouble(double value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeUTF(String value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBytes(byte[] value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBytes(byte[] value, int offset, int length) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeObject(Object value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() throws JMSException {
        throw new UnsupportedOperationException();
    }
}
