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

        // Registra i comandi
        SpawnerCommands commandExecutor = new SpawnerCommands(this);
        getCommand("setspawner").setExecutor(commandExecutor);
        getCommand("attivaspawner").setExecutor(commandExecutor);
        getCommand("eliminaspawner").setExecutor(commandExecutor);

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