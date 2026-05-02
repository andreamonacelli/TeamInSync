package com.monacdev.teaminsync.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.UploadCallback;
import com.monacdev.teaminsync.constants.Constants;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {

    /**
     * Initializes the Cloudinary service for the application
     * @param context the application context
     */
    public static void init(@NonNull Context context){
        try{
            Map<String, String> config = new HashMap<>();
            config.put(Constants.CLOUDINARY_CLOUD_NAME_KEY, Constants.CLOUDINARY_CLOUD_NAME_STRING);
            MediaManager.init(context.getApplicationContext(), config);
        } catch(IllegalStateException e){
            Log.i("cloudinary_init", "Cloudinary is already initialized!");
        }
    }

    /**
     * Given a byte array representing an image, the method uploads it to Cloudinary
     * @param imageBytes a byte array representing the image to be uploaded
     * @param callback the Cloudinary object holding the callback functions to be executed once the upload is over
     */
    public void uploadFromBytes(byte[] imageBytes, UploadCallback callback){
        MediaManager.get()
                .upload(imageBytes)
                .unsigned(Constants.CLOUDINARY_PRESET_STRING)
                .callback(callback)
                .dispatch();
    }

    /**
     * Given an image URI associated to an image, the method uploads the image to Cloudinary
     * @param imageUri an image URI associated to the image to be uploaded
     * @param callback the Cloudinary object holding the callback functions to be executed once the upload is over
     */
    public void uploadFromURI(Uri imageUri, UploadCallback callback){
        MediaManager.get()
                .upload(imageUri)
                .unsigned(Constants.CLOUDINARY_PRESET_STRING)
                .callback(callback)
                .dispatch();
    }
}
