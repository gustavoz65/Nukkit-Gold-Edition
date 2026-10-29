package cn.nukkit.network.protocol;

import cn.nukkit.entity.data.Skin;
import cn.nukkit.utils.Zlib;
import cn.nukkit.utils.PlayerName;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * Created by on 15-10-13.
 */
public class LoginPacket extends DataPacket {

    public static final byte NETWORK_ID = ProtocolInfo.LOGIN_PACKET;

    public String username;
    public int protocol;
    public UUID clientUUID;
    public long clientId;
    public String identityPublicKey;
    public String serverAddress;

    public Skin skin;

    @Override
    public byte pid() {
        return NETWORK_ID;
    }

    @Override
    public void decode() {
        this.protocol = this.getInt();
        byte[] str;
        try {
            str = Zlib.inflate(this.get(this.getInt()), 256 * 1024);
        } catch (Exception e) {
            throw new IllegalArgumentException("Dados de login comprimidos invalidos.", e);
        }
        this.setBuffer(str, 0);
        decodeChainData();
        decodeSkinData();
        if (!PlayerName.isValid(this.username) || this.clientUUID == null || this.skin == null || !this.skin.isValid()) {
            throw new IllegalArgumentException("Nome, identidade ou skin de login invalidos.");
        }
    }

    @Override
    public void encode() {

    }

    public int getProtocol() {
        return protocol;
    }

    private void decodeChainData() {
        String json = new String(this.get(getLInt()), StandardCharsets.UTF_8);
        validateJson(json);
        Map<String, List<String>> map = new Gson().fromJson(json,
                new TypeToken<Map<String, List<String>>>() {
                }.getType());
        if (map == null || !map.containsKey("chain") || map.get("chain") == null || map.get("chain").isEmpty() || map.get("chain").size() > 8) {
            throw new IllegalArgumentException("Cadeia de login invalida.");
        }
        List<String> chains = map.get("chain");
        for (String c : chains) {
            JsonObject chainMap = decodeToken(c);
            if (chainMap == null) continue;
            if (chainMap.has("extraData")) {
                JsonObject extra = chainMap.get("extraData").getAsJsonObject();
                if (extra.has("displayName")) this.username = extra.get("displayName").getAsString();
                if (extra.has("identity")) this.clientUUID = UUID.fromString(extra.get("identity").getAsString());
            }
            if (chainMap.has("identityPublicKey"))
                this.identityPublicKey = chainMap.get("identityPublicKey").getAsString();
        }
    }

    private void decodeSkinData() {
        JsonObject skinToken = decodeToken(new String(this.get(this.getLInt()), StandardCharsets.UTF_8));
        if (skinToken == null) { throw new IllegalArgumentException("Token de skin invalido."); }
        String skinId = null;
        if (skinToken.has("ClientRandomId")) this.clientId = skinToken.get("ClientRandomId").getAsLong();
        if (skinToken.has("ServerAddress")) this.serverAddress = skinToken.get("ServerAddress").getAsString();
        if (skinToken.has("SkinId")) skinId = skinToken.get("SkinId").getAsString();
        if (skinToken.has("SkinData")) this.skin = new Skin(skinToken.get("SkinData").getAsString(), skinId);
    }

    private JsonObject decodeToken(String token) {
        if (token == null || token.length() > 128 * 1024) {
            throw new IllegalArgumentException("Token de login excessivo.");
        }
        String[] base = token.split("\\.");
        if (base.length < 2) return null;
        String json = new String(Base64.getUrlDecoder().decode(base[1]), StandardCharsets.UTF_8);
        validateJson(json);
        return new Gson().fromJson(json, JsonObject.class);
    }

    private static void validateJson(String json) {
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (quoted && c == '\\') { ++i; continue; }
            if (c == '"') { quoted = !quoted; continue; }
            if (!quoted && (c == '{' || c == '[') && ++depth > 32) {
                throw new IllegalArgumentException("JSON de login profundo demais.");
            }
            if (!quoted && (c == '}' || c == ']') && --depth < 0) {
                throw new IllegalArgumentException("JSON de login malformado.");
            }
        }
        if (quoted || depth != 0) { throw new IllegalArgumentException("JSON de login incompleto."); }
    }

    @Override
    public Skin getSkin() {
        return this.skin;
    }
}
