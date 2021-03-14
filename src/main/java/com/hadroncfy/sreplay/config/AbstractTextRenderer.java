package com.hadroncfy.sreplay.config;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.text.HoverEvent.Action;

public abstract class AbstractTextRenderer<C> {
    protected abstract MutableText renderString(String s);

    public Text render(C ctx, Text template) {
        MutableText ret;
        if (template instanceof LiteralText) {
            ret = renderString(((LiteralText) template).getRawString());
        } else if (template instanceof TranslatableText) {
            final TranslatableText tc = (TranslatableText) template;
            Object[] args = new Object[tc.getArgs().length];
            for (int i = 0; i < args.length; i++) {
                Object obj = tc.getArgs()[i];
                if (obj instanceof Text) {
                    obj = render(ctx, (Text) obj);
                } else {
                    obj = "null";
                }
                args[i] = obj;
            }
            ret = new TranslatableText(tc.getKey(), args);
        } else {
            ret = template.copy();
        }
        ret.setStyle(renderStyle(ctx, template.getStyle()));

        for (Text t : template.getSiblings()) {
            ret.append(render(ctx, t));
        }
        return ret;
    }

    private Style renderStyle(C ctx, /* mut */ Style style) {
        HoverEvent h = style.getHoverEvent();
        if (h != null && h.getAction() == HoverEvent.Action.SHOW_TEXT) {
            Text action = h.getValue(HoverEvent.Action.SHOW_TEXT);
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, render(ctx, action)));
        }

        ClickEvent c = style.getClickEvent();
        if (c != null){
            style = style.withClickEvent(new ClickEvent(c.getAction(), renderString(c.getValue()).asString()));
        }

        String i = style.getInsertion();
        if (i != null){
            style = style.withInsertion(renderString(i).asString());
        }

        return style;
    }
}