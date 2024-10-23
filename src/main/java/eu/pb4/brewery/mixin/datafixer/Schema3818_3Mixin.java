package eu.pb4.brewery.mixin.datafixer;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.Schema3818_3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SequencedMap;
import java.util.function.Supplier;

@Mixin(Schema3818_3.class)
public class Schema3818_3Mixin {
    @Inject(method = "method_63573", at = @At("TAIL"))
    private static void addCustomComponents(Schema schema, CallbackInfoReturnable<SequencedMap<String, Supplier<TypeTemplate>>> cir) {
        cir.getReturnValue().put("brewery:cooking_data", () -> DSL.optionalFields(
                "ingredients", DSL.list(TypeReferences.ITEM_STACK.in(schema)),
                "heat_source", TypeReferences.BLOCK_NAME.in(schema)
        ));
    }
}
