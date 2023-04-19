package com.smashingmods.alchemistry.datagen;

import com.smashingmods.alchemistry.Alchemistry;
import com.smashingmods.alchemistry.registry.BlockRegistry;
import com.smashingmods.alchemistry.registry.MenuRegistry;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Objects;

public class LocalizationGenerator extends LanguageProvider {

    public LocalizationGenerator(DataGenerator gen, String locale) {
        super(gen, Alchemistry.MODID, locale);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void addTranslations() {
        BlockRegistry.BLOCKS.getEntries().stream()
                .map(RegistryObject::get)
                .map(ForgeRegistries.BLOCKS::getKey)
                .filter(Objects::nonNull)
                .map(ResourceLocation::getPath)
                .forEach(path -> add(String.format("block.alchemistry.%s", path), WordUtils.capitalize(path.replace("_", " "))));

        MenuRegistry.MENU_TYPES.getEntries().stream()
                .map(RegistryObject::get)
                .map(ForgeRegistries.MENU_TYPES::getKey)
                .filter(Objects::nonNull)
                .map(ResourceLocation::getPath)
                .forEach(path -> {
                    path = path.replace("_menu", "");
                    String translation = WordUtils.capitalize(path.replace("_", " "));
                    add(String.format("alchemistry.container.%s", path), translation);
                    // weird place to put this, but gives all of the stuff we want!
                    add(String.format("alchemistry.jei.%s", path), translation);
                });

        add("itemGroup.alchemistry", "Alchemistry");

        add("tooltip.alchemistry.energy_requirement", "Requires %d FE/t");
        add("tooltip.alchemistry.requires", "Requires");

        add("alchemistry.container.search", "Search...");
        add("alchemistry.container.select_recipe", "Select recipe:");
        add("alchemistry.container.current_recipe", "Current recipe:");
        add("alchemistry.container.required_input", "Required input item:");
        add("alchemistry.container.target", "Target");
        add("alchemistry.container.reset_target", "Reset Target");
        add("alchemistry.container.nothing", "Nothing");
        add("alchemistry.container.enable_autobalance", "Enable Auto-Balance");
        add("alchemistry.container.disable_autobalance", "Disable Auto-Balance");
        add("alchemistry.container.disable_active_pushing", "Currently pushing outputs actively");
        add("alchemistry.container.enable_active_pushing", "Currently providing outputs passively");

        add("alchemistry.jei.dissolver.relative", "Relative");
        add("alchemistry.jei.dissolver.absolute", "Absolute");
        add("alchemistry.jei.dissolver.type", "Type");
        add("alchemistry.jei.dissolver.rolls", "Rolls");
        add("alchemistry.jei.elements.description", "All elements (except Hydrogen) can be created with the Fusion Chamber multiblock.\\nThe multiblock accepts 2 elements as input and fuses them together to create a new element equal to the sum of their atomic numbers.\"");

        add("alchemistry.patchouli.book_name", "Alchemistry Labs Catalogue");
        add("alchemistry.patchouli.landing_text", "Looking to smash some atoms together? This catalogue will outline the machines you can manufacture in your progression through Alchemistry.");
    }
}
