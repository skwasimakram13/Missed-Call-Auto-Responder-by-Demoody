package com.demoody.missedcall.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "missed_calls",
    indices = {
        @Index(value = {"phone_number", "call_time"}, unique = true),
        @Index(value = "status"),
        @Index(value = "call_time")
    }
)
public class MissedCallEntity {
    
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    @ColumnInfo(name = "phone_number")
    public String phoneNumber;
    
    @ColumnInfo(name = "call_time")
    public long callTime;
    
    @ColumnInfo(name = "scheduled_time")
    public long scheduledTime;
    
    @ColumnInfo(name = "status")
    public String status; // PENDING, SENT, SKIPPED, FAILED
    
    @ColumnInfo(name = "attempt_count")
    public int attemptCount;
    
    @ColumnInfo(name = "message_text")
    public String messageText;
    
    @ColumnInfo(name = "provider_message_id")
    public String providerMessageId;
    
    @ColumnInfo(name = "sent_at")
    public Long sentAt;
    
    @ColumnInfo(name = "error_message")
    public String errorMessage;
    
    @ColumnInfo(name = "created_at")
    public long createdAt;
    
    public MissedCallEntity() {
        this.createdAt = System.currentTimeMillis();
        this.status = "PENDING";
        this.attemptCount = 0;
    }
    
    public MissedCallEntity(String phoneNumber, long callTime, long scheduledTime, String messageText) {
        this();
        this.phoneNumber = phoneNumber;
        this.callTime = callTime;
        this.scheduledTime = scheduledTime;
        this.messageText = messageText;
    }
}