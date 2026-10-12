package br.com.viladeferro;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Guarda quais vilas já receberam seus habitantes, para não repetir (nem reviver o chefe morto). */
public class VillageSeedData extends SavedData {
    private static final String NAME = "viladeferro_villages";
    private final List<BlockPos> seeded = new ArrayList<>();

    public static VillageSeedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(VillageSeedData::load, VillageSeedData::new, NAME);
    }

    public boolean isSeeded(BlockPos pos) {
        for (BlockPos p : seeded) {
            if (p.distSqr(pos) < 128.0D * 128.0D) return true;
        }
        return false;
    }

    public void add(BlockPos pos) {
        seeded.add(pos.immutable());
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("seeded", seeded.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }

    public static VillageSeedData load(CompoundTag tag) {
        VillageSeedData data = new VillageSeedData();
        for (long l : tag.getLongArray("seeded")) data.seeded.add(BlockPos.of(l));
        return data;
    }
}
