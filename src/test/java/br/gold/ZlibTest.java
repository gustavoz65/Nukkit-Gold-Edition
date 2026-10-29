package br.gold;

import cn.nukkit.utils.Zlib;
import java.io.IOException;
import java.util.Arrays;

/** Regressao para o limite real de memoria durante a descompressao. */
public final class ZlibTest {
    public static void main(String[] args) throws Exception {
        byte[] original = new byte[1024 * 1024];
        Arrays.fill(original, (byte) 65);
        byte[] comprimido = Zlib.deflate(original);
        if (!Arrays.equals(original, Zlib.inflate(comprimido, original.length))) {
            throw new AssertionError("Descompressao valida nao preservou o conteudo.");
        }
        try {
            Zlib.inflate(comprimido, original.length - 1);
            throw new AssertionError("Aceitou saida maior que o limite.");
        } catch (IOException esperado) {
            // A carga e pequena na rede, mas expande alem do limite em memoria.
        }
        try {
            Zlib.inflate(Arrays.copyOf(comprimido, comprimido.length / 2), original.length);
            throw new AssertionError("Aceitou um fluxo comprimido truncado.");
        } catch (IOException esperado) {
            // A entrada incompleta deve ser rejeitada.
        }
        System.out.println("ZlibTest: limites e dados truncados verificados.");
    }
}
