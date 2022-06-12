package com.example.musicplayer.Service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.Utils.MusicHelper;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class MusicService extends Service {

    private Integer nowPlaying = 0;
    public MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private Thread seekThread;
    private boolean isFirst = true;
    private Integer playMode = 0;
    private boolean isPaused = true;
    private Handler handler;
    private List<MusicHelper.Music> musicList;
    private MainActivity mainActivity;
    public MusicService(){
        new Thread(() -> {
            musicList = new MusicHelper().getMusicList();
            isPrepared = true;
            String call = "javascript:setMusicList(" + JSON.toJSONString(musicList) + ")";
            Message message = handler.obtainMessage();
            message.what = 3;
            message.obj = call;
            handler.sendMessage(message);
            setDefaultMusic();
        }).start();
        initPlayer();
    }

    public Thread getSeekThread(){
        return new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()){
                if(mediaPlayer.isPlaying()) {
                    Message msg = new Message();
                    msg.what = 1;
                    msg.arg1 = mediaPlayer.getCurrentPosition();
                    msg.arg2 = mediaPlayer.getDuration();
                    handler.sendMessage(msg);
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void setPlayMode(Integer mode){
        playMode = mode;
    }

    public void initPlayer(){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(mp -> {
            Message msg = new Message();
            msg.what = 1;
            msg.arg1 = mediaPlayer.getCurrentPosition();
            msg.arg2 = mediaPlayer.getDuration();
            handler.sendMessage(msg);
            if(seekThread!=null){
                seekThread.interrupt();
            }
            seekThread = null;
            seekThread = getSeekThread();
            seekThread.start();
            if(!isFirst) {
                mediaPlayer.start();
                isPaused = false;
            } else {
                isFirst = false;
            }
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            if(playMode == 0){
                next(0);
            }
            else if(playMode == 1){
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }
            else if(playMode == 2){
                next(new Random().nextInt(musicList.size()-1));
            }
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e("MY_TAG","Error");
            return false;
        });
    }

    public void selectPlay(int index){
        if(index<0 || index>=musicList.size()){
            next(0);
        } else {
            nowPlaying = index;
            setMusic(nowPlaying);
        }
    }

    public void pauseAndPlay(){
        if(mediaPlayer.isPlaying()&&!isPaused){
            mediaPlayer.pause();
            isPaused = true;
        } else if(isPrepared&&isPaused){
            mediaPlayer.start();
            isPaused = false;
        }
    }

    public void setMusic(Integer index){
        nowPlaying = index;
        if(nowPlaying<0 || nowPlaying>=musicList.size()){
            nowPlaying = 0;
        }
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
        try {
            MusicHelper.Music music = musicList.get(nowPlaying);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(music.url);
            mediaPlayer.prepareAsync();
            Message message = handler.obtainMessage();
            message.what = 2;
            message.obj = music;
            message.arg1 = nowPlaying;
            message.arg2 = music.id;
            handler.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prev(){
        if(nowPlaying == 0){
            nowPlaying = musicList.size() - 1;
        }
        else{
            nowPlaying--;
        }
        setMusic(nowPlaying);
    }

    public void next(int offset){
        if(offset!=0){
            int tmp = nowPlaying;
            nowPlaying += offset;
            if(nowPlaying >= musicList.size()-1){
                nowPlaying -= musicList.size();
            }
            if(tmp==nowPlaying){
                nowPlaying++;
            }
        }
        if(nowPlaying<0){
            nowPlaying = 0;
        }
        if(nowPlaying >= musicList.size()){
            nowPlaying = 0;
        }
        else{
            nowPlaying++;
        }
        setMusic(nowPlaying);
    }

    public void setHandler(Handler handler){
        this.handler = handler;
    }

    public void setDefaultMusic(){
        MusicHelper.Music music = musicList.get(nowPlaying);
        try {
            mediaPlayer.setDataSource(music.url);
        } catch (IOException e) {
            Log.e("MY_TAG",e.getMessage());
        }
        mediaPlayer.prepareAsync();
        Message message = handler.obtainMessage();
        message.what = 2;
        message.obj = music;
        message.arg1 = nowPlaying;
        message.arg2 = music.id;
        handler.sendMessage(message);
    }

    public List<MusicHelper.Music> getMusicList() {
        return musicList;
    }

    public void setActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public class LocalBinder extends Binder{
        public MusicService getMusicService(){
            return MusicService.this;
        }
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }
}
