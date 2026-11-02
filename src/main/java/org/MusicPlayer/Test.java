package org.MusicPlayer;

import org.MusicPlayer.player.AudioPlayer;
import org.MusicPlayer.player.MpvAudioPlayer;

public class Test {
    public static void main(String[] args) throws Exception{
        try(AudioPlayer player = new MpvAudioPlayer()){
            player.load(args[0]);
            player.setVolume(50);
            player.play();
            Thread.sleep(3000);
            System.out.printf("pos=%.1f dur=%.1f playing=%b%n",
                    player.position(), player.duration(), player.isPlaying());
            player.pause();
            Thread.sleep(1000);
            System.out.println("Playing=" + player.isPlaying());

            player.seek(30);
            player.play();
            Thread.sleep(3000);
            System.out.printf("pos=%.1f (tendría que rondar 33)%n",
                    player.position());
        }

    }
}
