package com.demoody.missedcall.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MissedCallDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(MissedCallEntity missedCall);
    
    @Update
    void update(MissedCallEntity missedCall);
    
    @Query("SELECT * FROM missed_calls WHERE id = :id")
    MissedCallEntity findById(int id);
    
    @Query("SELECT * FROM missed_calls WHERE phone_number = :phoneNumber AND call_time = :callTime")
    MissedCallEntity findByPhoneAndTime(String phoneNumber, long callTime);
    
    @Query("SELECT * FROM missed_calls WHERE phone_number = :phoneNumber AND call_time >= :timeWindow ORDER BY call_time DESC LIMIT 1")
    MissedCallEntity findRecentByPhone(String phoneNumber, long timeWindow);
    
    @Query("SELECT * FROM missed_calls WHERE status = 'PENDING' AND scheduled_time <= :currentTime ORDER BY scheduled_time ASC LIMIT :limit")
    List<MissedCallEntity> getPendingCalls(long currentTime, int limit);
    
    @Query("SELECT * FROM missed_calls ORDER BY call_time DESC LIMIT :limit OFFSET :offset")
    LiveData<List<MissedCallEntity>> getAllCalls(int limit, int offset);
    
    @Query("SELECT * FROM missed_calls ORDER BY call_time DESC")
    LiveData<List<MissedCallEntity>> getAllCallsLive();
    
    @Query("SELECT COUNT(*) FROM missed_calls")
    LiveData<Integer> getTotalCount();
    
    @Query("SELECT COUNT(*) FROM missed_calls WHERE status = :status")
    LiveData<Integer> getCountByStatus(String status);
    
    @Query("UPDATE missed_calls SET status = :status, sent_at = :sentAt, provider_message_id = :messageId WHERE id = :id")
    void updateStatusSent(int id, String status, long sentAt, String messageId);
    
    @Query("UPDATE missed_calls SET status = :status, attempt_count = :attemptCount, error_message = :errorMessage WHERE id = :id")
    void updateStatusFailed(int id, String status, int attemptCount, String errorMessage);
    
    @Query("UPDATE missed_calls SET status = :status, scheduled_time = :scheduledTime, attempt_count = :attemptCount WHERE id = :id")
    void updateStatusRetry(int id, String status, long scheduledTime, int attemptCount);
    
    @Query("DELETE FROM missed_calls WHERE call_time < :cutoffTime")
    void deleteOldCalls(long cutoffTime);
    
    @Query("SELECT COUNT(*) FROM missed_calls WHERE phone_number = :phoneNumber AND call_time >= :timeWindow")
    int getRecentCallCount(String phoneNumber, long timeWindow);
}