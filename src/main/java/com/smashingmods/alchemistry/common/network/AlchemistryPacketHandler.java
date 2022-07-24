package com.smashingmods.alchemistry.common.network;

import com.smashingmods.alchemistry.Alchemistry;
import com.smashingmods.alchemistry.client.jei.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class AlchemistryPacketHandler {
    private static int PACKET_ID = 0;
    private static final String PROTOCOL_VERSION = "1.0";
    public static SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Alchemistry.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {

        INSTANCE.messageBuilder(BlockEntityPacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(BlockEntityPacket::new)
                .encoder(BlockEntityPacket::encode)
                .consumer(BlockEntityPacket::handle)
                .add();

        INSTANCE.messageBuilder(ProcessingButtonPacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(ProcessingButtonPacket::new)
                .encoder(ProcessingButtonPacket::encode)
                .consumer(ProcessingButtonPacket::handle)
                .add();

        INSTANCE.messageBuilder(CompactorResetPacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(CompactorResetPacket::new)
                .encoder(CompactorResetPacket::encode)
                .consumer(CompactorResetPacket::handle)
                .add();

        INSTANCE.messageBuilder(CombinerRecipePacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(CombinerRecipePacket::new)
                .encoder(CombinerRecipePacket::encode)
                .consumer(CombinerRecipePacket::handle)
                .add();

        INSTANCE.messageBuilder(CombinerTransferPacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(CombinerTransferPacket::new)
                .encoder(CombinerTransferPacket::encode)
                .consumer(CombinerTransferPacket::handle)
                .add();

        INSTANCE.messageBuilder(CompactorTransferPacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(CompactorTransferPacket::new)
                .encoder(CompactorTransferPacket::encode)
                .consumer(CompactorTransferPacket::handle)
                .add();

        INSTANCE.messageBuilder(FissionTransferPacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(FissionTransferPacket::new)
                .encoder(FissionTransferPacket::encode)
                .consumer(FissionTransferPacket::handle)
                .add();

        INSTANCE.messageBuilder(FusionTransferPacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(FusionTransferPacket::new)
                .encoder(FusionTransferPacket::encode)
                .consumer(FusionTransferPacket::handle)
                .add();

        INSTANCE.messageBuilder(LiquifierTransferPacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(LiquifierTransferPacket::new)
                .encoder(LiquifierTransferPacket::encode)
                .consumer(LiquifierTransferPacket::handle)
                .add();
    }

    @SuppressWarnings("unused")
    public static <MSG> void sendToPlayer(MSG pMessage, ServerPlayer pPlayer) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> pPlayer), pMessage);
    }

    @SuppressWarnings("unused")
    public static <MSG> void sendToAll(MSG pMessage) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), pMessage);
    }

    public static <MSG> void sendToNear(MSG pMessage, Level pLevel, BlockPos pBlockPos, double pRadius) {
        ResourceKey<Level> dimension = pLevel.dimension();
        double posX = pBlockPos.getX();
        double posY = pBlockPos.getY();
        double posZ = pBlockPos.getZ();
        INSTANCE.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(posX, posY, posZ, pRadius, dimension)), pMessage);
    }
}
