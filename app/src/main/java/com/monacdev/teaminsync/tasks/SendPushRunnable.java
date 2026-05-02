package com.monacdev.teaminsync.tasks;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.monacdev.teaminsync.BuildConfig;
import com.monacdev.teaminsync.constants.NavigationTags;
import com.monacdev.teaminsync.utils.PushNotificationsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SendPushRunnable implements Runnable{
    private final ArrayList<String> targetUsernames;
    private final String title;
    private final String message;
    private final Handler mainThreadHandler;

    public SendPushRunnable(ArrayList<String> targetUsernames, String title, String message, Handler mainThreadHandler) {
        this.targetUsernames = targetUsernames;
        this.title = title;
        this.message = message;
        this.mainThreadHandler = mainThreadHandler;
    }

    /**
     * Deals with forming the HTTP request in order to send the push notifications through the remote service
     */
    @Override
    public void run() {
        boolean pushSent = false;
        try {
            URL url = new URL("https://onesignal.com/api/v1/notifications");
            HttpURLConnection connection = this.setupConnection(url);
            JSONObject payload = this.prepareRequestPayload();
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.close();
            int responseCode = connection.getResponseCode();
            pushSent = (responseCode == 200);
        } catch (Exception e){
            Log.e(NavigationTags.NOTIFICATIONS_MANAGER, "Error while sending the push notification to the remote system", e);
        }
        SendPushResultRunnable pushResultTask = new SendPushResultRunnable(pushSent);
        this.mainThreadHandler.post(pushResultTask);
    }

    /**
     * Prepares the HTTP request payload in form of a JSON object
     * @return the populated JSONObject
     * @throws JSONException in case there is any type of error while either parsing or populating JSON data
     */
    @NonNull
    private JSONObject prepareRequestPayload() throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("app_id", PushNotificationsManager.ONESIGNAL_APP_ID);
        payload.put("target_channel", "push");

        JSONArray aliasesArray = new JSONArray(this.targetUsernames);
        JSONObject aliases = new JSONObject();
        aliases.put("external_id", aliasesArray);
        payload.put("include_aliases", aliases);

        JSONObject contents = new JSONObject();
        contents.put("en", this.message);
        payload.put("contents", contents);

        JSONObject headings = new JSONObject();
        headings.put("en", this.title);
        payload.put("headings", headings);
        return payload;
    }

    /**
     * Configures the connection to the remote notifications service
     * @param url the target URL to the remote service
     * @return the adequately configured HttpURLConnection object
     * @throws IOException in case any type of error while creating the connection object occurs
     */
    @NonNull
    private HttpURLConnection setupConnection(@NonNull URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", BuildConfig.ONESIGNAL_REST_API_KEY);
        return connection;
    }
}
