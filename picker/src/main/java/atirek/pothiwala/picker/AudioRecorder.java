package atirek.pothiwala.picker;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.CountDownTimer;

import java.io.File;

public class AudioRecorder {

    public interface OnRecordListener {
        void OnTimer(int currentSeconds, int totalSeconds);

        void OnComplete(String path);

        void OnException(String exception);
    }

    private MediaRecorder recorder = new MediaRecorder();
    private String audioFilePath;
    private int totalSeconds = 30;
    private boolean isRecording = false;

    public boolean isRecording() {
        return isRecording;
    }

    public void setTotalSeconds(int totalSeconds) {
        this.totalSeconds = totalSeconds;
    }

    private CountDownTimer countDownTimer = new CountDownTimer(totalSeconds * 1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            onRecordListener.OnTimer(Math.round(millisUntilFinished / 1000), totalSeconds);
        }

        @Override
        public void onFinish() {
            stopRecording(false);
        }
    };

    private Context context;
    private OnRecordListener onRecordListener;

    public AudioRecorder(Context context, OnRecordListener onRecordListener) {
        this.context = context;
        this.onRecordListener = onRecordListener;
    }

    private void setUpRecorder() {
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            audioFilePath = FileConfigure.generateNewFile(context, "3gp").getAbsolutePath();
            recorder.setOutputFile(audioFilePath);
            recorder.prepare();

            onRecordListener.OnTimer(0, totalSeconds);

        } catch (Exception e) {
            e.printStackTrace();
            onRecordListener.OnException(e.getMessage());
        }
    }

    public boolean startRecording() {
        try {

            if (recorder == null){
                setUpRecorder();
            }
            recorder.start();
            countDownTimer.start();

            isRecording = true;

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            onRecordListener.OnException(e.getMessage());

            return false;
        }

    }

    public boolean stopRecording(boolean isDelete) {
        try {

            if (recorder != null){
                recorder.stop();
                recorder.release();
                recorder = null;
            }
            countDownTimer.cancel();
            isRecording = false;

            if (isDelete){
                FileConfigure.deleteFile(new File(audioFilePath));
            } else {
                onRecordListener.OnComplete(audioFilePath);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            onRecordListener.OnException(e.getMessage());

            return false;
        }

    }

}
