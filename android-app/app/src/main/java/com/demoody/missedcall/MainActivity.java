package com.demoody.missedcall;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.demoody.missedcall.databinding.ActivityMainBinding;
import com.demoody.missedcall.services.MissedCallService;
import com.demoody.missedcall.utils.DeviceUtils;
import com.demoody.missedcall.utils.PermissionUtils;
import com.demoody.missedcall.utils.PreferenceManager;
import com.demoody.missedcall.viewmodels.MainViewModel;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int BATTERY_OPTIMIZATION_REQUEST_CODE = 1002;
    
    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private PreferenceManager preferenceManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(this);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        setupUI();
        setupObservers();
        
        // Handle first run
        if (preferenceManager.isFirstRun()) {
            showWelcomeDialog();
            preferenceManager.setFirstRun(false);
        }
        
        // Check permissions on startup
        checkAndRequestPermissions();
    }
    
    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        
        // Auto-responder toggle
        binding.switchAutoResponder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !PermissionUtils.hasRequiredPermissions(this)) {
                binding.switchAutoResponder.setChecked(false);
                checkAndRequestPermissions();
                return;
            }
            
            preferenceManager.setAutoResponderEnabled(isChecked);
            updateServiceState(isChecked);
            updateUI();
        });
        
        // Settings button
        binding.btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        
        // Battery optimization button
        binding.btnBatteryOptimization.setOnClickListener(v -> {
            requestBatteryOptimizationExemption();
        });
        
        // Test message button
        binding.btnTestMessage.setOnClickListener(v -> {
            // For testing purposes - simulate a missed call
            if (preferenceManager.isAutoResponderEnabled()) {
                simulateMissedCall();
            } else {
                Toast.makeText(this, "Enable auto-responder first", Toast.LENGTH_SHORT).show();
            }
        });
        
        updateUI();
    }
    
    private void setupObservers() {
        // Observe call statistics
        viewModel.getTotalCalls().observe(this, count -> {
            binding.textTotalCalls.setText(String.valueOf(count != null ? count : 0));
        });
        
        viewModel.getSentCount().observe(this, count -> {
            binding.textSentMessages.setText(String.valueOf(count != null ? count : 0));
        });
        
        viewModel.getFailedCount().observe(this, count -> {
            binding.textFailedMessages.setText(String.valueOf(count != null ? count : 0));
        });
        
        // Observe recent calls
        viewModel.getRecentCalls().observe(this, calls -> {
            // Update recent calls list if you have a RecyclerView
            Log.d(TAG, "Recent calls updated: " + (calls != null ? calls.size() : 0));
        });
    }
    
    private void updateUI() {
        boolean isEnabled = preferenceManager.isAutoResponderEnabled();
        boolean hasPermissions = PermissionUtils.hasRequiredPermissions(this);
        
        binding.switchAutoResponder.setChecked(isEnabled);
        
        // Update status
        if (isEnabled && hasPermissions) {
            binding.textStatus.setText("Active - Monitoring missed calls");
            binding.textStatus.setTextColor(getColor(R.color.status_active));
        } else if (!hasPermissions) {
            binding.textStatus.setText("Permissions required");
            binding.textStatus.setTextColor(getColor(R.color.status_error));
        } else {
            binding.textStatus.setText("Inactive");
            binding.textStatus.setTextColor(getColor(R.color.status_inactive));
        }
        
        // Update message template preview
        String template = preferenceManager.getMessageTemplate();
        if (template.length() > 100) {
            template = template.substring(0, 100) + "...";
        }
        binding.textMessagePreview.setText(template);
        
        // Update delay
        binding.textDelay.setText(preferenceManager.getDelayMinutes() + " minutes");
    }
    
    private void updateServiceState(boolean enabled) {
        Intent serviceIntent = new Intent(this, MissedCallService.class);
        
        if (enabled) {
            serviceIntent.setAction(MissedCallService.ACTION_START_MONITORING);
            startForegroundService(serviceIntent);
        } else {
            serviceIntent.setAction(MissedCallService.ACTION_STOP_MONITORING);
            startService(serviceIntent);
        }
    }
    
    private void checkAndRequestPermissions() {
        String[] missingPermissions = PermissionUtils.getMissingRequiredPermissions(this);
        
        if (missingPermissions.length > 0) {
            showPermissionRationaleDialog(missingPermissions);
        } else {
            // Check notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!PermissionUtils.hasNotificationPermission(this)) {
                    ActivityCompat.requestPermissions(this, 
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                        PERMISSION_REQUEST_CODE);
                }
            }
        }
    }
    
    private void showPermissionRationaleDialog(String[] permissions) {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs phone and call log permissions to detect missed calls and send automatic responses. These permissions are essential for the app to function.")
            .setPositiveButton("Grant Permissions", (dialog, which) -> {
                ActivityCompat.requestPermissions(this, 
                    PermissionUtils.getAllRequiredPermissions(), 
                    PERMISSION_REQUEST_CODE);
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                Toast.makeText(this, "Permissions are required for the app to work", Toast.LENGTH_LONG).show();
            })
            .setCancelable(false)
            .show();
    }
    
    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            
            try {
                startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST_CODE);
            } catch (Exception e) {
                // Fallback to general battery optimization settings
                Intent fallbackIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(fallbackIntent);
            }
        }
    }
    
    private void simulateMissedCall() {
        // For testing - simulate a missed call
        Intent serviceIntent = new Intent(this, MissedCallService.class);
        serviceIntent.setAction(MissedCallService.ACTION_MISSED_CALL);
        serviceIntent.putExtra(MissedCallService.EXTRA_PHONE_NUMBER, "+1234567890");
        serviceIntent.putExtra(MissedCallService.EXTRA_CALL_TIME, System.currentTimeMillis());
        
        startForegroundService(serviceIntent);
        Toast.makeText(this, "Test missed call simulated", Toast.LENGTH_SHORT).show();
    }
    
    private void showWelcomeDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Welcome to Missed Call Auto-Responder")
            .setMessage("This app automatically sends messages to callers when you miss their calls. " +
                       "To get started:\n\n" +
                       "1. Grant required permissions\n" +
                       "2. Enable auto-responder\n" +
                       "3. Customize your message template\n\n" +
                       "The app will run in the background and send messages after a configurable delay.")
            .setPositiveButton("Get Started", (dialog, which) -> {
                checkAndRequestPermissions();
            })
            .setCancelable(false)
            .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "Permissions granted! You can now enable auto-responder.", Toast.LENGTH_LONG).show();
                updateUI();
            } else {
                Toast.makeText(this, "Some permissions were denied. The app may not work properly.", Toast.LENGTH_LONG).show();
                
                // Check if we should show rationale
                boolean shouldShowRationale = false;
                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }
                
                if (!shouldShowRationale) {
                    // User selected "Don't ask again", guide them to settings
                    showPermissionSettingsDialog();
                }
            }
        }
    }
    
    private void showPermissionSettingsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Please enable the required permissions in app settings for the auto-responder to work.")
            .setPositiveButton("Open Settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_logs) {
            // TODO: Implement logs activity
            Toast.makeText(this, "Logs feature coming soon", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showAboutDialog() {
        String deviceInfo = "Device: " + DeviceUtils.getDeviceName() + "\n" +
                           "App Version: " + DeviceUtils.getAppVersion(this) + "\n" +
                           "Device ID: " + DeviceUtils.getDeviceId(this);
        
        new AlertDialog.Builder(this)
            .setTitle("About")
            .setMessage("Missed Call Auto-Responder\n" +
                       "by Demoody Technologies\n\n" +
                       "Automatically responds to missed calls with customizable messages.\n\n" +
                       deviceInfo)
            .setPositiveButton("OK", null)
            .show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}