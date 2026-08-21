package ru.holyworld.invisindicator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("invisindicator.json");

    public int colorRgb = 0xFF3B30;

    // Стиль кольца: red/orange, purple, green, blue, white/pink.
    public String particleStyle = "red";
    public float alpha = 0.85f;
    public float radius = 0.6f;
    public float heightOffset = 0.55f;
    public boolean showNickname = true;
    public boolean includeSelf = false;

    private static Config instance;

    public static Config get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Config load() {
        if (Files.exists(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                Config cfg = GSON.fromJson(reader, Config.class);
                if (cfg != null) {
                    return cfg;
                }
            } catch (IOException e) {
                InvisIndicatorClient.LOGGER.warn("Не удалось прочитать invisindicator.json, использую значения по умолчанию", e);
            }
        }
        Config cfg = new Config();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            InvisIndicatorClient.LOGGER.warn("Не удалось сохранить invisindicator.json", e);
        }
    }
}
