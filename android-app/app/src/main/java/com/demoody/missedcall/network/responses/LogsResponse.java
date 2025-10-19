package com.demoody.missedcall.network.responses;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class LogsResponse {
    
    @SerializedName("logs")
    public List<Map<String, Object>> logs;
    
    @SerializedName("stats")
    public Map<String, Object> stats;
    
    @SerializedName("pagination")
    public Map<String, Object> pagination;
    
    public LogsResponse() {}
}