package com.hadroncfy.sreplay.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.hadroncfy.sreplay.Util;
import com.hadroncfy.sreplay.Util.Replacer;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class TextRenderer extends AbstractTextRenderer<TextRenderer> implements Replacer<String> {
    private List<String> vars = new ArrayList<>();
    private static final Pattern VAL_EXP = Pattern.compile("\\$[0-9]");
    
    public TextRenderer var(String ...vars){
        for (String v: vars){
            this.vars.add(v);
        }
        return this;
    }

    public Text render0(Text t){
        return render(this, t);
    }

    @Override
    protected Text renderString(String s) {
        return new LiteralText(Util.replaceAll(VAL_EXP, s, this));
    }

    @Override
    public String get(String a) {
        try {
            int i = Integer.parseInt(a.substring(1));
            if (i > 0 && i <= vars.size()){
                return vars.get(i - 1);
            }
        }
        catch(NumberFormatException e){}
        return a;
    }

    public static Text render(Text template, String ...vars){
        return new TextRenderer().var(vars).render0(template);
    }
}