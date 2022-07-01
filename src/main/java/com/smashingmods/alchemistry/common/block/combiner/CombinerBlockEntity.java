package com.smashingmods.alchemistry.common.block.combiner;

import com.smashingmods.alchemistry.Config;
import com.smashingmods.alchemistry.api.blockentity.AbstractAlchemistryBlockEntity;
import com.smashingmods.alchemistry.api.blockentity.EnergyBlockEntity;
import com.smashingmods.alchemistry.api.blockentity.InventoryBlockEntity;
import com.smashingmods.alchemistry.api.blockentity.handler.AutomationStackHandler;
import com.smashingmods.alchemistry.api.blockentity.handler.CustomEnergyStorage;
import com.smashingmods.alchemistry.api.blockentity.handler.ModItemStackHandler;
import com.smashingmods.alchemistry.common.recipe.combiner.CombinerRecipe;
import com.smashingmods.alchemistry.registry.BlockEntityRegistry;
import com.smashingmods.alchemistry.registry.RecipeRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CombinerBlockEntity extends AbstractAlchemistryBlockEntity implements InventoryBlockEntity, EnergyBlockEntity {

    protected final ContainerData data;

    private int progress = 0;
    private int maxProgress = Config.Common.combinerTicksPerOperation.get();

    private final ModItemStackHandler inputHandler = initializeInputHandler();
    private final ModItemStackHandler outputHandler = initializeOutputHandler();

    private final AutomationStackHandler automationInputHandler = getAutomationInputHandler(inputHandler);
    private final AutomationStackHandler automationOutputHandler = getAutomationOutputHandler(outputHandler);

    private final CombinedInvWrapper combinedInvWrapper = new CombinedInvWrapper(automationInputHandler, automationOutputHandler);
    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> combinedInvWrapper);

    private final ModItemStackHandler catalystHandler = initializeCatalystHandler();
    private final LazyOptional<IItemHandler> lazyCatalystHandler = LazyOptional.of(() -> catalystHandler);

    private final CustomEnergyStorage energyHandler = initializeEnergyStorage();
    private final LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.of(() -> energyHandler);

    private List<CombinerRecipe> recipes = new ArrayList<>();
    private CombinerRecipe currentRecipe;
    private int selectedRecipe = -1;
    private String editBoxText = "";
    private boolean recipeLocked;
    private boolean paused;

    public CombinerBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(BlockEntityRegistry.COMBINER_BLOCK_ENTITY.get(), pWorldPosition, pBlockState);

        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> energyHandler.getEnergyStored();
                    case 3 -> energyHandler.getMaxEnergyStored();
                    case 4 -> selectedRecipe;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> progress = pValue;
                    case 2 -> energyHandler.setEnergy(pValue);
                    case 4 -> selectedRecipe = pValue;
                }
            }

            @Override
            public int getCount() {
                return 5;
            }
        };
    }

    public void tick(Level pLevel) {
        if (!pLevel.isClientSide()) {
            if (!paused) {
                if (canProcessRecipe()) {
                    processRecipe();
                } else {
                    progress = 0;
                }
            }
        }
    }

    private boolean canProcessRecipe() {
        if (currentRecipe != null) {
            ItemStack output = outputHandler.getStackInSlot(0);
            return energyHandler.getEnergyStored() >= Config.Common.combinerEnergyPerTick.get()
                    && (currentRecipe.output.getCount() + output.getCount()) <= currentRecipe.output.getMaxStackSize()
                    && (ItemStack.isSameItemSameTags(output, currentRecipe.output) || output.isEmpty())
                    && currentRecipe.matchInputs(inputHandler);
        }
        return false;
    }

    private void processRecipe() {
        if (progress < maxProgress) {
            progress++;
        } else {
            progress = 0;
            outputHandler.setOrIncrement(0, currentRecipe.output.copy());
            for (int index = 0; index < currentRecipe.input.size(); index++) {
                ItemStack itemStack = currentRecipe.input.get(index);
                if (itemStack != null && !itemStack.isEmpty()) {
                    inputHandler.decrementSlot(index, itemStack.getCount());
                }
            }
        }
        energyHandler.extractEnergy(Config.Common.combinerEnergyPerTick.get(), false);
        setChanged();
    }

    public List<CombinerRecipe> getRecipes() {
        return this.recipes;
    }

    public void setRecipes(List<CombinerRecipe> pRecipes) {
        this.recipes = pRecipes;
    }

    public CombinerRecipe getCurrentRecipe() {
        return this.currentRecipe;
    }

    public void setCurrentRecipe(CombinerRecipe pRecipe) {
        this.currentRecipe = pRecipe;
    }

    public boolean getRecipeLocked() {
        return recipeLocked;
    }

    public void setRecipeLocked(boolean pLock) {
        recipeLocked = pLock;
    }

    public boolean getPaused() {
        return paused;
    }

    public void setPaused(boolean pPause) {
        paused = pPause;
    }

    protected String getEditBoxText() {
        return editBoxText;
    }

    protected void setEditBoxText(String pText) {
        editBoxText = pText;
    }

    @Override
    public CustomEnergyStorage initializeEnergyStorage() {
        return new CustomEnergyStorage(Config.Common.combinerEnergyCapacity.get()) {
            @Override
            protected void onEnergyChanged() {
                setChanged();
            }
        };
    }

    @Override
    public ModItemStackHandler initializeInputHandler() {
        return new ModItemStackHandler(this, 4) {
            @Override
            protected void onContentsChanged(int slot) {
                Optional<CombinerRecipe> combinerRecipe = RecipeRegistry.getRecipesByType(RecipeRegistry.COMBINER_TYPE, this.blockEntity.getLevel()).stream().filter(recipe -> recipe.matchInputs(this)).findFirst();
                combinerRecipe.ifPresent(recipe -> setCurrentRecipe(recipe));
            }
        };
    }

    @Override
    public ModItemStackHandler initializeOutputHandler() {
        return new ModItemStackHandler(this, 1) {
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return false;
            }
        };
    }

    private ModItemStackHandler initializeCatalystHandler() {
        return new ModItemStackHandler(this, 1);
    }

    public ModItemStackHandler getCatalystHandler() {
        return catalystHandler;
    }

    @Override
    public ModItemStackHandler getInputHandler() {
        return inputHandler;
    }

    @Override
    public ModItemStackHandler getOutputHandler() {
        return outputHandler;
    }

    @Override
    public AutomationStackHandler getAutomationInputHandler(IItemHandlerModifiable pHandler) {
        return new AutomationStackHandler(pHandler) {
            @NotNull
            @Override
            public ItemStack extractItem(int pSlot, int pAmount, boolean pSimulate) {
                return ItemStack.EMPTY;
            }
        };
    }

    @Override
    public AutomationStackHandler getAutomationOutputHandler(IItemHandlerModifiable pHandler) {
        return new AutomationStackHandler(pHandler) {
            @NotNull
            @Override
            public ItemStack extractItem(int pSlot, int pAmount, boolean pSimulate) {
                if (!getStackInSlot(pSlot).isEmpty()) {
                    return super.extractItem(pSlot, pAmount, pSimulate);
                } else {
                    return ItemStack.EMPTY;
                }
            }
        };
    }

    @Override
    public CombinedInvWrapper getAutomationInventory() {
        return combinedInvWrapper;
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction pDirection) {
        if (pDirection != null) {
            if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
                return switch (pDirection) {
                    case DOWN, EAST, WEST, NORTH, SOUTH -> lazyItemHandler.cast();
                    case UP -> super.getCapability(cap, pDirection);
                };
            } else if (cap == CapabilityEnergy.ENERGY) {
                return switch (pDirection) {
                    case UP -> lazyEnergyHandler.cast();
                    case DOWN, NORTH, EAST, SOUTH, WEST -> super.getCapability(cap, pDirection);
                };
            }
        }
        return super.getCapability(cap, pDirection);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyCatalystHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.putInt("progress", progress);
        pTag.put("input", inputHandler.serializeNBT());
        pTag.put("catalyst", catalystHandler.serializeNBT());
        pTag.put("output", outputHandler.serializeNBT());
        pTag.put("energy", energyHandler.serializeNBT());
        pTag.putString("editBoxText", editBoxText);
        pTag.putInt("selectedRecipe", selectedRecipe);
        pTag.putBoolean("locked", recipeLocked);
        pTag.putBoolean("paused", paused);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(@Nonnull CompoundTag pTag) {
        super.load(pTag);
        progress = pTag.getInt("progress");
        inputHandler.deserializeNBT(pTag.getCompound("input"));
        catalystHandler.deserializeNBT(pTag.getCompound("catalyst"));
        outputHandler.deserializeNBT(pTag.getCompound("output"));
        energyHandler.deserializeNBT(pTag.get("energy"));
        editBoxText = pTag.getString("editBoxText");
        selectedRecipe = pTag.getInt("selectedRecipe");
        recipeLocked = pTag.getBoolean("locked");
        paused = pTag.getBoolean("paused");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new CombinerMenu(pContainerId, pInventory, this, this.data);
    }
}