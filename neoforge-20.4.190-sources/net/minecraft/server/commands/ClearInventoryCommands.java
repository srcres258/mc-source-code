package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class ClearInventoryCommands {
    private static final DynamicCommandExceptionType ERROR_SINGLE = new DynamicCommandExceptionType(
        p_304193_ -> Component.translatableEscape("clear.failed.single", p_304193_)
    );
    private static final DynamicCommandExceptionType ERROR_MULTIPLE = new DynamicCommandExceptionType(
        p_304192_ -> Component.translatableEscape("clear.failed.multiple", p_304192_)
    );

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
        pDispatcher.register(
            Commands.literal("clear")
                .requires(p_136704_ -> p_136704_.hasPermission(2))
                .executes(
                    p_136721_ -> clearInventory(
                            p_136721_.getSource(), Collections.singleton(p_136721_.getSource().getPlayerOrException()), p_180029_ -> true, -1
                        )
                )
                .then(
                    Commands.argument("targets", EntityArgument.players())
                        .executes(p_136719_ -> clearInventory(p_136719_.getSource(), EntityArgument.getPlayers(p_136719_, "targets"), p_180027_ -> true, -1))
                        .then(
                            Commands.argument("item", ItemPredicateArgument.itemPredicate(pContext))
                                .executes(
                                    p_136715_ -> clearInventory(
                                            p_136715_.getSource(),
                                            EntityArgument.getPlayers(p_136715_, "targets"),
                                            ItemPredicateArgument.getItemPredicate(p_136715_, "item"),
                                            -1
                                        )
                                )
                                .then(
                                    Commands.argument("maxCount", IntegerArgumentType.integer(0))
                                        .executes(
                                            p_136702_ -> clearInventory(
                                                    p_136702_.getSource(),
                                                    EntityArgument.getPlayers(p_136702_, "targets"),
                                                    ItemPredicateArgument.getItemPredicate(p_136702_, "item"),
                                                    IntegerArgumentType.getInteger(p_136702_, "maxCount")
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int clearInventory(CommandSourceStack pSource, Collection<ServerPlayer> pTargetPlayers, Predicate<ItemStack> pItemPredicate, int pMaxCount) throws CommandSyntaxException {
        int i = 0;

        for(ServerPlayer serverplayer : pTargetPlayers) {
            i += serverplayer.getInventory().clearOrCountMatchingItems(pItemPredicate, pMaxCount, serverplayer.inventoryMenu.getCraftSlots());
            serverplayer.containerMenu.broadcastChanges();
            serverplayer.inventoryMenu.slotsChanged(serverplayer.getInventory());
        }

        if (i == 0) {
            if (pTargetPlayers.size() == 1) {
                throw ERROR_SINGLE.create(pTargetPlayers.iterator().next().getName());
            } else {
                throw ERROR_MULTIPLE.create(pTargetPlayers.size());
            }
        } else {
            int j = i;
            if (pMaxCount == 0) {
                if (pTargetPlayers.size() == 1) {
                    pSource.sendSuccess(() -> Component.translatable("commands.clear.test.single", j, pTargetPlayers.iterator().next().getDisplayName()), true);
                } else {
                    pSource.sendSuccess(() -> Component.translatable("commands.clear.test.multiple", j, pTargetPlayers.size()), true);
                }
            } else if (pTargetPlayers.size() == 1) {
                pSource.sendSuccess(() -> Component.translatable("commands.clear.success.single", j, pTargetPlayers.iterator().next().getDisplayName()), true);
            } else {
                pSource.sendSuccess(() -> Component.translatable("commands.clear.success.multiple", j, pTargetPlayers.size()), true);
            }

            return i;
        }
    }
}
