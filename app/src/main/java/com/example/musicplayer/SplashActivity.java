package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * 开屏页
 *
 */
public class SplashActivity extends Activity {

    private static final int sleepTime = 2000;

    @Override
    protected void onCreate(Bundle arg0) {
        final View view = View.inflate(this, R.layout.activity_splash, null);
        setContentView(view);
        super.onCreate(arg0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Thread(() -> {
            long start = System.currentTimeMillis();
            long costTime = System.currentTimeMillis() - start;
            if (sleepTime - costTime > 0) {
                try {
                    Thread.sleep(sleepTime - costTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //进入主页面
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }).start();
    }
}