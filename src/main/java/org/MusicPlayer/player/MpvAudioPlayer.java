package org.MusicPlayer.player;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AudioPlayer basado en mpv. Lanza mpv sin video, en modo idle, y lo controla
 * por IPC (socket Unix, solo Linux/macOS por ahora).
 */
public class MpvAudioPlayer implements AudioPlayer {

    // Eventos de mpv: {"event":"property-change","id":1,"name":"time-pos","data":12.34}
    private static final Pattern PROPERTY =
            Pattern.compile("\"name\":\"(time-pos|duration|pause)\",\"data\":([^,}]+)");

    private final Process process;
    private final Path socketPath;
    private final SocketChannel channel;

    private volatile double position;
    private volatile double duration;
    private volatile boolean paused = true;
    private volatile Runnable onTrackEnd;

    public MpvAudioPlayer() throws IOException {
        socketPath = Path.of(System.getProperty("java.io.tmpdir"),
                "musicplayer-" + ProcessHandle.current().pid() + ".sock");
        Files.deleteIfExists(socketPath);

        process = new ProcessBuilder(
                "mpv", "--no-video", "--no-config", "--no-terminal", "--idle=yes",
                "--input-ipc-server=" + socketPath)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start();

        channel = connect();
        startReader();

        send("{\"command\":[\"observe_property\",1,\"time-pos\"]}");
        send("{\"command\":[\"observe_property\",2,\"duration\"]}");
        send("{\"command\":[\"observe_property\",3,\"pause\"]}");
    }

    // ---- AudioPlayer ----

    @Override
    public void load(String source) {
        position = 0;
        duration = 0;
        send("{\"command\":[\"set_property\",\"pause\",true]}");
        send("{\"command\":[\"loadfile\",\"" + escape(source) + "\"]}");
    }

    @Override
    public void play() {
        paused = false; // optimista: mpv lo confirma después por evento
        send("{\"command\":[\"set_property\",\"pause\",false]}");
    }

    @Override
    public void pause() {
        paused = true; // optimista: mpv lo confirma después por evento
        send("{\"command\":[\"set_property\",\"pause\",true]}");
    }

    @Override
    public void seek(double seconds) {
        send("{\"command\":[\"seek\"," + seconds + ",\"absolute\"]}");
    }

    @Override
    public void setVolume(int percent) {
        int volume = Math.max(0, Math.min(100, percent));
        send("{\"command\":[\"set_property\",\"volume\"," + volume + "]}");
    }

    @Override
    public void setOnTrackEnd(Runnable callback) {
        this.onTrackEnd = callback;

    }

    @Override
    public boolean isPlaying() {
        return !paused;
    }

    @Override
    public double position() {
        return position;
    }

    @Override
    public double duration() {
        return duration;
    }

    @Override
    public void close() {
        try {
            send("{\"command\":[\"quit\"]}");
            channel.close();
        } catch (Exception ignored) {
        } finally {
            process.destroy();
            try {
                Files.deleteIfExists(socketPath);
            } catch (IOException ignored) {
            }
        }
    }


    /** mpv tarda un momento en crear el socket: reintentamos hasta ~3 segundos. */
    private SocketChannel connect() throws IOException {
        for (int i = 0; i < 60; i++) {
            if (Files.exists(socketPath)) {
                SocketChannel ch = SocketChannel.open(StandardProtocolFamily.UNIX);
                try {
                    ch.connect(UnixDomainSocketAddress.of(socketPath));
                    return ch;
                } catch (IOException e) {
                    ch.close();
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        process.destroy();
        throw new IOException("No se pudo conectar con mpv (¿está instalado?)");
    }

    private synchronized void send(String json) {
        try {
            ByteBuffer buf = ByteBuffer.wrap((json + "\n").getBytes(StandardCharsets.UTF_8));
            while (buf.hasRemaining()) {
                channel.write(buf);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void startReader() {
        Thread reader = new Thread(() -> {
            ByteBuffer buf = ByteBuffer.allocate(8192);
            ByteArrayOutputStream pending = new ByteArrayOutputStream();
            try {
                while (channel.read(buf) != -1) {
                    buf.flip();
                    while (buf.hasRemaining()) {
                        byte b = buf.get();
                        if (b == '\n') {
                            handle(pending.toString(StandardCharsets.UTF_8));
                            pending.reset();
                        } else {
                            pending.write(b);
                        }
                    }
                    buf.clear();
                }
            } catch (IOException ignored) {
                // socket cerrado
            }
        }, "mpv-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void handle(String line) {
        if (line.contains("\"event\":\"end-file\"") && line.contains("\"reason\":\"eof\"")){
            Runnable cb = onTrackEnd;
            if(cb != null){
                cb.run();
            }
            return;
        }

        Matcher m = PROPERTY.matcher(line);
        if (!m.find()) {
            return;
        }
        String value = m.group(2);
        try {
            switch (m.group(1)) {
                case "time-pos" -> position = Double.parseDouble(value);
                case "duration" -> duration = Double.parseDouble(value);
                case "pause" -> paused = Boolean.parseBoolean(value);
                default -> { }
            }
        } catch (NumberFormatException ignored) {
            // mpv manda null cuando el dato todavía no existe
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}