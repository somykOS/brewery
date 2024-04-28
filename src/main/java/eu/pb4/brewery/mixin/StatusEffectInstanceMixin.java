package eu.pb4.brewery.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import eu.pb4.brewery.duck.StatusEffectInstanceExt;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffectInstance.class)
public class StatusEffectInstanceMixin implements StatusEffectInstanceExt {
    @Unique
    private boolean brewery$locked;

    @Override
    public void brewery$setLocked(boolean value) {
        this.brewery$locked = value;
    }

    @Override
    public boolean brewery$isLocked() {
        return this.brewery$locked;
    }

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"))
    private static Codec<StatusEffectInstance> replaceCodec(Codec<StatusEffectInstance> codec) {
        return StatusEffectInstanceExt.codec(codec);
    }
}
