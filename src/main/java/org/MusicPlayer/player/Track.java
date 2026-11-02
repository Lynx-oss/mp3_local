package org.MusicPlayer.player;

public record Track(String id, String title, String artist, int durationSeconds, String source) {
}
