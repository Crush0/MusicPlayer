package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.musicplayer.Service.MusicService;

import java.util.ArrayList;
import java.util.List;

/**
 * 开屏页
 *
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends Activity {

    private static final int sleepTime = 2000;
    private long startTime;
    private final int mRequestCode=100;
    private ServiceConnection serviceConnection;
    private final String[] permissions = {
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };
    private boolean permissionCallBack = false;
    private final List<String> mPermissionlist = new ArrayList<>();

    @Override
    protected void onCreate(Bundle arg0) {
        final View view = View.inflate(this, R.layout.activity_splash, null);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(view);
        initPermission();
        super.onCreate(arg0);
    }
    private void initPermission(){
        mPermissionlist.clear();//清空没有通过的权限
        //逐个判断你要的权限是否已经通过
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionlist.add(permission);//添加还未授予的权限
            }
        }

        //申请权限
        if(mPermissionlist.size()>0){//有权限没有通过，需要申请
            ActivityCompat.requestPermissions(this,permissions,mRequestCode);
        } else {
            permissionCallBack = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss=false;//有权限没有通过
        if (mRequestCode == requestCode) {
            for (int grantResult : grantResults) {
                if (grantResult == -1) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
            //如果有权限没有被允许
            if (hasPermissionDismiss) {
                Toast.makeText(this, "权限未被允许，部分功能可能异常", Toast.LENGTH_LONG).show();
            }
        }
        permissionCallBack = true;
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
                Thread permissionCallBackThread = new Thread(() -> {
                    while(!permissionCallBack){
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    long nowTime = System.currentTimeMillis();
                    if (nowTime - startTime < sleepTime) {
                        Looper.prepare();//增加部分
                        new Handler().postDelayed(() -> {
                            startActivity(intent);
                            finish();
                        }, sleepTime - (nowTime - startTime));
                        Looper.loop();
                    } else{
                        startActivity(intent);
                        finish();
                    }
                });
                permissionCallBackThread.start();
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