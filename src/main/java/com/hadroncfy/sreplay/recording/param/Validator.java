package com.hadroncfy.sreplay.recording.param;

import java.util.function.Consumer;

import net.minecraft.text.Text;

public interface Validator<T> {
    boolean validate(T val, Consumer<Text> errorReceiver);
}