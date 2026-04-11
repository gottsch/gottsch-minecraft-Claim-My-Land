/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * Claim My Land is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Claim My Land is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Claim My Land.  If not, see <http://www.gnu.org/licenses/lgpl>.
 *
 */

package mod.gottsch.neo.claimmyland.core.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author by Mark Gottschling on 2/20/2026
 */
public class RollingJsonSaver<T> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final File saveDirectory;
    private final String fileBaseName;
    private final int maxFiles;
    private final long saveIntervalTicks;
    private final Supplier<T> dataSupplier;
    private final Type dataType;

    private long tickCounter = 0;

    /**
     * @param saveDirectory Directory where JSON files will be stored
     * @param fileBaseName Base name for files (e.g., "parcels" -> "parcels_2024-02-20_14-30-45.json")
     * @param maxFiles Maximum number of JSON files to keep (older ones will be deleted)
     * @param saveIntervalMinutes How often to save (in minutes)
     * @param dataSupplier Function that provides the data to serialize
     * @param dataType Type of the data (for deserialization)
     */
    public RollingJsonSaver(File saveDirectory, String fileBaseName, int maxFiles,
                            int saveIntervalMinutes, Supplier<T> dataSupplier, Type dataType) {
        this.saveDirectory = saveDirectory;
        this.fileBaseName = fileBaseName;
        this.maxFiles = maxFiles;
        this.saveIntervalTicks = saveIntervalMinutes * 60 * 20; // Convert minutes to ticks
        this.dataSupplier = dataSupplier;
        this.dataType = dataType;

        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
    }

    /**
     * Call this from your server tick event
     */
    public void tick() {
        tickCounter++;

        if (tickCounter >= saveIntervalTicks) {
            save();
            tickCounter = 0;
        }
    }

    /**
     * Manually trigger a save
     */
    public Optional<File> save() {
        try {
            // Get the data to save
            T data = dataSupplier.get();

            // Generate filename with timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = dateFormat.format(new Date());
            String fileName = fileBaseName + "_" + timestamp + ".json";
            File saveFile = new File(saveDirectory, fileName);

            // Write JSON
            try (FileWriter writer = new FileWriter(saveFile)) {
                GSON.toJson(data, writer);
            }

            LOGGER.debug("Saved data to: {}", saveFile.getName());

            // Clean up old files
            cleanupOldFiles();

            return Optional.of(saveFile);
        } catch (IOException e) {
            LOGGER.error("Failed to save data", e);
            return Optional.empty();
        }
    }

    /**
     * Remove old files, keeping only the most recent maxFiles
     */
    private void cleanupOldFiles() {
        File[] savedFiles = saveDirectory.listFiles((dir, name) ->
                name.startsWith(fileBaseName) && name.endsWith(".json"));

        if (savedFiles == null || savedFiles.length <= maxFiles) {
            return;
        }

        // Sort by last modified time (oldest first)
        Arrays.sort(savedFiles, Comparator.comparingLong(File::lastModified));

        // Delete oldest files
        int toDelete = savedFiles.length - maxFiles;
        for (int i = 0; i < toDelete; i++) {
            if (savedFiles[i].delete()) {
                LOGGER.debug("Deleted old save: {}", savedFiles[i].getName());
            }
        }
    }

    /**
     * Load from the most recent save file
     */
    public T loadLatest() {
        File[] savedFiles = saveDirectory.listFiles((dir, name) ->
                name.startsWith(fileBaseName) && name.endsWith(".json"));

        if (savedFiles == null || savedFiles.length == 0) {
            LOGGER.warn("No save files found");
            return null;
        }

        // Find the most recent file
        Arrays.sort(savedFiles, Comparator.comparingLong(File::lastModified).reversed());
        File latestFile = savedFiles[0];

        return loadFromFile(latestFile);
    }

    /**
     * Load from a specific file
     */
    public T loadFromFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            T data = GSON.fromJson(reader, dataType);
            LOGGER.debug("Loaded data from: {}", file.getName());
            return data;
        } catch (IOException e) {
            LOGGER.error("Failed to load from file", e);
            return null;
        }
    }

    /**
     * Get list of all available save files
     */
    public File[] getAvailableSaves() {
        File[] savedFiles = saveDirectory.listFiles((dir, name) ->
                name.startsWith(fileBaseName) && name.endsWith(".json"));

        if (savedFiles != null) {
            Arrays.sort(savedFiles, Comparator.comparingLong(File::lastModified).reversed());
        }

        return savedFiles != null ? savedFiles : new File[0];
    }

    /**
     * Reset the tick counter (useful if you manually save)
     */
    public void resetTimer() {
        tickCounter = 0;
    }
}