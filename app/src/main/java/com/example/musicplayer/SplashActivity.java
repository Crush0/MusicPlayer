package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.Window;

import com.example.musicplayer.Service.MusicService;

/**
 * 开屏页
 *
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends Activity {

    private static final int sleepTime = 2000;
    private long startTime;
    private ServiceConnection serviceConnection;

    @Override
    protected void onCreate(Bundle arg0) {
        final View view = View.inflate(this, R.layout.activity_splash, null);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(view);
        super.onCreate(arg0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startTime = System.currentTimeMillis();
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBinder("musicService", service);
                intent.putExtras(bundle);
                long nowTime = System.currentTimeMillis();
                if (nowTime - startTime < sleepTime) {
                    new Handler().postDelayed(() -> {
                        startActivity(intent);
                        finish();
                    }, sleepTime - (nowTime - startTime));
                } else{
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        bindService(new Intent(this, MusicService.class), serviceConnection, BIND_AUTO_CREATE);
    }
    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }
}