package eu.pb4.brewery.duck;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public interface StatusEffectInstanceExt {
    static Codec<StatusEffectInstance> codec(Codec<StatusEffectInstance> codec) {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<StatusEffectInstance, T>> decode(DynamicOps<T> ops, T input) {
                var decoded = codec.decode(ops, input);

                if (decoded.result().isPresent()) {
                    ops.get(input, "brewery:locked").result().ifPresent(x -> {
                         ((StatusEffectInstanceExt) decoded.getOrThrow().getFirst()).brewery$setLocked(ops.getBooleanValue(x).getOrThrow());
                    });
                }

                return decoded;
            }

            @Override
            public <T> DataResult<T> encode(StatusEffectInstance input, DynamicOps<T> ops, T prefix) {
                var encoded = codec.encode(input, ops, prefix);

                if (encoded.result().isPresent() && ((StatusEffectInstanceExt) input).brewery$isLocked()) {
                    ops.set(encoded.getOrThrow(), "brewery:locked", ops.createBoolean(true));
                }

                return encoded;
            }
        };
    }

    void brewery$setLocked(boolean value);
    boolean brewery$isLocked();
}
