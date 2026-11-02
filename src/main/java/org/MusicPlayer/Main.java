package org.MusicPlayer;

import com.formdev.flatlaf.FlatDarkLaf;
import org.MusicPlayer.player.MpvAudioPlayer;
import org.MusicPlayer.player.PlaybackController;
import org.MusicPlayer.player.Track;
import org.MusicPlayer.ui.MainFrame;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Main{
    public static void main(String[] args) throws IOException {
        FlatDarkLaf.setup();
        List<Track> tracks = Arrays.stream(args).map(Main::toTrack).toList();
        PlaybackController controller = new PlaybackController(new MpvAudioPlayer());

        SwingUtilities.invokeLater(() -> new MainFrame(controller, tracks).setVisible(true));


    }

    private static Track toTrack(String path){
        String name = Path.of(path).getFileName().toString().replaceFirst("\\.[^.]+$", "");
        return new Track(path, name, "Desconocido", 0, path);
    }
}