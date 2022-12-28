package com.simley.ndk_day78.player.audio.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.simley.ndk_day78.player.YEPlayer;
import com.simley.ndk_day78.player.audio.listener.IPlayerListener;

import java.io.File;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, IPlayerListener {
    private static final String TAG = "David";
    private YEPlayer yePlayer;

//    private final Handler handler = new Handler(Looper.getMainLooper());
    /*操作指令*/
    public static final String ACTION_OPT_MUSIC_PLAY = "ACTION_OPT_MUSIC_PLAY";
    public static final String ACTION_OPT_MUSIC_PAUSE = "ACTION_OPT_MUSIC_PAUSE";
    public static final String ACTION_OPT_MUSIC_RESUME = "ACTION_OPT_MUSIC_RESUME";
    public static final String ACTION_OPT_MUSIC_NEXT = "ACTION_OPT_MUSIC_NEXT";
    public static final String ACTION_OPT_MUSIC_LAST = "ACTION_OPT_MUSIC_LAST";
    public static final String ACTION_OPT_MUSIC_SEEK_TO = "ACTION_OPT_MUSIC_SEEK_TO";
    public static final String ACTION_OPT_MUSIC_LEFT = "ACTION_OPT_MUSIC_LEFT";
    public static final String ACTION_OPT_MUSIC_RIGHT = "ACTION_OPT_MUSIC_RIGHT";
    public static final String ACTION_OPT_MUSIC_CENTER = "ACTION_OPT_MUSIC_CENTER";
    public static final String ACTION_OPT_MUSIC_VOLUME = "ACTION_OPT_MUSIC_VOLUME";
    public static final String ACTION_OPT_MUSIC_SPEED_AN_NO_PITCH = "ACTION_OPT_MUSIC_SPEED_AN_NO_PITCH";
    public static final String ACTION_OPT_MUSIC_SPEED_NO_AN_PITCH = "ACTION_OPT_MUSIC_SPEED_NO_AN_PITCH";
    public static final String ACTION_OPT_MUSIC_SPEED_AN_PITCH = "ACTION_OPT_MUSIC_SPEED_AN_PITCH";
    public static final String ACTION_OPT_MUSIC_SPEED_PITCH_NOMAORL = "ACTION_OPT_MUSIC_SPEED_PITCH_NOMAORL";

    /*状态指令*/
    public static final String ACTION_STATUS_MUSIC_PLAY = "ACTION_STATUS_MUSIC_PLAY";
    public static final String ACTION_STATUS_MUSIC_PAUSE = "ACTION_STATUS_MUSIC_PAUSE";
    public static final String ACTION_STATUS_MUSIC_COMPLETE = "ACTION_STATUS_MUSIC_COMPLETE";
    public static final String ACTION_STATUS_MUSIC_DURATION = "ACTION_STATUS_MUSIC_DURATION";
    public static final String ACTION_STATUS_MUSIC_PLAYER_TIME = "ACTION_STATUS_MUSIC_PLAYER_TIME";
    public static final String PARAM_MUSIC_DURATION = "PARAM_MUSIC_DURATION";
    public static final String PARAM_MUSIC_SEEK_TO = "PARAM_MUSIC_SEEK_TO";
    public static final String PARAM_MUSIC_CURRENT_POSITION = "PARAM_MUSIC_CURRENT_POSITION";
    public static final String PARAM_MUSIC_IS_OVER = "PARAM_MUSIC_IS_OVER";

    private int mCurrentMusicIndex = 0;
    private MusicReceiver mMusicReceiver = new MusicReceiver();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initBoardCastReceiver();
        yePlayer = new YEPlayer();
        yePlayer.setPlayerListener(this);
        yePlayer.setWlOnPreparedListener(() -> {
            Log.d(MusicService.class.getSimpleName(), "准备好了，可以开始播放声音了");
            yePlayer.start();
        });
    }

    private void initBoardCastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_OPT_MUSIC_PLAY);
        intentFilter.addAction(ACTION_OPT_MUSIC_PAUSE);
        intentFilter.addAction(ACTION_OPT_MUSIC_RESUME);
        intentFilter.addAction(ACTION_OPT_MUSIC_NEXT);
        intentFilter.addAction(ACTION_OPT_MUSIC_LAST);
        intentFilter.addAction(ACTION_OPT_MUSIC_SEEK_TO);
        intentFilter.addAction(ACTION_OPT_MUSIC_LEFT);
        intentFilter.addAction(ACTION_OPT_MUSIC_RIGHT);
        intentFilter.addAction(ACTION_OPT_MUSIC_VOLUME);
        intentFilter.addAction(ACTION_OPT_MUSIC_CENTER);
        intentFilter.addAction(ACTION_OPT_MUSIC_SPEED_AN_NO_PITCH);
        intentFilter.addAction(ACTION_OPT_MUSIC_SPEED_NO_AN_PITCH);
        intentFilter.addAction(ACTION_OPT_MUSIC_SPEED_AN_PITCH);
        intentFilter.addAction(ACTION_OPT_MUSIC_SPEED_PITCH_NOMAORL);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMusicReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMusicReceiver);
    }

    //监听
    private void play(final int index) {
        String mp3File = new File(Environment.getExternalStorageDirectory(), "/Download/琵琶语-林海.mp3").getAbsolutePath();
//        String mp3File = new File(Environment.getExternalStorageDirectory(), "/Music/琵琶语-林海.mp3").getAbsolutePath();
        yePlayer.setDataSource(mp3File);
        yePlayer.prepare();
    }

    private void pause() {
        yePlayer.pause();
    }

    private void resume() {
        yePlayer.resume();
    }

    private void stop() {
    }

    private void next() {
    }

    private void last() {

    }

    private void seekTo(int position) {
        yePlayer.seekTo(position);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }

    @Override
    public void onLoad(boolean load) {

    }

    @Override
    public void onCurrentTime(int currentTime, int totalTime) {
        Intent intent = new Intent(ACTION_STATUS_MUSIC_PLAYER_TIME);
        intent.putExtra("currentTime", currentTime);
        intent.putExtra("totalTime", totalTime);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onError(int code, String msg) {
        Log.e(this.getClass().getSimpleName(), "code: " + code + "msg: " + msg);
    }

    @Override
    public void onPause(boolean pause) {

    }

    @Override
    public void onDbValue(int db) {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public String onNext() {
        return null;
    }

    class MusicReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive: " + action);
            switch (action) {
                case ACTION_OPT_MUSIC_PLAY:
                    play(mCurrentMusicIndex);
                    break;
                case ACTION_OPT_MUSIC_LAST:
                    last();
                    break;
                case ACTION_OPT_MUSIC_NEXT:
                    next();
                    break;
                case ACTION_OPT_MUSIC_SEEK_TO:
                    int position = intent.getIntExtra(MusicService.PARAM_MUSIC_SEEK_TO, 0);
                    seekTo(position);
                    break;
                case ACTION_OPT_MUSIC_RESUME:
                    resume();
                    break;
                case ACTION_OPT_MUSIC_PAUSE:
                    pause();
                    break;
                case ACTION_OPT_MUSIC_RIGHT:
                    yePlayer.setMute(0);
                    break;
                case ACTION_OPT_MUSIC_LEFT:
                    yePlayer.setMute(1);
                    break;
                case ACTION_OPT_MUSIC_CENTER:
                    yePlayer.setMute(2);
                    break;
                case ACTION_OPT_MUSIC_VOLUME:
//                    yePlayer.setVolume(i++);
//                    Log.i(TAG, "onReceive: " + i);
                    break;
                case ACTION_OPT_MUSIC_SPEED_AN_NO_PITCH:
                    yePlayer.setSpeed(1.5f);
                    yePlayer.setPitch(1.0f);
                    break;
                case ACTION_OPT_MUSIC_SPEED_NO_AN_PITCH:
                    yePlayer.setPitch(1.5f);
                    yePlayer.setSpeed(1.0f);
                    break;
                case ACTION_OPT_MUSIC_SPEED_AN_PITCH:
                    yePlayer.setSpeed(1.5f);
                    yePlayer.setPitch(1.5f);
                    break;
                case ACTION_OPT_MUSIC_SPEED_PITCH_NOMAORL:
//                yePlayer.setSpeed(1.0f);
//                yePlayer.setPitch(1.0f);
                    yePlayer.stop();
                    break;
            }
        }
    }

//    int i = 0;
}
