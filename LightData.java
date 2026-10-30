package com.lightutils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record LightData(String tag, int color, int intensity, int radius, String dim, BlockPos pos) {

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(tag);
        buf.writeInt(color);
        buf.writeInt(intensity);
        buf.writeInt(radius);
        buf.writeUtf(dim);
        buf.writeBlockPos(pos);
    }

    public static LightData read(FriendlyByteBuf buf) {
        return new LightData(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readUtf(), buf.readBlockPos());
    }

    public CompoundTag toNbt() {
        CompoundTag t = new CompoundTag();
        t.putString("tag", tag);
        t.putInt("color", color);
        t.putInt("intensity", intensity);
        t.putInt("radius", radius);
        t.putString("dim", dim);
        t.putLong("pos", pos.asLong());
        return t;
    }

    public static LightData fromNbt(CompoundTag t) {
        return new LightData(t.getString("tag"), t.getInt("color"), t.getInt("intensity"),
                t.getInt("radius"), t.getString("dim"), BlockPos.of(t.getLong("pos")));
    }
}
