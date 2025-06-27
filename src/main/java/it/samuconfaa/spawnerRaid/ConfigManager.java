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

        if (!config.contains("spawn-distance")) {
            config.set("spawn-distance", 50.0);
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

        // Nuovi messaggi per i tipi di mob
        if (!config.contains("messages.mob-not-exists")) {
            config.set("messages.mob-not-exists", "&cIl mob '{mob}' non esiste!");
        }

        if (!config.contains("messages.vanilla-mob-not-exists")) {
            config.set("messages.vanilla-mob-not-exists", "&cIl mob vanilla '{mob}' non esiste!");
        }

        if (!config.contains("messages.mythic-mob-not-exists")) {
            config.set("messages.mythic-mob-not-exists", "&cIl mob MythicMobs '{mob}' non esiste!");
        }

        if (!config.contains("messages.spawner-already-exists")) {
            config.set("messages.spawner-already-exists", "&cEsiste già uno spawner con il nome '{name}'!");
        }

        if (!config.contains("messages.world-not-found")) {
            config.set("messages.world-not-found", "&cIl mondo '{world}' non esiste!");
        }

        if (!config.contains("messages.invalid-quantity")) {
            config.set("messages.invalid-quantity", "&cLa quantità deve essere un numero maggiore di 0!");
        }

        if (!config.contains("messages.invalid-spawner-type")) {
            config.set("messages.invalid-spawner-type", "&cTipo spawner non valido! Usa 'vanilla' o 'mythicmob'");
        }

        if (!config.contains("messages.only-players")) {
            config.set("messages.only-players", "&cQuesto comando può essere eseguito solo da un giocatore!");
        }

        if (!config.contains("messages.permission-denied")) {
            config.set("messages.permission-denied", "&cNon hai il permesso per utilizzare questo comando!");
        }

        // Impostazioni avanzate
        if (!config.contains("settings.auto-save-interval")) {
            config.set("settings.auto-save-interval", 300);
        }

        if (!config.contains("settings.max-spawners-per-world")) {
            config.set("settings.max-spawners-per-world", 50);
        }

        if (!config.contains("settings.debug")) {
            config.set("settings.debug", false);
        }

        plugin.saveConfig();
    }

    public double getActivationDistance() {
        return config.getDouble("activation-distance", 10.0);
    }

    public double getSpawnDistance() {
        return config.getDouble("spawn-distance", 50.0);
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

    public int getAutoSaveInterval() {
        return config.getInt("settings.auto-save-interval", 300);
    }

    public int getMaxSpawnersPerWorld() {
        return config.getInt("settings.max-spawners-per-world", 50);
    }

    public boolean isDebugMode() {
        return config.getBoolean("settings.debug", false);
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
}