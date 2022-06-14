package com.example.musicplayer.Database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.musicplayer.Entity.Music;

import java.util.ArrayList;
import java.util.List;

public class FavMusicHelper extends SQLiteOpenHelper {

    public FavMusicHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table fav_music(id integer, name text, url text, article text, imageUrl text)";
        db.execSQL(sql);
    }

    public void insertMusic(Music music){
        SQLiteDatabase db = getWritableDatabase();

        String sql = "insert into fav_music(id, name, url, article, imageUrl) values(?, ?, ?, ?, ?)";
        db.execSQL(sql, new Object[]{music.id, music.name, music.url, music.article, music.imageUrl});
    }

    public void insertMusic(Integer id, String name, String url, String article, String imageUrl){
        SQLiteDatabase db = getWritableDatabase();
        String exist = "select * from fav_music where id = ?";
        Cursor cursor = db.rawQuery(exist, new String[]{id.toString()});
        if(cursor.getCount() == 0){
            String sql = "insert into fav_music(id, name, url, article, imageUrl) values(?, ?, ?, ?, ?)";
            db.execSQL(sql, new Object[]{id, name, url, article, imageUrl});
            Log.e("insertMusic", "insertMusic: " + id + " " + name + " " + url + " " + article + " " + imageUrl);
        }
        cursor.close();
    }

    public void removeMusic(Music music){
        SQLiteDatabase db = getWritableDatabase();
        String sql = "delete from fav_music where id = ?";
        db.execSQL(sql, new Object[]{music.id});
    }

    public void removeMusic(Integer id){
        SQLiteDatabase db = getWritableDatabase();
        String sql = "delete from fav_music where id = ?";
        db.execSQL(sql, new Object[]{id});
        Log.e("removeMusic", "removeMusic: " + id);
    }

    public List<Music> getFavMusicList(){
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query("fav_music", new String[]{"id", "name", "url", "article", "imageUrl"}, null, null, null, null, null);
        List<Music> musicList = new ArrayList<>();
        while(cursor.moveToNext()){
            int idIndex = cursor.getColumnIndex("id");
            int nameIndex = cursor.getColumnIndex("name");
            int urlIndex = cursor.getColumnIndex("url");
            int articleIndex = cursor.getColumnIndex("article");
            int imageUrlIndex = cursor.getColumnIndex("imageUrl");
            if(idIndex >= 0 || nameIndex >= 0 || urlIndex >= 0 || articleIndex >= 0 || imageUrlIndex >= 0){
                Music music = new Music();
                music.id = cursor.getInt(idIndex);
                music.name = cursor.getString(nameIndex);
                music.url = cursor.getString(urlIndex);
                music.article = cursor.getString(articleIndex);
                music.imageUrl = cursor.getString(imageUrlIndex);
                musicList.add(music);
            }
        }
        cursor.close();
        return musicList;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
