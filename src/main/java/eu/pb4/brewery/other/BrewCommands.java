package eu.pb4.brewery.other;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import eu.pb4.brewery.BreweryInit;
import eu.pb4.brewery.GenericModInfo;
import eu.pb4.brewery.block.entity.TickableContents;
import eu.pb4.brewery.drink.AlcoholManager;
import eu.pb4.brewery.drink.DefaultDefinitions;
import eu.pb4.brewery.drink.DrinkType;
import eu.pb4.brewery.drink.DrinkUtils;
import eu.pb4.brewery.item.BookOfBreweryItem;
import eu.pb4.brewery.item.BrewItems;
import eu.pb4.sgui.api.gui.BookGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import java.nio.file.Files;
import java.util.Locale;
import java.util.function.Function;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BrewCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                literal("brewery")
                        .executes(BrewCommands::about)
                        .then(literal("create")
                                .requires(Permissions.require("brewery.create", 2))
                                .then(argument("type", IdentifierArgumentType.identifier())
                                        .suggests((context, builder) -> {
                                            Iterable<Identifier> candidates = BreweryInit.DRINK_TYPES.keySet()::iterator;
                                            var remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

                                            CommandSource.forEachMatching(candidates, remaining, Function.identity(), id -> {
                                                builder.suggest(id.toString(), BreweryInit.DRINK_TYPES.get(id).name().text());
                                            });
                                            return builder.buildFuture();
                                        })
                                        .executes(BrewCommands::createDrink)
                                        .then(argument("quality", DoubleArgumentType.doubleArg(0, 10))
                                                .executes(BrewCommands::createDrink)
                                                .then(argument("age", DoubleArgumentType.doubleArg())
                                                        .executes(BrewCommands::createDrink)
                                                        .then(argument("distillated", BoolArgumentType.bool())
                                                                .executes(BrewCommands::createDrink)
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(literal("force_age")
                                .requires(Permissions.require("brewery.force_age", 2))
                                .then(argument("position", BlockPosArgumentType.blockPos())
                                        .then(argument("time", TimeArgumentType.time())
                                                .executes(BrewCommands::ageContainer)
                                        )
                                )
                        )
                        .then(literal("stats")
                                .requires(Permissions.require("brewery.stats", 2))
                                .executes((ctx) -> showStats(ctx, ctx.getSource().getPlayerOrThrow()))
                                .then(argument("player", EntityArgumentType.entity()).requires(Permissions.require("brewery.stats_others", 2))
                                        .executes(ctx -> showStats(ctx, EntityArgumentType.getEntity(ctx, "player"))))
                        )
                        .then(literal("set")
                                .requires(Permissions.require("brewery.set", 2))
                                .then(argument("target", EntityArgumentType.entities())
                                        .then(argument("alcohol", DoubleArgumentType.doubleArg())
                                                .then(argument("quality", FloatArgumentType.floatArg(0, 10))
                                                        .executes(BrewCommands::setAlcoholValue)

                                                )
                                        )
                                )


                        ).then(literal("dump_defaults")
                                .requires(Permissions.require("brewery.dump_defaults", 4))
                                .executes((ctx) -> dumpDefaultDefinitions())
                        ).then(literal("gui")
                                .then(argument("id", StringArgumentType.greedyString())
                                        .executes(BrewCommands::gui)
                                )
                        )
        );
    }

    private static int gui(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        int r = 0;
        var id = Identifier.tryParse(StringArgumentType.getString(ctx, "id"));
        var player = ctx.getSource().getPlayer();

        if (id != null && player != null) {
            player.server.execute(() -> {
                player.playSoundToPlayer(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS, 1f, 1);
                new BookOfBreweryItem.BrewGui(player, id, true, ()->new BookGui(player, BrewItems.BOOK_ITEM.getDefaultStack()).open()).open();
                //player.getMainHandStack().isOf(BrewItems.BOOK_ITEM) ? player.getMainHandStack() : player.getOffHandStack()
            });
            r = 1;
        }

        return r;
    }

    private static int setAlcoholValue(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        var alcohol = DoubleArgumentType.getDouble(ctx, "alcohol");
        var quality = FloatArgumentType.getFloat(ctx, "quality");
        int r = 0;
        for (var entity : EntityArgumentType.getEntities(ctx, "target")) {
            if (entity instanceof LivingEntity livingEntity) {
                var m = AlcoholManager.of(livingEntity);
                m.alcoholLevel = alcohol;
                m.quality = quality;
                r++;
            }
        }
        return r;
    }

    private static int ageContainer(CommandContext<ServerCommandSource> ctx) {
        var pos = BlockPosArgumentType.getBlockPos(ctx, "position");
        var time = ctx.getArgument("time", Integer.class);

        if (ctx.getSource().getWorld().getBlockEntity(pos) instanceof TickableContents tickableContents) {
            tickableContents.tickContents(time);
        }
        return 0;
    }

    private static int dumpDefaultDefinitions() {
        var dir = FabricLoader.getInstance().getGameDir().resolve("brewery_defaults_dump");

        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            var gson = new GsonBuilder().setPrettyPrinting().create();

            DefaultDefinitions.createBrews((key, type) -> {
                try {
                    Files.writeString(dir.resolve(key + ".json"), gson.toJson(DrinkType.CODEC.encodeStart(JsonOps.INSTANCE, type).getOrThrow()));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return 0;
    }

    private static int showStats(CommandContext<ServerCommandSource> ctx, Entity player) {
        if (player instanceof LivingEntity livingEntity) {
            var m = AlcoholManager.of(livingEntity);
            ctx.getSource().sendFeedback(() -> Text.translatable("text.brewery.stats", player.getDisplayName(), m.alcoholLevel, m.quality), false);
        }
        return 0;
    }

    private static int createDrink(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var id = IdentifierArgumentType.getIdentifier(context, "type");
        var type = BreweryInit.DRINK_TYPES.get(id);

        if (type == null) {
            context.getSource().sendError(Text.literal("Invalid type!"));
        }

        double quality;

        try {
            quality = DoubleArgumentType.getDouble(context, "quality");
        } catch (Throwable t) {
            quality = 10;
        }

        int age;

        try {
            age = (int) (DoubleArgumentType.getDouble(context, "age") * 20);
        } catch (Throwable e) {
            age = 0;
        }

        int distillated;

        try {
            distillated = BoolArgumentType.getBool(context, "distillated") ? type.distillationRuns() : 0;
        } catch (Throwable e) {
            distillated = 0;
        }

        context.getSource().getPlayerOrThrow().giveItemStack(DrinkUtils.createDrink(id, age, quality, distillated, Blocks.AIR));

        return 1;
    }


    private static int about(CommandContext<ServerCommandSource> context) {
        for (var text : context.getSource().getEntity() instanceof ServerPlayerEntity ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole()) {
            context.getSource().sendFeedback(() -> text, false);
        }

        return 1;
    }
}
