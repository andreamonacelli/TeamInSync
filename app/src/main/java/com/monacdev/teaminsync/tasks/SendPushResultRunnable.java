package com.monacdev.teaminsync.tasks;

import android.util.Log;

import com.monacdev.teaminsync.utils.Constants;

public class SendPushResultRunnable implements Runnable{
    private boolean pushSentSuccess;

    public SendPushResultRunnable(boolean pushSentSuccess) {
        this.pushSentSuccess = pushSentSuccess;
    }

    /**
     * Deals with the result of the thread that sends the push notifications through the remote system
     */
    @Override
    public void run() {
        if(this.pushSentSuccess){
            Log.d(Constants.NOTIFICATIONS_MANAGER_TAG, "Push notification sent successfully!");
        } else {
            Log.e(Constants.NOTIFICATIONS_MANAGER_TAG, "Failure while sending push notification");
        }
    }
}
