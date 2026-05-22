package com.medisync.diabo.service;

import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;
import com.medisync.diabo.R;

public class NotificationHelper {

    public static void showSuccess(View view, String message) {
        if (view == null || message == null) return;
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.vitalGreen));
        
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(view.getContext(), R.color.white));
            textView.setTextSize(16);
        }
        snackbar.show();
    }

    public static void showError(View view, String message) {
        if (view == null || message == null) return;
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.vitalRed));
        
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(view.getContext(), R.color.white));
            textView.setTextSize(16);
        }
        snackbar.show();
    }
}
