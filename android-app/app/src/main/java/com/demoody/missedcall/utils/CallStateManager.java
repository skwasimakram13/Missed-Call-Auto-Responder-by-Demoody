package com.demoody.missedcall.utils;

import android.util.Log;

public class CallStateManager {
    
    private static final String TAG = "CallStateManager";
    private static CallStateManager instance;
    
    private String incomingNumber;
    private long incomingTime;
    private boolean callAnswered;
    
    private CallStateManager() {
        reset();
    }
    
    public static synchronized CallStateManager getInstance() {
        if (instance == null) {
            instance = new CallStateManager();
        }
        return instance;
    }
    
    public void onIncomingCall(String phoneNumber) {
        Log.d(TAG, "Incoming call: " + phoneNumber);
        this.incomingNumber = phoneNumber;
        this.incomingTime = System.currentTimeMillis();
        this.callAnswered = false;
    }
    
    public void onCallAnswered() {
        Log.d(TAG, "Call answered");
        this.callAnswered = true;
    }
    
    public String onCallEnded() {
        Log.d(TAG, "Call ended - answered: " + callAnswered + ", number: " + incomingNumber);
        
        String missedNumber = null;
        
        // If there was an incoming call and it wasn't answered, it's a missed call
        if (incomingNumber != null && !callAnswered) {
            missedNumber = incomingNumber;
            Log.d(TAG, "Missed call detected: " + missedNumber);
        }
        
        reset();
        return missedNumber;
    }
    
    private void reset() {
        this.incomingNumber = null;
        this.incomingTime = 0;
        this.callAnswered = false;
    }
    
    public boolean hasIncomingCall() {
        return incomingNumber != null;
    }
    
    public String getCurrentIncomingNumber() {
        return incomingNumber;
    }
    
    public long getCurrentIncomingTime() {
        return incomingTime;
    }
}