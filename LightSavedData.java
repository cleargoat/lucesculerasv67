package com.lightutils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class LightSavedData extends SavedData {
    public final List<LightData> lights = new ArrayList<>();

    public static LightSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(LightSavedData::load, LightSavedData::new, "lightutils_lights");
    }

    public static LightSavedData load(CompoundTag tag) {
        LightSavedData d = new LightSavedData();
        ListTag list = tag.getList("lights", 10);
        for (int i = 0; i < list.size(); i++) d.lights.add(LightData.fromNbt(list.getCompound(i)));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (LightData l : lights) list.add(l.toNbt());
        tag.put("lights", list);
        return tag;
    }
}
