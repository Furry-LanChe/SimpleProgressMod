package com.lanche.simpleprogress;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LanguageManager {
    private static String currentLanguage = "en_us";
    private static final Map<String, Properties> LANGUAGE_MAP = new HashMap<>();

    public static String getTranslation(String key) {
        return getTranslation(key, currentLanguage);
    }

    public static String getTranslation(String key, String language) {
        Properties langProps = LANGUAGE_MAP.get(language);
        if (langProps != null) {
            return langProps.getProperty(key, key);
        }
        return key;
    }

    public static String getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setCurrentLanguage(String language) {
        currentLanguage = language;
    }

    public static void initialize() {
        // 加载英文
        loadLanguage("en_us");

        // 加载中文
        loadLanguage("zh_cn");

        // 默认使用英文
        currentLanguage = "en_us";

        SimpleProgressMod.LOGGER.info("Language manager initialized");
    }

    private static void loadLanguage(String langCode) {
        try {
            Properties props = new Properties();
            String resourcePath = "/assets/simpleprogress/lang/" + langCode + ".json";

            // 尝试从JAR加载
            InputStream stream = LanguageManager.class.getResourceAsStream(resourcePath);
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line);
                    }

                    // 简单解析JSON（简化版，实际应该用JSON库）
                    String json = content.toString();
                    json = json.substring(1, json.length() - 1); // 移除{}
                    String[] pairs = json.split(",");

                    for (String pair : pairs) {
                        if (pair.contains(":")) {
                            String[] keyValue = pair.split(":", 2);
                            if (keyValue.length == 2) {
                                String key = keyValue[0].trim().replace("\"", "");
                                String value = keyValue[1].trim().replace("\"", "");
                                props.setProperty(key, value);
                            }
                        }
                    }
                }
            }

            LANGUAGE_MAP.put(langCode, props);
            SimpleProgressMod.LOGGER.info("Loaded language: " + langCode);
        } catch (Exception e) {
            SimpleProgressMod.LOGGER.error("Failed to load language: " + langCode, e);
        }
    }
}