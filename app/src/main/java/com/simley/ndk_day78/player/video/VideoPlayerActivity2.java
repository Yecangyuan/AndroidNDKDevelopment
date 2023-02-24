package com.simley.ndk_day78.player.video;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.opengl.YEGLSurfaceView;
import com.simley.ndk_day78.player.YEPlayer;
import com.simley.ndk_day78.player.audio.listener.IPlayerListener;
import com.simley.ndk_day78.player.audio.listener.WlOnPreparedListener;
import com.simley.ndk_day78.player.audio.ui.utils.DisplayUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideoPlayerActivity2 extends AppCompatActivity {

    private YEPlayer yePlayer;
    private TextView tvTime;
    private YEGLSurfaceView yeglSurfaceView;
    private SeekBar seekBar;
    private int position;
    private boolean seek = false;
    List<String> paths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player2);
        tvTime = findViewById(R.id.tv_time);
        yeglSurfaceView = findViewById(R.id.wlglsurfaceview);
        seekBar = findViewById(R.id.seekbar);
        checkPermission();
        yePlayer = new YEPlayer();
        yePlayer.setYeglSurfaceView(yeglSurfaceView);
        File file = new File(Environment.getExternalStorageDirectory(), "input.mkv");
        paths.add(file.getAbsolutePath());
        file = new File(Environment.getExternalStorageDirectory(), "input.avi");
        paths.add(file.getAbsolutePath());

        file = new File(Environment.getExternalStorageDirectory(), "input.rmvb");
        paths.add(file.getAbsolutePath());
        paths.add("http://mn.maliuedu.com/music/input.mp4");
        yePlayer.setPlayerListener(new IPlayerListener() {
            @Override
            public void onLoad(boolean load) {

            }

            @Override
            public void onCurrentTime(int currentTime, int totalTime) {
                if (!seek && totalTime > 0) {
                    runOnUiThread(() -> {
                        seekBar.setProgress(currentTime * 100 / totalTime);
                        tvTime.setText(DisplayUtil.secdsToDateFormat(currentTime)
                                + "/" + DisplayUtil.secdsToDateFormat(totalTime));
                    });

                }
            }

            @Override
            public void onError(int code, String msg) {

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
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                position = progress * yePlayer.getDuration() / 100;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seek = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                yePlayer.seekTo(position);
                seek = false;
            }
        });
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);
        }
    }

    public void begin(View view) {

        yePlayer.setWlOnPreparedListener(new WlOnPreparedListener() {
            @Override
            public void onPrepared() {
                Log.d(this.getClass().getSimpleName(), "准备好了，可以开始播放声音了");
                yePlayer.start();
            }

        });
//音视频面试   30道 心里分析  切入点   步骤
//       File file = new File(Environment.getExternalStorageDirectory(),"input.rmvb");
        String mp4File = new File(Environment.getExternalStorageDirectory(), "/Movies/kali_hacker_video.mp4").getAbsolutePath();
        yePlayer.setDataSource(mp4File);
//       yePlayer.setDataSource(file.getAbsolutePath());
//       yePlayer.setDataSource("rtmp://58.200.131.2:1935/livetv/cctv1");
//        yePlayer.setDataSource("http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8");
//        yePlayer.setDataSource("http://mn.maliuedu.com/music/input.mp4");
//        yePlayer.setDataSource("/mnt/shared/Other/testvideo/楚乔传第一集.mp4");
//        yePlayer.setDataSource("/mnt/shared/Other/testvideo/屌丝男士.mov");
//        yePlayer.setDataSource("http://ngcdn004.cnr.cn/live/dszs/index12.m3u8");
        yePlayer.prepare();
    }

    public void pause(View view) {
        yePlayer.pause();
    }

    public void resume(View view) {
        yePlayer.resume();
    }

    public void stop(View view) {
        yePlayer.stop();
    }


    public void next(View view) {
        // yePlayer.playNext("/mnt/shared/Other/testvideo/楚乔传第一集.mp4");
    }

    public void speed1(View view) {
        yePlayer.setSpeed(1.5f);
    }

    public void speed2(View view) {
        yePlayer.setSpeed(2.0f);
    }

}