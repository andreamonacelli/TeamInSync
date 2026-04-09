package com.monacdev.teaminsync.loaders;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.monacdev.teaminsync.R;

public class LoaderDialog {
    private final Activity callingActivity;
    private AlertDialog dialog;

    public LoaderDialog(Activity callingActivity){
        this.callingActivity = callingActivity;
    }

    /**
     * Shows the application loader displaying the message passed as argument in order to make it
     * more significant to the user who is seeing it
     * @param loadingMessage the message to be displayed within the loader
     */
    public void show(String loadingMessage){
        if(this.dialog != null && this.dialog.isShowing()){
            return;
        }
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this.callingActivity);
        LayoutInflater inflater = this.callingActivity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_loader, null);
        if(loadingMessage != null && !loadingMessage.isEmpty()){
            TextView loaderMsgTV = view.findViewById(R.id.loaderMsgTV);
            loaderMsgTV.setText(loadingMessage);
        }
        dialogBuilder.setView(view).setCancelable(false);
        this.dialog = dialogBuilder.create();
        this.dialog.show();
    }

    /**
     * Hides the loader and destroys the previously built dialog
     */
    public void hide(){
        if(this.dialog != null && this.dialog.isShowing()){
            this.dialog.dismiss();
        }
    }
}
