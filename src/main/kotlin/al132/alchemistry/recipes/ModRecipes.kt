package al132.alchemistry.recipes

import al132.alchemistry.chemistry.CompoundRegistry
import al132.alchemistry.chemistry.ElementRegistry
import al132.alchemistry.items.ModItems
import al132.alchemistry.utils.toCompoundStack
import al132.alchemistry.utils.toElementStack
import al132.alchemistry.utils.toStack
import al132.alib.utils.Utils.firstOre
import al132.alib.utils.Utils.oreExists
import al132.alib.utils.extensions.toDict
import al132.alib.utils.extensions.toStack
import net.minecraft.block.Block
import net.minecraft.block.BlockTallGrass
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.Ingredient
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fml.common.IFuelHandler
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.oredict.OreDictionary
import net.minecraftforge.oredict.OreIngredient
import java.util.*

/**
 * Created by al132 on 1/16/2017.
 */

data class DissolverOreData(val prefix: String, val quantity: Int, val strs: List<String>) {
    fun toDictName(index: Int) = prefix + strs[index].first().toUpperCase() + strs[index].substring(1)
    val size = strs.size
}


object ModRecipes {

    val electrolyzerRecipes = ArrayList<ElectrolyzerRecipe>()
    val dissolverRecipes = ArrayList<DissolverRecipe>()
    val combinerRecipes = ArrayList<CombinerRecipe>()
    val evaporatorRecipes = ArrayList<EvaporatorRecipe>()
    val atomizerRecipes = ArrayList<AtomizerRecipe>()
    val liquifierRecipes = ArrayList<LiquifierRecipe>()
    //val alloyRecipes = ArrayList<AlloyRecipe>()

    val metals: List<String> = listOf(//not all technically metals, I know
            "aluminum",
            "arsenic",
            "beryllium",
            "bismuth",
            "boron",
            "cadmium",
            "calcium",
            "cerium",
            "chromium",
            "cobalt",
            "copper",
            "dysprosium",
            "erbium",
            "gadolinium",
            "gold",
            "holmium",
            "iridium",
            "iron",
            "lanthanum",
            "lead",
            "lithium",
            "lutetium",
            "magnesium",
            "manganese",
            "neodymium",
            "nickel",
            "niobium",
            "osmium",
            "phosphorus",
            "platinum",
            "potassium",
            "praseodymium",
            "samarium",
            "scandium",
            "silicon",
            "silver",
            "sodium",
            "sulfur",
            "tantalum",
            "terbium",
            "thorium",
            "thulium",
            "tin",
            "titanium",
            "tungsten",
            "uranium",
            "ytterbium",
            "yttrium",
            "zinc")

    val metalOreData: List<DissolverOreData> = listOf(
            DissolverOreData("ingot", 16, metals),
            DissolverOreData("ore", 32, metals),
            DissolverOreData("dust", 16, metals),
            DissolverOreData("block", 144, metals),
            DissolverOreData("nugget", 1, metals),
            DissolverOreData("plate", 16, metals))

    fun init() {
        initElectrolyzerRecipes()
        initEvaporatorRecipes()
        initFuelHandler()
        initDissolverRecipes() //before combiner, so combiner can use reversible recipes
        initCombinerRecipes()
        initAtomizerRecipes()
        initLiquifierRecipes()
    }


    fun addDissolverRecipesForAlloy(alloySuffix: String, //Should start with uppercase, i.e. "Bronze" or "ElectricalSteel"
                                    ingotOne: String,
                                    quantityOne: Int,
                                    ingotTwo: String,
                                    quantityTwo: Int,
                                    ingotThree: String = "",
                                    quantityThree: Int = 0,
                                    conservationOfMass: Boolean = true) {

        fun fitInto16(q1: Int, q2: Int, q3: Int): List<Int>? {
            val sum = q1 + q2 + q3
            val new1 = Math.round(q1.toDouble() / sum * 16.0).toInt()
            val new2 = Math.round(q2.toDouble() / sum * 16.0).toInt()
            val new3 = Math.round(q3.toDouble() / sum * 16.0).toInt()
            if ((16).rem(sum) == 0) return listOf(new1, new2, new3)
            else return null
        }

        val ores: List<String> = listOf(("ingot$alloySuffix"), ("plate$alloySuffix"), ("dust$alloySuffix"))
        val threeIngredients: Boolean = ingotThree.length > 0 && quantityThree > 0
        val fractionalQuantities = fitInto16(quantityOne, quantityTwo, quantityThree)
        val isConserved = fractionalQuantities != null && conservationOfMass
        val calculatedQuantity1 = if (isConserved) fractionalQuantities!![0] else quantityOne * 16
        val calculatedQuantity2 = if (isConserved) fractionalQuantities!![1] else quantityOne * 16
        val calculatedQuantity3 = if (isConserved) fractionalQuantities!![2] else quantityOne * 16

        ores.filter { oreExists(it) && OreDictionary.getOres(it).isNotEmpty() }
                .forEach { ore ->
                    dissolverRecipes.add(dissolverRecipe {
                        input = ore.toOre()
                        //if (fractionalQuantities == null && conservationOfMass) inputQuantity = quantityOne + quantityTwo + quantityThree
                        output {
                            addGroup {
                                addStack { ingotOne.toStack(quantity = calculatedQuantity1) }
                                addStack { ingotTwo.toStack(quantity = calculatedQuantity2) }
                                if (threeIngredients) {
                                    addStack { ingotThree.toStack(quantity = calculatedQuantity3) }
                                }
                            }
                        }
                    })
                }

        if (oreExists("block$alloySuffix") && OreDictionary.getOres("block$alloySuffix").isNotEmpty()) {
            dissolverRecipes.add(dissolverRecipe {
                input = ("block$alloySuffix").toOre()
                output {
                    addGroup {
                        if (threeIngredients) {
                            addStack { ingotOne.toStack(calculatedQuantity1 * 9) }//quantity = 144 / (quantityOne + quantityTwo + quantityThree) * quantityOne) }
                            addStack { ingotTwo.toStack(calculatedQuantity2 * 9) }//quantity = 144 / (quantityOne + quantityTwo + quantityThree) * quantityTwo) }
                            addStack { ingotThree.toStack(calculatedQuantity3 * 9) }//quantity = 144 / (quantityOne + quantityTwo + quantityThree) * quantityThree) }
                        } else {
                            addStack { ingotOne.toStack(quantity = calculatedQuantity1 * 9) }//144 / (quantityOne + quantityTwo) * quantityOne) }
                            addStack { ingotTwo.toStack(quantity = calculatedQuantity2 * 9) }//144 / (quantityOne + quantityTwo) * quantityTwo) }
                        }
                    }
                }
            })
        }
    }


