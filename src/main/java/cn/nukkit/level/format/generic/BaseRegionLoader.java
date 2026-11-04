package cn.nukkit.level.format.generic;

import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.LevelProvider;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
abstract public class BaseRegionLoader {
    public static final int VERSION = 1;
    public static final byte COMPRESSION_GZIP = 1;
    public static final byte COMPRESSION_ZLIB = 2;
    public static final int MAX_SECTOR_LENGTH = 256 << 12;
    public static final int COMPRESSION_LEVEL = 7;

    protected int x;
    protected int z;
    protected String filePath;
    protected File file;
    protected RandomAccessFile randomAccessFile;
    protected int lastSector;
    protected LevelProvider levelProvider;
    protected final Map<Integer, Integer[]> locationTable = new HashMap<>();

    public long lastUsed;
    private boolean corruptBackup;

    public BaseRegionLoader(LevelProvider level, int regionX, int regionZ, String ext) {
        try {
            this.x = regionX;
            this.z = regionZ;
            this.levelProvider = level;
            this.filePath = this.levelProvider.getPath() + "region/r." + regionX + "." + regionZ + "." + ext;
            this.file = new File(this.filePath);
            boolean exists = this.file.exists();
            if (!exists) {
                file.createNewFile();
            }
            this.randomAccessFile = new RandomAccessFile(this.filePath, "rw");
            if (!exists) {
                this.createBlank();
            } else {
                this.loadLocationTable();
            }

            this.lastUsed = System.currentTimeMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    protected abstract boolean isChunkGenerated(int index);

    public abstract BaseFullChunk readChunk(int x, int z) throws IOException;

    protected abstract BaseFullChunk unserializeChunk(byte[] data);

    public abstract boolean chunkExists(int x, int z);

    protected abstract void saveChunk(int x, int z, byte[] chunkData) throws IOException;

    public abstract void removeChunk(int x, int z);

    public abstract void writeChunk(FullChunk chunk) throws Exception;

    public abstract void close() throws IOException;

    protected abstract void loadLocationTable() throws IOException;

    public abstract int doSlowCleanUp() throws Exception;

    protected abstract void writeLocationIndex(int index) throws IOException;

    protected abstract void createBlank() throws IOException;

    public abstract int getX();

    public abstract int getZ();


    /**
     * Chunk que aponta para fora do arquivo ou foi gravado pela metade (servidor desligado a forca durante a gravacao):
     * guarda uma copia do arquivo original (uma vez por regiao), apaga a entrada do indice para o chunk ser gerado de novo
     * e avisa no console. Antes isso virava RuntimeException a cada tick e o jogador caia por timeout.
     */
    protected void dropCorruptChunk(int index, int x, int z, String why) {
        if (!this.corruptBackup) {
            this.corruptBackup = true;
            try { Files.copy(this.file.toPath(), new File(this.filePath + ".corrompido").toPath(), StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) { }
        }
        cn.nukkit.utils.MainLogger.getLogger().warning("[Mapa] Chunk " + (this.x * 32 + x) + "," + (this.z * 32 + z) + " de " + this.filePath + " estava " + why
                + " (servidor desligado no meio de uma gravacao?). Ele sera gerado de novo; copia do arquivo original em " + this.filePath + ".corrompido");
        Integer[] table = this.locationTable.get(index);
        if (table != null) {
            table[0] = 0;
            table[1] = 0;
            try { this.writeLocationIndex(index); } catch (IOException ignored) { }
        }
    }
}
