package com.monacdev.teaminsync.utils;

import android.os.Handler;
import android.os.Looper;

import com.monacdev.teaminsync.tasks.SendPushRunnable;

import java.util.ArrayList;

public class PushNotificationsManager {
    public static final String ONESIGNAL_APP_ID = "c48ca5f9-5ddd-457a-a553-b647ee0ee7b0";

    /**
     * Effectively sends the notification to the target users
     * @param targets the list of the IDs of the target users
     * @param title the title of the notification
     * @param message the body of the notification message
     */
    public static void sendPushNotification(ArrayList<String> targets, String title, String message){
        /* Handler bound to the main UI thread in order to communicate with it and providing a smooth UX */
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        SendPushRunnable sendPushBackgroundTask = new SendPushRunnable(targets, title, message, mainThreadHandler);
        Thread pushBackgroundThread = new Thread(sendPushBackgroundTask);
        pushBackgroundThread.start();
    }
}
