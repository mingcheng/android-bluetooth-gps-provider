package com.gracecode.android.btgps.helper;

import android.content.Context;
import android.widget.Toast;

public final class UIHelper {

    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
