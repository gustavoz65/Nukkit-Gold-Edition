package br.gold;

import cn.nukkit.network.Network;
import cn.nukkit.network.PacketDecoder;
import cn.nukkit.network.protocol.CraftingEventPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.network.protocol.TextPacket;
import cn.nukkit.utils.BinaryStream;
import cn.nukkit.utils.Zlib;
import java.io.IOException;
import java.util.UUID;

public final class PacketTest {
    private interface Acao { void executar() throws Exception; }

    private static void rejeita(Acao acao) throws Exception {
        try {
            acao.executar();
            throw new AssertionError("Pacote malformado foi aceito.");
        } catch (IllegalArgumentException | IOException esperado) {
            // Erros de memoria e de pilha nao contam como rejeicao segura.
        }
    }

    private static cn.nukkit.item.Item slot(int id, int count) {
        BinaryStream stream = new BinaryStream();
        stream.putShort(id);
        stream.putByte((byte) count);
        stream.putShort(0);
        stream.putLShort(0);
        return new BinaryStream(stream.getBuffer()).getSlot();
    }

    public static void main(String[] args) throws Exception {
        Network network = new Network(null);
        TextPacket texto = new TextPacket();
        texto.type = TextPacket.TYPE_RAW;
        texto.message = "Ação e proteção";
        texto.encode();
        TextPacket recebido = (TextPacket) PacketDecoder.decode(texto.getBuffer(), network::getPacket);
        if (!texto.message.equals(recebido.message)) { throw new AssertionError("Texto valido foi alterado."); }
        BinaryStream lote = new BinaryStream();
        lote.putInt(texto.getBuffer().length);
        lote.put(texto.getBuffer());
        if (PacketDecoder.decodeBatch(Zlib.deflate(lote.getBuffer()), network::getPacket).size() != 1) {
            throw new AssertionError("Lote valido foi recusado.");
        }
        rejeita(() -> PacketDecoder.decode(new byte[0], network::getPacket));
        rejeita(() -> PacketDecoder.decode(new byte[]{(byte) 0xfe}, network::getPacket));
        rejeita(() -> PacketDecoder.decode(new byte[]{ProtocolInfo.TEXT_PACKET, 0, 0, 20, 65}, network::getPacket));
        rejeita(() -> new BinaryStream(new byte[4]).get(-1));
        rejeita(() -> new BinaryStream(new byte[4]).get(5));
        BinaryStream naoFinito = new BinaryStream();
        naoFinito.putFloat(Float.NaN);
        rejeita(() -> new BinaryStream(naoFinito.getBuffer()).getFloat());
        BinaryStream infinito = new BinaryStream();
        infinito.putLFloat(Float.POSITIVE_INFINITY);
        rejeita(() -> new BinaryStream(infinito.getBuffer()).getLFloat());
        for (int tamanho : new int[]{-1, 0, Integer.MAX_VALUE}) {
            BinaryStream invalido = new BinaryStream();
            invalido.putInt(tamanho);
            rejeita(() -> PacketDecoder.decodeBatch(Zlib.deflate(invalido.getBuffer()), network::getPacket));
        }
        BinaryStream aninhado = new BinaryStream();
        aninhado.putInt(1);
        aninhado.putByte(ProtocolInfo.BATCH_PACKET);
        rejeita(() -> PacketDecoder.decodeBatch(Zlib.deflate(aninhado.getBuffer()), network::getPacket));
        BinaryStream muitos = new BinaryStream();
        for (int i = 0; i <= PacketDecoder.MAX_BATCH_PACKETS; i++) {
            muitos.putInt(texto.getBuffer().length);
            muitos.put(texto.getBuffer());
        }
        rejeita(() -> PacketDecoder.decodeBatch(Zlib.deflate(muitos.getBuffer()), network::getPacket));
        BinaryStream receita = new BinaryStream();
        receita.putByte(ProtocolInfo.CRAFTING_EVENT_PACKET);
        receita.putByte((byte) 0);
        receita.putInt(0);
        receita.putUUID(new UUID(0, 0));
        receita.putInt(Integer.MAX_VALUE);
        rejeita(() -> PacketDecoder.decode(receita.getBuffer(), network::getPacket));
        cn.nukkit.block.Block.init();
        cn.nukkit.item.Item.init();
        // Cabeca empilha ate 64 no cliente; quantidade zero e repassada para a transacao decidir.
        if (slot(397, 2).getCount() != 2 || slot(1, 0).getId() != 1) {
            throw new AssertionError("Pilha legitima foi recusada na leitura do pacote.");
        }
        rejeita(() -> slot(1, 65));
        System.out.println("PacketTest: texto, lotes, truncamento e contagens excessivas e pilhas verificados.");
    }
}
