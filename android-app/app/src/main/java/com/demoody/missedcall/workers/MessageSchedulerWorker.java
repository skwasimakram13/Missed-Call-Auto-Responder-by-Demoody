package com.demoody.missedcall.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.demoody.missedcall.MissedCallApplication;
import com.demoody.missedcall.db.MissedCallDao;
import com.demoody.missedcall.db.MissedCallEntity;
import com.demoody.missedcall.network.ApiClient;
import com.demoody.missedcall.network.ApiService;
import com.demoody.missedcall.network.requests.MissedCallRequest;
import com.demoody.missedcall.network.responses.ApiResponse;
import com.demoody.missedcall.utils.DeviceUtils;
import com.demoody.missedcall.utils.PreferenceManager;

import retrofit2.Call;
import retrofit2.Response;

public class MessageSchedulerWorker extends Worker {
    
    private static final String TAG = "MessageSchedulerWorker";
    
    public static final String KEY_PHONE_NUMBER = "phone_number";
    public static final String KEY_CALL_TIME = "call_time";
    public static final String KEY_MESSAGE_TEXT = "message_text";
    
    private MissedCallDao missedCallDao;
    private ApiService apiService;
    private PreferenceManager preferenceManager;
    
    public MessageSchedulerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        
        MissedCallApplication app = (MissedCallApplication) context.getApplicationContext();
        this.missedCallDao = app.getDatabase().missedCallDao();
        this.apiService = ApiClient.getInstance(context).getApiService();
        this.preferenceManager = new PreferenceManager(context);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        String phoneNumber = getInputData().getString(KEY_PHONE_NUMBER);
        long callTime = getInputData().getLong(KEY_CALL_TIME, 0);
        String messageText = getInputData().getString(KEY_MESSAGE_TEXT);
        
        if (phoneNumber == null || callTime == 0 || messageText == null) {
            Log.e(TAG, "Invalid input data for worker");
            return Result.failure();
        }
        
        Log.d(TAG, "Processing scheduled message for: " + phoneNumber);
        
        try {
            // Find the missed call record
            MissedCallEntity missedCall = missedCallDao.findByPhoneAndTime(phoneNumber, callTime);
            if (missedCall == null) {
                Log.w(TAG, "Missed call record not found");
                return Result.failure();
            }
            
            // Check if already processed
            if (!"PENDING".equals(missedCall.status)) {
                Log.d(TAG, "Call already processed with status: " + missedCall.status);
                return Result.success();
            }
            
            // Check if auto-responder is still enabled
            if (!preferenceManager.isAutoResponderEnabled()) {
                Log.d(TAG, "Auto-responder disabled, skipping message");
                missedCall.status = "SKIPPED";
                missedCall.errorMessage = "Auto-responder disabled";
                missedCallDao.update(missedCall);
                return Result.success();
            }
            
            // Check business hours if enabled
            if (preferenceManager.isBusinessHoursEnabled() && !isWithinBusinessHours()) {
                Log.d(TAG, "Outside business hours, skipping message");
                missedCall.status = "SKIPPED";
                missedCall.errorMessage = "Outside business hours";
                missedCallDao.update(missedCall);
                return Result.success();
            }
            
            // Send message via backend
            boolean success = sendMessageViaBackend(phoneNumber, callTime, messageText);
            
            if (success) {
                missedCall.status = "SENT";
                missedCall.sentAt = System.currentTimeMillis();
                missedCallDao.update(missedCall);
                Log.d(TAG, "Message sent successfully for: " + phoneNumber);
                return Result.success();
            } else {
                missedCall.attemptCount++;
                if (missedCall.attemptCount >= 3) {
                    missedCall.status = "FAILED";
                    missedCall.errorMessage = "Max retry attempts reached";
                    missedCallDao.update(missedCall);
                    Log.e(TAG, "Max retry attempts reached for: " + phoneNumber);
                    return Result.failure();
                } else {
                    missedCallDao.update(missedCall);
                    Log.w(TAG, "Message send failed, will retry. Attempt: " + missedCall.attemptCount);
                    return Result.retry();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing scheduled message", e);
            return Result.failure();
        }
    }
    
    private boolean sendMessageViaBackend(String phoneNumber, long callTime, String messageText) {
        try {
            String deviceId = DeviceUtils.getDeviceId(getApplicationContext());
            
            MissedCallRequest request = new MissedCallRequest();
            request.deviceId = deviceId;
            request.phoneNumber = phoneNumber;
            request.callTime = callTime;
            request.messageText = messageText;
            request.delayMinutes = preferenceManager.getDelayMinutes();
            
            Call<ApiResponse<Object>> call = apiService.logMissedCall(request);
            Response<ApiResponse<Object>> response = call.execute();
            
            if (response.isSuccessful() && response.body() != null) {
                ApiResponse<Object> apiResponse = response.body();
                if (apiResponse.success) {
                    Log.d(TAG, "Backend API call successful");
                    return true;
                } else {
                    Log.e(TAG, "Backend API error: " + apiResponse.error);
                    return false;
                }
            } else {
                Log.e(TAG, "Backend API call failed: " + response.code());
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Exception during backend API call", e);
            return false;
        }
    }
    
    private boolean isWithinBusinessHours() {
        // Simple business hours check - can be enhanced
        int currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        return currentHour >= 9 && currentHour <= 18; // 9 AM to 6 PM
    }
}