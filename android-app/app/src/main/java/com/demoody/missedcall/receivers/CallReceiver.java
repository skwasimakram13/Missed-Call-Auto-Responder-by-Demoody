package com.demoody.missedcall.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.demoody.missedcall.services.MissedCallService;
import com.demoody.missedcall.utils.CallStateManager;
import com.demoody.missedcall.utils.PermissionUtils;

public class CallReceiver extends BroadcastReceiver {
    
    private static final String TAG = "CallReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!PermissionUtils.hasRequiredPermissions(context)) {
            Log.w(TAG, "Missing required permissions");
            return;
        }
        
        String action = intent.getAction();
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            
            Log.d(TAG, "Phone state changed: " + state + ", number: " + phoneNumber);
            
            CallStateManager callStateManager = CallStateManager.getInstance();
            
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                // Incoming call
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    callStateManager.onIncomingCall(phoneNumber);
                    Log.d(TAG, "Incoming call from: " + phoneNumber);
                }
                
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                // Call ended or no call
                String missedNumber = callStateManager.onCallEnded();
                if (missedNumber != null) {
                    Log.d(TAG, "Missed call detected from: " + missedNumber);
                    
                    // Start the service to handle the missed call
                    Intent serviceIntent = new Intent(context, MissedCallService.class);
                    serviceIntent.setAction(MissedCallService.ACTION_MISSED_CALL);
                    serviceIntent.putExtra(MissedCallService.EXTRA_PHONE_NUMBER, missedNumber);
                    serviceIntent.putExtra(MissedCallService.EXTRA_CALL_TIME, System.currentTimeMillis());
                    
                    context.startForegroundService(serviceIntent);
                }
                
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                // Call answered
                callStateManager.onCallAnswered();
                Log.d(TAG, "Call answered");
            }
        }
    }
}