package com.demoody.missedcall.network;

import android.content.Context;

import com.demoody.missedcall.BuildConfig;
import com.demoody.missedcall.utils.PreferenceManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {
    
    private static ApiClient instance;
    private ApiService apiService;
    private Retrofit retrofit;
    
    private ApiClient(Context context) {
        PreferenceManager prefManager = new PreferenceManager(context);
        String baseUrl = prefManager.getApiBaseUrl();
        
        if (baseUrl.isEmpty()) {
            baseUrl = BuildConfig.API_BASE_URL;
        }
        
        // Ensure base URL ends with /
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS);
        
        // Add logging in debug builds
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(logging);
        }
        
        // Add authentication interceptor
        httpClient.addInterceptor(new AuthInterceptor(context));
        
        retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build();
        
        apiService = retrofit.create(ApiService.class);
    }
    
    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }
    
    public ApiService getApiService() {
        return apiService;
    }
    
    public void updateBaseUrl(Context context, String newBaseUrl) {
        instance = new ApiClient(context);
    }
}