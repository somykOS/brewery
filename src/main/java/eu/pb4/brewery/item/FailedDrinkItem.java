package eu.pb4.brewery.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UseAction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FailedDrinkItem extends Item implements PolymerItem {
    public FailedDrinkItem(Item.Settings settings) {
        super(settings.food(new FoodComponent.Builder().alwaysEdible().saturationModifier(-0.2f)
                .statusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 30 * 20), 0.95f)
                .statusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 10 * 20), 0.80f)
                .statusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10 * 20), 0.60f)
                .statusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10 * 20, 1), 0.30f)
                .statusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 10 * 20), 0.60f)
                .build()));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return Items.POTION;
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType context, RegistryWrapper.WrapperLookup lookup, @Nullable ServerPlayerEntity player) {
        var out =  PolymerItem.super.getPolymerItemStack(itemStack, context, lookup, player);
        out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(),
                Optional.of(0x051a0a), List.of()));
        return out;
    }
}
