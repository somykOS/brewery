package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class IngredientMixtureItem extends Item implements PolymerItem {
    public IngredientMixtureItem(Item.Settings settings) {
        super(settings);
    }

    public static List<ItemStack> getIngredients(ItemStack stack) {
        if (stack.contains(BrewComponents.COOKING_DATA)) {
            return Objects.requireNonNull(stack.get(BrewComponents.COOKING_DATA)).ingredients();
        }
        return List.of();
    }

    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        var world = BreweryInit.getOverworld();

        if (stack.contains(BrewComponents.COOKING_DATA) && world != null && world.getGameRules().getBoolean(BrewGameRules.SHOW_AGE)) {
            var time = Objects.requireNonNull(stack.get(BrewComponents.COOKING_DATA)).time();
            double mult = world.getGameRules().get(BrewGameRules.CAULDRON_COOKING_TIME_MULTIPLIER).get();

            var age = DrinkUtils.getAgeInSeconds(stack) / mult;

            if (age > 0) {
                tooltip.add(Text.translatable("text.brewery.age", BrewUtils.fromTimeShort(age)).formatted(Formatting.DARK_GRAY));
            }

            tooltip.add(Text.translatable("text.brewery.cooked_for", BrewUtils.fromTimeShort(time / 20d / mult)).formatted(Formatting.DARK_GRAY));
            for (var ingredient : getIngredients(stack)) {
                tooltip.add(Text.empty().append("" + ingredient.getCount()).append(" × ").append(ingredient.getName()).formatted(Formatting.DARK_GRAY));
            }
        }
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.POTION;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType context, RegistryWrapper.WrapperLookup lookup, @Nullable ServerPlayerEntity player) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, context, lookup, player);
        out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(),
                Optional.of(3694022), List.of()));
        return out;
    }
}
