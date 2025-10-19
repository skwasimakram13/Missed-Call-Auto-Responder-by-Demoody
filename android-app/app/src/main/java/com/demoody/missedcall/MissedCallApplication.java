package com.demoody.missedcall;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.demoody.missedcall.db.AppDatabase;

public class MissedCallApplication extends Application {
    
    public static final String NOTIFICATION_CHANNEL_ID = "missed_call_service";
    public static final String NOTIFICATION_CHANNEL_NAME = "Missed Call Service";
    
    private AppDatabase database;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        createNotificationChannel();
        initializeWorkManager();
        initializeDatabase();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps the missed call detection service running");
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void initializeWorkManager() {
        Configuration config = new Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build();
        
        WorkManager.initialize(this, config);
    }
    
    private void initializeDatabase() {
        database = AppDatabase.getInstance(this);
    }
    
    public AppDatabase getDatabase() {
        return database;
    }
}