package com.demoody.missedcall.network.requests;

import com.google.gson.annotations.SerializedName;

public class OptOutRequest {
    
    @SerializedName("phone_number")
    public String phoneNumber;
    
    public OptOutRequest() {}
    
    public OptOutRequest(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}