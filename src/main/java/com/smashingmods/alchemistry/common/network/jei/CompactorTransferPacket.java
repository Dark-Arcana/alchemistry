package com.smashingmods.alchemistry.common.network.jei;

import com.smashingmods.alchemistry.Alchemistry;
import com.smashingmods.alchemistry.common.block.compactor.CompactorBlockEntity;
import com.smashingmods.alchemistry.common.block.compactor.CompactorMenu;
import com.smashingmods.alchemistry.common.recipe.compactor.CompactorRecipe;
import com.smashingmods.alchemistry.registry.RecipeRegistry;
import com.smashingmods.alchemylib.api.network.AlchemyPacket;
import com.smashingmods.alchemylib.api.storage.ProcessingSlotHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CompactorTransferPacket implements AlchemyPacket {

    private final BlockPos blockPos;
    private final ItemStack output;
    private final boolean maxTransfer;

    public CompactorTransferPacket(BlockPos pBlockPos, ItemStack pOutput, boolean pMaxTransfer) {
        this.blockPos = pBlockPos;
        this.output = pOutput;
        this.maxTransfer = pMaxTransfer;
    }

    public CompactorTransferPacket(FriendlyByteBuf pBuffer) {
        this.blockPos = pBuffer.readBlockPos();
        this.output = pBuffer.readItem();
        this.maxTransfer = pBuffer.readBoolean();
    }

    public void encode(FriendlyByteBuf pBuffer) {
        pBuffer.writeBlockPos(blockPos);
        pBuffer.writeItem(output);
        pBuffer.writeBoolean(maxTransfer);
    }

    public void handle(NetworkEvent.Context pContext) {
        ServerPlayer player = pContext.getSender();
        Objects.requireNonNull(player);

        CompactorBlockEntity blockEntity = (CompactorBlockEntity) player.getLevel().getBlockEntity(blockPos);
        Objects.requireNonNull(blockEntity);

        ProcessingSlotHandler inputHandler = blockEntity.getInputHandler();
        ProcessingSlotHandler outputHandler = blockEntity.getOutputHandler();
        Inventory inventory = player.getInventory();

        RecipeRegistry.getCompactorRecipe(recipe -> ItemStack.isSameItemSameTags(recipe.getOutput(), output), player.getLevel())
            .ifPresent(recipe -> {

                inputHandler.emptyToInventory(inventory);
                outputHandler.emptyToInventory(inventory);

                ItemStack inventoryInput = TransferUtils.matchIngredientToItemStack(inventory.items, recipe.getInput());
                ItemStack recipeInput = new ItemStack(inventoryInput.getItem(), recipe.getInput().getCount());
                boolean creative = player.gameMode.isCreative();
                boolean canTransfer = (!inventoryInput.isEmpty() || creative) && inputHandler.isEmpty() && outputHandler.isEmpty();

                if (canTransfer) {
                    if (creative) {
                        ItemStack creativeInput = new ItemStack(recipe.getInput().getIngredient().getItems()[0].getItem(), recipe.getInput().getCount());
                        int maxOperations = TransferUtils.getMaxOperations(creativeInput, maxTransfer);
                        inputHandler.setOrIncrement(0, new ItemStack(creativeInput.getItem(), recipe.getInput().getCount() * maxOperations));
                    } else {
                        int slot = inventory.findSlotMatchingItem(inventoryInput);
                        int maxOperations = TransferUtils.getMaxOperations(recipeInput, inventory.getItem(slot), maxTransfer, false);
                        inventory.removeItem(slot, recipe.getInput().getCount() * maxOperations);
                        inputHandler.setOrIncrement(0, new ItemStack(recipeInput.getItem(), recipe.getInput().getCount() * maxOperations));
                    }
                    blockEntity.setProgress(0);
                    blockEntity.setRecipe(recipe);
                    blockEntity.setCanProcess(true);
                }
            });
    }

    public static class TransferHandler implements IRecipeTransferHandler<CompactorMenu, CompactorRecipe> {

        public TransferHandler() {}

        @Override
        public Class<CompactorMenu> getContainerClass() {
            return CompactorMenu.class;
        }

        @Override
        public Class<CompactorRecipe> getRecipeClass() {
            return CompactorRecipe.class;
        }

        @Override
        public @Nullable IRecipeTransferError transferRecipe(CompactorMenu pContainer, CompactorRecipe pRecipe, IRecipeSlotsView pRecipeSlots, Player pPlayer, boolean pMaxTransfer, boolean pDoTransfer) {
            if (pDoTransfer) {
                pContainer.getBlockEntity().setRecipe(pRecipe);
                Alchemistry.PACKET_HANDLER.sendToServer(new CompactorTransferPacket(pContainer.getBlockEntity().getBlockPos(), pRecipe.getOutput(), pMaxTransfer));
            }
            return null;
        }
    }
}
