package com.grarak.ytfetcher.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.grarak.ytfetcher.LoginActivity;
import com.grarak.ytfetcher.R;
import com.grarak.ytfetcher.utils.Utils;
import com.grarak.ytfetcher.utils.server.youtube.YoutubeSearchResult;

import java.util.concurrent.atomic.AtomicBoolean;

public class MusicPlayerNotification {

    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL = "music_channel";

    private NotificationManager manager;
    private MusicPlayerService service;

    private AtomicBoolean fetching = new AtomicBoolean();
    private AtomicBoolean playing = new AtomicBoolean();
    private YoutubeSearchResult result;
    private Bitmap playingBitmap;

    MusicPlayerNotification(MusicPlayerService service) {
        this.service = service;

        manager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    private Bitmap getBitmap(String url) {
        try {
            return Glide.with(service).asBitmap().load(url).submit().get();
        } catch (Exception ignored) {
            return BitmapFactory.decodeResource(service.getResources(), R.mipmap.ic_launcher);
        }
    }

    private PendingIntent getBroadcast(String action) {
        return PendingIntent.getBroadcast(service, 0, new Intent(action), 0);
    }

    void showProgress(YoutubeSearchResult result) {
        fetching.set(true);
        playing.set(false);
        this.result = result;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
                        .setContentTitle(service.getString(R.string.loading))
                        .setContentText(result.title)
                        .setSmallIcon(R.drawable.ic_bookmark_music)
                        .setProgress(0, 0, true);

        service.startForeground(NOTIFICATION_ID, builder.build());
    }

    void showFailure(YoutubeSearchResult result) {
        fetching.set(false);
        playing.set(false);
        this.result = result;

        Intent intent = new Intent(service, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, intent, 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
                        .setContentTitle(service.getString(R.string.failed))
                        .setContentText(result.title)
                        .setContentIntent(contentIntent)
                        .setSmallIcon(R.drawable.ic_bookmark_music);

        manager.notify(NOTIFICATION_ID, builder.build());
        service.stopForeground(false);
    }

    void showPlay(YoutubeSearchResult result) {
        fetching.set(false);
        playing.set(true);
        this.result = result;
        new Thread(() -> {
            NotificationCompat.Builder builder = baseBuilder(result,
                    playingBitmap = getBitmap(result.thumbnail), true);

            service.startForeground(NOTIFICATION_ID, builder.build());
        }).start();
    }

    void showPause() {
        fetching.set(false);
        playing.set(false);
        if (result == null) return;
        new Thread(() -> {
            NotificationCompat.Builder builder = baseBuilder(result,
                    playingBitmap == null ? getBitmap(result.thumbnail) : playingBitmap, false)
                    .setAutoCancel(true);

            service.startForeground(NOTIFICATION_ID, builder.build());
        }).start();
    }

    void stop() {
        manager.cancel(NOTIFICATION_ID);
    }

    private NotificationCompat.Builder baseBuilder(
            YoutubeSearchResult result, Bitmap bitmap, boolean play) {
        Intent intent = new Intent(service, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(service, 0, intent, 0);

        String[] titleFormatted = Utils.formatResultTitle(result);

        android.support.v4.media.app.NotificationCompat.MediaStyle mediaStyle =
                new android.support.v4.media.app.NotificationCompat.DecoratedMediaCustomViewStyle();
        mediaStyle.setShowActionsInCompactView(2);

        return new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL)
                .setContentTitle(titleFormatted[0])
                .setContentText(titleFormatted[1])
                .setSubText(result.duration)
                .setSmallIcon(R.drawable.ic_bookmark_music)
                .setLargeIcon(bitmap)
                .setContentIntent(contentIntent)
                .addAction(new NotificationCompat.Action(
                        play ? R.drawable.ic_pause : R.drawable.ic_play,
                        service.getString(play ? R.string.pause : R.string.play),
                        getBroadcast(MusicPlayerService.ACTION_MUSIC_PLAY_PAUSE)))
                .addAction(new NotificationCompat.Action(
                        R.drawable.ic_skip_next,
                        service.getString(R.string.next),
                        getBroadcast(MusicPlayerService.ACTION_MUSIC_NEXT)))
                .addAction(new NotificationCompat.Action(
                        R.drawable.ic_stop,
                        service.getString(R.string.stop),
                        getBroadcast(MusicPlayerService.ACTION_MUSIC_PLAYER_STOP)))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setStyle(mediaStyle);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL, service.getString(R.string.music_player),
                NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);

        manager.createNotificationChannel(channel);
    }
}
