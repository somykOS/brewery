package eu.pb4.brewery.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import eu.pb4.brewery.drink.AlcoholManager;
import eu.pb4.brewery.duck.StatusEffectInstanceExt;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ClearAllEffectsConsumeEffect;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ConsumableComponent.class)
public class ConsumableComponentMixin {
    @Inject(method = "finishConsumption", at = @At("TAIL"))
    private void brewery$eat(World world, LivingEntity user, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        AlcoholManager.of(user).eat(stack);
    }
}
