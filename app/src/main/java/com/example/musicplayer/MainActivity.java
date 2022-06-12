package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.example.musicplayer.Service.MusicService;
import com.example.musicplayer.Utils.MusicHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public WebView webView;
    private MusicService musicService;
    public ClipboardManager clipboardManager;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.music_view);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        webSettings.setAppCachePath(appCachePath);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setUseWideViewPort(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setBlockNetworkImage(true);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        Handler handler = new Handler((Handler.Callback) msg->{
            switch (msg.what){
                case 1:{
                    //TODO:更新进度条
                    String call = "javascript:setVueTime("+msg.arg1+","+msg.arg2+")";
                    webView.loadUrl(call);
                    break;
                }
                case 2:{
                    //TODO:换歌
                    MusicHelper.Music music = (MusicHelper.Music) msg.obj;
                    String call = "javascript:setVueMusic("+msg.arg2+","+msg.arg1+",'"+music.name.split(":")[1]+"',"+"'"+music.article+"'"+",'"+music.imageUrl+"')";
                    Log.e("call",call);
                    webView.loadUrl(call);
                    break;
                }
                case 3:{
                    //传递歌曲列表
                    String call = (String) msg.obj;
                    webView.loadUrl(call);
                }
            }
            return true;
        });
        ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                musicService = ((MusicService.LocalBinder) service).getMusicService();
                musicService.setActivity(MainActivity.this);
                musicService.setHandler(handler);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //使用WebView加载显示url
                view.loadUrl(url);
                //返回true
                return true;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                webSettings.setBlockNetworkImage(false);
                boolean result = bindService(new Intent(MainActivity.this, MusicService.class), serviceConnection, BIND_AUTO_CREATE);
                Log.e("MY_TAG","绑定完毕:"+result);
                super.onPageFinished(view, url);
            }
        });
        webView.addJavascriptInterface(new JSInterface(), "control");
        webView.loadData("","text/html","UTF-8");
        webView.loadUrl("file:///android_asset/Player.html");
    }

    public void shadow(){
        Vibrator vibrator = (Vibrator)this.getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(50);
    }

    public class JSInterface {
        @JavascriptInterface
        public void setPlayMode(String mode){
            musicService.setPlayMode(Integer.parseInt(mode));
        }

        @JavascriptInterface
        public void share(int index,String name,String article){
            ClipData clipData = ClipData.newPlainText("音乐分享链接", "Hi,我找到一首好听的音乐，快来听听吧！\n歌名："+name+"\n歌手："+article + "\n点击链接就能听！\n" + ((List<MusicHelper.Music>)musicService.getMusicList()).get(index).url);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(MainActivity.this,"已复制到剪贴板,快去分享吧!",Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void playWhich(int index){
            musicService.selectPlay(index);
        }

        @JavascriptInterface
        public String getMusicList(){
            return JSON.toJSONString(((List<MusicHelper.Music>)musicService.getMusicList()));
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

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}