package cn.nukkit.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;
import java.util.zip.InflaterInputStream;


public abstract class Zlib {

    public static byte[] deflate(byte[] data) throws Exception {
        return deflate(data, Deflater.DEFAULT_COMPRESSION);
    }

    public static byte[] deflate(byte[] data, int level) throws Exception {
        Deflater deflater = new Deflater(level);
        deflater.reset();
        deflater.setInput(data);
        deflater.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
        byte[] buf = new byte[1024];
        try {
            while (!deflater.finished()) {
                int i = deflater.deflate(buf);
                bos.write(buf, 0, i);
            }
        } finally {
            deflater.end();
        }
        return bos.toByteArray();
    }

    public static byte[] inflate(InputStream stream) throws IOException {
        return inflate(stream, 64 * 1024 * 1024);
    }

    public static byte[] inflate(byte[] data) throws IOException {
        return inflate(new ByteArrayInputStream(data));
    }

    public static byte[] inflate(byte[] data, int maxSize) throws IOException {
        return inflate(new ByteArrayInputStream(data), maxSize);
    }

    private static byte[] inflate(InputStream stream, int maxSize) throws IOException {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("O limite de descompressao deve ser positivo.");
        }
        try (InflaterInputStream input = new InflaterInputStream(stream);
             ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxSize, 8192))) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) {
                if (length > maxSize - output.size()) {
                    throw new IOException("Dados descomprimidos excedem o limite permitido.");
                }
                output.write(buffer, 0, length);
            }
            return output.toByteArray();
        }
    }

}

