package com.lightutils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class LightNet {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LightUtils.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void init() {
        CHANNEL.registerMessage(0, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
    }

    public static void syncAll(MinecraftServer server) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncPacket(LightSavedData.get(server).lights));
    }

    public static void syncTo(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncPacket(LightSavedData.get(player.server).lights));
    }

    public static class SyncPacket {
        final List<LightData> lights;

        public SyncPacket(List<LightData> lights) {
            this.lights = new ArrayList<>(lights);
        }

        static void encode(SyncPacket p, FriendlyByteBuf buf) {
            buf.writeVarInt(p.lights.size());
            for (LightData l : p.lights) l.write(buf);
        }

        static SyncPacket decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            List<LightData> list = new ArrayList<>();
            for (int i = 0; i < n; i++) list.add(LightData.read(buf));
            return new SyncPacket(list);
        }

        static void handle(SyncPacket p, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientLights.set(p.lights)));
            ctx.get().setPacketHandled(true);
        }
    }
}
