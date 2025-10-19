package com.demoody.missedcall.network;

import com.demoody.missedcall.network.requests.DeviceRegistrationRequest;
import com.demoody.missedcall.network.requests.MissedCallRequest;
import com.demoody.missedcall.network.requests.OptOutRequest;
import com.demoody.missedcall.network.responses.ApiResponse;
import com.demoody.missedcall.network.responses.DeviceRegistrationResponse;
import com.demoody.missedcall.network.responses.LogsResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    
    @POST("api/v1/missed_calls")
    Call<ApiResponse<Object>> logMissedCall(@Body MissedCallRequest request);
    
    @POST("api/v1/register_device")
    Call<ApiResponse<DeviceRegistrationResponse>> registerDevice(@Body DeviceRegistrationRequest request);
    
    @GET("api/v1/logs")
    Call<ApiResponse<LogsResponse>> getLogs(
        @Query("device_id") String deviceId,
        @Query("page") int page,
        @Query("limit") int limit
    );
    
    @POST("api/v1/opt_out")
    Call<ApiResponse<Object>> optOut(@Body OptOutRequest request);
    
    @GET("api/v1/health")
    Call<ApiResponse<Object>> healthCheck();
}