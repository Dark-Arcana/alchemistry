package com.smashingmods.alchemistry.datagen.recipe.dissolver;

import com.smashingmods.alchemistry.Alchemistry;
import com.smashingmods.alchemistry.common.recipe.dissolver.ProbabilitySet;
import com.smashingmods.alchemylib.api.item.IngredientStack;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class DissolverRecipeBuilder implements RecipeBuilder {

    private String group;
    private final ResourceLocation recipeId;
    private final IngredientStack input;
    private final ProbabilitySet result;
    private final Advancement.Builder advancementBuilder = Advancement.Builder.advancement();

    public DissolverRecipeBuilder(IngredientStack input, ProbabilitySet output, ResourceLocation pRecipeId) {
        this.input = input;
        this.result = output;
        this.recipeId = pRecipeId;
    }

    public static DissolverRecipeBuilder createRecipe(IngredientStack input, ProbabilitySet output, ResourceLocation pRecipeId) {
        return new DissolverRecipeBuilder(input, output, pRecipeId);
    }

    @Override
    public RecipeBuilder unlockedBy(String pCriterionName, CriterionTriggerInstance pCriterionTrigger) {
        this.advancementBuilder.addCriterion(pCriterionName, pCriterionTrigger)
                .rewards(AdvancementRewards.Builder.recipe(recipeId))
                .requirements(RequirementsStrategy.OR);
        return this;
    }

    @Override
    public RecipeBuilder group(@Nullable String pGroupName) {
        this.group = pGroupName;
        return this;
    }

    @Override
    public Item getResult() {
        return input.getIngredient().getItems()[0].getItem();
    }

    @Override
    public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
        ResourceLocation recipeLocation = new ResourceLocation(Alchemistry.MODID, String.format("dissolver/%s", pRecipeId.getPath()));
        ResourceLocation advancementLocation = new ResourceLocation(Alchemistry.MODID, String.format("recipes/dissolver/%s", pRecipeId.getPath()));

        pFinishedRecipeConsumer.accept(new DissolverRecipeResult(
                group,
                advancementBuilder,
                recipeLocation,
                advancementLocation,
                input,
                result
        ));
    }
}
