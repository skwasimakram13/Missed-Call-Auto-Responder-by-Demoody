package com.demoody.missedcall.network.requests;

import com.google.gson.annotations.SerializedName;

public class DeviceRegistrationRequest {
    
    @SerializedName("device_id")
    public String deviceId;
    
    @SerializedName("device_name")
    public String deviceName;
    
    public DeviceRegistrationRequest() {}
    
    public DeviceRegistrationRequest(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }
}