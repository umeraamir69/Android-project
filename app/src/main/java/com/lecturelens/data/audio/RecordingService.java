package com.lecturelens.data.audio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.lecturelens.R;

/**
 * Track 3 — foreground service that keeps LectureLens alive (and the mic active)
 * while a recording is in progress, so screen-off / backgrounding doesn't kill
 * capture (arch: long lectures; diagram 02 "WakeLock" note).
 *
 * <p>The {@link AudioRecorder} itself is owned by {@code UploadViewModel}; this
 * service exists for the foreground privilege + user-visible notification. The
 * Fragment starts it via {@link #start(Context)} before recording and stops it via
 * {@link #stop(Context)} after Stop/Save.
 *
 * <p>Foreground type is {@code microphone}: required in the manifest and, on
 * Android 14+ (API 34), also passed to {@code startForeground(...)}.
 */
public class RecordingService extends Service {

    private static final String ACTION_START = "com.lecturelens.action.START_RECORDING";
    private static final String ACTION_STOP = "com.lecturelens.action.STOP_RECORDING";
    private static final String CHANNEL_ID = "recording";
    private static final int NOTIFICATION_ID = 42;

    /** Start the foreground recording service. */
    public static void start(@NonNull Context context) {
        Intent intent = new Intent(context, RecordingService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /** Stop the service and dismiss the notification. */
    public static void stop(@NonNull Context context) {
        context.startService(new Intent(context, RecordingService.class).setAction(ACTION_STOP));
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopForegroundCompat();
            stopSelf();
            return START_NOT_STICKY;
        }
        startForegroundCompat();
        return START_NOT_STICKY;
    }

    private void startForegroundCompat() {
        createChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.recording_notification_title))
                .setContentText(getString(R.string.recording_notification_text))
                .setSmallIcon(R.drawable.ic_mic)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @SuppressWarnings("deprecation")
    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.recording_channel_name),
                        NotificationManager.IMPORTANCE_LOW);
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        return null; // not a bound service
    }
}
