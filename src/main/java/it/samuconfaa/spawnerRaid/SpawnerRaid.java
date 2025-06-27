package it.samuconfaa.spawnerRaid;

import org.bukkit.plugin.java.JavaPlugin;
import it.samuconfaa.spawnerRaid.SpawnerCommands;
import it.samuconfaa.spawnerRaid.SpawnerManager;
import it.samuconfaa.spawnerRaid.ConfigManager;
import it.samuconfaa.spawnerRaid.PlayerMoveListener;

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

        // Registra i comandi con TabCompleter
        SpawnerCommands commandExecutor = new SpawnerCommands(this);

        // Registra CommandExecutor e TabCompleter per ogni comando
        getCommand("setspawner").setExecutor(commandExecutor);
        getCommand("setspawner").setTabCompleter(commandExecutor);

        getCommand("attivaspawner").setExecutor(commandExecutor);
        getCommand("attivaspawner").setTabCompleter(commandExecutor);

        getCommand("stopspawner").setExecutor(commandExecutor);
        getCommand("stopspawner").setTabCompleter(commandExecutor);

        getCommand("eliminaspawner").setExecutor(commandExecutor);
        getCommand("eliminaspawner").setTabCompleter(commandExecutor);

        getCommand("debugspawners").setExecutor(commandExecutor);
        getCommand("debugspawners").setTabCompleter(commandExecutor);

        // Nuovo comando per il debug visivo
        getCommand("spawnerdebug").setExecutor(commandExecutor);
        getCommand("spawnerdebug").setTabCompleter(commandExecutor);

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
            spawnerManager.stopAllTasks(); // Usa il metodo corretto
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