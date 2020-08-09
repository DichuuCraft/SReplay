package com.hadroncfy.sreplay.asm;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode;
import org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

@AtCode("sreplay_MultipleOrdinalField")
public class MultipleOrdinalFieldInjectionPoint extends BeforeFieldAccess {
    private final Set<Integer> ordinals = new HashSet<>();

    public MultipleOrdinalFieldInjectionPoint(InjectionPointData data) {
        super(data);
        for (String part: data.get("ordinals", "").split(",")){
            part = part.trim();
            try {
                if (part.indexOf("..") != -1){
                    String[] s = part.split("\\.\\.");
                    for (int i = Integer.parseInt(s[0]); i <= Integer.parseInt(s[1]); i++){
                        ordinals.add(i);
                    }
                }
                else {
                    ordinals.add(Integer.parseInt(part));
                }
            } catch(NumberFormatException e){
            }
        }
    }
    
    @Override
    protected boolean matchesOrdinal(int ordinal) {
        return ordinals.contains(ordinal);
    }
}