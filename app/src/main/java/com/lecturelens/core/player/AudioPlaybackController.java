package com.lecturelens.core.player;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import java.io.File;

/**
 * Track 5 — thin ExoPlayer wrapper with prepare / seek / position helpers.
 * Owned by {@code LectureViewFragment} so all three tabs share one player.
 */
public class AudioPlaybackController {

    public interface Listener {
        void onPlaybackError(@NonNull String message);

        void onReady();
    }

    @Nullable private ExoPlayer player;
    @Nullable private Listener listener;
    private long pendingSeekMs = -1L;
    private boolean prepared;

    public void attach(@NonNull Context context) {
        if (player != null) {
            return;
        }
        player = new ExoPlayer.Builder(context.getApplicationContext()).build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    prepared = true;
                    if (pendingSeekMs >= 0L && player != null) {
                        player.seekTo(pendingSeekMs);
                        pendingSeekMs = -1L;
                    }
                    if (listener != null) {
                        listener.onReady();
                    }
                }
            }

            @Override
            public void onPlayerError(@NonNull androidx.media3.common.PlaybackException error) {
                prepared = false;
                if (listener != null) {
                    listener.onPlaybackError(error.getMessage() != null
                            ? error.getMessage()
                            : "Playback failed");
                }
            }
        });
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    /**
     * Loads local audio. Missing / null paths report an error and leave the
     * player idle so the UI can show an empty state.
     */
    public void prepare(@Nullable String audioPath) {
        if (player == null) {
            return;
        }
        prepared = false;
        pendingSeekMs = -1L;
        player.stop();
        player.clearMediaItems();

        if (audioPath == null || audioPath.trim().isEmpty()) {
            if (listener != null) {
                listener.onPlaybackError("NO_AUDIO");
            }
            return;
        }
        File file = new File(audioPath);
        if (!file.exists()) {
            if (listener != null) {
                listener.onPlaybackError("MISSING_FILE");
            }
            return;
        }
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)));
        player.prepare();
    }

    public void seekTo(long positionMs) {
        if (player == null || positionMs < 0L) {
            return;
        }
        if (!prepared) {
            pendingSeekMs = positionMs;
            return;
        }
        player.seekTo(positionMs);
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0L;
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public boolean isPrepared() {
        return prepared;
    }

    @Nullable
    public ExoPlayer getPlayer() {
        return player;
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
        prepared = false;
        pendingSeekMs = -1L;
        listener = null;
    }
}
