package com.pingwinno.application.twitch.playlist.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public class MediaPlaylistDownloader {
    private BufferedReader reader;

    public BufferedReader getMediaPlaylist(String m3u8Link) throws IOException {
        URL url = new URL(m3u8Link);
        URLConnection connection = url.openConnection();
        reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        return reader;
    }

    public void close() throws IOException {
        reader.close();
    }

}
