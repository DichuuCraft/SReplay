package com.hadroncfy.sreplay.recording.param;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {
    String name() default "";
    String desc() default "";

    @SuppressWarnings({"rawtypes"})
    Class<? extends Validator>[] validators() default {};
}