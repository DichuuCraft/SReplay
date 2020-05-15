package com.hadroncfy.sreplay.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.hadroncfy.sreplay.SReplayMod;
import com.hadroncfy.sreplay.config.TextRenderer;

import net.minecraft.server.command.ServerCommandSource;

public class ConfirmationManager {
    private static final Random random = new Random();
    private final Map<String, ConfirmationEntry> confirms = new HashMap<>();
    private final long timeout;
    private final int codeBound;
    public ConfirmationManager(long timeout, int codeBound){
        this.timeout = timeout;
        this.codeBound = codeBound;
    }

    public synchronized void submit(String label, ServerCommandSource src, Runnable h){
        ConfirmationEntry e = confirms.get(label);
        if (e != null){
            e.t.cancel();
        }
        final int code = random.nextInt(codeBound);
        src.sendFeedback(TextRenderer.render(SReplayMod.getFormats().confirmingHint, Integer.toString(code)), false);
        confirms.put(label, new ConfirmationEntry(src, label, code, h));
    }

    public synchronized boolean confirm(String label, int code){
        ConfirmationEntry h = confirms.get(label);
        if (h != null){
            if (code == h.code){
                h.t.cancel();
                h.handler.run();
                confirms.remove(label);
            }
            else {
                h.src.sendError(SReplayMod.getFormats().incorrectConfirmationCode);
            }
            return true;
        }
        return false;
    }

    public synchronized boolean cancel(String label){
        ConfirmationEntry h = confirms.get(label);
        if (h != null){
            h.t.cancel();
            confirms.remove(label);
            h.src.sendFeedback(SReplayMod.getFormats().operationCancelled, true);
            return true;
        }
        return false;
    }

    @FunctionalInterface
    public interface ConfirmationHandler {
        void onConfirm(boolean codeMatch, boolean cancelled);
    }

    private class ConfirmationEntry extends TimerTask {
        final String label;
        final ServerCommandSource src;
        final Runnable handler;
        final Timer t;
        final int code;
        public ConfirmationEntry(ServerCommandSource src, String label, int code, Runnable h){
            this.src = src;
            this.label = label;
            this.handler = h;
            t = new Timer();
            this.code = code;
            t.schedule(this, timeout);
        }

        @Override
        public void run() {
            synchronized(ConfirmationManager.this){
                confirms.remove(label);
                src.sendFeedback(SReplayMod.getFormats().operationCancelled, true);
            }
        }
    }
}