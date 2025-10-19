package com.demoody.missedcall.network;

import android.content.Context;

import com.demoody.missedcall.utils.PreferenceManager;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class AuthInterceptor implements Interceptor {
    
    private PreferenceManager preferenceManager;
    
    public AuthInterceptor(Context context) {
        this.preferenceManager = new PreferenceManager(context);
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        String apiToken = preferenceManager.getApiToken();
        
        if (apiToken != null && !apiToken.isEmpty()) {
            Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + apiToken)
                .header("User-Agent", "MissedCall-Android/1.0")
                .build();
            
            return chain.proceed(authenticatedRequest);
        }
        
        // Add User-Agent even without auth
        Request requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", "MissedCall-Android/1.0")
            .build();
        
        return chain.proceed(requestWithUserAgent);
    }
}