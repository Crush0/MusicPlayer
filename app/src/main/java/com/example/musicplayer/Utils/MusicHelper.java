package com.example.musicplayer.Utils;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.musicplayer.Entity.Music;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MusicHelper {


    public List<Music> getMusicList() {
        List<Music> ret = new ArrayList<>();
        String url = "http://cloud-music.pl-fe.cn/top/song?type=7";
        String doc = sendGet(url);
        JSONObject jsonObject = JSON.parseObject(doc);
        JSONArray musicArray = jsonObject.getJSONArray("data");
        int i=1;
        for(Object musicObject:musicArray){
            Music music = new Music();
            JSONObject tmp = ((JSONObject)musicObject);
            JSONArray articleArray = tmp.getJSONArray("artists");
            for(int i_=0;i_<articleArray.size();i_++){
                Object articleObject = articleArray.get(i_);
                JSONObject articleJson = ((JSONObject)articleObject);
                music.article += articleJson.getString("name");
                if(i_!=articleArray.size()-1){
                    music.article += "/";
                }
            }
            music.id = tmp.getInteger("id");
            String newUrlPrefix = "https://link.hhtjim.com/163/";
            music.url = newUrlPrefix + tmp.getString("id") + ".mp3";
            music.imageUrl = tmp.getJSONObject("album").getString("blurPicUrl");
            music.name = (i++) +":"+ tmp.getString("name");
            ret.add(music);
        }
        return ret;
    }

    public static String sendGet(String url) {
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            // 定义 BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            Log.e("TAG","发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result.toString();
    }

}
