package br.gold;

import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.tag.CompoundTag;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

public final class NbtTest {
    private static void rejeita(byte[] bytes) throws Exception {
        try {
            NBTIO.read(bytes);
            throw new AssertionError("NBT malformado foi aceito.");
        } catch (IOException esperado) {
            // A rejeicao deve acontecer antes de alocar o tamanho informado.
        }
    }

    public static void main(String[] args) throws Exception {
        CompoundTag valido = new CompoundTag().putString("nome", "Proteção").putInt("valor", 42);
        for (ByteOrder ordem : new ByteOrder[]{ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN}) {
            if (!valido.equals(NBTIO.read(NBTIO.write(valido, ordem), ordem))) {
                throw new AssertionError("NBT valido nao preservado.");
            }
        }
        rejeita(new byte[]{10, 0, 0, 7, 0, 0, 0x7f, -1, -1, -1});
        rejeita(new byte[]{10, 0, 0, 11, 0, 0, -1, -1, -1, -1});
        rejeita(new byte[]{10, 0, 0, 9, 0, 0, 0, 0, 0, 0, 1});
        rejeita(new byte[]{10, 0, 0, 99, 0, 0});
        rejeita(new byte[]{10, 0, 0, 8, 0, 0, 0, 5, 65});
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        for (int i = 0; i < 100; i++) { out.writeByte(10); out.writeShort(0); }
        for (int i = 0; i < 100; i++) { out.writeByte(0); }
        rejeita(bytes.toByteArray());
        try (NBTInputStream in = new NBTInputStream(new ByteArrayInputStream(new byte[]{0, (byte) 0x80}), ByteOrder.LITTLE_ENDIAN)) {
            if (in.readUnsignedShort() != 32768) { throw new AssertionError("Unsigned short little-endian incorreto."); }
        }
        System.out.println("NbtTest: formatos validos, profundidade, tipos e tamanhos malformados verificados.");
    }
}
