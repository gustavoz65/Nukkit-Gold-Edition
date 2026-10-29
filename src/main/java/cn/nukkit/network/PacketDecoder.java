package cn.nukkit.network;

import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.utils.BinaryStream;
import cn.nukkit.utils.Zlib;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Valida o enquadramento antes de entregar qualquer pacote de um lote ao jogador. */
public final class PacketDecoder {
    public static final int MAX_PAYLOAD_BYTES = 2 * 1024 * 1024;
    public static final int MAX_BATCH_PACKETS = 256;

    private PacketDecoder() { }

    public static DataPacket decode(byte[] buffer, Function<Byte, DataPacket> factory) {
        if (buffer == null || buffer.length == 0 || buffer.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Tamanho de pacote invalido.");
        }
        int start = buffer[0] == (byte) 0xfe ? 2 : 1;
        if (buffer.length < start) {
            throw new IllegalArgumentException("Cabecalho de pacote incompleto.");
        }
        DataPacket packet = factory.apply(buffer[start - 1]);
        if (packet != null) {
            packet.setBuffer(buffer, start);
            packet.decode();
        }
        return packet;
    }

    public static List<DataPacket> decodeBatch(byte[] payload, Function<Byte, DataPacket> factory) throws IOException {
        if (payload == null || payload.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Tamanho de lote invalido.");
        }
        BinaryStream stream = new BinaryStream(Zlib.inflate(payload, MAX_PAYLOAD_BYTES));
        List<DataPacket> packets = new ArrayList<>();
        int total = 0;
        while (!stream.feof()) {
            if (++total > MAX_BATCH_PACKETS) {
                throw new IllegalArgumentException("Quantidade de pacotes no lote excedida.");
            }
            int length = stream.getInt();
            if (length <= 0 || length > stream.remaining()) {
                throw new IllegalArgumentException("Comprimento de pacote no lote invalido.");
            }
            byte[] buffer = stream.get(length);
            if (buffer[0] == ProtocolInfo.BATCH_PACKET || buffer[0] == (byte) 0xfe) {
                throw new IllegalArgumentException("Lote aninhado ou cabecalho invalido.");
            }
            DataPacket packet = decode(buffer, factory);
            if (packet != null) {
                packets.add(packet);
            }
        }
        return packets;
    }
}
