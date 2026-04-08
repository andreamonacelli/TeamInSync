package com.monacdev.teaminsync.tasks;

import android.util.Log;

import com.monacdev.teaminsync.constants.NavigationTags;

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
            Log.d(NavigationTags.NOTIFICATIONS_MANAGER, "Push notification sent successfully!");
        } else {
            Log.e(NavigationTags.NOTIFICATIONS_MANAGER, "Failure while sending push notification");
        }
    }
}
