package org.MusicPlayer.youtube;

import org.MusicPlayer.player.Track;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeClient {

    private static final String API_URL =
            "https://www.googleapis.com/youtube/v3/videos"
                    + "?myRating=like&part=snippet,contentDetails"
                    + "&maxResults=50&videoCategoryId=10";

    private final HttpClient http = HttpClient.newHttpClient();

    private void parseItems(String json, List<Track> out){
        Pattern itemPattern = Pattern.compile(
                "\"id\"\\s*:\\s*\"([^\"]+)\".*?"
                        + "\"title\"\\s*:\\s*\"([^\"]+)\".*?"
                        + "\"channelTitle\"\\s*:\\s*\"([^\"]+)\".*?"
                        + "\"duration\"\\s*:\\s*\"([^\"]+)\"",
                Pattern.DOTALL

        );

        Matcher m = itemPattern.matcher(json);
        while(m.find()){
            String id = m.group(1);
            String title = unescapeJson(m.group(2));
            String artist = unescapeJson(m.group(3));
            int duration = parseDuration(m.group(4));
            String source = "https://www.youtube.com/watch?v=" + id;

            out.add(new Track(id,title,artist,duration,source));


        }
    }

    private static int parseDuration(String iso){
        Matcher m = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?").matcher(iso);
        if(!m.find()) return 0;
        int h = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
        int min = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
        int s = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        return h * 3600 + min * 60 + s;
    }



    public List<Track> fetchLikedMusic(String accessToken)
        throws IOException, InterruptedException{
        List<Track> all = new ArrayList<>();
        String pageToken = null;

        do{
            String url = API_URL;
            if(pageToken != null) url += "&pageToken=" + pageToken;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET().build();

            String json = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            parseItems(json,all);
            pageToken = GoogleAuth.jsonValue(json, "nextPageToken");



        } while (pageToken != null);

        return all;


    }

    private static String unescapeJson(String s ){
        return s.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", " ");
    }

}
