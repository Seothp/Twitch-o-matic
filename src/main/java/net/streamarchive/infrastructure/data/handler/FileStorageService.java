package net.streamarchive.infrastructure.data.handler;

import lombok.SneakyThrows;
import net.streamarchive.application.StorageHelper;
import net.streamarchive.infrastructure.SettingsProvider;
import net.streamarchive.infrastructure.models.StreamDataModel;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService implements StorageService {
    @Autowired
    private SettingsProvider settingsProperties;

    @Autowired
    private StorageHelper storageHelper;

    @SneakyThrows
    @Override
    public long size(StreamDataModel stream, String fileName) {
        Path filePath = Paths.get(settingsProperties.getRecordedStreamPath() + stream.getStreamerName()
                + "/" + stream.getUuid().toString() + "/" + "chunked" + "/" + fileName);
        if (Files.notExists(filePath)) {
            return -1;
        }
        return Files.size(filePath);
    }

    @SneakyThrows
    @Override
    public void write(InputStream inputStream, StreamDataModel stream, String fileName) {
        Path filePath = Paths.get(settingsProperties.getRecordedStreamPath() + stream.getStreamerName()
                + "/" + stream.getUuid().toString() + "/" + "chunked" + "/" + fileName);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

    }

    @SneakyThrows
    @Override
    public InputStream read(StreamDataModel stream, String fileName) {
        Path filePath = Paths.get(settingsProperties.getRecordedStreamPath() + stream.getStreamerName()
                + "/" + stream.getUuid().toString() + "/" + "chunked" + "/" + fileName);
        return Files.newInputStream(filePath);
    }

    @SneakyThrows
    @PostConstruct
    @Override
    public void initialization() {
        storageHelper.initialStorageCheck();
    }

    @SneakyThrows
    @Override
    public void deleteStream(UUID uuid, String streamer) {
        FileUtils.deleteDirectory(new File(settingsProperties.getRecordedStreamPath() + "" + streamer + "/" + uuid));
    }

    @Override
    public UUID getUUID() {
        return storageHelper.getUuidName();
    }
}
