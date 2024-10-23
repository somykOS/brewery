package eu.pb4.brewery.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import eu.pb4.brewery.duck.StatusEffectInstanceExt;
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

@Mixin(ClearAllEffectsConsumeEffect.class)
public class ClearAllEffectsConsumeEffectItemMixin {

    @Inject(method = "onConsume", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;clearStatusEffects()Z", shift = At.Shift.BEFORE))
    private void brewery$storeEffects(World world, ItemStack stack, LivingEntity user, CallbackInfoReturnable<Boolean> cir,
                                      @Share("storedEffects") LocalRef<List<StatusEffectInstance>> storedEffects) {
        var list = new ArrayList<StatusEffectInstance>();

        for (var effect : user.getStatusEffects()) {
            if (((StatusEffectInstanceExt) effect).brewery$isLocked()) {
                list.add(effect);
            }
        }
        storedEffects.set(list);
    }

    @Inject(method = "onConsume", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;clearStatusEffects()Z", shift = At.Shift.AFTER))
    private void brewery$restoreEffects(World world, ItemStack stack, LivingEntity user, CallbackInfoReturnable<Boolean> cir,
                                        @Share("storedEffects") LocalRef<List<StatusEffectInstance>> storedEffects) {
        for (var effect : storedEffects.get()) {
            user.addStatusEffect(effect);
        }
    }
}
