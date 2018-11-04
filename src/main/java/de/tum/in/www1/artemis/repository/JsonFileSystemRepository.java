package de.tum.in.www1.artemis.repository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Warning: File repository is not thread save!
 */
@SuppressWarnings("unused")
@Repository
abstract class JsonFileSystemRepository {

    private final Logger log = LoggerFactory.getLogger(JsonFileSystemRepository.class);

    /**
     * Write a new or update an existing file
     *
     * @param path the path to the new or existing file
     * @param json the file content
     * @return success or failure
     */
    boolean write(Path path, String json) {
        try {
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            File file = path.toFile();
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    boolean delete(Path path) {
        return path.toFile().delete();
    }

    /**
     * Read a file
     *
     * @param path the file path
     * @return the JSON content of the file
     */
    JsonObject read(Path path) {
        if (Files.notExists(path)) {
            return null;
        }
        JsonParser parser = new JsonParser();
        try {
            FileReader reader = new FileReader(path.toFile());
            JsonObject json = parser.parse(reader).getAsJsonObject();
            reader.close();
            return json;
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    /**
     * Read all files as json in the given folder
     *
     * @param path the path to the folder
     * @param filenameContains read only files which contains this substring
     * @return a map of ids (model / assessment id) to the corresponding JSON file content
     */
    Map<Long, JsonObject> readInFolder(Path path, String filenameContains) {
        if (Files.notExists(path)) {
            return new HashMap<>();
        }
        JsonParser parser = new JsonParser();
        Map<Long, JsonObject> jsons = new HashMap<>();
        try {
            Files.walk(path).filter(p -> Files.isRegularFile(p) && p.toString().contains(filenameContains))
                .forEach(p -> {
                    try {
                        FileReader fileReader = new FileReader(p.toFile());
                        // The id of the model / assessment is part of its filename
                        jsons.put(Long.valueOf(p.getFileName().toString().split("\\.")[1]),(JsonObject) parser.parse(fileReader));
                        fileReader.close();
                    } catch (IOException | NumberFormatException e) {
                        log.error(e.getMessage());
                    }
                });
            return jsons;
        } catch (IOException e) {
            log.error(e.getMessage());
            return new HashMap<>();
        }
    }

    boolean exists(Path path) {
        return Files.exists(path);
    }
}
