package eu.pb4.brewery.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.block.Block;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class TexturedPolymerBlockItem extends BlockItem implements PolymerItem {
    private final PolymerModelData modelData;

    public TexturedPolymerBlockItem(Block block, Settings settings, Identifier modelPath) {
        super(block, settings);
        this.modelData = PolymerResourcePackUtils.requestModel(Items.PAPER, modelPath);
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return this.modelData.item();
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return this.modelData.value();
    }
}