    fun initDissolverRecipes() {

        CompoundRegistry.compounds
                .filter { it.autoDissolverRecipe }
                .forEach { compound ->
                    dissolverRecipes.add(dissolverRecipe {
                        input = compound.toItemStack(1).toIngredient()
                        output {
                            addGroup {
                                compound.components.forEach { component ->
                                    addStack { component.compound.toItemStack(component.quantity) }
                                }
                            }
                        }
                    })
                }

        for (meta in 0..BlockTallGrass.EnumType.values().size) {
            dissolverRecipes.add(dissolverRecipe {
                input = Blocks.TALLGRASS.toIngredient(meta = BlockTallGrass.EnumType.byMetadata(meta).ordinal)
                output {
                    relativeProbability = false
                    addGroup { addStack { "cellulose".toCompoundStack() }; probability = 25 }
                }
            })
        }

        listOf("ingotChrome", "plateChrome", "dustChrome")
                .filter { oreExists(it) }
                .forEach { ore ->
                    dissolverRecipes.add(dissolverRecipe {
                        input = ore.toOre()
                        output {
                            addGroup {
                                addStack { "chromium".toElementStack(16) }
                            }
                        }
                    })
                }

        if (oreExists("blockChrome")) {
            dissolverRecipes.add(dissolverRecipe {
                input = "blockChrome".toOre()
                output {
                    addGroup {
                        addStack { "chromium".toElementStack(16 * 9) }
                    }
                }
            })
        }

        if (oreExists("oreChrome")) {
            dissolverRecipes.add(dissolverRecipe {
                input = "oreChrome".toOre()
                output {
                    addGroup {
                        addStack { "chromium".toElementStack(16 * 2) }
                    }
                }
            })
        }

        if (oreExists("dustAsh")) {
            dissolverRecipes.add(dissolverRecipe {
                input = "dustAsh".toOre()
                reversible = true
                output {
                    addGroup {
                        addStack { "potassium_carbonate".toCompoundStack(4) }
                    }
                }
            })
        }

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.COAL_ORE.toIngredient()
            output {
                addGroup { addStack { "carbon".toElementStack(quantity = 32) } }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.NETHERRACK.toIngredient()
            output {
                addGroup { addStack { ItemStack.EMPTY }; probability = 15 }
                addGroup { addStack { "zinc_oxide".toCompoundStack() }; probability = 2 }
                addGroup { addStack { "gold".toElementStack() }; probability = 1 }
                addGroup { addStack { "phosphorus".toElementStack() }; probability = 1 }
                addGroup { addStack { "sulfur".toElementStack() }; probability = 1 }
                addGroup { addStack { "germanium".toElementStack() }; probability = 1 }
                addGroup { addStack { "silicon".toElementStack() }; probability = 4 }

            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Items.NETHERBRICK.toIngredient()
            output {
                addGroup { addStack { ItemStack.EMPTY }; probability = 10 }
                addGroup { addStack { "zinc_oxide".toCompoundStack() }; probability = 2 }
                addGroup { addStack { "gold".toElementStack() }; probability = 1 }
                addGroup { addStack { "phosphorus".toElementStack() }; probability = 1 }
                addGroup { addStack { "sulfur".toElementStack() }; probability = 1 }
                addGroup { addStack { "germanium".toElementStack() }; probability = 1 }
                addGroup { addStack { "silicon".toElementStack() }; probability = 4 }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Items.IRON_HORSE_ARMOR.toIngredient()
            output {
                addStack { "iron".toElementStack(64) }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.ANVIL.toIngredient()
            output {
                addStack { "iron".toElementStack((144 * 3) + (16 * 4)) }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Items.IRON_DOOR.toIngredient()
            output {
                addStack { "iron".toElementStack(32) }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.IRON_TRAPDOOR.toIngredient()
            output {
                addStack { "iron".toElementStack(64) }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.CHEST.toIngredient()
            output {
                addStack { "cellulose".toCompoundStack(2) }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.CRAFTING_TABLE.toIngredient()
            output {
                addStack { "cellulose".toCompoundStack(1) }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.WEB.toIngredient()
            output {
                addStack { "protein".toCompoundStack(2) }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Items.GOLDEN_HORSE_ARMOR.toIngredient()
            output {
                addStack { "gold".toElementStack(64) }
            }
        })

        (0 until 16).forEach { index ->
            dissolverRecipes.add(dissolverRecipe {
                input = Blocks.WOOL.toIngredient(meta = index)
                output {
                    addStack { "protein".toCompoundStack(1) }
                    addStack { "triglyceride".toCompoundStack(1) }

                }
            })
        }

        dissolverRecipes.add(dissolverRecipe {
            input = Items.EMERALD.toIngredient()
            output {
                reversible = true
                addGroup {
                    addStack { "beryl".toCompoundStack(16) }
                    addStack { "chromium".toElementStack(8) }
                    addStack { "vanadium".toElementStack(4) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.END_STONE.toIngredient()
            output {
                addGroup { addStack { "mercury".toElementStack() }; probability = 60 }
                addGroup { addStack { "neodymium".toElementStack() }; probability = 4 }
                addGroup { addStack { "silicon_dioxide".toCompoundStack(2) }; probability = 300 }
                addGroup { addStack { "lithium".toElementStack() }; probability = 50 }
            }
        })

        listOf(Blocks.GRASS.toStack(), Blocks.DIRT.toStack(), Blocks.DIRT.toStack(meta = 1), Blocks.DIRT.toStack(meta = 2)).forEach {
            dissolverRecipes.add(dissolverRecipe {
                input = it.toIngredient()
                output {
                    addGroup { addStack { "water".toCompoundStack() }; probability = 30 }
                    addGroup { addStack { "silicon_dioxide".toCompoundStack() }; probability = 50; }
                    addGroup { addStack { "cellulose".toCompoundStack() }; probability = 10 }
                    addGroup { addStack { "kaolinite".toCompoundStack() }; probability = 10 }
                }
            })
        }

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.EMERALD_BLOCK.toIngredient()
            output {
                addGroup {
                    addStack { "beryl".toCompoundStack(16 * 9) }
                    addStack { "chromium".toElementStack(8 * 9) }
                    addStack { "vanadium".toElementStack(4 * 9) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = "blockGlass".toOre()
            output {
                addStack { "silicon_dioxide".toCompoundStack(4) }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = "treeSapling".toOre()
            output {
                relativeProbability = false
                addGroup { addStack { "cellulose".toCompoundStack(1) }; probability = 25 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.DEADBUSH.toIngredient()
            output {
                relativeProbability = false
                addGroup { addStack { "cellulose".toCompoundStack() }; probability = 25 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.VINE.toIngredient()
            output {
                relativeProbability = false
                addGroup { addStack { "cellulose".toCompoundStack() }; probability = 25 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.WATERLILY.toIngredient()
            output {
                relativeProbability = false
                addGroup { addStack { "cellulose".toCompoundStack() }; probability = 25 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.PUMPKIN.toIngredient()
            output {
                relativeProbability = false
                addGroup {
                    probability = 50
                    addStack { "cucurbitacin".toCompoundStack() }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.QUARTZ.toIngredient()
            reversible = true
            output {
                addGroup {
                    addStack { "barium".toElementStack(8) }
                    addStack { "silicon_dioxide".toCompoundStack(16) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.QUARTZ_BLOCK.toIngredient()
            reversible = true
            output {
                addGroup {
                    addStack { "barium".toElementStack(8 * 4) }
                    addStack { "silicon_dioxide".toCompoundStack(16 * 4) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.BROWN_MUSHROOM.toIngredient()
            reversible = true
            output {
                addGroup {
                    addStack { "psilocybin".toCompoundStack() }
                    addStack { "cellulose".toCompoundStack() }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.RED_MUSHROOM.toIngredient()
            reversible = true
            output {
                addGroup {
                    addStack { "cellulose".toCompoundStack() }
                    addStack { "psilocybin".toCompoundStack() }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.DYE.toIngredient(quantity = 4, meta = 4) //lapis
            output {
                reversible = true
                addGroup {
                    addStack { "sodium".toElementStack(6) }
                    addStack { "calcium".toElementStack(2) }
                    addStack { "aluminum".toElementStack(6) }
                    addStack { "silicon".toElementStack(6) }
                    addStack { "oxygen".toElementStack(24) }
                    addStack { "sulfur".toElementStack(2) }

                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.LAPIS_BLOCK.toIngredient()
            output {
                addGroup {
                    addStack { "sodium".toElementStack(6 * 9) }
                    addStack { "calcium".toElementStack(2 * 9) }
                    addStack { "aluminum".toElementStack(6 * 9) }
                    addStack { "silicon".toElementStack(6 * 9) }
                    addStack { "oxygen".toElementStack(24 * 9) }
                    addStack { "sulfur".toElementStack(2 * 9) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.STRING.toIngredient()
            output {
                relativeProbability = false
                addGroup {
                    probability = 50
                    addStack { "protein".toCompoundStack() }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = ModItems.condensedMilk.toIngredient()
            output {
                relativeProbability = false
                addGroup { addStack { "calcium".toElementStack(4) }; probability = 40 }
                addGroup { addStack { "protein".toCompoundStack() }; probability = 20 }
                addGroup { addStack { "sucrose".toCompoundStack() }; probability = 20 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.WHEAT.toIngredient()
            output {
                relativeProbability = false
                addGroup { addStack { "starch".toCompoundStack() }; probability = 5 }
                addGroup { addStack { "cellulose".toCompoundStack() }; probability = 25 }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.GRAVEL.toIngredient()
            output {
                addGroup { addStack { "silicon_dioxide".toCompoundStack() } }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.HAY_BLOCK.toIngredient()
            output {
                rolls = 9
                relativeProbability = false
                addGroup { addStack { "starch".toCompoundStack() }; probability = 5 }
                addGroup { addStack { "cellulose".toCompoundStack() }; probability = 25 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.POTATO.toIngredient()
            output {
                relativeProbability = false
                addGroup { addStack { "starch".toCompoundStack() }; probability = 10 }
                addGroup { addStack { "potassium".toElementStack(5) }; probability = 25 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.BAKED_POTATO.toIngredient()
            output {
                relativeProbability = false
                addGroup { addStack { "starch".toCompoundStack() }; probability = 10 }
                addGroup { addStack { "potassium".toElementStack(5) }; probability = 25 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.REDSTONE.toIngredient()
            output {
                reversible = true
                addGroup {
                    addStack { "iron_oxide".toCompoundStack() }
                    addStack { "strontium_carbonate".toCompoundStack() }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.BEEF.toIngredient()
            output {
                //reversible = true
                addGroup {
                    addStack { "protein".toCompoundStack(4) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.COOKED_BEEF.toIngredient()
            output {
                //reversible = true
                addGroup {
                    addStack { "protein".toCompoundStack(4) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Ingredient.fromStacks(Items.CHICKEN.toStack())
            output {
                //reversible = true
                addGroup {
                    addStack { "protein".toCompoundStack(4) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.COOKED_CHICKEN.toIngredient()
            output {
                //reversible = true
                addGroup {
                    addStack { "protein".toCompoundStack(4) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.FISH.toIngredient()
            output {
                //reversible = true
                addGroup {
                    addStack { "protein".toCompoundStack(4) }
                    addStack { "selenium".toElementStack(2) }

                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.COOKED_FISH.toIngredient()
            output {
                //reversible = true
                addGroup {
                    addStack { "protein".toCompoundStack(4) }
                    addStack { "selenium".toElementStack(2) }

                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.RABBIT.toIngredient()
            output {
                //reversible = true
                addGroup {
                    addStack { "protein".toCompoundStack(4) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.COOKED_RABBIT.toIngredient()
            output {
                //reversible = true
                addGroup {
                    addStack { "protein".toCompoundStack(4) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = "dyeRed".toOre()
            output {
                addStack { "iron_oxide".toCompoundStack(quantity = 2) }
            }
        })



        dissolverRecipes.add(dissolverRecipe {
            input = "dyeYellow".toOre()
            output {
                addStack { "lead_iodide".toCompoundStack(quantity = 2) }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.REDSTONE_BLOCK.toIngredient()
            output {
                addGroup {
                    addStack { "iron_oxide".toCompoundStack(9) }
                    addStack { "strontium_carbonate".toCompoundStack(9) }
                }
            }
        })
        dissolverRecipes.add(dissolverRecipe
        {
            input = "protein".toCompoundStack().toIngredient()
            output {
                rolls = 14
                addGroup { addStack { "oxygen".toElementStack() }; probability = 10 }
                addGroup { addStack { "carbon".toElementStack() }; probability = 30 }
                addGroup { addStack { "nitrogen".toElementStack() }; probability = 5 }
                addGroup { addStack { "sulfur".toElementStack() }; probability = 5 }
                addGroup { addStack { "hydrogen".toElementStack() }; probability = 20 }
            }
        })


        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.CLAY.toIngredient()
            output {
                addStack { "kaolinite".toCompoundStack(4) }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.CLAY_BALL.toIngredient()
            reversible = true
            output {
                addStack { "kaolinite".toCompoundStack() }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.SUGAR.toIngredient()
            reversible = true
            output {
                addStack { "sucrose".toCompoundStack() }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.BONE.toIngredient()
            reversible = true
            output {
                addStack { "hydroxylapatite".toCompoundStack(2) }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.DYE.toIngredient(meta = 15) //bonemeal
            output {
                relativeProbability = false
                addGroup { addStack { "hydroxylapatite".toCompoundStack(1) }; probability = 50 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.BONE_BLOCK.toIngredient()
            output {
                rolls = 9
                relativeProbability = false
                addGroup { addStack { "hydroxylapatite".toCompoundStack(1) }; probability = 50 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.EGG.toIngredient()
            reversible = true
            output {
                addGroup {
                    addStack { "calcium_carbonate".toCompoundStack(8) }
                    addStack { "protein".toCompoundStack(2) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = ModItems.mineralSalt.toIngredient()
            output {
                addGroup { addStack { "sodium_chloride".toCompoundStack() }; probability = 80 }
                addGroup { addStack { "lithium".toElementStack() }; probability = 2 }
                addGroup { addStack { "potassium_chloride".toCompoundStack() }; probability = 5 }
                addGroup { addStack { "iron".toElementStack() }; probability = 2 }
                addGroup { addStack { "copper".toElementStack() }; probability = 2 }
                addGroup { addStack { "zinc".toElementStack() } }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.COAL.toIngredient()
            reversible = true
            output {
                addStack { "carbon".toElementStack(quantity = 8) }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Items.COAL.toIngredient(meta = 1)
            reversible = true
            output {
                addStack { "carbon".toElementStack(quantity = 8) }
            }
        })


        dissolverRecipes.add(dissolverRecipe
        {
            input = "slabWood".toOre()
            output {
                relativeProbability = false
                addGroup { addStack { "cellulose".toCompoundStack() }; probability = 12 }
            }
        })


        if (oreExists("itemSilicon")) {
            dissolverRecipes.add(dissolverRecipe {
                input = "itemSilicon".toOre()
                reversible = true
                output {
                    addStack { "silicon".toElementStack(16) }
                }
            })
        }

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.ENDER_PEARL.toIngredient()
            reversible = true
            output {
                addGroup {
                    addStack { "silicon".toElementStack(16) }
                    addStack { "mercury".toElementStack(16) }
                    addStack { "neodymium".toElementStack(16) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.DIAMOND.toIngredient()
            output {
                addStack { "carbon".toElementStack(quantity = 64 * 8) }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.DIAMOND_BLOCK.toIngredient()
            output {
                addStack { "carbon".toElementStack(quantity = 64 * 8 * 9) }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = "plankWood".toOre()
            output {
                relativeProbability = false
                addGroup { addStack { "cellulose".toCompoundStack() }; probability = 25 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = "cobblestone".toOre()
            output {
                addGroup { addStack { ItemStack.EMPTY }; probability = 350 }
                addGroup { addStack { "aluminum".toElementStack(1) } }
                addGroup { addStack { "iron".toElementStack(1) }; probability = 2 }
                addGroup { addStack { "gold".toElementStack(1) } }
                addGroup { addStack { "silicon_dioxide".toCompoundStack(1) }; probability = 5 }
            }
        })

        listOf("stoneGranite", "stoneGranitePolished").forEach {
            dissolverRecipes.add(dissolverRecipe
            {
                input = it.toOre()
                output {
                    addGroup { addStack { ItemStack.EMPTY }; probability = 100 }
                    addGroup { addStack { "aluminum_oxide".toCompoundStack(1) }; probability = 5 }
                    addGroup { addStack { "iron".toElementStack(1) }; probability = 2 }
                    addGroup { addStack { "potassium_chloride".toCompoundStack(1) }; probability = 2 }
                    addGroup { addStack { "silicon_dioxide".toCompoundStack(1) }; probability = 10 }
                }
            })
        }


        listOf("stoneDiorite", "stoneDioritePolished").forEach {
            dissolverRecipes.add(dissolverRecipe
            {
                input = it.toOre()
                output {
                    addGroup { addStack { ItemStack.EMPTY }; probability = 100 }
                    addGroup { addStack { "aluminum_oxide".toCompoundStack(1) }; probability = 5 }
                    addGroup { addStack { "iron".toElementStack(1) }; probability = 2 }
                    addGroup { addStack { "potassium_chloride".toCompoundStack(1) }; probability = 2 }
                    addGroup { addStack { "silicon_dioxide".toCompoundStack(1) }; probability = 10 }
                }
            })
        }



        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.MAGMA.toIngredient()
            output {
                rolls = 2
                addGroup { addStack { ItemStack.EMPTY }; probability = 80 }
                addGroup { addStack { "aluminum_oxide".toCompoundStack(1) }; probability = 5 }
                addGroup { addStack { "magnesium_oxide".toCompoundStack(1) }; probability = 20 }
                addGroup { addStack { "potassium_chloride".toCompoundStack(1) }; probability = 2 }
                addGroup { addStack { "silicon_dioxide".toCompoundStack(2) }; probability = 10 }
                addGroup { addStack { "sulfur".toElementStack() }; probability = 5 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = "treeLeaves".toOre()
            output {
                relativeProbability = false
                addGroup { addStack { "cellulose".toCompoundStack() }; probability = 5 }
            }
        })

        listOf("stoneAndesite", "stoneAndesitePolished").forEach {
            dissolverRecipes.add(dissolverRecipe
            {
                input = it.toOre()
                output {
                    addGroup { addStack { ItemStack.EMPTY }; probability = 100 }
                    addGroup { addStack { "aluminum_oxide".toCompoundStack(1) }; probability = 5 }
                    addGroup { addStack { "iron".toElementStack(1) }; probability = 2 }
                    addGroup { addStack { "potassium_chloride".toCompoundStack(1) }; probability = 2 }
                    addGroup { addStack { "silicon_dioxide".toCompoundStack(1) }; probability = 10 }
                }
            })
        }

        dissolverRecipes.add(dissolverRecipe
        {
            input = "stone".toOre()
            output {
                addGroup { addStack { ItemStack.EMPTY }; probability = 100 }
                addGroup { addStack { "aluminum".toElementStack(1) } }
                addGroup { addStack { "iron".toElementStack(1) }; probability = 2 }
                addGroup { addStack { "gold".toElementStack(1) } }
                addGroup { addStack { "silicon_dioxide".toCompoundStack(1) }; probability = 5 }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.SAND.toIngredient()
            output {
                relativeProbability = false
                addGroup { addStack { "silicon_dioxide".toCompoundStack(quantity = 4) }; probability = 100 }
                addGroup { addStack { "gold".toElementStack() } }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.SAND.toIngredient(meta = 1) //red sand
            output {
                relativeProbability = false
                addGroup { addStack { "silicon_dioxide".toCompoundStack(quantity = 4) }; probability = 100 }
                addGroup { addStack { "iron_oxide".toCompoundStack() }; probability = 10 }
            }
        })



        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.GUNPOWDER.toIngredient()
            reversible = true
            output {
                addGroup {
                    addStack { "potassium_nitrate".toCompoundStack(2) }
                    addStack { "sulfur".toElementStack(8) }
                    addStack { "carbon".toElementStack(8) }
                }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = "logWood".toOre()
            output {
                addStack { "cellulose".toCompoundStack() }
            }
        })

        metalOreData.forEach { list ->
            (0 until list.size).forEach { index ->
                val elementName = list.strs[index]
                val oreName = list.toDictName(index)
                if (OreDictionary.doesOreNameExist(oreName) && OreDictionary.getOres(oreName).isNotEmpty()) {
                    dissolverRecipes.add(dissolverRecipe {
                        input = oreName.toOre()
                        output {
                            addStack {
                                ModItems.elements.toStack(quantity = list.quantity, meta = ElementRegistry.getMeta(elementName))
                            }
                        }
                    })
                }
            }
        }

        dissolverRecipes.add(dissolverRecipe
        {
            input = Items.GLOWSTONE_DUST.toIngredient()
            reversible = true
            output {
                addStack { "phosphorus".toElementStack(quantity = 4) }
            }
        })

        dissolverRecipes.add(dissolverRecipe
        {
            input = Blocks.IRON_BARS.toIngredient()
            output {
                addStack { "iron".toElementStack(quantity = 6) }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Items.BLAZE_POWDER.toIngredient()
            reversible = true
            output {
                addStack { "germanium".toElementStack(quantity = 8) }
                addStack { "carbon".toElementStack(quantity = 8) }
                addStack { "sulfur".toElementStack(quantity = 8) }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Items.NETHER_WART.toIngredient()
            output {
                addStack { "cellulose".toCompoundStack() }
                addStack { "germanium".toElementStack(quantity = 4) }
                addStack { "selenium".toElementStack(quantity = 4) }
            }
        })

        if (oreExists("dropHoney")) {
            dissolverRecipes.add(dissolverRecipe {
                input = "dropHoney".toOre()
                output {
                    addStack { "sucrose".toCompoundStack(quantity = 4) }
                }
            })
        }

        if (oreExists("shardPrismarine")) {
            dissolverRecipes.add(dissolverRecipe {
                input = "shardPrismarine".toOre()
                reversible = true
                output {
                    addGroup {
                        addStack { "beryl".toCompoundStack(quantity = 2) }
                        addStack { "cobalt_aluminate".toCompoundStack(quantity = 4) }
                    }
                }
            })
        }

        addDissolverRecipesForAlloy("Bronze", "copper", 3, "tin", 1, conservationOfMass = true)
        addDissolverRecipesForAlloy("Electrum", "gold", 1, "silver", 1, conservationOfMass = true)
        addDissolverRecipesForAlloy("ElectricalSteel", "iron", 1, "carbon", 1, "silicon", 1, conservationOfMass = false)
        addDissolverRecipesForAlloy("Invar", "iron", 2, "copper", 1, conservationOfMass = true)


        listOf("gemRuby", "dustRuby", "plateRuby")
                .filter { oreExists(it) }
                .forEach { ore ->
                    dissolverRecipes.add(dissolverRecipe {
                        input = ore.toOre()
                        output {
                            addGroup {
                                addStack { "aluminum_oxide".toCompoundStack(quantity = 16) }
                                addStack { "chromium".toElementStack(quantity = 8) }
                            }
                        }
                    })
                }

        listOf("gemSapphire", "dustSapphire", "plateSapphire")
                .filter { oreExists(it) }
                .forEach { ore ->
                    dissolverRecipes.add(dissolverRecipe {
                        input = ore.toOre()
                        output {
                            addGroup {
                                addStack { "aluminum_oxide".toCompoundStack(quantity = 16) }
                                addStack { "iron".toElementStack(quantity = 4) }
                                addStack { "titanium".toElementStack(quantity = 4) }

                            }
                        }
                    })
                }

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.MELON_BLOCK.toIngredient()
            output {
                addGroup {
                    relativeProbability = false
                    probability = 50
                    addStack { "cucurbitacin".toCompoundStack(); }
                }
                addGroup {
                    addStack { "water".toCompoundStack(quantity = 4) }
                    addStack { "sucrose".toCompoundStack(quantity = 2) }
                }
            }
        })

        if (oreExists("itemSalt") && OreDictionary.getOres("itemSalt").isNotEmpty()) {
            dissolverRecipes.add(dissolverRecipe {
                input = "itemSalt".toOre()
                reversible = true
                output {
                    addStack { "sodium_chloride".toCompoundStack(quantity = 16) }
                }
            })
        }
        dissolverRecipes.add(dissolverRecipe {
            input = "blockCactus".toOre()
            reversible = true
            output {
                addStack { "cellulose".toCompoundStack() }
                addStack { "mescaline".toCompoundStack() }
            }
        })

        dissolverRecipes.add(dissolverRecipe {
            input = Blocks.HARDENED_CLAY.toIngredient()
            reversible = true
            output {
                addStack { "mullite".toCompoundStack(quantity = 2) }
            }
        })

        (0 until 16).forEach {
            dissolverRecipes.add(dissolverRecipe {
                input = Blocks.STAINED_HARDENED_CLAY.toIngredient(meta = it)
                reversible = false
                output {
                    addStack { "mullite".toCompoundStack(quantity = 2) }
                }
            })
        }
        listOf(Blocks.BLACK_GLAZED_TERRACOTTA,
                Blocks.BLUE_GLAZED_TERRACOTTA,
                Blocks.BROWN_GLAZED_TERRACOTTA,
                Blocks.CYAN_GLAZED_TERRACOTTA,
                Blocks.GRAY_GLAZED_TERRACOTTA,
                Blocks.GREEN_GLAZED_TERRACOTTA,
                Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
                Blocks.LIME_GLAZED_TERRACOTTA,
                Blocks.MAGENTA_GLAZED_TERRACOTTA,
                Blocks.ORANGE_GLAZED_TERRACOTTA,
                Blocks.PINK_GLAZED_TERRACOTTA,
                Blocks.PURPLE_GLAZED_TERRACOTTA,
                Blocks.RED_GLAZED_TERRACOTTA,
                Blocks.SILVER_GLAZED_TERRACOTTA,
                Blocks.WHITE_GLAZED_TERRACOTTA,
                Blocks.YELLOW_GLAZED_TERRACOTTA).forEach {
            dissolverRecipes.add(dissolverRecipe {
                input = it.toIngredient()
                reversible = false
                output {
                    addStack { "mullite".toCompoundStack(quantity = 2) }
                }
            })
        }

        if (oreExists("cropRice")) {
            dissolverRecipes.add(dissolverRecipe {
                input = "cropRice".toOre()
                output {
                    addGroup {
                        relativeProbability = false
                        probability = 10
                        addStack { "starch".toCompoundStack(); }
                    }
                }
            })
        }
    }

    fun Fluid.toStack(quantity: Int): FluidStack = FluidStack(this, quantity)

    fun initElectrolyzerRecipes() {
        electrolyzerRecipes.add(ElectrolyzerRecipe(
                inputFluid = FluidRegistry.WATER.toStack(quantity = 125),
                electrolyteInternal = "calcium_carbonate".toCompoundStack(),
                electrolyteConsumptionChanceInternal = 20,
                outputOne = "hydrogen".toElementStack(4),
                outputTwo = "oxygen".toElementStack(2)))

        electrolyzerRecipes.add(ElectrolyzerRecipe(
                inputFluid = FluidRegistry.WATER.toStack(125),
                electrolyteInternal = "sodium_chloride".toCompoundStack(),
                electrolyteConsumptionChanceInternal = 20,
                outputOne = "hydrogen".toElementStack(2),
                outputTwo = "oxygen".toElementStack(1),
                outputThree = "chlorine".toElementStack(2), output3Probability = 10))
    }


    fun initEvaporatorRecipes() {
        evaporatorRecipes.add(EvaporatorRecipe(FluidRegistry.WATER, 125, ModItems.mineralSalt.toStack()))
        FluidRegistry.getFluid("milk")?.let {
            evaporatorRecipes.add(EvaporatorRecipe(FluidRegistry.getFluid("milk"), 500, ModItems.condensedMilk.toStack()))
        }
    }


    fun initFuelHandler() {
        val fuelHandler = IFuelHandler { fuel ->
            if (fuel.item == ModItems.elements) {
                when (fuel.itemDamage) {
                    ElementRegistry["hydrogen"]!!.meta -> return@IFuelHandler 10
                    ElementRegistry["carbon"]!!.meta   -> return@IFuelHandler 200
                }
            }
            return@IFuelHandler 0
        }
        GameRegistry.registerFuelHandler(fuelHandler)
    }


    fun initCombinerRecipes() {

        combinerRecipes.add(CombinerRecipe(Items.COAL.toStack(meta = 1),
                listOf(ItemStack.EMPTY, "carbon".toElementStack(8))))


        metals.forEach { entry ->
            val dustOutput: ItemStack? = firstOre(entry.toDict("dust"))
            if (dustOutput != null && !dustOutput.isEmpty) {
                combinerRecipes.add(CombinerRecipe(dustOutput,
                        listOf(entry.toElementStack(4), entry.toElementStack(4), entry.toElementStack(4),
                                entry.toElementStack(4))))
            }

            val ingotOutput: ItemStack? = firstOre(entry.toDict("ingot"))
            if (ingotOutput != null && !ingotOutput.isEmpty) {
                combinerRecipes.add(CombinerRecipe(ingotOutput,
                        listOf(entry.toElementStack(16))))
            }
        }

        CompoundRegistry.compounds
                .filter { it.autoCombinerRecipe }
                .forEach { compound ->
                    if (compound.hasShiftedRecipe) {
                        val inputList: List<Any> = compound.toItemStackList().toMutableList().apply { this.add(0, ItemStack.EMPTY) }
                        combinerRecipes.add(CombinerRecipe(compound.toItemStack(1), inputList))
                    } else combinerRecipes.add(CombinerRecipe(compound.toItemStack(1), compound.toItemStackList()))
                }

        dissolverRecipes.filter { it.reversible }.forEach { recipe ->
            if (recipe.inputs.size > 0) combinerRecipes.add(CombinerRecipe(recipe.inputs[0], recipe.outputs.toStackList()))
        }


        val carbon = "carbon".toElementStack(quantity = 64)
        combinerRecipes.add(CombinerRecipe(Items.DIAMOND.toStack(),
                listOf(carbon, carbon, carbon,
                        carbon, null, carbon,
                        carbon, carbon, carbon)))

        combinerRecipes.add(CombinerRecipe(Blocks.SAND.toStack(),
                listOf("silicon_dioxide".toCompoundStack(), "silicon_dioxide".toCompoundStack(), null,
                        "silicon_dioxide".toCompoundStack(), "silicon_dioxide".toCompoundStack())))

        combinerRecipes.add(CombinerRecipe(Blocks.SAND.toStack(quantity = 8, meta = 1), //red sand
                listOf("silicon_dioxide".toCompoundStack(quantity = 8), "silicon_dioxide".toCompoundStack(quantity = 8), "iron_oxide".toCompoundStack(),
                        "silicon_dioxide".toCompoundStack(quantity = 8), "silicon_dioxide".toCompoundStack(quantity = 8))))

        combinerRecipes.add(CombinerRecipe(Blocks.COBBLESTONE.toStack(quantity = 2),
                listOf("silicon_dioxide".toCompoundStack())))

        combinerRecipes.add(CombinerRecipe(Blocks.STONE.toStack(),
                listOf(ItemStack.EMPTY, "silicon_dioxide".toCompoundStack())))

        combinerRecipes.add(CombinerRecipe(Blocks.OBSIDIAN.toStack(),
                listOf("magnesium_oxide".toCompoundStack(8), "potassium_chloride".toCompoundStack(8), "aluminum_oxide".toCompoundStack(8),
                        "silicon_dioxide".toCompoundStack(8), "silicon_dioxide".toCompoundStack(8), "silicon_dioxide".toCompoundStack(8)
                )))

        combinerRecipes.add(CombinerRecipe(Blocks.CLAY.toStack(),
                listOf(ItemStack.EMPTY, "kaolinite".toCompoundStack(4))))

        combinerRecipes.add(CombinerRecipe(Blocks.DIRT.toStack(4),
                listOf("water".toCompoundStack(), "cellulose".toCompoundStack(), "kaolinite".toCompoundStack())))

        combinerRecipes.add(CombinerRecipe(Blocks.GRASS.toStack(),
                listOf(null, null, null,
                        "water".toCompoundStack(), "cellulose".toCompoundStack(), "kaolinite".toCompoundStack())))

        combinerRecipes.add(CombinerRecipe(Blocks.GRAVEL.toStack(),
                listOf(null, null, "silicon_dioxide".toCompoundStack(1))))

        combinerRecipes.add(CombinerRecipe(Items.WATER_BUCKET.toStack(),
                listOf(null, null, null,
                        null, "water".toCompoundStack(16), null,
                        null, Items.BUCKET, null)))


        combinerRecipes.add(CombinerRecipe(Items.POTIONITEM.toStack()
                .apply { this.setTagInfo("Potion", net.minecraft.nbt.NBTTagString("water")) },
                listOf(null, null, null,
                        null, "water".toCompoundStack(16), null,
                        null, Items.GLASS_BOTTLE, null)))

        combinerRecipes.add(CombinerRecipe(Blocks.REDSTONE_BLOCK.toStack(),
                listOf(null, null, null,
                        "iron_oxide".toCompoundStack(36), "strontium_carbonate".toCompoundStack(36))))

        combinerRecipes.add(CombinerRecipe(Items.STRING.toStack(4),
                listOf(null, "protein".toCompoundStack(), null,
                        null, "protein".toCompoundStack())))

        combinerRecipes.add(CombinerRecipe(Blocks.WOOL.toStack(),
                listOf(null, null, null,
                        null, null, null,
                        "protein".toCompoundStack(), null, "protein".toCompoundStack())))



        combinerRecipes.add(CombinerRecipe(Items.REEDS.toStack(),
                listOf(null, null, null,
                        "cellulose".toCompoundStack(), "sucrose".toCompoundStack())))

        combinerRecipes.add(CombinerRecipe(Blocks.STONE.toStack(meta = 1), //granite
                listOf(null, null, null,
                        "silicon_dioxide".toCompoundStack(1))))

        combinerRecipes.add(CombinerRecipe(Blocks.STONE.toStack(meta = 3), //diorite
                listOf(null, null, null,
                        null, "silicon_dioxide".toCompoundStack(1))))

        combinerRecipes.add(CombinerRecipe(Blocks.STONE.toStack(meta = 5), //andesite
                listOf(null, null, null,
                        null, null, "silicon_dioxide".toCompoundStack(1))))

        combinerRecipes.add(CombinerRecipe(Items.FLINT.toStack(),
                listOf(null, null, null,
                        null, null, null,
                        null, "silicon_dioxide".toCompoundStack(4), null)))

        combinerRecipes.add(CombinerRecipe(Items.POTATO.toStack(),
                listOf("starch".toCompoundStack(), "potassium".toCompoundStack(4))))

        combinerRecipes.add(CombinerRecipe(Items.APPLE.toStack(),
                listOf(null, "cellulose".toCompoundStack(), null,
                        null, "sucrose".toCompoundStack(1), null)))

        combinerRecipes.add(CombinerRecipe(ModItems.fertilizer.toStack(8),
                listOf("urea".toCompoundStack(4),
                        "diammonium_phosphate".toCompoundStack(4),
                        "potassium_chloride".toCompoundStack(4))))

        if (oreExists("gemRuby")) {
            val rubyStack = firstOre("gemRuby")
            combinerRecipes.add(CombinerRecipe(rubyStack,
                    listOf("aluminum_oxide".toCompoundStack(16), "chromium".toElementStack(8))))
        }

        if (oreExists("gemSapphire")) {
            combinerRecipes.add(CombinerRecipe(firstOre("gemSapphire"),
                    listOf("aluminum_oxide".toCompoundStack(16),
                            "iron".toElementStack(4),
                            "titanium".toElementStack(4))))
        }

        val seeds = listOf(Items.WHEAT_SEEDS.toStack(),
                Items.PUMPKIN_SEEDS.toStack(),
                Items.MELON_SEEDS.toStack(),
                Items.BEETROOT_SEEDS.toStack())

        seeds.withIndex().forEach { (index: Int, stack: ItemStack) ->
            val inputs = mutableListOf(null, "triglyceride".toCompoundStack(), null)
            (0 until index).forEach { inputs.add(null) }
            inputs.add("sucrose".toCompoundStack())
            combinerRecipes.add(CombinerRecipe(stack, inputs))
        }

        Item.REGISTRY.getObject(ResourceLocation("forestry", "iodine_capsule"))?.let {
            combinerRecipes.add(CombinerRecipe(it.toStack(),
                    listOf(null, null, null,
                            "iodine".toElementStack(8), "iodine".toElementStack(8))))
        }



        (0..5).forEach { i ->
            val input = (0 until i).mapTo(ArrayList<ItemStack>(), { ItemStack.EMPTY })
            input.add("cellulose".toCompoundStack())
            input.add("cellulose".toCompoundStack())
            combinerRecipes.add(CombinerRecipe(Blocks.SAPLING.toStack(quantity = 4, meta = i), input))
        }

        (0 until 6).forEach { i ->
            val input = (0 until i).mapTo(ArrayList<ItemStack>(), { ItemStack.EMPTY })
            input.add("cellulose".toCompoundStack())

            //y u gotta do dis mojang
            if (i < 4) combinerRecipes.add(CombinerRecipe(ItemStack(Blocks.LOG, 1, i), input))
            else combinerRecipes.add(CombinerRecipe(ItemStack(Blocks.LOG2, 1, i - 4), input))
        }
    }

    fun initAtomizerRecipes() {
        atomizerRecipes.add(AtomizerRecipe(FluidStack(FluidRegistry.WATER, 500), "water".toCompoundStack(8)))

        if (fluidExists("canolaoil")) {
            atomizerRecipes.add(AtomizerRecipe(
                    FluidRegistry.getFluidStack("canolaoil", 500)!!, "triglyceride".toCompoundStack(1)))
        }

        ElementRegistry.getAllElements().forEach {
            if (fluidExists(it.name)) {
                atomizerRecipes.add(AtomizerRecipe(
                        FluidRegistry.getFluidStack(it.name, 500)!!, it.name.toElementStack(8)))
            }
        }
    }

    fun initLiquifierRecipes() {
        liquifierRecipes.add(LiquifierRecipe("water".toCompoundStack(8), FluidStack(FluidRegistry.WATER, 500)))

        ElementRegistry.getAllElements().forEach {
            if (fluidExists(it.name)) {
                liquifierRecipes.add(LiquifierRecipe(
                        it.name.toElementStack(8), FluidRegistry.getFluidStack(it.name, 500)!!
                ))
            }
        }

    }
}

fun fluidExists(name: String): Boolean = FluidRegistry.isFluidRegistered(name)

fun Item.toIngredient(quantity: Int = 1, meta: Int = 0): Ingredient = Ingredient.fromStacks(this.toStack(quantity, meta))
fun Block.toIngredient(quantity: Int = 1, meta: Int = 0): Ingredient = Ingredient.fromStacks(this.toStack(quantity, meta))
fun ItemStack.toIngredient() = Ingredient.fromStacks(this)
fun String.toOre(): OreIngredient = OreIngredient(this)
