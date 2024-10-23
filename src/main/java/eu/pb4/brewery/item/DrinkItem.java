package eu.pb4.brewery.item;

import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.drink.AlcoholManager;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.drink.ExpressionUtil;
import eu.pb4.brewery.item.comp.BrewData;
import eu.pb4.brewery.other.BrewGameRules;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.Optional;

public class DrinkItem extends Item implements PolymerItem {
    public DrinkItem(Settings settings) {
        super(settings.maxCount(1).component(DataComponentTypes.CONSUMABLE, new ConsumableComponent(32 / 20f, UseAction.DRINK, SoundEvents.ENTITY_GENERIC_DRINK, false, List.of())));
    }

    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        PlayerEntity playerEntity = user instanceof PlayerEntity ? (PlayerEntity) user : null;
        if (playerEntity instanceof ServerPlayerEntity) {
            Criteria.CONSUME_ITEM.trigger((ServerPlayerEntity) playerEntity, stack);
        }

        if (!world.isClient) {
            try {
                var type = DrinkUtils.getType(stack);

                if (type != null) {
                    var age = DrinkUtils.getAgeInSeconds(stack);
                    var quality = DrinkUtils.getQuality(stack);

                    var alcoholicValue = type.alcoholicValue().expression()
                            .setVariable(ExpressionUtil.AGE_KEY, age)
                            .setVariable(ExpressionUtil.QUALITY_KEY, quality)
                            .evaluate();

                    AlcoholManager.of(user).drink(type, quality, alcoholicValue);

                    for (var effect : (type.isFinished(stack) ? type.consumptionEffects() : type.unfinishedEffects())) {
                        effect.apply(user, age, quality);
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        if (playerEntity != null) {
            playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
            if (!playerEntity.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }

        if (playerEntity == null || !playerEntity.getAbilities().creativeMode) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (playerEntity != null) {
                playerEntity.getInventory().insertStack(new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        user.emitGameEvent(GameEvent.DRINK);
        return stack;
    }

    @Override
    public Text getName(ItemStack stack) {
        var type = DrinkUtils.getType(stack);

        if (type != null) {
            if (type.isFinished(stack)) {
                return type.name().text();
            } else {
                return Text.translatable("item.brewery.ingredient_mixture_specific", type.name().text());
            }
        } else {
            var id = DrinkUtils.getType(stack);

            Text text;

            if (id != null) {
                text = id.name().text();
            } else {
                text = Text.literal("<Unknown>");
            }

            return Text.translatable(this.getTranslationKey(), text);
        }
    }

    public String getTranslationKey(ItemStack stack) {
        return this.getTranslationKey();
    }

    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType typex) {
        var world = BreweryInit.getOverworld();
        if (world != null) {
            var type = DrinkUtils.getType(stack);
            if (type != null && type.showQuality() && world.getGameRules().getBoolean(BrewGameRules.SHOW_QUALITY)) {
                var quality
                        = DrinkUtils.getQuality(stack);
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

                tooltip.add(Text.translatable("text.brewery.quality", Text.empty()
                        .append(Text.literal(stars.toString()).formatted(Formatting.YELLOW))
                        .append(Text.literal(antistars.toString()).formatted(Formatting.DARK_GRAY))
                ));
            }

            if (world.getGameRules().getBoolean(BrewGameRules.SHOW_AGE)) {
                double mult = world != null ? world.getGameRules().get(BrewGameRules.BARREL_AGING_MULTIPLIER).get() : 1;

                var age = DrinkUtils.getAgeInSeconds(stack) / mult;
                if (age > 0) {
                    tooltip.add(Text.translatable("text.brewery.age", BrewUtils.fromTimeShort(age).formatted(Formatting.GRAY)));
                }
            }

            if (BreweryInit.DISPLAY_DEV) {
                tooltip.add(Text.literal("== DEV ==").formatted(Formatting.AQUA));
                tooltip.add(Text.literal("BrewType: ").append(stack.getOrDefault(BrewComponents.BREW_DATA, BrewData.DEFAULT).type().toString()).formatted(Formatting.GRAY));
                tooltip.add(Text.literal("BrewQuality: ").append("" + DrinkUtils.getQuality(stack)).formatted(Formatting.GRAY));
                tooltip.add(Text.literal("BrewAge: ").append("" + DrinkUtils.getAgeInTicks(stack)).formatted(Formatting.GRAY));
                tooltip.add(Text.literal("BrewDistillated: ").append("" + DrinkUtils.getDistillationStatus(stack)).formatted(Formatting.GRAY));
            }
        }
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        var type = DrinkUtils.getType(itemStack);
        if (type != null) {
            return type.visuals().item();
        }

        return Items.POTION;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
        var type = DrinkUtils.getType(itemStack);

        if (type != null) {
            int color;
            if (type.isFinished(itemStack)) {
                color = type.color().getRgb();
            } else {
                color = ColorHelper.lerp(0.5f, type.color().getRgb(), 0x385dc6);
            }
            out.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.of(), Optional.empty()));

            if (type.visuals().components().isPresent()) {
                out.applyComponentsFrom(type.visuals().components().get());
            }
        }

        if (!out.contains(DataComponentTypes.CUSTOM_NAME)) {
            out.set(DataComponentTypes.CUSTOM_NAME, Text.empty().append(out.get(DataComponentTypes.ITEM_NAME)).setStyle(Style.EMPTY.withItalic(false)));
        }

        return out;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        var type = DrinkUtils.getType(stack);

        if (type != null && type.visuals().model().isPresent()) {
            return type.visuals().model().get();
        }

        return null;
    }
}
