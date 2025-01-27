package eu.pb4.brewery.block;

import com.mojang.serialization.MapCodec;
import eu.pb4.brewery.block.entity.BrewBarrelSpigotBlockEntity;
import eu.pb4.brewery.block.entity.BrewBlockEntities;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import static eu.pb4.brewery.BreweryInit.MOD_ID;

public final class BrewSpigotBlock extends HorizontalFacingBlock implements PolymerBlock, BlockEntityProvider, BlockWithElementHolder {
    private static final MapCodec<BrewSpigotBlock> CODEC = createCodec(BrewSpigotBlock::new);
    private final ItemStack model;

    public BrewSpigotBlock(Settings settings) {
        super(settings);
        model = BaseItemProvider.requestModel(Identifier.of(MOD_ID, "block/spigot"));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.STRUCTURE_VOID.getDefaultState();
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        var facing = state.get(FACING);

        for (var pattern : BrewBlocks.BARREL_PATTERNS) {
            var result = pattern.getRight().testTransform(world, pos.offset(facing)
                    .add(facing.getOffsetZ(), 1, -facing.getOffsetX()), facing, Direction.UP);
            if (result != null) {
                var mat = pattern.getLeft();
                var partBlockType = BrewBlocks.BARREL_PARTS.get(mat.type());

                var be = world.getBlockEntity(pos);
                if (be instanceof BrewBarrelSpigotBlockEntity spigotBlock) {
                    spigotBlock.setBarrelType(pattern.getLeft());
                    for (var x = 0; x < result.getWidth(); x++) {
                        for (var y = 0; y < result.getHeight(); y++) {
                            for (var z = 0; z < result.getDepth(); z++) {
                                var blockPosition = result.translate(x, y, z);
                                var partState = partBlockType.getState(blockPosition.getBlockState().getBlock(), x, y, facing);
                                if (partState != null) {
                                    world.setBlockState(blockPosition.getBlockPos(), partState, 2);
                                    spigotBlock.addPart(blockPosition.getBlockPos());
                                    world.getBlockEntity(blockPosition.getBlockPos(), BrewBlockEntities.BARREL_PART).get().setContainer(pos);
                                }
                            }
                        }
                    }
                }
            }
        }

        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (ctx.getSide().getAxis() != Direction.Axis.Y) {
            for (var pattern : BrewBlocks.BARREL_PATTERNS) {
                var result = pattern.getRight().testTransform(ctx.getWorld(), ctx.getBlockPos().offset(ctx.getSide().getOpposite())
                        .add(-ctx.getSide().getOffsetZ(), 1, ctx.getSide().getOffsetX()), ctx.getSide().getOpposite(), Direction.UP);
                if (result != null) {
                    return this.getDefaultState().with(FACING, ctx.getSide().getOpposite());
                }
            }
        }

        return null;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,  BlockHitResult hit) {
        var blockEntity = world.getBlockEntity(pos);

        if (blockEntity instanceof BrewBarrelSpigotBlockEntity barrelBlock && barrelBlock.canPlayerUse(player)) {
            barrelBlock.openGui((ServerPlayerEntity) player);
            world.playSound(null,
                    barrelBlock.getPos().getX() + 0.5,
                    barrelBlock.getPos().getY() + 0.5,
                    barrelBlock.getPos().getZ() + 0.5, SoundEvents.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            var blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof BrewBarrelSpigotBlockEntity barrelBlock) {
                ItemScatterer.spawn(world, pos, barrelBlock);
                world.updateComparators(pos, this);

                for (var part : barrelBlock.iterableParts()) {
                    var partState = world.getBlockState(part);

                    if (partState.getBlock() instanceof BrewBarrelPartBlock block) {
                        world.setBlockState(part, partState.get(BrewBarrelPartBlock.SHAPE).state.apply(block.barrelMaterial));
                    }
                }
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BrewBarrelSpigotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return BrewBarrelSpigotBlockEntity::ticker;
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        Model finalModel = new Model(model);
        finalModel.setDirection(initialBlockState.get(FACING).getOpposite());
        return finalModel;
    }

    private static final class Model extends BlockModel {

        private final ItemDisplayElement mainElement;

        private Model(ItemStack model) {
            mainElement = ItemDisplayElementUtil.createSimple(model);
//            mainElement.setScale(new Vector3f(3f));
//            mainElement.setOffset(new Vec3d(0f, -0.16f, -0.13f));
            this.addElement(mainElement);
        }

        public void setDirection(Direction direction) {
            Matrix4f matrix = new Matrix4f();
            matrix.rotateY((float) Math.toRadians(-direction.asRotation()));
            mainElement.setTransformation(matrix);
        }
    }
}
