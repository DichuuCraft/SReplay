package com.hadroncfy.sreplay.recording.param;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.ServerCommandSource;

public class ParamEntry<T> {
    // public final Class<T> clazz;
    public final Field field;
    public final Class<T> type;
    public final String name, desc;
    public final List<Validator<T>> validators = new ArrayList<>();

    @SuppressWarnings({ "rawtypes", "unchecked" })
    ParamEntry(Field field) {
        this.field = field;
        type = (Class<T>) field.getType();
        Param p = field.getAnnotation(Param.class);
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
            if (!v.validate(val, e -> src.sendError(e))) {
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
            val = (T)(Object)ctx.getArgument(name, type);
        }
        else {
            try {
                val = (T)(Object)Enum.valueOf((Class<? extends Enum>)type, StringArgumentType.getString(ctx, name).toUpperCase());
            }
            catch(IllegalArgumentException e){
                throw new InvalidEnumException(e);
            }
        }
        return setVal(src, param, val);
    }
}