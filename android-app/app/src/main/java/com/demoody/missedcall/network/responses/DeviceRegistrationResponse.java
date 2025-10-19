package com.demoody.missedcall.network.responses;

import com.google.gson.annotations.SerializedName;

public class DeviceRegistrationResponse {
    
    @SerializedName("message")
    public String message;
    
    @SerializedName("api_token")
    public String apiToken;
    
    public DeviceRegistrationResponse() {}
}