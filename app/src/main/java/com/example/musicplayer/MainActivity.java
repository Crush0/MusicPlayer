package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.example.musicplayer.Database.FavMusicHelper;
import com.example.musicplayer.Entity.Music;
import com.example.musicplayer.Service.MusicService;
import com.just.agentweb.AgentWeb;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public AgentWeb agentWeb;
    private FavMusicHelper favMusicHelper;
    private MusicService musicService;
    private ClipboardManager clipboardManager;

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
        Handler handler = new Handler(msg->{
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
    }

    // 震动
    public void shadow(){
        Vibrator vibrator = (Vibrator)this.getSystemService(VIBRATOR_SERVICE);
        vibrator.vibrate(50);
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