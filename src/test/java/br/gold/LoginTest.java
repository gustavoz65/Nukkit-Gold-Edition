package br.gold;

import cn.nukkit.network.PacketDecoder;
import cn.nukkit.network.Network;
import cn.nukkit.network.protocol.LoginPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.utils.BinaryStream;
import cn.nukkit.utils.PlayerName;
import cn.nukkit.utils.Zlib;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class LoginTest {
    private static String token(String json) {
        return "e30." + Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8)) + ".assinatura";
    }

    private static byte[] login(String nome) throws Exception {
        String cadeia = "{\"chain\":[\"" + token("{\"extraData\":{\"displayName\":\"" + nome + "\",\"identity\":\"00000000-0000-0000-0000-000000000001\"}}") + "\"]}";
        String skin = token("{\"SkinId\":\"Standard_Steve\",\"SkinData\":\"" + Base64.getEncoder().encodeToString(new byte[8192]) + "\"}");
        BinaryStream dados = new BinaryStream();
        dados.putLInt(cadeia.getBytes(StandardCharsets.UTF_8).length);
        dados.put(cadeia.getBytes(StandardCharsets.UTF_8));
        dados.putLInt(skin.getBytes(StandardCharsets.UTF_8).length);
        dados.put(skin.getBytes(StandardCharsets.UTF_8));
        byte[] comprimido = Zlib.deflate(dados.getBuffer());
        BinaryStream pacote = new BinaryStream();
        pacote.putByte(ProtocolInfo.LOGIN_PACKET);
        pacote.putInt(84);
        pacote.putInt(comprimido.length);
        pacote.put(comprimido);
        return pacote.getBuffer();
    }

    public static void main(String[] args) throws Exception {
        for (String invalido : new String[]{null, "ab", "../jogador", "Nome Com Espaco", "console", "RCON", "José", "nome_com_17_letras"}) {
            if (PlayerName.isValid(invalido)) { throw new AssertionError("Nome invalido aceito: " + invalido); }
        }
        Network network = new Network(null);
        LoginPacket recebido = (LoginPacket) PacketDecoder.decode(login("Jogador_123"), network::getPacket);
        if (!"Jogador_123".equals(recebido.username) || recebido.clientUUID == null || !recebido.skin.isValid()) {
            throw new AssertionError("Login valido nao foi preservado.");
        }
        try {
            PacketDecoder.decode(login("console"), network::getPacket);
            throw new AssertionError("Nome reservado aceito no login.");
        } catch (IllegalArgumentException esperado) { }
        try {
            PacketDecoder.decode(new byte[]{ProtocolInfo.LOGIN_PACKET, 0, 0, 0, 84, 0, 0, 0, 1, 0}, network::getPacket);
            throw new AssertionError("Login comprimido invalido foi aceito.");
        } catch (IllegalArgumentException esperado) { }
        System.out.println("LoginTest: nomes, JWT, skin valida e login truncado verificados.");
    }
}
