package atirek.pothiwala.picker;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;

public class AudioPlayer {

    public interface OnPlayerListener {

        void OnStart();

        void OnPause();

        void OnStop(boolean isComplete);

        void OnTimer(int currentSeconds, int totalSeconds);
    }

    private Context context;
    private OnPlayerListener listener;
    private MediaPlayer player;
    private String audioPath;
    private Handler handler = new Handler();

    public AudioPlayer(Context context, String audioPath, OnPlayerListener listener) {
        this.context = context;
        this.audioPath = audioPath;
        this.listener = listener;
    }

    private void setupPlayer() {
        player = MediaPlayer.create(context, Uri.parse(audioPath));
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                listener.OnStop(true);
            }
        });
        player.prepareAsync();
    }

    public void startPlaying() {
        if (player == null) {
            setupPlayer();
        }
        boolean isPlaying = player.isPlaying();
        if (isPlaying) {
            player.pause();
            listener.OnPause();
        } else {
            player.start();
            listener.OnStart();
            updateMediaTime();
        }
    }

    public void stopPlaying() {
        if (player != null && player.isPlaying()) {
            player.stop();
            listener.OnStop(false);
        }
    }

    public void releasePlayer() {
        if (player != null) {
            player.stop();
            listener.OnStop(false);
            player.reset();
            player.release();
            player = null;
        }
    }

    private void updateMediaTime() {
        handler.postDelayed(UpdateMediaTime, 0);
    }

    private Runnable UpdateMediaTime = new Runnable() {
        public void run() {
            listener.OnTimer(player.getCurrentPosition(), player.getDuration());
            if (player.isPlaying()) {
                handler.postDelayed(this, 1000);
            }
        }
    };
}
