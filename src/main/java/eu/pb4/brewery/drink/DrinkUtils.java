package eu.pb4.brewery.drink;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.item.BrewComponents;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.brewery.item.comp.BrewData;
import eu.pb4.brewery.item.comp.CookingData;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.brewery.other.BrewUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DrinkUtils {
    @Nullable
    public static DrinkType getType(ItemStack stack) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            if (Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).type().isPresent()) {
                return BreweryInit.DRINK_TYPES.get(Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).type().get());
            }
        }

        return null;
    }

    public static double getQuality(ItemStack stack) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).quality();
        }

        return -1;
    }

    public static String getBarrelType(ItemStack stack) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).barrelType();
        }

        return "";
    }

    public static boolean getDistillationStatus(ItemStack stack) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            var type = getType(stack);
            if (type != null) {
                return Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).distillations() >= type.distillationRuns();
            }
        }

        return false;
    }

    @Nullable
    public static Block getHeatSource(ItemStack stack) {
        if (stack.contains(BrewComponents.COOKING_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.COOKING_DATA)).heatSource();
        }

        return Blocks.FIRE;
    }

    public static boolean canBeDistillated(ItemStack stack) {
        var type = DrinkUtils.getType(stack);
        var already = DrinkUtils.getDistillationStatus(stack);
        return !already && ((type != null && type.requireDistillation()) || stack.isOf(BrewItems.INGREDIENT_MIXTURE));
    }

    public static double getAgeInTicks(ItemStack stack) {
        return getAgeInTicks(stack, Double.MIN_VALUE);
    }

    public static double getAgeInTicks(ItemStack stack, double defaultValue) {
        if (stack.contains(BrewComponents.BREW_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.BREW_DATA)).age();
        }

        return defaultValue;
    }

    public static double getAgeInSeconds(ItemStack stack) {
        return getAgeInTicks(stack) / 20d;
    }

    public static ItemStack createDrink(Identifier type, int age, double quality, int distillated, Block heatingSource) {
        var stack = new ItemStack(BrewItems.DRINK_ITEM);

        stack.set(BrewComponents.BREW_DATA, new BrewData(Optional.of(type), quality, "", distillated, age));
        stack.set(BrewComponents.COOKING_DATA, new CookingData(0, List.of(), heatingSource));

        return stack;
    }

    public static ItemStack appendLore(ItemStack polymerItemStack, ItemStack itemStack){
        var world = BreweryInit.getOverworld();
        if (world != null) {
            var type = DrinkUtils.getType(itemStack);
            //LoreComponent lore = stack.getOrDefault(DataComponentTypes.LORE, new LoreComponent(new ArrayList<>()));
            LoreComponent lore = polymerItemStack.getComponents().get(DataComponentTypes.LORE);
            if(lore == null) {
                // Create new LoreComponent with mutable ArrayList
                lore = new LoreComponent(new ArrayList<>());
            } else {
                // Create new LoreComponent with mutable copy of existing lines
                lore = new LoreComponent(new ArrayList<>(lore.lines()));
            }

            if (type != null && type.showQuality() && world.getGameRules().getBoolean(BrewGameRules.SHOW_QUALITY)) {
                var quality
                        = DrinkUtils.getQuality(itemStack);
                var starCount = (Math.round((quality / 2) * 10)) / 10d;

                StringBuilder stars = new StringBuilder();
                StringBuilder antistars = new StringBuilder();

                while (starCount >= 1) {
                    stars.append("⭐");
                    starCount--;
                }

                if (starCount > 0) {
                    stars.append("☆");
                }

                var starsLeft = 5 - stars.length();
                for (int i = 0; i < starsLeft; i++) {
                    antistars.append("☆");
                }

                lore.lines().add(Text.translatable("text.brewery.quality", Text.empty()
                        .append(Text.literal(stars.toString()).formatted(Formatting.YELLOW))
                        .append(Text.literal(antistars.toString()).formatted(Formatting.DARK_GRAY))
                ).formatted(Formatting.DARK_GRAY));
            }

            if (world.getGameRules().getBoolean(BrewGameRules.SHOW_AGE)) {
                double mult = world != null ? world.getGameRules().get(BrewGameRules.BARREL_AGING_MULTIPLIER).get() : 1;

                var age = DrinkUtils.getAgeInSeconds(itemStack) / mult;
                if (age > 0) {
                    lore.lines().add(Text.translatable("text.brewery.age", BrewUtils.fromTimeShort(age)).formatted(Formatting.DARK_GRAY));
                }
            }

            if (BreweryInit.DISPLAY_DEV) {
                lore.lines().add(Text.literal("== DEV ==").formatted(Formatting.AQUA));
                lore.lines().add(Text.literal("BrewType: ").append(itemStack.getOrDefault(BrewComponents.BREW_DATA, BrewData.DEFAULT).type().toString()).formatted(Formatting.GRAY));
                lore.lines().add(Text.literal("BrewQuality: ").append("" + DrinkUtils.getQuality(itemStack)).formatted(Formatting.GRAY));
                lore.lines().add(Text.literal("BrewAge: ").append("" + DrinkUtils.getAgeInTicks(itemStack)).formatted(Formatting.GRAY));
                lore.lines().add(Text.literal("BrewDistillated: ").append("" + DrinkUtils.getDistillationStatus(itemStack)).formatted(Formatting.GRAY));
            }

            //System.out.println(lore.lines());
            polymerItemStack.set(DataComponentTypes.LORE, lore);
        }
        return polymerItemStack;
    }

    public static List<DrinkType> findTypes(List<ItemStack> ingredients, Identifier barrelType, Block heatSource) {
        if (ingredients.isEmpty()) {
            return List.of();
        }
        var list = new ArrayList<DrinkType>();
        base:
        for (var type : BreweryInit.DRINK_TYPES.values()) {
            if (((barrelType == null && type.barrelInfo().isEmpty()) || (barrelType != null && type.getBarrelInfo(barrelType) != null))
                    && !type.ingredients().isEmpty() && (type.heatSource().isEmpty() || type.heatSource().get().contains(Registries.BLOCK.getEntry(heatSource)))) {
                var ing = new ArrayList<ItemStack>(ingredients.size());
                for (var i : ingredients) {
                    ing.add(new ItemStack(i.getItem(), i.getCount()));
                }

                for (var ingredient : type.ingredients()) {
                    int count = ingredient.count();
                    for (var stack : ing) {
                        if (!stack.isEmpty() && ingredient.items().contains(stack.getItem())) {
                            count -= stack.getCount();
                            if (count < 0) {
                                continue base;
                            } else {
                                stack.setCount(0);
                            }
                        }
                    }

                    if (count != 0) {
                        continue base;
                    }
                }
                list.add(type);
            }
        }

        return list;
    }
}
