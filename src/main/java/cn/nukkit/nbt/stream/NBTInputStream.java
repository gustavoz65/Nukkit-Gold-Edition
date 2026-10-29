package cn.nukkit.nbt.stream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class NBTInputStream implements DataInput, AutoCloseable {
    private final DataInputStream stream;
    private final ByteOrder endianness;
    private final LimitedInputStream limited;
    private int depth;
    private int tags;
    public static final int MAX_DEPTH = 64;
    public static final int MAX_TAGS = 65536;
    public static final int MAX_COLLECTION_ITEMS = 1024 * 1024;

    public NBTInputStream(InputStream stream) {
        this(stream, ByteOrder.BIG_ENDIAN);
    }

    public NBTInputStream(InputStream stream, ByteOrder endianness) {
        this.limited = new LimitedInputStream(stream, 16 * 1024 * 1024);
        this.stream = new DataInputStream(this.limited);
        this.endianness = endianness;
    }

    public ByteOrder getEndianness() {
        return endianness;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        this.stream.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        this.stream.readFully(b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return this.stream.skipBytes(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return this.stream.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return this.stream.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return this.stream.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        short s = this.stream.readShort();
        if (endianness == ByteOrder.LITTLE_ENDIAN) {
            s = Short.reverseBytes(s);
        }
        return s;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        int s = this.stream.readUnsignedShort();
        if (endianness == ByteOrder.LITTLE_ENDIAN) {
            s = Short.toUnsignedInt(Short.reverseBytes((short) s));
        }
        return s;
    }

    @Override
    public char readChar() throws IOException {
        char c = this.stream.readChar();
        if (endianness == ByteOrder.LITTLE_ENDIAN) {
            c = Character.reverseBytes(c);
        }
        return c;
    }

    @Override
    public int readInt() throws IOException {
        int i = this.stream.readInt();
        if (endianness == ByteOrder.LITTLE_ENDIAN) {
            i = Integer.reverseBytes(i);
        }
        return i;
    }

    @Override
    public long readLong() throws IOException {
        long l = this.stream.readLong();
        if (endianness == ByteOrder.LITTLE_ENDIAN) {
            l = Long.reverseBytes(l);
        }
        return l;
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(this.readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(this.readLong());
    }

    @Override
    @Deprecated
    public String readLine() throws IOException {
        return this.stream.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        int length = this.readUnsignedShort();
        this.checkCollectionLength(length, 1);
        byte[] bytes = new byte[length];
        this.stream.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public int available() throws IOException {
        return this.stream.available();
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    public void enterTag() throws IOException {
        if (++this.depth > MAX_DEPTH || ++this.tags > MAX_TAGS) {
            throw new IOException("Estrutura NBT profunda ou extensa demais.");
        }
    }

    public void exitTag() {
        --this.depth;
    }

    public void checkCollectionLength(int length, int bytesPerItem) throws IOException {
        if (length < 0 || length > MAX_COLLECTION_ITEMS || (long) length * bytesPerItem > this.limited.remaining) {
            throw new IOException("Tamanho de colecao NBT invalido ou excessivo.");
        }
    }

    /** O orcamento acompanha os bytes realmente lidos, inclusive apos GZIP. */
    private static final class LimitedInputStream extends FilterInputStream {
        private int remaining;

        private LimitedInputStream(InputStream input, int limit) {
            super(input);
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (this.remaining <= 0) { throw new IOException("Limite de bytes NBT excedido."); }
            int value = this.in.read();
            if (value >= 0) { --this.remaining; }
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (length == 0) { return 0; }
            if (this.remaining <= 0) { throw new IOException("Limite de bytes NBT excedido."); }
            int read = this.in.read(bytes, offset, Math.min(length, this.remaining));
            if (read > 0) { this.remaining -= read; }
            return read;
        }

        @Override
        public long skip(long length) throws IOException {
            long skipped = this.in.skip(Math.min(Math.max(length, 0), this.remaining));
            this.remaining -= (int) skipped;
            return skipped;
        }
    }
}
