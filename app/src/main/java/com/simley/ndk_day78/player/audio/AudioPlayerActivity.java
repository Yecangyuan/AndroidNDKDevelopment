package com.simley.ndk_day78.player.audio;

import static com.simley.ndk_day78.player.audio.service.MusicService.ACTION_OPT_MUSIC_VOLUME;
import static com.simley.ndk_day78.player.audio.ui.widget.DiscView.DURATION_NEEDLE_ANIAMTOR;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.databinding.ActivityAudioPlayerBinding;
import com.simley.ndk_day78.player.PlayerActivity;
import com.simley.ndk_day78.player.YEPlayer;
import com.simley.ndk_day78.player.audio.service.MusicService;
import com.simley.ndk_day78.player.audio.ui.model.MusicData;
import com.simley.ndk_day78.player.audio.ui.utils.DisplayUtil;
import com.simley.ndk_day78.player.audio.ui.widget.DiscView;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AudioPlayerActivity extends AppCompatActivity implements View.OnClickListener, DiscView.IPlayInfo {

    private ActivityAudioPlayerBinding binding;
    private int totalTime;
    public static final String PARAM_MUSIC_LIST = "PARAM_MUSIC_LIST";

    private DiscView mDisc;
    private MusicReceiver mMusicReceiver = new MusicReceiver();
    DisplayUtil displayUtil = new DisplayUtil();
    private List<MusicData> mMusicDatas = new ArrayList<>();
    public static final int MUSIC_MESSAGE = 0;
    private int position;
    private boolean playState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAudioPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        String mp3File = new File(Environment.getExternalStorageDirectory(), "/Music/琵琶语-林海.mp3").getAbsolutePath();
//        yePlayer = new YEPlayer();
        setSupportActionBar(binding.toolBar);
        initMusicDatas();
        initMusicReceiver();
        initView();
        initMusicReceiver();
        DisplayUtil.makeStatusBarTransparent(this);
        new Thread(() -> {
            while (true) {
                optMusic(ACTION_OPT_MUSIC_VOLUME);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initView() {
        mDisc = findViewById(R.id.discview);
        mDisc.setPlayInfoListener(this);
        binding.ivLast.setOnClickListener(this);
        binding.ivNext.setOnClickListener(this);
        binding.ivPlayOrPause.setOnClickListener(this);
        binding.musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                position = totalTime * progress / 100;
                binding.tvCurrentTime.setText(displayUtil.duration2Time(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i(AudioPlayerActivity.class.getSimpleName(), "onStopTrackingTouch: " + position);
                seekTo(position);
            }
        });
        binding.tvCurrentTime.setText(displayUtil.duration2Time(0));
        binding.tvTotalTime.setText(displayUtil.duration2Time(0));
        mDisc.setMusicDataList(mMusicDatas);
    }

    private void initMusicReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PLAY);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PAUSE);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_DURATION);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_COMPLETE);
        intentFilter.addAction(MusicService.ACTION_STATUS_MUSIC_PLAYER_TIME);
        /*注册本地广播*/
        LocalBroadcastManager.getInstance(this).registerReceiver(mMusicReceiver, intentFilter);
    }

    private void initMusicDatas() {
        MusicData musicData1 = new MusicData(R.raw.music1, R.raw.ic_music1, "等你归来", "程响");
        MusicData musicData2 = new MusicData(R.raw.music2, R.raw.ic_music2, "Nightingale", "YANI");
        MusicData musicData3 = new MusicData(R.raw.music3, R.raw.ic_music3, "Cornfield Chase", "Hans Zimmer");
        mMusicDatas.add(musicData1);
        mMusicDatas.add(musicData2);
        mMusicDatas.add(musicData3);
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra(PARAM_MUSIC_LIST, (Serializable) mMusicDatas);
        startService(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivPlayOrPause:
                playState = !playState;
                if (playState) {
                    binding.ivPlayOrPause.setImageResource(R.drawable.ic_play);
                    pause();
                    mDisc.stop();
                } else {
                    binding.ivPlayOrPause.setImageResource(R.drawable.ic_pause);
                    resume();
                    mDisc.play();
                }
                break;
            case R.id.ivNext:
                mDisc.next();
            case R.id.ivLast:
                mDisc.last();
            default:
                break;
        }
    }

    private void displayCurrentTime(int currentTime, int totalTime) {
        binding.musicSeekBar.setProgress(currentTime * 100 / totalTime);
        this.totalTime = totalTime;
        binding.tvCurrentTime.setText(DisplayUtil.secdsToDateFormat(currentTime, totalTime));
        binding.tvTotalTime.setText(DisplayUtil.secdsToDateFormat(totalTime, totalTime));
    }


    @Override
    public void onMusicInfoChanged(String musicName, String musicAuthor) {
        getSupportActionBar().setTitle(musicName);
        getSupportActionBar().setSubtitle(musicAuthor);
    }

    @Override
    public void onMusicPicChanged(int musicPicRes) {
        displayUtil.try2UpdateMusicPicBackground(this, binding.rootLayout, musicPicRes);
    }

    @Override
    public void onMusicChanged(DiscView.MusicChangedStatus musicChangedStatus) {
        switch (musicChangedStatus) {
            case PLAY:
                play();
                break;
//            case PAUSE:
//                pause();
//                break;
            case NEXT: {
                next();
                break;
            }
            case LAST: {
                last();
                break;
            }
            case STOP: {
                stop();
                break;
            }
        }
    }

    private void play() {
        optMusic(MusicService.ACTION_OPT_MUSIC_PLAY);
    }

    private void pause() {
        optMusic(MusicService.ACTION_OPT_MUSIC_PAUSE);
    }

    public void resume() {
        optMusic(MusicService.ACTION_OPT_MUSIC_RESUME);
    }

    private void stop() {
        binding.ivPlayOrPause.setImageResource(R.drawable.ic_play);
//        binding.tvCurrentTime.setText(displayUtil.duration2Time(0));
//        binding.tvTotalTime.setText(displayUtil.duration2Time(0));
//        binding.musicSeekBar.setProgress(0);
    }

    private void next() {
        binding.rootLayout.postDelayed(() -> optMusic(MusicService.ACTION_OPT_MUSIC_NEXT), DURATION_NEEDLE_ANIAMTOR);
        binding.tvCurrentTime.setText(displayUtil.duration2Time(0));
        binding.tvTotalTime.setText(displayUtil.duration2Time(0));
    }

    private void last() {
        binding.rootLayout.postDelayed(() -> optMusic(MusicService.ACTION_OPT_MUSIC_LAST), DURATION_NEEDLE_ANIAMTOR);
        binding.tvCurrentTime.setText(displayUtil.duration2Time(0));
        binding.tvTotalTime.setText(displayUtil.duration2Time(0));
    }

    private void complete(boolean isOver) {
        if (isOver) {
            mDisc.stop();
        } else {
            mDisc.next();
        }
    }

    private void optMusic(final String action) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action));
    }

    private void seekTo(int position) {
        Intent intent = new Intent(MusicService.ACTION_OPT_MUSIC_SEEK_TO);
        intent.putExtra(MusicService.PARAM_MUSIC_SEEK_TO, position);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    class MusicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MusicService.ACTION_STATUS_MUSIC_PLAY)) {
                binding.ivPlayOrPause.setImageResource(R.drawable.ic_pause);
                int currentPosition = intent.getIntExtra(MusicService.PARAM_MUSIC_CURRENT_POSITION, 0);
                binding.musicSeekBar.setProgress(currentPosition);
                if (!mDisc.isPlaying()) {
                    mDisc.playOrPause();
                }
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_PAUSE)) {
                binding.ivPlayOrPause.setImageResource(R.drawable.ic_play);
                if (mDisc.isPlaying()) {
                    mDisc.playOrPause();
                }
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_DURATION)) {
                int duration = intent.getIntExtra(MusicService.PARAM_MUSIC_DURATION, 0);
//                updateMusicDurationInfo(duration);
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_COMPLETE)) {
                boolean isOver = intent.getBooleanExtra(MusicService.PARAM_MUSIC_IS_OVER, true);
                complete(isOver);
            } else if (action.equals(MusicService.ACTION_STATUS_MUSIC_PLAYER_TIME)) {
                int currentTime = intent.getIntExtra("currentTime", 0);
                int totalTime = intent.getIntExtra("totalTime", 0);
                displayCurrentTime(currentTime, totalTime);
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMusicReceiver);
    }

    public void left(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_LEFT);
    }

    public void right(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_RIGHT);
    }

    public void center(View view) {
        optMusic(MusicService.ACTION_OPT_MUSIC_CENTER);
    }

    public void speed(View view) {
    }

    public void pitch(View view) {
    }

    public void speedpitch(View view) {
    }

    public void normalspeedpitch(View view) {
    }

}