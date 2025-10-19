package com.demoody.missedcall.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class PermissionUtils {
    
    public static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG
    };
    
    public static final String[] OPTIONAL_PERMISSIONS = {
        Manifest.permission.POST_NOTIFICATIONS
    };
    
    public static boolean hasRequiredPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
    
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return hasPermission(context, Manifest.permission.POST_NOTIFICATIONS);
        }
        return true; // Not required for older versions
    }
    
    public static String[] getMissingRequiredPermissions(Context context) {
        java.util.List<String> missing = new java.util.ArrayList<>();
        
        for (String permission : REQUIRED_PERMISSIONS) {
            if (!hasPermission(context, permission)) {
                missing.add(permission);
            }
        }
        
        return missing.toArray(new String[0]);
    }
    
    public static String[] getAllRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] allPermissions = new String[REQUIRED_PERMISSIONS.length + 1];
            System.arraycopy(REQUIRED_PERMISSIONS, 0, allPermissions, 0, REQUIRED_PERMISSIONS.length);
            allPermissions[REQUIRED_PERMISSIONS.length] = Manifest.permission.POST_NOTIFICATIONS;
            return allPermissions;
        }
        return REQUIRED_PERMISSIONS;
    }
}