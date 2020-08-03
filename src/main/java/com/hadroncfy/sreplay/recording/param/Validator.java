package com.hadroncfy.sreplay.recording.param;

import com.github.steveice10.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;

import net.minecraft.text.Text;

public interface Validator<T> {
    boolean validate(T val, Consumer<Text> errorReceiver);
}