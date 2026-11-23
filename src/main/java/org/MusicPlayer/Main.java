package org.MusicPlayer;

import com.formdev.flatlaf.FlatDarkLaf;
import org.MusicPlayer.db.TrackRepository;
import org.MusicPlayer.player.MpvAudioPlayer;
import org.MusicPlayer.player.PlaybackController;
import org.MusicPlayer.player.Track;
import org.MusicPlayer.ui.MainFrame;
import org.MusicPlayer.youtube.GoogleAuth;
import org.MusicPlayer.youtube.YoutubeClient;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Main{
    public static void main(String[] args) throws IOException {
        FlatDarkLaf.setup();

        Path dataDir = Path.of(System.getProperty("user.home"),
                 ".musicplayer");
        TrackRepository repo = new TrackRepository(dataDir.resolve("musicplayer.db"));

        if(args.length > 0){
            List<Track> fromArgs = Arrays.stream(args).map(Main::toTrack).toList();
            repo.saveAll(fromArgs);
        } else {
            Path credsFile = dataDir.resolve("client.properties");
            if(Files.exists(credsFile)){
                try {
                    Properties props = new Properties();
                    try (FileInputStream in = new FileInputStream(credsFile.toFile())) {
                        props.load(in);
                    }

                    String clientId = props.getProperty("client_id");
                    String clientSecret = props.getProperty("client_secret");

                    if(clientId != null && clientSecret != null){
                        System.out.println("Sincronizando con youtube music");

                        GoogleAuth auth = new GoogleAuth(clientId, clientSecret, dataDir.resolve("token.dat"));
                        String token = auth.getAccessToken();
                        YoutubeClient yt = new YoutubeClient();
                        List<Track> liked = yt.fetchLikedMusic(token);
                        if(!liked.isEmpty()){
                            repo.saveAll(liked);
                            System.out.println("Sincronizados" + liked.size() + "temitas de youtube music");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("no se pudo sincronizar con Youtube (usando cache local)" + e.getMessage());

                }
            }
        }

        List<Track> tracks =repo.findAll();
        PlaybackController controller = new PlaybackController(new MpvAudioPlayer());

        SwingUtilities.invokeLater(() -> new MainFrame(controller, tracks).setVisible(true));


    }

    private static Track toTrack(String path){
        String name = Path.of(path).getFileName().toString().replaceFirst("\\.[^.]+$", "");
        return new Track(path, name, "Desconocido", 0, path);
    }
}