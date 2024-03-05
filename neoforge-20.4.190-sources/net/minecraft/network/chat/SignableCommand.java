package net.minecraft.network.chat;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.arguments.SignedArgument;

public record SignableCommand<S>(List<SignableCommand.Argument<S>> arguments) {
    public static <S> SignableCommand<S> of(ParseResults<S> pResults) {
        String s = pResults.getReader().getString();
        CommandContextBuilder<S> commandcontextbuilder = pResults.getContext();
        CommandContextBuilder<S> commandcontextbuilder1 = commandcontextbuilder;

        List<SignableCommand.Argument<S>> list;
        CommandContextBuilder<S> commandcontextbuilder2;
        for(list = collectArguments(s, commandcontextbuilder);
            (commandcontextbuilder2 = commandcontextbuilder1.getChild()) != null;
            commandcontextbuilder1 = commandcontextbuilder2
        ) {
            // NEO: Check if the command node is a RootCommandNode, instead of simply being the original root node; fixes #186
            boolean flag = !(commandcontextbuilder2.getRootNode() instanceof com.mojang.brigadier.tree.RootCommandNode);
            if (!flag) {
                break;
            }

            list.addAll(collectArguments(s, commandcontextbuilder2));
        }

        return new SignableCommand<>(list);
    }

    private static <S> List<SignableCommand.Argument<S>> collectArguments(String pKey, CommandContextBuilder<S> pContextBuilder) {
        List<SignableCommand.Argument<S>> list = new ArrayList<>();

        for(ParsedCommandNode<S> parsedcommandnode : pContextBuilder.getNodes()) {
            CommandNode $$5 = parsedcommandnode.getNode();
            if ($$5 instanceof ArgumentCommandNode argumentcommandnode && argumentcommandnode.getType() instanceof SignedArgument) {
                ParsedArgument<S, ?> parsedargument = pContextBuilder.getArguments().get(argumentcommandnode.getName());
                if (parsedargument != null) {
                    String s = parsedargument.getRange().get(pKey);
                    list.add(new SignableCommand.Argument<>(argumentcommandnode, s));
                }
            }
        }

        return list;
    }

    public static record Argument<S>(ArgumentCommandNode<S, ?> node, String value) {
        public String name() {
            return this.node.getName();
        }
    }
}
