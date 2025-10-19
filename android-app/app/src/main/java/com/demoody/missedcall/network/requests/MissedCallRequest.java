package com.demoody.missedcall.network.requests;

import com.google.gson.annotations.SerializedName;

public class MissedCallRequest {
    
    @SerializedName("device_id")
    public String deviceId;
    
    @SerializedName("phone_number")
    public String phoneNumber;
    
    @SerializedName("call_time")
    public long callTime;
    
    @SerializedName("message_text")
    public String messageText;
    
    @SerializedName("delay_minutes")
    public int delayMinutes;
    
    public MissedCallRequest() {}
    
    public MissedCallRequest(String deviceId, String phoneNumber, long callTime, String messageText, int delayMinutes) {
        this.deviceId = deviceId;
        this.phoneNumber = phoneNumber;
        this.callTime = callTime;
        this.messageText = messageText;
        this.delayMinutes = delayMinutes;
    }
}