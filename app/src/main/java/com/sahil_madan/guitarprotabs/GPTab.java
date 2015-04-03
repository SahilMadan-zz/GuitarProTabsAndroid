package com.sahil_madan.guitarprotabs;

/**
 * Created by sahil on 23/03/2015.
 */
public class GPTab {
    public String artist;
    public String song;
    public int rating;
    public int votes;
    public String link;

    public GPTab(String artist, String song, int rating, int votes, String link) {
        this.artist = artist;
        this.song = song;
        this.rating = rating;
        this.votes = votes;
        this.link = link;
    }
}
