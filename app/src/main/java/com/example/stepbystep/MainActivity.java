package com.example.stepbystep;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 1001;
    private static final float SHAKE_THRESHOLD = 2.5f;
    private static final int SHAKE_DEBOUNCE_MS = 500;

    // Sensor Management
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private Sensor accelerometerSensor;
    private boolean isStepSensorAvailable = false;
    private boolean isAccelSensorAvailable = false;

    // Live Sensor Baseline & Session Delta
    private int currentHardwareSteps = 0;
    private int initialSessionStepCount = 0;
    private int sessionSteps = 0;

    private int totalShakes = 0;
    private int initialSessionShakeCount = 0;
    private int sessionShakes = 0;
    private long lastShakeTime = 0;

    // Session State & Timer
    private boolean isSessionActive = false;
    private long elapsedDurationSeconds = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isSessionActive) {
                elapsedDurationSeconds++;
                updateTimerUI(elapsedDurationSeconds);
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    // Database & History UI
    private DBHelper dbHelper;
    private ActivityAdapter activityAdapter;
    private List<ActivityItem> activityList = new ArrayList<>();

    // Views
    private TextView tvStepCount, tvShakeCount, tvTimer;
    private TextView tvTotalSteps, tvTotalShakes, tvRecordSteps, tvRecordShakes;
    private TextView tvStatusBadge;
    private View viewLiveDot;
    private Button btnStart, btnStop;
    private RecyclerView rvHistory;
    private View layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);

        initViews();
        setupSensors();
        setupRecyclerView();
        checkPermissions();
        loadHistoryData();
    }

    private void initViews() {
        tvStepCount = findViewById(R.id.tvStepCount);
        tvShakeCount = findViewById(R.id.tvShakeCount);
        tvTimer = findViewById(R.id.tvTimer);

        tvTotalSteps = findViewById(R.id.tvTotalSteps);
        tvTotalShakes = findViewById(R.id.tvTotalShakes);
        tvRecordSteps = findViewById(R.id.tvRecordSteps);
        tvRecordShakes = findViewById(R.id.tvRecordShakes);

        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        viewLiveDot = findViewById(R.id.viewLiveDot);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);

        rvHistory = findViewById(R.id.rvHistory);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        btnStart.setOnClickListener(v -> startSession());
        btnStop.setOnClickListener(v -> stopAndSaveSession());
    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if (stepCounterSensor != null) {
                isStepSensorAvailable = true;
            } else {
                Toast.makeText(this, "อุปกรณ์นี้ไม่มี Step Counter Sensor (ใช้อุปกรณ์จริงเพื่อทดสอบนับก้าว)", Toast.LENGTH_LONG).show();
            }

            if (accelerometerSensor != null) {
                isAccelSensorAvailable = true;
            } else {
                Toast.makeText(this, "อุปกรณ์นี้ไม่มี Accelerometer Sensor", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupRecyclerView() {
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        activityAdapter = new ActivityAdapter(activityList,
                item -> showDetailDialog(item),
                (item, position) -> confirmDeleteSession(item)
        );
        rvHistory.setAdapter(activityAdapter);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "อนุญาตการเข้าถึง Sensor นับก้าวเรียบร้อย", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "การปฏิเสธ Permission จะทำให้นับก้าวไม่ได้", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            if (isStepSensorAvailable) {
                sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
            }
            if (isAccelSensorAvailable) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_UI);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalHardwareSteps = (int) event.values[0];
            currentHardwareSteps = totalHardwareSteps;

            if (isSessionActive) {
                if (initialSessionStepCount == -1) {
                    initialSessionStepCount = totalHardwareSteps;
                }
                sessionSteps = Math.max(0, currentHardwareSteps - initialSessionStepCount);
                tvStepCount.setText(String.valueOf(sessionSteps));
            }
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            if (gForce > SHAKE_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastShakeTime > SHAKE_DEBOUNCE_MS) {
                    lastShakeTime = currentTime;
                    totalShakes++;

                    if (isSessionActive) {
                        sessionShakes = totalShakes - initialSessionShakeCount;
                        tvShakeCount.setText(String.valueOf(sessionShakes));
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    // Session Control
    private void startSession() {
        isSessionActive = true;
        initialSessionStepCount = currentHardwareSteps;
        initialSessionShakeCount = totalShakes;
        sessionSteps = 0;
        sessionShakes = 0;

        elapsedDurationSeconds = 0;
        tvStepCount.setText("0");
        tvShakeCount.setText("0");
        updateTimerUI(0);

        if (tvStatusBadge != null) tvStatusBadge.setText(R.string.status_recording);
        if (viewLiveDot != null) viewLiveDot.setVisibility(View.VISIBLE);

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);

        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);

        Toast.makeText(this, "เริ่มบันทึก Session ใหม่เรียบร้อย", Toast.LENGTH_SHORT).show();
    }

    private void stopAndSaveSession() {
        if (!isSessionActive) return;

        isSessionActive = false;
        timerHandler.removeCallbacks(timerRunnable);

        if (tvStatusBadge != null) tvStatusBadge.setText(R.string.status_ready);
        if (viewLiveDot != null) viewLiveDot.setVisibility(View.GONE);

        btnStart.setEnabled(true);
        btnStop.setEnabled(false);

        long durationSecs = elapsedDurationSeconds;

        // Calculate Average per Minute with zero protection
        double avgStepsPerMin = durationSecs > 0 ? ((double) sessionSteps / durationSecs) * 60.0 : 0.0;
        double avgShakesPerMin = durationSecs > 0 ? ((double) sessionShakes / durationSecs) * 60.0 : 0.0;

        String dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        ActivityItem item = new ActivityItem(dateTime, sessionSteps, sessionShakes, durationSecs, avgStepsPerMin, avgShakesPerMin);
        long id = dbHelper.insertSession(item);

        if (id != -1) {
            Toast.makeText(this, "บันทึกกิจกรรมเรียบร้อย (Session ID: #" + id + ")", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "เกิดข้อผิดพลาดในการบันทึกกิจกรรม", Toast.LENGTH_SHORT).show();
        }

        tvStepCount.setText("0");
        tvShakeCount.setText("0");
        updateTimerUI(0);

        loadHistoryData();
    }

    private void updateTimerUI(long seconds) {
        long hrs = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;

        String timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs);
        tvTimer.setText(timeStr);
    }

    // Database Queries & UI Sync
    private void loadHistoryData() {
        activityList = dbHelper.getAllSessions();
        activityAdapter.updateData(activityList);

        if (activityList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
        }

        // Recalculate Totals & Records
        int totalSteps = dbHelper.getTotalSteps();
        int totalShakes = dbHelper.getTotalShakes();
        int maxSteps = dbHelper.getMaxSteps();
        int maxShakes = dbHelper.getMaxShakes();

        tvTotalSteps.setText(String.format(Locale.getDefault(), "%d ก้าว", totalSteps));
        tvTotalShakes.setText(String.format(Locale.getDefault(), "%d ครั้ง", totalShakes));
        tvRecordSteps.setText(String.format(Locale.getDefault(), "%d ก้าว", maxSteps));
        tvRecordShakes.setText(String.format(Locale.getDefault(), "%d ครั้ง", maxShakes));
    }

    // Detail Dialog & Delete Action
    private void showDetailDialog(ActivityItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_activity_detail, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView tvDetailId = dialogView.findViewById(R.id.tvDetailId);
        TextView tvDetailDateTime = dialogView.findViewById(R.id.tvDetailDateTime);
        TextView tvDetailSteps = dialogView.findViewById(R.id.tvDetailSteps);
        TextView tvDetailShakes = dialogView.findViewById(R.id.tvDetailShakes);
        TextView tvDetailDuration = dialogView.findViewById(R.id.tvDetailDuration);
        TextView tvDetailAvgSteps = dialogView.findViewById(R.id.tvDetailAvgSteps);
        TextView tvDetailAvgShakes = dialogView.findViewById(R.id.tvDetailAvgShakes);

        Button btnDialogDelete = dialogView.findViewById(R.id.btnDialogDelete);
        Button btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

        tvDetailId.setText("#" + item.getId());
        tvDetailDateTime.setText(item.getDateTime());
        tvDetailSteps.setText(String.format(Locale.getDefault(), "%d ก้าว", item.getSteps()));
        tvDetailShakes.setText(String.format(Locale.getDefault(), "%d ครั้ง", item.getShakes()));
        tvDetailDuration.setText(item.getFormattedDetailedDuration());
        tvDetailAvgSteps.setText(String.format(Locale.getDefault(), "%.1f ก้าว/นาที", item.getAvgStepsPerMin()));
        tvDetailAvgShakes.setText(String.format(Locale.getDefault(), "%.1f ครั้ง/นาที", item.getAvgShakesPerMin()));

        btnDialogClose.setOnClickListener(v -> dialog.dismiss());

        btnDialogDelete.setOnClickListener(v -> {
            dialog.dismiss();
            confirmDeleteSession(item);
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void confirmDeleteSession(ActivityItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_msg)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deleteSession(item.getId());
                    Toast.makeText(MainActivity.this, "ลบประวัติเรียบร้อย", Toast.LENGTH_SHORT).show();
                    loadHistoryData();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
