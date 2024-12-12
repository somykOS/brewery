package eu.pb4.brewery.drink;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.brewery.other.BrewUtils;
import eu.pb4.brewery.other.WrappedText;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.Block;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryCodecs;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static eu.pb4.brewery.BreweryInit.id;


public record DrinkType(WrappedText name, TextColor color, ItemLookData visuals, List<BarrelInfo> barrelInfo, WrappedExpression baseQuality,
                        WrappedExpression alcoholicValue, List<ConsumptionEffect> consumptionEffects,
                        WrappedExpression cookingQualityMult, List<BrewIngredient> ingredients, int distillationRuns,
                        List<ConsumptionEffect> unfinishedEffects, Optional<DrinkInfo> info, boolean showQuality,
                        Optional<RegistryEntryList<Block>> heatSource) {
    public static MapCodec<DrinkType> CODEC_V2 = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WrappedText.CODEC.fieldOf("name").forGetter(DrinkType::name),
            TextColor.CODEC.fieldOf("color").forGetter(DrinkType::color),
            ItemLookData.CODEC.optionalFieldOf("visual", ItemLookData.DEFAULT).forGetter(DrinkType::visuals),
            Codec.list(BarrelInfo.CODEC_V1).optionalFieldOf("barrel_definitions", List.of()).forGetter(DrinkType::barrelInfo),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("base_quality_value").forGetter(DrinkType::baseQuality),
            ExpressionUtil.COMMON_EXPRESSION.fieldOf("alcoholic_value").forGetter(DrinkType::alcoholicValue),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("entries", new ArrayList<>()).forGetter(DrinkType::consumptionEffects),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("cooking_quality_multiplier").forGetter(DrinkType::cookingQualityMult),
            Codec.list(BrewIngredient.CODEC_V1).optionalFieldOf("ingredients", new ArrayList<>()).forGetter(DrinkType::ingredients),
            Codec.INT.optionalFieldOf("distillation_runs", 0).forGetter(DrinkType::distillationRuns),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("unfinished_brew_effects", new ArrayList<>()).forGetter(DrinkType::unfinishedEffects),
            DrinkInfo.CODEC.optionalFieldOf("book_information").forGetter(DrinkType::info),
            Codec.BOOL.optionalFieldOf("show_quality", true).forGetter(DrinkType::showQuality),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).optionalFieldOf("required_heat_source").forGetter(DrinkType::heatSource)
    ).apply(instance, DrinkType::new));

    public static MapCodec<DrinkType> CODEC_V1 = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WrappedText.CODEC.fieldOf("name").forGetter(DrinkType::name),
            TextColor.CODEC.fieldOf("color").forGetter(DrinkType::color),
            ItemLookData.CODEC.optionalFieldOf("visual", ItemLookData.DEFAULT).forGetter(DrinkType::visuals),
            Codec.list(BarrelInfo.CODEC_V1).optionalFieldOf("barrel_definitions", List.of()).forGetter(DrinkType::barrelInfo),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("base_quality_value").forGetter(DrinkType::baseQuality),
            ExpressionUtil.COMMON_EXPRESSION.fieldOf("alcoholic_value").forGetter(DrinkType::alcoholicValue),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("entries", new ArrayList<>()).forGetter(DrinkType::consumptionEffects),
            ExpressionUtil.createCodec(ExpressionUtil.AGE_KEY).fieldOf("cooking_quality_multiplier").forGetter(DrinkType::cookingQualityMult),
            Codec.list(BrewIngredient.CODEC_V1).optionalFieldOf("ingredients", new ArrayList<>()).forGetter(DrinkType::ingredients),
            Codec.BOOL.xmap(x -> x ? 1 : 0, i -> i == 1).optionalFieldOf("require_distillation", 0).forGetter(DrinkType::distillationRuns),
            Codec.list(ConsumptionEffect.CODEC).optionalFieldOf("unfinished_brew_effects", new ArrayList<>()).forGetter(DrinkType::unfinishedEffects),
            DrinkInfo.CODEC.optionalFieldOf("book_information").forGetter(DrinkType::info),
            Codec.BOOL.optionalFieldOf("show_quality", true).forGetter(DrinkType::showQuality),
            RegistryCodecs.entryList(RegistryKeys.BLOCK).optionalFieldOf("required_heat_source").forGetter(DrinkType::heatSource)
    ).apply(instance, DrinkType::new));
    public static final Codec<DrinkType> CODEC = new MapCodec.MapCodecCodec<>(new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString("version"));
        }

        @Override
        public <T> DataResult<DrinkType> decode(DynamicOps<T> ops, MapLike<T> input) {
            var version = ops.getNumberValue(input.get("version"), 0).intValue();
            return switch (version) {
                case 1 -> DrinkType.CODEC_V1.decode(ops, input);
                default -> DrinkType.CODEC_V2.decode(ops, input);
            };
        }

        @Override
        public <T> RecordBuilder<T> encode(DrinkType input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            return CODEC_V2.encode(input, ops, prefix.add("version", ops.createInt(2)));
        }
    });

    public static DrinkType create(Text name, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, DrinkInfo info) {
        return create(name, color, barrelInfo, quality, alcoholicValue, consumptionEffects, cookingTime, ingredients, false, List.of(), info);
    }


    public static DrinkType create(Text name, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, boolean requireDistillation, List<ConsumptionEffect> unfinishedEffects, DrinkInfo info) {
        return create(name, color, barrelInfo, quality, alcoholicValue, consumptionEffects, cookingTime, ingredients, requireDistillation ? 1 : 0, unfinishedEffects, info);

    }

    public static DrinkType create(Text name, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, int distillationRuns, List<ConsumptionEffect> unfinishedEffects, DrinkInfo info) {
        return create(name, color, barrelInfo, quality, alcoholicValue, consumptionEffects, cookingTime, ingredients, distillationRuns, unfinishedEffects, info, Optional.empty());
    }

    public static DrinkType create(Text name, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, int distillationRuns, List<ConsumptionEffect> unfinishedEffects, DrinkInfo info, TagKey<Block> heatSource) {
        return create(name, color, barrelInfo, quality, alcoholicValue, consumptionEffects, cookingTime, ingredients, distillationRuns, unfinishedEffects, info, Optional.of(RegistryEntryList.of(Registries.BLOCK.getEntryOwner(), heatSource)));
    }

    public static DrinkType create(Text name, TextColor color, List<BarrelInfo> barrelInfo, String quality, String alcoholicValue, List<ConsumptionEffect> consumptionEffects, String cookingTime, List<BrewIngredient> ingredients, int distillationRuns, List<ConsumptionEffect> unfinishedEffects, DrinkInfo info, Optional<RegistryEntryList<Block>> heatSource) {
        return new DrinkType(WrappedText.of(name), color, ItemLookData.DEFAULT, barrelInfo, WrappedExpression.create(quality, ExpressionUtil.AGE_KEY), WrappedExpression.createDefault(alcoholicValue), consumptionEffects, WrappedExpression.create(cookingTime, ExpressionUtil.AGE_KEY), ingredients, distillationRuns, unfinishedEffects, Optional.of(info), true, heatSource);
    }

    public boolean requireDistillation() {
        return this.distillationRuns > 0;
    }

    @Nullable
    public BarrelInfo getBarrelInfo(Identifier barrelType) {
        BarrelInfo def = null;
        for (var info : this.barrelInfo) {
            if (info.type.equals(barrelType)) {
                return info;
            } else if (info.type.equals(BarrelInfo.ANY)) {
                def = info;
            }
        }
        return def;
    }

    public boolean isFinished(ItemStack itemStack) {
        return DrinkUtils.getAgeInSeconds(itemStack) >= 0 && (!this.requireDistillation() || DrinkUtils.getDistillationStatus(itemStack));
    }

    public record BarrelInfo(Identifier type, WrappedExpression qualityChange, int baseTime) {
        public static final Identifier ANY = id("any_barrel");
        public static final Identifier NONE = id("none");
        protected static Codec<Identifier> TYPE_CODEC = Codec.STRING.xmap(x -> x.equals("*") ? ANY : BrewUtils.tryParsingId(x, NONE), x -> x.equals(ANY) ? "*" : x.toString());
        public static Codec<BarrelInfo> CODEC_V1 = RecordCodecBuilder.create(instance -> instance.group(TYPE_CODEC.fieldOf("type").forGetter(BarrelInfo::type), ExpressionUtil.COMMON_EXPRESSION.fieldOf("quality_value").forGetter(BarrelInfo::qualityChange), Codecs.POSITIVE_INT.fieldOf("reveal_time").forGetter(BarrelInfo::baseTime)).apply(instance, BarrelInfo::new));

        public static BarrelInfo of(String type, String qualityChange, int baseTime) {
            return of(type.equals("*") ? ANY : Identifier.of(type), qualityChange, baseTime);
        }

        public static BarrelInfo of(Identifier type, String qualityChange, int baseTime) {
            return new BarrelInfo(type, WrappedExpression.createDefault(qualityChange), baseTime);
        }
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public record ItemLookData(Item item, Optional<ComponentMap> components, Optional<PolymerModelData> model) {
        public static final ItemLookData DEFAULT = new ItemLookData(Items.POTION, Optional.empty(), Optional.empty());
        public static Codec<ItemLookData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Registries.ITEM.getCodec().optionalFieldOf("item", Items.POTION).forGetter(ItemLookData::item),
                ComponentMap.CODEC.optionalFieldOf("components").forGetter(ItemLookData::components),
                Identifier.CODEC.optionalFieldOf("model")
                        .forGetter(x -> x.model.map(PolymerModelData::modelPath))
        ).apply(instance, ItemLookData::create));

        private static ItemLookData create(Item item, Optional<ComponentMap> nbtCompound, Optional<Identifier> identifier) {
            return new ItemLookData(item, nbtCompound, identifier.map(value -> PolymerResourcePackUtils.requestModel(item, value)));
        }
    }

    public record BrewIngredient(List<Item> items, int count, ItemStack returnedItemStack) {
        public static Codec<BrewIngredient> CODEC_V1 = RecordCodecBuilder.create(instance -> instance.group(Codec.list(Registries.ITEM.getCodec()).fieldOf("items").forGetter(BrewIngredient::items), Codec.INT.optionalFieldOf("count", 1).forGetter(BrewIngredient::count), ItemStack.CODEC.optionalFieldOf("dropped_stack", ItemStack.EMPTY).forGetter(BrewIngredient::returnedItemStack)).apply(instance, BrewIngredient::new));

        public static BrewIngredient of(int count, Item... items) {
            return new BrewIngredient(List.of(items), count, ItemStack.EMPTY);
        }

        public static BrewIngredient of(int count, ItemStack stack, Item... items) {
            return new BrewIngredient(List.of(items), count, stack);
        }

        public static BrewIngredient of(Item... items) {
            return new BrewIngredient(List.of(items), 1, ItemStack.EMPTY);
        }
    }
}
