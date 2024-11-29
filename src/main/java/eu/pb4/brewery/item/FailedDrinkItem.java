package eu.pb4.brewery.item;

import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.Optional;

public class FailedDrinkItem extends Item implements PolymerItem {
    public FailedDrinkItem(Item.Settings settings) {
        super(settings.food(new FoodComponent.Builder().alwaysEdible().saturationModifier(-0.2f).build(),
                ConsumableComponent.builder()
                        .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.HUNGER, 30 * 20), 0.95f))
                        .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 10 * 20), 0.80f))
                        .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10 * 20), 0.60f))
                        .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10 * 20, 1), 0.30f))
                        .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 10 * 20), 0.60f))
                        .consumeSeconds(32 / 20f)
                        .consumeParticles(false)
                        .useAction(UseAction.DRINK)
                        .build()
        ));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return Items.TRIAL_KEY;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return Items.POTION.getComponents().get(DataComponentTypes.ITEM_MODEL);
    }


    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context) {
        out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(),
                Optional.of(0x051a0a), List.of(), Optional.empty()));

        var type = DrinkUtils.getType(stack);

        out.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
                FloatList.of(0, (float) DrinkUtils.getAgeInSeconds(stack), (float) DrinkUtils.getCookingAgeInSeconds(stack), DrinkUtils.getDistillationCount(stack)),
                BooleanList.of(false, false),
                List.of(type != null ? DrinkUtils.getTypeId(stack).toString() : "", DrinkUtils.getBarrelType(stack), "failed_drink"),
                IntList.of(0x051a0a, type != null ? type.color().getRgb() : 0x051a0a))
        );
    }
}
