package com.hadroncfy.sreplay.recording;

import com.github.steveice10.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import com.hadroncfy.sreplay.config.Config;
import com.hadroncfy.sreplay.recording.param.Param;
import com.hadroncfy.sreplay.recording.param.Validator;

import static com.hadroncfy.sreplay.SReplayMod.getFormats;

import net.minecraft.text.Text;

public class RecordingParam {
    @Param(
        desc = "sreplay.param.sizeLimit.desc",
        validators = SizeLimitValidator.class
    )
    public int sizeLimit = -1;

    @Param(
        desc = "sreplay.param.timeLimit.desc",
        validators = TimeLimitValidator.class
    )
    public int timeLimit = -1;

    @Param(desc = "sreplay.param.autoReconnect.desc")
    public boolean autoReconnect = true;
    
    @Param(desc = "sreplay.param.autoPause.desc")
    public boolean autoPause = false;

    @Param(
        desc = "sreplay.param.watchDistance.desc",
        validators = PositiveValidator.class
    )
    public int watchDistance;

    @Param(
        desc = "sreplay.param.dayTime.desc",
        validators = NonNegativeOrMinus1.class
    )
    public int dayTime = -1;

    @Param(desc = "sreplay.param.forcedWeather.desc")
    public ForcedWeather forcedWeather = ForcedWeather.NONE;

    public static RecordingParam createDefaultRecordingParam(Config config, int watchDistance) {
        RecordingParam p = new RecordingParam();
        p.autoReconnect = config.autoReconnect;
        p.sizeLimit = config.sizeLimit;
        p.timeLimit = config.timeLimit;
        p.watchDistance = watchDistance;
        return p;
    }

    private static class SizeLimitValidator implements Validator<Integer> {
        @Override
        public boolean validate(Integer val, Consumer<Text> errorReceiver) {
            if (val != -1 && val < 10){
                errorReceiver.accept(getFormats().sizeLimitTooSmall);
                return false;
            }
            return true;
        }
    }

    private static class TimeLimitValidator implements Validator<Integer> {
        @Override
        public boolean validate(Integer val, Consumer<Text> errorReceiver) {
            if (val != -1 && val < 10){
                errorReceiver.accept(getFormats().timeLimitTooSmall);
                return false;
            }
            return true;
        }
    }

    private static class PositiveValidator implements Validator<Integer> {
        @Override
        public boolean validate(Integer val, Consumer<Text> errorReceiver) {
            if (val <= 0){
                errorReceiver.accept(getFormats().positiveParam);
                return false;
            }
            return true;
        }
    }

    private static class NonNegativeOrMinus1 implements Validator<Integer> {
        @Override
        public boolean validate(Integer val, Consumer<Text> errorReceiver) {
            if (val != -1 && val < 0){
                errorReceiver.accept(getFormats().nonNegativeOrMinusOne);
                return false;
            }
            return true;
        }
    }
}