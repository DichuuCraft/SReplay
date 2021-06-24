package com.hadroncfy.sreplay.recording.param;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import static net.minecraft.command.CommandSource.suggestMatching;

public class EnumArgumentType<T extends Enum<T>> implements ArgumentType<T> {
    private final Class<T> clazz;
    private final boolean lowercase;
    private final List<String> consts = new ArrayList<>();

    private EnumArgumentType(Class<T> clazz, boolean lowercase){
        this.clazz = clazz;
        this.lowercase = lowercase;
        for (T t: clazz.getEnumConstants()){
            String n = t.name();
            if (lowercase){
                n = n.toLowerCase();
            }
            consts.add(n);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return suggestMatching(consts, builder);
    }

    @SuppressWarnings({"unchecked"})
    public static <T extends Enum<T>> EnumArgumentType<T> enumType(Class<?> clazz, boolean lowercase){
        return new EnumArgumentType<T>((Class<T>) clazz, lowercase);
    }

    @SuppressWarnings({"unchecked"})
    public static <T extends Enum<T>> T getEnum(final CommandContext<?> context, Class<?> c, final String name){
        return (T) context.getArgument(name, c);
    }

    @Override
    public T parse(StringReader reader) throws CommandSyntaxException {
        String name = reader.readString();
        if (lowercase){
            name = name.toUpperCase();
        }
        try {
            return Enum.valueOf(clazz, name);
        }
        catch(IllegalArgumentException e){
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, name);
        }
    }
    
}