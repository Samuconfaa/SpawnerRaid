package it.samuconfaa.spawnerRaid;

import org.bukkit.plugin.java.JavaPlugin;

public class SpawnerRaid extends JavaPlugin {

    private static SpawnerRaid instance;
    private SpawnerManager spawnerManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        // Inizializza i manager
        configManager = new ConfigManager(this);
        spawnerManager = new SpawnerManager(this);

        // Crea il TabCompleter
        SpawnerTabCompleter tabCompleter = new SpawnerTabCompleter(this);

        // Registra i comandi e i loro TabCompleter
        SpawnerCommands commandExecutor = new SpawnerCommands(this);

        getCommand("setspawner").setExecutor(commandExecutor);
        getCommand("setspawner").setTabCompleter(tabCompleter);

        getCommand("attivaspawner").setExecutor(commandExecutor);
        getCommand("attivaspawner").setTabCompleter(tabCompleter);

        getCommand("eliminaspawner").setExecutor(commandExecutor);
        getCommand("eliminaspawner").setTabCompleter(tabCompleter);

        getCommand("debugspawners").setExecutor(commandExecutor);
        getCommand("debugspawners").setTabCompleter(tabCompleter);

        // Registra i listener
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);

        // Carica i file di configurazione
        configManager.loadConfig();
        spawnerManager.loadSpawners();

        getLogger().info("SpawnerRaid plugin abilitato!");
    }

    @Override
    public void onDisable() {
        if (spawnerManager != null) {
            spawnerManager.saveSpawners();
            spawnerManager.clearActiveMobs();
        }
        getLogger().info("SpawnerRaid plugin disabilitato!");
    }

    public static SpawnerRaid getInstance() {
        return instance;
    }

    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}