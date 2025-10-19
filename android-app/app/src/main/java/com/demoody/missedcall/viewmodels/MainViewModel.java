package com.demoody.missedcall.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.demoody.missedcall.MissedCallApplication;
import com.demoody.missedcall.db.MissedCallDao;
import com.demoody.missedcall.db.MissedCallEntity;

import java.util.List;

public class MainViewModel extends AndroidViewModel {
    
    private MissedCallDao missedCallDao;
    private LiveData<List<MissedCallEntity>> recentCalls;
    private LiveData<Integer> totalCalls;
    private LiveData<Integer> sentCount;
    private LiveData<Integer> failedCount;
    private LiveData<Integer> pendingCount;
    
    public MainViewModel(@NonNull Application application) {
        super(application);
        
        MissedCallApplication app = (MissedCallApplication) application;
        missedCallDao = app.getDatabase().missedCallDao();
        
        // Initialize LiveData
        recentCalls = missedCallDao.getAllCallsLive();
        totalCalls = missedCallDao.getTotalCount();
        sentCount = missedCallDao.getCountByStatus("SENT");
        failedCount = missedCallDao.getCountByStatus("FAILED");
        pendingCount = missedCallDao.getCountByStatus("PENDING");
    }
    
    public LiveData<List<MissedCallEntity>> getRecentCalls() {
        return recentCalls;
    }
    
    public LiveData<Integer> getTotalCalls() {
        return totalCalls;
    }
    
    public LiveData<Integer> getSentCount() {
        return sentCount;
    }
    
    public LiveData<Integer> getFailedCount() {
        return failedCount;
    }
    
    public LiveData<Integer> getPendingCount() {
        return pendingCount;
    }
}