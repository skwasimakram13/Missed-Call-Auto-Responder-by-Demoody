package com.demoody.missedcall.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.demoody.missedcall.services.MissedCallService;
import com.demoody.missedcall.utils.PermissionUtils;
import com.demoody.missedcall.utils.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            Log.d(TAG, "Boot/Package event received: " + action);
            
            PreferenceManager prefManager = new PreferenceManager(context);
            
            // Only start service if auto-responder is enabled and permissions are granted
            if (prefManager.isAutoResponderEnabled() && 
                PermissionUtils.hasRequiredPermissions(context)) {
                
                Log.d(TAG, "Starting MissedCallService after boot");
                
                Intent serviceIntent = new Intent(context, MissedCallService.class);
                serviceIntent.setAction(MissedCallService.ACTION_START_MONITORING);
                
                try {
                    context.startForegroundService(serviceIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to start service after boot", e);
                }
            } else {
                Log.d(TAG, "Service not started - auto-responder disabled or missing permissions");
            }
        }
    }
}