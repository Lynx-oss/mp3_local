package org.MusicPlayer.youtube;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleAuth {
    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "https://www.googleapis.com/auth/youtube.readonly";

    private final String clientId;
    private final String clientSecret;
    private final Path tokenPath;
    private final HttpClient http = HttpClient.newHttpClient();

    public GoogleAuth(String clientId, String clientSecret, Path tokenPath) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenPath = tokenPath;
    }

    private String refresh(String refreshToken)throws IOException, InterruptedException{
        String body = "refresh_token=" + enc(refreshToken) + "&client_id=" + enc(clientId) + "&client_secret=" + enc(clientSecret) + "&grant_type=refresh_token";
        try {
            return jsonValue(post(body), "access_token");
        } catch (Exception e){
            return null;
        }
    }

    public String getAccessToken() throws IOException, InterruptedException{
        String refreshToken = loadRefreshToken();
        if(refreshToken != null){
            String access = refresh(refreshToken);
            if(access != null) return access;
        }
        return authorize();
    }

    private String authorize() throws IOException, InterruptedException {
        try (ServerSocket server = new ServerSocket(0)){
            int port = server.getLocalPort();
            String redirectUri = "http://localhost:" + port;

            String url = AUTH_URL + "?client_id=" + enc(clientId) + "&redirect_uri=" + enc(redirectUri) + "&response_type=code" + "&scope=" + enc(SCOPE) + "&access_type=offline" + "&prompt=consent";

            openBrowser(url);
            server.setSoTimeout(120_000);

            try(Socket sock = server.accept()){
                String requestLine = new BufferedReader(
                        new
                        InputStreamReader(sock.getInputStream())).readLine();

                String code = extractParam(requestLine, "code");

                String html = "<html><body><h2>Listo! podes cerrar esta pestaña!.</h2><body><html>";
                sock.getOutputStream().write(
                        ("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n\r\n" + html).getBytes(StandardCharsets.UTF_8)

                );

                return exchangeCode(code, redirectUri);
            }

        }
    }

    private String exchangeCode(String code, String redirectUri)throws IOException, InterruptedException{
        String body = "code=" + enc(code)
                +"&client_id=" + enc(clientId)
                +"&client_secret=" + enc(clientSecret)
                +"&redirect_uri=" + enc(redirectUri)
                +"&grant_type=authorization_code";

        String json = post(body);
        String refreshToken = jsonValue(json, "refresh_token");
        if(refreshToken != null) saveRefreshToken(refreshToken);
        return jsonValue(json, "access_token");
    }


    private String post(String formBody) throws IOException,
            InterruptedException{
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(TOKEN_URL))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body();

    }

    private String loadRefreshToken(){
        try{
            return Files.exists(tokenPath) ? Files.readString(tokenPath).strip() : null;
        } catch (IOException e){
            return null;
        }
    }

    private void saveRefreshToken(String token) throws IOException {
        Files.createDirectories(tokenPath.getParent());
        Files.writeString(tokenPath, token);
    }

    private static String enc(String s){
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    static String jsonValue(String json, String key){
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static String extractParam(String requestLine, String param) {
        Matcher m = Pattern.compile("[?&]" + param + "=([^&\\s]+)").matcher(requestLine);
        return m.find() ? URLDecoder.decode(m.group(1), StandardCharsets.UTF_8) : null;
    }

    private static void openBrowser(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            } catch (Exception ignored) {
            }
        }
        try {
            new ProcessBuilder("xdg-open", url).start();
            return;
        } catch (Exception ignored) {
        }
        System.out.println("No se pudo abrir el navegador automáticamente. Abrí este enlace:");
        System.out.println(url);
    }
}
