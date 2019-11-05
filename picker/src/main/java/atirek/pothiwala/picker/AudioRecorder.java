package atirek.pothiwala.picker;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
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
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            File audioFile = FileConfigure.generateNewFile(context, "3gp");
            audioFilePath = audioFile.getAbsolutePath();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                recorder.setOutputFile(audioFile);
            } else {
                recorder.setOutputFile(audioFilePath);
            }
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

            recorder.prepare();
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
                recorder.reset();
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
