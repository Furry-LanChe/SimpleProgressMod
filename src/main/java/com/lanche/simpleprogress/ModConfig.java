package com.lanche.simpleprogress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ModConfig {
    private static final Path CONFIG_PATH = Path.of("config/simpleprogress/simpleprogress.properties");
    private static final Properties PROPS = new Properties();

    public static int theme = 0;
    public static boolean enableSounds = true;

    public static void init() {
        load();
    }

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                save();
                return;
            }

            PROPS.load(Files.newInputStream(CONFIG_PATH));

            theme = Integer.parseInt(PROPS.getProperty("theme", "0"));
            enableSounds = Boolean.parseBoolean(PROPS.getProperty("enableSounds", "true"));

            SimpleProgressMod.LOGGER.info("Config loaded");
        } catch (Exception e) {
            SimpleProgressMod.LOGGER.error("Failed to load config", e);
            PROPS.clear();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            PROPS.setProperty("theme", String.valueOf(theme));
            PROPS.setProperty("enableSounds", String.valueOf(enableSounds));

            PROPS.store(Files.newOutputStream(CONFIG_PATH), "Simple Progress Configuration");

            SimpleProgressMod.LOGGER.info("Config saved");
        } catch (IOException e) {
            SimpleProgressMod.LOGGER.error("Failed to save config", e);
        }
    }
}