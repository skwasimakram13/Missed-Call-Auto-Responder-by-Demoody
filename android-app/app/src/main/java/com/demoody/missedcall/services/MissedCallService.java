package com.demoody.missedcall.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.demoody.missedcall.MainActivity;
import com.demoody.missedcall.MissedCallApplication;
import com.demoody.missedcall.R;
import com.demoody.missedcall.db.MissedCallEntity;
import com.demoody.missedcall.utils.PreferenceManager;
import com.demoody.missedcall.workers.MessageSchedulerWorker;

import java.util.concurrent.TimeUnit;

public class MissedCallService extends Service {
    
    private static final String TAG = "MissedCallService";
    private static final int NOTIFICATION_ID = 1001;
    
    public static final String ACTION_START_MONITORING = "START_MONITORING";
    public static final String ACTION_STOP_MONITORING = "STOP_MONITORING";
    public static final String ACTION_MISSED_CALL = "MISSED_CALL";
    
    public static final String EXTRA_PHONE_NUMBER = "phone_number";
    public static final String EXTRA_CALL_TIME = "call_time";
    
    private PreferenceManager preferenceManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        preferenceManager = new PreferenceManager(this);
        Log.d(TAG, "Service created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : ACTION_START_MONITORING;
        
        Log.d(TAG, "Service started with action: " + action);
        
        switch (action) {
            case ACTION_START_MONITORING:
                startMonitoring();
                break;
                
            case ACTION_STOP_MONITORING:
                stopMonitoring();
                break;
                
            case ACTION_MISSED_CALL:
                handleMissedCall(intent);
                break;
                
            default:
                startMonitoring();
                break;
        }
        
        return START_STICKY; // Restart if killed
    }
    
    private void startMonitoring() {
        if (!preferenceManager.isAutoResponderEnabled()) {
            Log.d(TAG, "Auto-responder is disabled, stopping service");
            stopSelf();
            return;
        }
        
        startForeground(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "Started foreground monitoring");
    }
    
    private void stopMonitoring() {
        Log.d(TAG, "Stopping monitoring");
        stopForeground(true);
        stopSelf();
    }
    
    private void handleMissedCall(Intent intent) {
        String phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER);
        long callTime = intent.getLongExtra(EXTRA_CALL_TIME, System.currentTimeMillis());
        
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Log.w(TAG, "Invalid phone number for missed call");
            return;
        }
        
        Log.d(TAG, "Handling missed call from: " + phoneNumber);
        
        // Check if auto-responder is enabled
        if (!preferenceManager.isAutoResponderEnabled()) {
            Log.d(TAG, "Auto-responder disabled, skipping missed call");
            return;
        }
        
        // Get message template and delay
        String messageTemplate = preferenceManager.getMessageTemplate();
        int delayMinutes = preferenceManager.getDelayMinutes();
        
        // Create missed call entity
        long scheduledTime = callTime + (delayMinutes * 60 * 1000L);
        MissedCallEntity missedCall = new MissedCallEntity(phoneNumber, callTime, scheduledTime, messageTemplate);
        
        // Save to local database
        new Thread(() -> {
            try {
                MissedCallApplication app = (MissedCallApplication) getApplication();
                long id = app.getDatabase().missedCallDao().insert(missedCall);
                
                if (id > 0) {
                    Log.d(TAG, "Missed call saved with ID: " + id);
                    scheduleMessage(phoneNumber, callTime, messageTemplate, delayMinutes);
                } else {
                    Log.w(TAG, "Duplicate missed call, not scheduling message");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving missed call", e);
            }
        }).start();
    }
    
    private void scheduleMessage(String phoneNumber, long callTime, String messageText, int delayMinutes) {
        Data inputData = new Data.Builder()
            .putString(MessageSchedulerWorker.KEY_PHONE_NUMBER, phoneNumber)
            .putLong(MessageSchedulerWorker.KEY_CALL_TIME, callTime)
            .putString(MessageSchedulerWorker.KEY_MESSAGE_TEXT, messageText)
            .build();
        
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MessageSchedulerWorker.class)
            .setInputData(inputData)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag("missed_call_" + phoneNumber + "_" + callTime)
            .build();
        
        WorkManager.getInstance(this).enqueue(workRequest);
        
        Log.d(TAG, "Scheduled message for " + phoneNumber + " in " + delayMinutes + " minutes");
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, MissedCallApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_phone_missed)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }
}