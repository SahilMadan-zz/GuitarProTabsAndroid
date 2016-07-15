package com.sahil_madan.guitarprotabs;

public class GPTab {
    public String artist;
    public String song;
    public double rating;
    public int votes;
    public String link;

    public GPTab(String artist, String song, double rating, int votes, String link) {
        this.artist = artist;
        this.song = song;
        this.rating = rating;
        this.votes = votes;
        this.link = link;
    }
}
