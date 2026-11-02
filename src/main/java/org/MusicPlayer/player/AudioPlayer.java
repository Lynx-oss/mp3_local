package org.MusicPlayer.player;

public interface AudioPlayer  extends AutoCloseable{
    void load(String source);

    void play();

    void pause();

    void seek(double seconds);

    void setVolume(int percent);

    boolean isPlaying();

    double position();

    double duration();

    @Override
    void close();

}
