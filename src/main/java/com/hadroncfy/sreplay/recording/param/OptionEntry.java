package com.hadroncfy.sreplay.recording.param;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.ServerCommandSource;

public class OptionEntry<T> {
    // public final Class<T> clazz;
    public final Field field;
    public final Class<T> type;
    public final String name, desc;
    public final List<Validator<T>> validators = new ArrayList<>();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    OptionEntry(Field field) {
        this.field = field;
        type = (Class<T>) field.getType();
        Option p = field.getAnnotation(Option.class);
        desc = p.desc();
        if (p.name().equals("")){
            name = field.getName();
        }
        else {
            name = p.name();
        }

        for (Class<? extends Validator> c : p.validators()) {
            try {
                Constructor<? extends Validator> ctor = c.getDeclaredConstructor();
                ctor.setAccessible(true);
                validators.add((Validator<T>) ctor.newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean setVal(ServerCommandSource src, Object param, T val)
            throws IllegalArgumentException, IllegalAccessException {
        for (Validator<T> v : validators) {
            if (!v.validate(val, src::sendError)) {
                return false;
            }
        }
        field.set(param, val);
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean set(CommandContext<ServerCommandSource> ctx, Object param)
            throws IllegalArgumentException, IllegalAccessException, InvalidEnumException {
        ServerCommandSource src = ctx.getSource();
        T val;
        if (!type.isEnum()){
            val = ctx.getArgument(name, type);
        }
        else {
            String valName = StringArgumentType.getString(ctx, name);
            try {
                val = (T)(Object)Enum.valueOf((Class<? extends Enum>)type, valName.toUpperCase());
            }
            catch(IllegalArgumentException e){
                throw new InvalidEnumException(e);
            }
        }
        return setVal(src, param, val);
    }
}