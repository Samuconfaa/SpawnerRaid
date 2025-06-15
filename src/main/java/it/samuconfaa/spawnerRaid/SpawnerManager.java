package it.samuconfaa.spawnerRaid;

import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import io.lumine.mythic.bukkit.MythicBukkit;
import it.samuconfaa.spawnerRaid.SpawnerRaid;
import it.samuconfaa.spawnerRaid.CustomSpawner;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpawnerManager {

    private final SpawnerRaid plugin;
    private final Map<String, CustomSpawner> spawners;
    private final Set<Entity> activeMobs;
    private File spawnersFile;
    private YamlConfiguration spawnersConfig;

    public SpawnerManager(SpawnerRaid plugin) {
        this.plugin = plugin;
        this.spawners = new HashMap<>();
        this.activeMobs = new HashSet<>();

        // Crea il file spawners.yml
        spawnersFile = new File(plugin.getDataFolder(), "spawners.yml");
        if (!spawnersFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                spawnersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossibile creare il file spawners.yml: " + e.getMessage());
            }
        }
        spawnersConfig = YamlConfiguration.loadConfiguration(spawnersFile);
    }

    public void addSpawner(CustomSpawner spawner) {
        spawners.put(spawner.getName(), spawner);
    }

    public CustomSpawner getSpawner(String name) {
        return spawners.get(name);
    }

    public boolean removeSpawner(String name) {
        return spawners.remove(name) != null;
    }

    public void saveSpawners() {
        spawnersConfig = new YamlConfiguration();

        for (CustomSpawner spawner : spawners.values()) {
            ConfigurationSection section = spawnersConfig.createSection("spawners." + spawner.getName());
            Location loc = spawner.getLocation();

            section.set("world", loc.getWorld().getName());
            section.set("x", loc.getX());
            section.set("y", loc.getY());
            section.set("z", loc.getZ());
            section.set("mobType", spawner.getMobType());
            section.set("quantity", spawner.getQuantity());
        }

        try {
            spawnersConfig.save(spawnersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Errore nel salvare gli spawner: " + e.getMessage());
        }
    }

    public void loadSpawners() {
        spawnersConfig = YamlConfiguration.loadConfiguration(spawnersFile);

        ConfigurationSection spawnersSection = spawnersConfig.getConfigurationSection("spawners");
        if (spawnersSection == null) return;

        for (String name : spawnersSection.getKeys(false)) {
            ConfigurationSection section = spawnersSection.getConfigurationSection(name);

            String worldName = section.getString("world");
            World world = Bukkit.getWorld(worldName);

            if (world == null) {
                plugin.getLogger().warning("Mondo '" + worldName + "' non trovato per lo spawner '" + name + "'");
                continue;
            }

            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            String mobType = section.getString("mobType");
            int quantity = section.getInt("quantity");

            Location location = new Location(world, x, y, z);
            CustomSpawner spawner = new CustomSpawner(name, location, mobType, quantity);

            spawners.put(name, spawner);
        }

        plugin.getLogger().info("Caricati " + spawners.size() + " spawner dal file.");
    }

    public int activateSpawnersInWorld(World world) {
        int activated = 0;

        for (CustomSpawner spawner : spawners.values()) {
            if (spawner.getLocation().getWorld().equals(world)) {
                spawnMobs(spawner);
                activated++;
            }
        }

        return activated;
    }

    private void spawnMobs(CustomSpawner spawner) {
        Location location = spawner.getLocation();

        for (int i = 0; i < spawner.getQuantity(); i++) {
            // Offset casuale per evitare che i mob si sovrappongano
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetZ = (Math.random() - 0.5) * 2;
            Location spawnLoc = location.clone().add(offsetX, 0, offsetZ);

            // Spawn con MythicMobs usando la nuova API
            try {
                Entity entity = MythicBukkit.inst().getMobManager()
                        .spawnMob(spawner.getMobType(), BukkitAdapter.adapt(spawnLoc))
                        .getEntity().getBukkitEntity();

                if (entity != null) {
                    // Rimuovi l'AI per far rimanere il mob fermo
                    if (entity instanceof org.bukkit.entity.LivingEntity) {
                        org.bukkit.entity.LivingEntity livingEntity = (org.bukkit.entity.LivingEntity) entity;
                        livingEntity.setAI(false);
                    }

                    // Imposta per non despawnare
                    entity.setPersistent(true);

                    // Aggiungi alla lista dei mob attivi
                    activeMobs.add(entity);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Errore nel spawn del mob " + spawner.getMobType() + ": " + e.getMessage());
            }
        }
    }

    public void activateMobsNearPlayer(Player player) {
        double activationDistance = plugin.getConfigManager().getActivationDistance();

        for (Entity mob : new HashSet<>(activeMobs)) {
            if (mob.isDead() || !mob.isValid()) {
                activeMobs.remove(mob);
                continue;
            }

            if (mob.getLocation().distance(player.getLocation()) <= activationDistance) {
                if (mob instanceof org.bukkit.entity.LivingEntity) {
                    org.bukkit.entity.LivingEntity livingEntity = (org.bukkit.entity.LivingEntity) mob;
                    if (!livingEntity.hasAI()) {
                        livingEntity.setAI(true);
                    }
                }
            }
        }
    }

    public void clearActiveMobs() {
        for (Entity mob : activeMobs) {
            if (!mob.isDead() && mob.isValid()) {
                mob.remove();
            }
        }
        activeMobs.clear();
    }

    public Collection<CustomSpawner> getAllSpawners() {
        return spawners.values();
    }
}