package it.samuconfaa.spawnerRaid;

import org.bukkit.configuration.file.FileConfiguration;
import it.samuconfaa.spawnerRaid.SpawnerRaid;

public class ConfigManager {

    private final SpawnerRaid plugin;
    private FileConfiguration config;

    public ConfigManager(SpawnerRaid plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Imposta i valori di default se non esistono
        if (!config.contains("activation-distance")) {
            config.set("activation-distance", 10.0);
        }

        if (!config.contains("messages.spawner-created")) {
            config.set("messages.spawner-created", "&aSpawner '{name}' creato con successo!");
        }

        if (!config.contains("messages.spawner-removed")) {
            config.set("messages.spawner-removed", "&aSpawner '{name}' eliminato con successo!");
        }

        if (!config.contains("messages.spawner-not-found")) {
            config.set("messages.spawner-not-found", "&cSpawner '{name}' non trovato!");
        }

        if (!config.contains("messages.spawners-activated")) {
            config.set("messages.spawners-activated", "&aAttivati {count} spawner nel mondo '{world}'!");
        }

        if (!config.contains("messages.no-spawners-found")) {
            config.set("messages.no-spawners-found", "&eNessuno spawner trovato nel mondo '{world}'!");
        }

        plugin.saveConfig();
    }

    public double getActivationDistance() {
        return config.getDouble("activation-distance", 10.0);
    }

    public String getMessage(String key) {
        return config.getString("messages." + key, "&cMessaggio non trovato: " + key)
                .replace("&", "§");
    }

    public String getMessage(String key, String placeholder, String value) {
        return getMessage(key).replace("{" + placeholder + "}", value);
    }

    public String getMessage(String key, String placeholder1, String value1, String placeholder2, String value2) {
        return getMessage(key)
                .replace("{" + placeholder1 + "}", value1)
                .replace("{" + placeholder2 + "}", value2);
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
}