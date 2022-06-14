package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.alibaba.fastjson.JSON;
import com.example.musicplayer.Database.FavMusicHelper;
import com.example.musicplayer.Entity.Music;
import com.example.musicplayer.Service.MusicService;
import com.example.musicplayer.version.VersionControl;
import com.just.agentweb.AgentWeb;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public AgentWeb agentWeb;
    private FavMusicHelper favMusicHelper;
    private MusicService musicService;
    private static final String MESSAGES_CHANNEL = "mp_messages";
    private ClipboardManager clipboardManager;
    private Handler handler;
    private VersionControl versionControl;
    public NotificationManager notificationManager;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        LinearLayout linearLayout = findViewById(R.id.linearLayout);
        versionControl = new VersionControl(this);
        musicService = ((MusicService.LocalBinder)getIntent().getExtras().getBinder("musicService")).getMusicService();
        favMusicHelper = new FavMusicHelper(MainActivity.this, "fav_music.db", null, 1);
        agentWeb = AgentWeb.with(this)
                .setAgentWebParent(linearLayout, new LinearLayout.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT))
                .closeIndicator()
                .createAgentWeb()
                .ready()
                .go("file:///android_asset/Player.html");
        agentWeb.getJsInterfaceHolder().addJavaObject("control", new JSInterface());
        agentWeb.getAgentWebSettings().getWebSettings().setSupportZoom(false);
        agentWeb.getAgentWebSettings().getWebSettings().setAllowUniversalAccessFromFileURLs(true);
        handler = new Handler(msg->{
            switch (msg.what){
                case 1:{
                    //更新进度条
                    String call = "javascript:setVueTime("+msg.arg1+","+msg.arg2+")";
                    agentWeb.getJsAccessEntrace().callJs(call);
                    break;
                }
                case 2:{
                    //换歌
                    Music music = (Music) msg.obj;
                    String name;
                    try{
                        name = music.name.split(":")[1];
                    } catch (Exception e){
                        name = music.name;
                    }
                    String call = "javascript:setVueMusic("+msg.arg2+","+msg.arg1+",'"+name+"',"+"'"+music.article+"'"+",'"+music.imageUrl+"','"+music.url+"')";
                    Log.e("call",call);
                    agentWeb.getJsAccessEntrace().callJs(call);
                    break;
                }
                case 3:{
                    //传递歌曲列表
                    String call = (String) msg.obj;
                    agentWeb.getJsAccessEntrace().callJs(call);
                    break;
                }
                case 4:{
                    VersionControl.Version version = (VersionControl.Version) msg.obj;
                    //检查更新
                    AlertDialog.Builder normalDialog =
                            new AlertDialog.Builder(MainActivity.this);
                    normalDialog.setIcon(R.drawable.ic_logo);
                    normalDialog.setTitle("检查更新").setMessage("发现新版本 version:"+version.name+"，是否下载?");
                    normalDialog.setPositiveButton("立即下载",
                            (dialog, which) -> {
                                show();
                                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                                    //SD卡已装入
                                    File externalFilesDir = new File(getExternalFilesDir("update").getPath() + String.format("/update_%s.apk", version.name));
                                    if(externalFilesDir.exists()) {
                                        if (version.size != externalFilesDir.length()) {
                                            //文件存在且大小不一致
                                            externalFilesDir.delete();
                                        } else {
                                            String versionCode = externalFilesDir.getName().substring(0, externalFilesDir.getName().lastIndexOf(".")).split("_")[1];
                                            if (!versionControl.compareVersion(versionCode)) {
                                                if (!externalFilesDir.delete()) {
                                                    Toast.makeText(MainActivity.this, "删除旧版本失败", Toast.LENGTH_SHORT).show();
                                                }
                                            } else {
                                                Uri uri;
                                                if (android.os.Build.VERSION.SDK_INT >= 24) {
                                                    uri = FileProvider.getUriForFile(this.getApplicationContext(), "com.example.musicplayer.fileprovider", externalFilesDir);
                                                } else {
                                                    uri = Uri.fromFile(externalFilesDir);
                                                }
                                                if (Build.VERSION.SDK_INT >= 26) {
                                                    boolean hasInstallPermission = isHasInstallPermissionWithO(this);
                                                    if (!hasInstallPermission) {
                                                        startInstallPermissionSettingActivity(this);
                                                        Toast.makeText(this, "请允许歌吧安装未知来源应用权限", Toast.LENGTH_LONG).show();
                                                        return;
                                                    }
                                                }
                                                Intent intent = new Intent("android.intent.action.VIEW");
                                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                                if (Build.VERSION.SDK_INT >= 24) {
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                }
                                                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                                                startActivity(intent);
                                                return;
                                            }
                                        }
                                    }
                                    new Thread(()->{
                                            versionControl.downloadAPK(MainActivity.this, String.format("/%s.apk", version.name),getExternalFilesDir("update").getPath() ,String.format("/update_%s.apk", version.name),externalFilesDir);
                                    }).start();
                                    Toast.makeText(MainActivity.this, "开始下载", Toast.LENGTH_LONG).show();
                                } else {
                                    //SD卡未装入
                                    Toast.makeText(MainActivity.this, "SD卡未装入,无法下载", Toast.LENGTH_SHORT).show();
                                }
                                dialog.dismiss();
                            });
                    normalDialog.setNegativeButton("取消",
                            (dialog, which) -> {
                                dialog.dismiss();
                            });
                    normalDialog.show();
                    break;
                }
                default:{
                    Toast.makeText(MainActivity.this, "未知消息", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });
        WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                musicService.setHandler(handler);
                List<Music> favMusicList = favMusicHelper.getFavMusicList();
                MusicService.favMusicList = favMusicList;
                String favMusicJson = JSON.toJSONString(favMusicList);
                String call = "javascript:setFavMusic("+favMusicJson+")";
                agentWeb.getJsAccessEntrace().callJs(call);
                super.onPageFinished(view, url);
            }
        };
        agentWeb.getWebCreator().getWebView().setWebViewClient(webViewClient);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        show();
        checkUpdate();
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static boolean isHasInstallPermissionWithO(Context context) {
        return context != null && context.getPackageManager().canRequestPackageInstalls();
    }
    private static void startInstallPermissionSettingActivity(Context context) {
        if (context != null) {
            Uri uri = Uri.parse("package:" + context.getPackageName());
            Intent intent = new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES", uri);
            ((Activity) context).startActivity(intent);
        }
    }
    public NotificationCompat.Builder builder;
    public void show() {
        createMessageNotificationChannel();
        builder = new NotificationCompat.Builder(this, MESSAGES_CHANNEL);
        String title = "正在下载";
        String text = "正在下载最新版本";
        builder.setSmallIcon(R.drawable.ic_logo) // //小图标
                .setContentTitle(title)  //通知标题
                .setContentText(text)  //描述性文本
                .setAutoCancel(true)    //点击通知后关闭通知
                .setOnlyAlertOnce(true); //设置提示音只响一次
    }
    private void createMessageNotificationChannel() {
        //Build.VERSION.SDK_INT 代表操作系统的版本号
        //Build.VERSION_CODES.O 版本号为26 对应的Android8.0版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = this.getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(
                    MESSAGES_CHANNEL,
                    name,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager = this.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    // 震动
    public void shadow(){
        Vibrator vibrator = (Vibrator)this.getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(50);
    }

    public void checkUpdate(){
        new Thread(()->{
            VersionControl.Version version = versionControl.hasLeastVersion();
            if(version != null){
                Message message = handler.obtainMessage();
                message.what = 4;
                message.obj = version;
                handler.sendMessage(message);
            }
        }).start();
    }

    @SuppressWarnings("unused")
    public class JSInterface {
        @JavascriptInterface
        public void setPlayMode(String mode){
            musicService.setPlayMode(Integer.parseInt(mode));
        }

        @JavascriptInterface
        public void share(int index,String name,String article){
            ClipData clipData = ClipData.newPlainText("音乐分享链接", "Hi,我找到一首好听的音乐，快来听听吧！\n歌名："+name+"\n歌手："+article + "\n点击链接就能听！\n" + ((List<Music>)musicService.getMusicList()).get(index).url);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(MainActivity.this,"已复制到剪贴板,快去分享吧!",Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void playWhich(int list_index,int index){
            musicService.selectPlay(list_index,index);
        }

        @JavascriptInterface
        public boolean insertFavMusic(int id, String name, String url, String article, String imageUrl){
            try {
                favMusicHelper.insertMusic(id, name, url, article, imageUrl);
                MusicService.favMusicList.add(new Music(id, name, url, article, imageUrl));
                Toast.makeText(MainActivity.this,"收藏成功",Toast.LENGTH_SHORT).show();
                return true;
            } catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }

        @JavascriptInterface
        public boolean removeFavMusic(int id){
            try {
                favMusicHelper.removeMusic(id);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    MusicService.favMusicList.removeIf(music -> music.id == id);
                } else {
                    for (int i = 0; i < MusicService.favMusicList.size(); i++) {
                        if (MusicService.favMusicList.get(i).id == id) {
                            MusicService.favMusicList.remove(i);
                            break;
                        }
                    }
                }
                return true;
            } catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }

        @JavascriptInterface
        public String getMusicList(){
            return JSON.toJSONString(((List<Music>)musicService.getMusicList()));
        }

        @JavascriptInterface
        public void pauseAndPlay(){
            shadow();
            musicService.pauseAndPlay();
        }

        @JavascriptInterface
        public void setCurrentTime(int time){
            musicService.mediaPlayer.seekTo(time);
        }

        @JavascriptInterface
        public void next(){
            shadow();
            musicService.next(0);
        }

        @JavascriptInterface
        public void prev(){
            shadow();
            musicService.prev();
        }
    }
}