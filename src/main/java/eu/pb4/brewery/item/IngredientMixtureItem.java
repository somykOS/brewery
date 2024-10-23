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
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

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
                tooltip.add(Text.translatable("text.brewery.age", BrewUtils.fromTimeShort(age).formatted(Formatting.GRAY)));
            }

            tooltip.add(Text.translatable("text.brewery.cooked_for", BrewUtils.fromTimeShort(time / 20d / mult).formatted(Formatting.GRAY)));
            for (var ingredient : getIngredients(stack)) {
                tooltip.add(Text.empty().append("" + ingredient.getCount()).append(" × ").append(ingredient.getName()).formatted(Formatting.GRAY));
            }
        }
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.POTION;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
        out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(),
                Optional.of(3694022), List.of(), Optional.empty()));

        if (!out.contains(DataComponentTypes.CUSTOM_NAME)) {
            out.set(DataComponentTypes.CUSTOM_NAME, Text.empty().append(out.get(DataComponentTypes.ITEM_NAME)).setStyle(Style.EMPTY.withItalic(false)));
        }
        return out;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return null;
    }
}
