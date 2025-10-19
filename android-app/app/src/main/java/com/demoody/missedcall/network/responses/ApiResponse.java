package com.demoody.missedcall.network.responses;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    
    @SerializedName("success")
    public boolean success;
    
    @SerializedName("data")
    public T data;
    
    @SerializedName("error")
    public String error;
    
    @SerializedName("timestamp")
    public String timestamp;
    
    public ApiResponse() {}
    
    public boolean isSuccess() {
        return success;
    }
    
    public T getData() {
        return data;
    }
    
    public String getError() {
        return error;
    }
}