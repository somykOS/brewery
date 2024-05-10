package eu.pb4.brewery.item;

import com.mojang.serialization.Codec;
import eu.pb4.brewery.item.comp.BrewData;
import eu.pb4.brewery.item.comp.CookingData;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.component.DataComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import static eu.pb4.brewery.BreweryInit.id;

public class BrewComponents {
    public static final DataComponentType<Integer> BOOK_PAGE = register("book_page",
            DataComponentType.<Integer>builder().codec(Codec.INT).build());

    public static final DataComponentType<Integer> TICK_COUNT = register("tick_count",
            DataComponentType.<Integer>builder().codec(Codec.INT).build());

    public static final DataComponentType<CookingData> COOKING_DATA = register("cooking_data",
            DataComponentType.<CookingData>builder().codec(CookingData.CODEC).build());

    public static final DataComponentType<BrewData> BREW_DATA = register("brew_data",
            DataComponentType.<BrewData>builder().codec(BrewData.CODEC).build());

    public static void register() {
    }

    private static <T> DataComponentType<T> register(String path, DataComponentType<T> block) {
        PolymerItemUtils.markAsPolymer(block);
        return Registry.register(Registries.DATA_COMPONENT_TYPE, id(path), block);
    }
}
