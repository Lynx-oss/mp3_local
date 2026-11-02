package org.MusicPlayer.player;

import java.util.List;
import java.util.Optional;

public class PlaybackController implements AutoCloseable{
    private final AudioPlayer player;
    private List<Track> queue = List.of();
    private int index = -1;

    public PlaybackController(AudioPlayer player) {
        this.player = player;
    }

    public void setQueue(List<Track> tracks){
    this.queue = List.copyOf(tracks);
    this.index= -1;
    }

    public void playAt(int newIndex){
        if(newIndex < 0 || newIndex >= queue.size()){
            return;
        }
        index = newIndex;
        player.load(queue.get(index).source());
        player.play();
    }

    public void togglePlayPause(){
        if(index < 0){
            return;
        }
        if(player.isPlaying()){
            player.pause();
        } else {
            player.play();
        }
    }

    public void next(){
        playAt(index + 1);
    }
    public void previous(){
        playAt(index - 1);
    }

    public void seek(double seconds){
        player.seek(seconds);
    }

    public void setVolume(int percent){
        player.setVolume(percent);
    }

    public Optional<Track> current(){
        return index >= 0 ? Optional.of(queue.get(index)) : Optional.empty();
    }

    public int currentIndex() {
        return index;
    }

    public double position(){
        return player.position();
    }

    public double duration(){
        return player.duration();
    }

    public boolean isPlaying(){
        return  index >= 0 && player.isPlaying();
    }

    @Override
    public void close(){
        player.close();
    }



}
