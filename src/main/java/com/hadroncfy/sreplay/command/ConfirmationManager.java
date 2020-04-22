package com.hadroncfy.sreplay.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ConfirmationManager {
    private final Map<String, ConfirmationEntry> confirms = new HashMap<>();
    private final long timeout;
    public ConfirmationManager(long timeout){
        this.timeout = timeout;
    }

    public void submit(String label, String code, ConfirmationHandler h){
        synchronized(this){
            confirms.put(label, new ConfirmationEntry(label, code, h));
        }
    }

    public boolean confirm(String label, String code){
        synchronized(this){
            ConfirmationEntry h = confirms.get(label);
            if (h != null){
                h.t.cancel();
                h.handler.onConfirm(code.equals(h.code), false);
                confirms.remove(label);
                return true;
            }
            return false;
        }
    }

    public boolean cancel(String label){
        synchronized(this){
            ConfirmationEntry h = confirms.get(label);
            if (h != null){
                h.t.cancel();
                h.handler.onConfirm(false, true);
                confirms.remove(label);
                return true;
            }
            return false;
        }
    }

    @FunctionalInterface
    public interface ConfirmationHandler {
        void onConfirm(boolean codeMatch, boolean cancelled);
    }

    private class ConfirmationEntry extends TimerTask {
        final String label;
        final ConfirmationHandler handler;
        final Timer t;
        final String code;
        public ConfirmationEntry(String label, String code, ConfirmationHandler h){
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
                handler.onConfirm(false, true);
            }
        }
    }
}