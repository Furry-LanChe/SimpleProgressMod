package com.lanche.simpleprogress;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"deprecation", "removal"})
public class LanguageManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<String, String> TRANSLATIONS = new HashMap<>();
    private static String currentLanguage = "en_us";

    public static void loadLanguage(String languageCode) {
        TRANSLATIONS.clear();
        currentLanguage = languageCode;

        try {
            // 强制使用已弃用的ResourceLocation构造函数
            // 添加@SuppressWarnings在类级别屏蔽所有警告
            ResourceLocation langFile = new ResourceLocation("simpleprogress", String.format("lang/%s.json", languageCode));
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(langFile).orElse(null);

            if (resource != null) {
                try (InputStream stream = resource.open();
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    for (String key : json.keySet()) {
                        TRANSLATIONS.put(key, json.get(key).getAsString());
                    }
                }
                LOGGER.info("Loaded {} translations for language: {}", TRANSLATIONS.size(), languageCode);
            } else {
                LOGGER.warn("Language file not found: {}, falling back to English", languageCode);
                loadLanguage("en_us");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load language file: {}", languageCode, e);
            loadLanguage("en_us");
        }
    }

    public static String getTranslation(String key) {
        return TRANSLATIONS.getOrDefault(key, key);
    }

    public static String getTranslation(String key, Object... args) {
        String template = TRANSLATIONS.getOrDefault(key, key);
        return String.format(template, args);
    }

    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    public static String[] getSupportedLanguages() {
        return new String[] {
                "en_us", "zh_cn"
        };
    }

    public static String getLanguageDisplayName(String code) {
        switch(code) {
            case "en_us": return "English (US)";
            case "zh_cn": return "简体中文";
            default: return code;
        }
    }

    public static void initialize() {
        String systemLanguage = java.util.Locale.getDefault().toString().toLowerCase();
        if (systemLanguage.startsWith("zh")) {
            loadLanguage("zh_cn");
        } else {
            loadLanguage("en_us");
        }
    }
}