package cn.nukkit.block;

import cn.nukkit.item.Item;
import cn.nukkit.item.ItemTool;

/**
 * Created by Pub4Game on 27.12.2015.
 */
public class BlockMobSpawner extends BlockSolid {

    public BlockMobSpawner() {
        this(0);
    }

    public BlockMobSpawner(int meta) {
        super(0);
    }

    @Override
    public String getName() {
        return "Gerador de monstros";
    }

    @Override
    public int getId() {
        return MONSTER_SPAWNER;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public double getHardness() {
        return 5;
    }

    @Override
    public boolean place(Item item, Block block, Block target, int face, double fx, double fy, double fz, cn.nukkit.Player player) {
        if (!getLevel().setBlock(block, this, true, true)) { return false; }
        cn.nukkit.nbt.tag.CompoundTag nbt = new cn.nukkit.nbt.tag.CompoundTag()
                .putString("id", cn.nukkit.blockentity.BlockEntity.MOB_SPAWNER)
                .putInt("x", block.getFloorX()).putInt("y", block.getFloorY()).putInt("z", block.getFloorZ())
                .putInt("EntityId", 32).putInt("Delay", 200);
        if (item.hasCustomBlockData()) {
            int entityId = item.getCustomBlockData().getInt("EntityId");
            if (cn.nukkit.level.MobSpawning.isSupportedMonster(entityId)) { nbt.putInt("EntityId", entityId); }
        }
        new cn.nukkit.blockentity.BlockEntityMobSpawner(getLevel().getChunk(block.getFloorX() >> 4, block.getFloorZ() >> 4), nbt);
        return true;
    }

    @Override public boolean canBeActivated() { return true; }

    @Override
    public boolean onActivate(Item item, cn.nukkit.Player player) {
        cn.nukkit.blockentity.BlockEntity tile = getLevel().getBlockEntity(this);
        if (item.getId() != Item.SPAWN_EGG || !(tile instanceof cn.nukkit.blockentity.BlockEntityMobSpawner)) { return false; }
        if (!((cn.nukkit.blockentity.BlockEntityMobSpawner) tile).setEntityId(item.getDamage())) { return false; }
        if (player != null && player.isSurvival()) {
            item.setCount(item.getCount() - 1);
            player.getInventory().setItemInHand(item);
        }
        return true;
    }

    @Override
    public boolean onBreak(Item item) {
        cn.nukkit.blockentity.BlockEntity tile = getLevel().getBlockEntity(this);
        if (tile != null) { tile.close(); }
        return getLevel().setBlock(this, new BlockAir(), true, true);
    }

    @Override
    public int[][] getDrops(Item item) {
        // Como no jogo original, o gerador nao cai ao ser quebrado. Agora que ele
        // gera mobs de verdade, deixar cair com qualquer picareta criaria farms faceis;
        // plugins (ex.: SilkSpawners) podem definir drops pelo BlockBreakEvent.
        return new int[0][0];
    }
}
