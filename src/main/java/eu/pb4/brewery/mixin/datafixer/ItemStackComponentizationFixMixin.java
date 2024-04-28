package eu.pb4.brewery.mixin.datafixer;

import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.fix.ItemStackComponentizationFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(ItemStackComponentizationFix.class)
public class ItemStackComponentizationFixMixin {
    @Inject(method = "fixStack", at = @At("TAIL"))
    private static void fixCustomStacks(ItemStackComponentizationFix.StackData data, Dynamic dynamic, CallbackInfo ci) {
        if (data.itemEquals("brewery:ingredient_mixture") || data.itemEquals("brewery:drink_bottle")) {
            data.setComponent("brewery:cooking_data", dynamic.emptyMap()
                    .set("time", data.getAndRemove("BrewCookAge").result().orElse(dynamic.createDouble(0)))
                    .set("heat_source", data.getAndRemove("BrewHeatSource").result().orElse(dynamic.createString("air")))
                    .set("ingredients", data.getAndRemove("Ingredients").result().orElse(dynamic.emptyList()))
            );
        }
        if (data.itemEquals("brewery:drink_bottle")) {
            data.setComponent("brewery:brew_data", dynamic.emptyMap()
                    .set("age", data.getAndRemove("BrewAge").result().orElse(dynamic.createDouble(0)))
                    .set("quality", data.getAndRemove("BrewQuality").result().orElse(dynamic.createDouble(0)))
                    .set("barrel", data.getAndRemove("BrewBarrelType").result().orElse(dynamic.createString("")))
                    .set("type", data.getAndRemove("BrewType").result().orElse(dynamic.createString("")))
                    .set("distillation_runs", data.getAndRemove("BrewDistillated").result().orElse(dynamic.createInt(0)))
            );
        }
    }
}
