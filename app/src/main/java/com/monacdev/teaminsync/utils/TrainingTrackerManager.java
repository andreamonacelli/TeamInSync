package com.monacdev.teaminsync.utils;

import android.os.Handler;
import android.os.Message;

import com.monacdev.teaminsync.constants.Constants;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class TrainingTrackerManager {
    private Timer workoutTimer;
    private int elapsedSeconds = 0;
    private boolean timerRunning = false;
    private Handler UIHandler;
    private int targetSeconds;
    private boolean targetReachedMsgSent = false;

    public TrainingTrackerManager(Handler UIHandler){
        this.UIHandler = UIHandler;
    }

    /**
     * Starts the timer in order to track the training's progress
     */
    public void startTimer(){
        if(this.timerRunning){
            return;
        }
        this.workoutTimer = new Timer();
        this.timerRunning = true;
        this.workoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TrainingTrackerManager.this.elapsedSeconds++;
                int minutesTotal = TrainingTrackerManager.this.elapsedSeconds / 60;
                int hours = minutesTotal / 60;
                int minutes = minutesTotal % 60;
                int seconds = TrainingTrackerManager.this.elapsedSeconds % 60;
                String elapsedTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d.%02d", hours, minutes, seconds);
                Message message = TrainingTrackerManager.this.UIHandler.obtainMessage(Constants.MSG_UPDATE_TIMER, elapsedTimeFormatted);
                TrainingTrackerManager.this.UIHandler.sendMessage(message);
                TrainingTrackerManager.this.sendMsgIfTargetReached();
            }
        }, 1000, 1000);
    }

    /**
     * Pauses the timer due to a break that is being taken during the training
     */
    public void pauseTimer(){
        if(this.workoutTimer != null){
            this.workoutTimer.cancel();
            this.workoutTimer = null;
        }
        this.timerRunning = false;
    }

    public void setTargetSeconds(int targetSeconds){
        this.targetSeconds = targetSeconds;
        this.targetReachedMsgSent = false;
    }

    /**
     * If the target of the training has been reached, sends a message flagging this to the respective Handler.
     * Thanks to the targetReachedMsgSent parameter, this message will be sent only once
     */
    private void sendMsgIfTargetReached(){
        if(this.isTargetReached() && !this.targetReachedMsgSent){
            this.targetReachedMsgSent = true;
            Message targetReachedMsg = this.UIHandler.obtainMessage(Constants.MSG_TRAINING_TARGET_REACHED);
            this.UIHandler.sendMessage(targetReachedMsg);
        }
    }

    /**
     * Defines whether the target of the current training has been reached or not
     * @return <strong>true</strong> if the target of the training has been reached, <strong>false</strong> otherwise
     */
    public boolean isTargetReached(){
        return (this.targetSeconds > 0 && this.elapsedSeconds >= this.targetSeconds);
    }

    public boolean isTimerRunning() {
        return this.timerRunning;
    }
}
