package com.demoody.missedcall.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    
    private static final String PREF_NAME = "missed_call_prefs";
    
    // Preference keys
    private static final String KEY_AUTO_RESPONDER_ENABLED = "auto_responder_enabled";
    private static final String KEY_MESSAGE_TEMPLATE = "message_template";
    private static final String KEY_DELAY_MINUTES = "delay_minutes";
    private static final String KEY_BUSINESS_HOURS_ENABLED = "business_hours_enabled";
    private static final String KEY_BUSINESS_START_HOUR = "business_start_hour";
    private static final String KEY_BUSINESS_END_HOUR = "business_end_hour";
    private static final String KEY_API_BASE_URL = "api_base_url";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_API_TOKEN = "api_token";
    private static final String KEY_FIRST_RUN = "first_run";
    
    // Default values
    private static final String DEFAULT_MESSAGE = "Hello! We missed your call. We're sorry we couldn't pick up. Reply CALLBACK or visit our website and we'll get back to you shortly. Reply STOP to opt out.";
    private static final int DEFAULT_DELAY_MINUTES = 5;
    private static final int DEFAULT_BUSINESS_START = 9; // 9 AM
    private static final int DEFAULT_BUSINESS_END = 18; // 6 PM
    
    private SharedPreferences prefs;
    
    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    // Auto Responder Settings
    public boolean isAutoResponderEnabled() {
        return prefs.getBoolean(KEY_AUTO_RESPONDER_ENABLED, false);
    }
    
    public void setAutoResponderEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_RESPONDER_ENABLED, enabled).apply();
    }
    
    public String getMessageTemplate() {
        return prefs.getString(KEY_MESSAGE_TEMPLATE, DEFAULT_MESSAGE);
    }
    
    public void setMessageTemplate(String template) {
        prefs.edit().putString(KEY_MESSAGE_TEMPLATE, template).apply();
    }
    
    public int getDelayMinutes() {
        return prefs.getInt(KEY_DELAY_MINUTES, DEFAULT_DELAY_MINUTES);
    }
    
    public void setDelayMinutes(int minutes) {
        prefs.edit().putInt(KEY_DELAY_MINUTES, minutes).apply();
    }
    
    // Business Hours Settings
    public boolean isBusinessHoursEnabled() {
        return prefs.getBoolean(KEY_BUSINESS_HOURS_ENABLED, false);
    }
    
    public void setBusinessHoursEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BUSINESS_HOURS_ENABLED, enabled).apply();
    }
    
    public int getBusinessStartHour() {
        return prefs.getInt(KEY_BUSINESS_START_HOUR, DEFAULT_BUSINESS_START);
    }
    
    public void setBusinessStartHour(int hour) {
        prefs.edit().putInt(KEY_BUSINESS_START_HOUR, hour).apply();
    }
    
    public int getBusinessEndHour() {
        return prefs.getInt(KEY_BUSINESS_END_HOUR, DEFAULT_BUSINESS_END);
    }
    
    public void setBusinessEndHour(int hour) {
        prefs.edit().putInt(KEY_BUSINESS_END_HOUR, hour).apply();
    }
    
    // API Settings
    public String getApiBaseUrl() {
        return prefs.getString(KEY_API_BASE_URL, "");
    }
    
    public void setApiBaseUrl(String url) {
        prefs.edit().putString(KEY_API_BASE_URL, url).apply();
    }
    
    public String getDeviceId() {
        return prefs.getString(KEY_DEVICE_ID, "");
    }
    
    public void setDeviceId(String deviceId) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
    }
    
    public String getApiToken() {
        return prefs.getString(KEY_API_TOKEN, "");
    }
    
    public void setApiToken(String token) {
        prefs.edit().putString(KEY_API_TOKEN, token).apply();
    }
    
    // App State
    public boolean isFirstRun() {
        return prefs.getBoolean(KEY_FIRST_RUN, true);
    }
    
    public void setFirstRun(boolean firstRun) {
        prefs.edit().putBoolean(KEY_FIRST_RUN, firstRun).apply();
    }
    
    // Utility methods
    public void resetToDefaults() {
        prefs.edit()
            .putBoolean(KEY_AUTO_RESPONDER_ENABLED, false)
            .putString(KEY_MESSAGE_TEMPLATE, DEFAULT_MESSAGE)
            .putInt(KEY_DELAY_MINUTES, DEFAULT_DELAY_MINUTES)
            .putBoolean(KEY_BUSINESS_HOURS_ENABLED, false)
            .putInt(KEY_BUSINESS_START_HOUR, DEFAULT_BUSINESS_START)
            .putInt(KEY_BUSINESS_END_HOUR, DEFAULT_BUSINESS_END)
            .apply();
    }
    
    public void clearApiCredentials() {
        prefs.edit()
            .remove(KEY_API_TOKEN)
            .remove(KEY_DEVICE_ID)
            .apply();
    }
}