package com.example.musicplayer.Entity;

import androidx.annotation.NonNull;

public class Music {
    public Integer id = 0;
    public String name = "";
    public String url = "";
    public String article = "";
    public String imageUrl = "";

    public Music(){}

    public Music(int id, String name, String url, String article, String imageUrl) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.article = article;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public String toString() {
        return "Music{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", article='" + article + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
