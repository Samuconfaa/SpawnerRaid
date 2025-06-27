package it.samuconfaa.spawnerRaid;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import io.lumine.mythic.bukkit.MythicBukkit;
import it.samuconfaa.spawnerRaid.SpawnerRaid;
import it.samuconfaa.spawnerRaid.CustomSpawner;
import it.samuconfaa.spawnerRaid.CustomSpawner.SpawnerType;
import it.samuconfaa.spawnerRaid.CustomSpawner.SpawnerState;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpawnerManager {

    private final SpawnerRaid plugin;
    private final Map<String, CustomSpawner> spawners;
    private final Set<Entity> activeMobs;
    private final Set<Player> debugPlayers;
    private final Map<String, Hologram> debugHolograms;
    private final Map<String, Set<CustomSpawner>> activeWorlds; // Mondi con spawner attivi
    private final Map<String, Set<Entity>> worldMobs; // Mob per mondo
    private BukkitRunnable debugTask;
    private BukkitRunnable spawnerCheckTask; // Task per controllare spawner attivi
    private File spawnersFile;
    private YamlConfiguration spawnersConfig;

    public SpawnerManager(SpawnerRaid plugin) {
        this.plugin = plugin;
        this.spawners = new HashMap<>();
        this.activeMobs = new HashSet<>();
        this.debugPlayers = new HashSet<>();
        this.debugHolograms = new HashMap<>();
        this.activeWorlds = new HashMap<>();
        this.worldMobs = new HashMap<>();

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

        // Inizializza i task
        startDebugTask();
        startSpawnerCheckTask();
    }

    /**
     * Attiva gli spawner in un mondo specifico
     */
    public int activateSpawnersInWorld(World world) {
        plugin.getLogger().info("Attivazione spawner nel mondo: " + world.getName());

        // Carica gli spawner per questo mondo se necessario
        loadSpawnersForWorld(world);

        // Crea il set di spawner attivi per questo mondo
        Set<CustomSpawner> worldSpawners = new HashSet<>();

        for (CustomSpawner spawner : spawners.values()) {
            if (spawner.getLocation().getWorld().getName().equals(world.getName())) {
                spawner.setState(SpawnerState.WAITING);
                worldSpawners.add(spawner);
                plugin.getLogger().info("Spawner '" + spawner.getName() + "' impostato in attesa");
            }
        }

        activeWorlds.put(world.getName(), worldSpawners);
        worldMobs.put(world.getName(), new HashSet<>());

        // Aggiorna gli ologrammi se il debug è attivo
        updateDebugHolograms();

        plugin.getLogger().info("Attivati " + worldSpawners.size() + " spawner nel mondo '" + world.getName() + "'");
        return worldSpawners.size();
    }

    /**
     * Ferma gli spawner in un mondo specifico
     */
    public int stopSpawnersInWorld(World world) {
        plugin.getLogger().info("Fermando spawner nel mondo: " + world.getName());

        Set<CustomSpawner> worldSpawners = activeWorlds.get(world.getName());
        if (worldSpawners == null) {
            return 0;
        }

        // Cambia stato degli spawner
        for (CustomSpawner spawner : worldSpawners) {
            spawner.setState(SpawnerState.STOPPED);
        }

        // Rimuovi mob del mondo
        Set<Entity> mobsToRemove = worldMobs.get(world.getName());
        if (mobsToRemove != null) {
            for (Entity mob : mobsToRemove) {
                if (!mob.isDead() && mob.isValid()) {
                    mob.remove();
                }
            }
            mobsToRemove.clear();
        }

        // Rimuovi il mondo dagli attivi
        int stoppedCount = worldSpawners.size();
        activeWorlds.remove(world.getName());
        worldMobs.remove(world.getName());

        // Aggiorna gli ologrammi
        updateDebugHolograms();

        plugin.getLogger().info("Fermati " + stoppedCount + " spawner nel mondo '" + world.getName() + "'");
        return stoppedCount;
    }

    /**
     * Task che controlla continuamente i giocatori vicini agli spawner attivi
     */
    private void startSpawnerCheckTask() {
        spawnerCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkSpawnersForAllPlayers();
            }
        };

        // Esegui ogni 40 tick (2 secondi) per non sovraccaricare il server
        spawnerCheckTask.runTaskTimer(plugin, 0L, 40L);
    }

    /**
     * Controlla tutti i giocatori online per vedere se sono vicini a spawner in attesa
     */
    private void checkSpawnersForAllPlayers() {
        if (activeWorlds.isEmpty()) {
            return; // Nessun mondo con spawner attivi
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String worldName = player.getWorld().getName();
            Set<CustomSpawner> worldSpawners = activeWorlds.get(worldName);

            if (worldSpawners == null) {
                continue; // Questo mondo non ha spawner attivi
            }

            Location playerLocation = player.getLocation();

            for (CustomSpawner spawner : new HashSet<>(worldSpawners)) {
                if (spawner.getState() != SpawnerState.WAITING) {
                    continue; // Spawner non in attesa
                }

                if (!canCalculateDistance(playerLocation, spawner.getLocation())) {
                    continue; // Non si può calcolare la distanza
                }

                double distance = playerLocation.distance(spawner.getLocation());

                if (distance <= 20.0) { // 20 blocchi di distanza
                    plugin.getLogger().info("Giocatore " + player.getName() + " è vicino allo spawner " +
                            spawner.getName() + " (distanza: " + String.format("%.2f", distance) + ")");

                    spawnMobsFromSpawner(spawner, worldName);
                    break; // Spawna solo da uno spawner per volta per giocatore
                }
            }
        }
    }

    /**
     * Spawna i mob da uno spawner specifico
     */
    private void spawnMobsFromSpawner(CustomSpawner spawner, String worldName) {
        plugin.getLogger().info("Spawning mob da: " + spawner.getName());

        Location location = spawner.getLocation();
        Set<Entity> worldMobSet = worldMobs.get(worldName);

        int spawnedCount = 0;
        for (int i = 0; i < spawner.getQuantity(); i++) {
            // Offset casuale per evitare sovrapposizioni
            double offsetX = (Math.random() - 0.5) * 3;
            double offsetZ = (Math.random() - 0.5) * 3;
            Location spawnLoc = location.clone().add(offsetX, 0, offsetZ);

            Entity entity = null;

            if (spawner.getSpawnerType() == SpawnerType.VANILLA) {
                // Spawn di mob vanilla
                try {
                    EntityType entityType = EntityType.valueOf(spawner.getMobType().toUpperCase());
                    entity = spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);
                    spawnedCount++;
                } catch (Exception e) {
                    plugin.getLogger().warning("Errore spawn mob vanilla " + spawner.getMobType() + ": " + e.getMessage());
                }
            } else {
                // Spawn di mob MythicMobs
                try {
                    entity = MythicBukkit.inst().getMobManager()
                            .spawnMob(spawner.getMobType(), BukkitAdapter.adapt(spawnLoc))
                            .getEntity().getBukkitEntity();
                    spawnedCount++;
                } catch (Exception e) {
                    plugin.getLogger().warning("Errore spawn mob MythicMobs " + spawner.getMobType() + ": " + e.getMessage());
                }
            }

            if (entity != null) {
                // Configura il mob
                entity.setPersistent(true);

                // Aggiungi ai set
                activeMobs.add(entity);
                worldMobSet.add(entity);
            }
        }

        // Cambia stato dello spawner
        spawner.setState(SpawnerState.SPAWNED);

        // Aggiorna ologrammi se necessario
        updateDebugHologramForSpawner(spawner);

        plugin.getLogger().info("Spawnati " + spawnedCount + " mob dallo spawner " + spawner.getName());
    }

    /**
     * Attiva/disattiva il debug visivo per un giocatore
     */
    public boolean toggleDebugMode(Player player) {
        if (debugPlayers.contains(player)) {
            debugPlayers.remove(player);
            updateHologramVisibility(player, false);

            if (debugPlayers.isEmpty()) {
                removeAllDebugHolograms();
            }

            return false;
        } else {
            debugPlayers.add(player);

            if (debugPlayers.size() == 1) {
                createDebugHolograms();
            } else {
                updateHologramVisibility(player, true);
            }

            return true;
        }
    }

    /**
     * Crea gli ologrammi di debug per tutti gli spawner
     */
    private void createDebugHolograms() {
        for (CustomSpawner spawner : spawners.values()) {
            createHologramForSpawner(spawner);
        }
    }

    /**
     * Crea un ologramma per uno spawner specifico
     */
    private void createHologramForSpawner(CustomSpawner spawner) {
        String hologramId = "spawner_debug_" + spawner.getName();

        // Rimuovi l'ologramma esistente se presente
        if (debugHolograms.containsKey(hologramId)) {
            DHAPI.removeHologram(hologramId);
            debugHolograms.remove(hologramId);
        }

        // Crea la location 2 blocchi sopra lo spawner
        Location hologramLocation = spawner.getLocation().clone().add(0, 2, 0);

        // Crea le linee dell'ologramma
        List<String> lines = new ArrayList<>();
        lines.add("&e&l[" + spawner.getName() + "]");

        // Informazioni sul mob con colore basato sul tipo
        String mobInfo;
        if (spawner.getSpawnerType() == SpawnerType.VANILLA) {
            mobInfo = "&a" + spawner.getMobType() + " &7x&b" + spawner.getQuantity();
        } else {
            mobInfo = "&d" + spawner.getMobType() + " &7x&b" + spawner.getQuantity();
        }
        lines.add(mobInfo);

        // Stato dello spawner (nuova linea)
        lines.add("&7Stato: " + spawner.getState().getDisplayName());

        // Crea l'ologramma
        try {
            Hologram hologram = DHAPI.createHologram(hologramId, hologramLocation, lines);
            hologram.setDefaultVisibleState(false);

            // Mostra solo ai giocatori in debug
            for (Player debugPlayer : debugPlayers) {
                hologram.setShowPlayer(debugPlayer);
            }

            debugHolograms.put(hologramId, hologram);
        } catch (Exception e) {
            plugin.getLogger().warning("Errore creazione ologramma per " + spawner.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Aggiorna l'ologramma di un singolo spawner
     */
    private void updateDebugHologramForSpawner(CustomSpawner spawner) {
        if (debugPlayers.isEmpty()) {
            return; // Nessuno sta guardando il debug
        }

        String hologramId = "spawner_debug_" + spawner.getName();
        if (debugHolograms.containsKey(hologramId)) {
            // Ricrea l'ologramma con le nuove informazioni
            createHologramForSpawner(spawner);
        }
    }

    /**
     * Aggiorna tutti gli ologrammi di debug
     */
    private void updateDebugHolograms() {
        if (debugPlayers.isEmpty()) {
            return;
        }

        // Ricrea tutti gli ologrammi
        removeAllDebugHolograms();
        createDebugHolograms();
    }

    /**
     * Rimuove tutti gli ologrammi di debug
     */
    private void removeAllDebugHolograms() {
        for (String hologramId : new HashSet<>(debugHolograms.keySet())) {
            try {
                DHAPI.removeHologram(hologramId);
                debugHolograms.remove(hologramId);
            } catch (Exception e) {
                plugin.getLogger().warning("Errore rimozione ologramma " + hologramId + ": " + e.getMessage());
            }
        }
    }

    /**
     * Aggiorna la visibilità degli ologrammi per un giocatore
     */
    private void updateHologramVisibility(Player player, boolean show) {
        for (Hologram hologram : debugHolograms.values()) {
            try {
                if (show) {
                    hologram.setShowPlayer(player);
                } else {
                    hologram.removeShowPlayer(player);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Errore aggiornamento visibilità per " + player.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Avvia il task per mostrare le particelle di debug
     */
    private void startDebugTask() {
        debugTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (debugPlayers.isEmpty()) {
                    return;
                }

                for (Player player : new HashSet<>(debugPlayers)) {
                    if (!player.isOnline()) {
                        debugPlayers.remove(player);
                        if (debugPlayers.isEmpty()) {
                            removeAllDebugHolograms();
                        }
                        continue;
                    }

                    for (CustomSpawner spawner : spawners.values()) {
                        if (spawner.getLocation().getWorld().getName().equals(player.getWorld().getName())) {
                            showDebugParticles(player, spawner);
                        }
                    }
                }
            }
        };

        debugTask.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Mostra le particelle di debug per uno spawner
     */
    private void showDebugParticles(Player player, CustomSpawner spawner) {
        Location loc = spawner.getLocation();

        if (!canCalculateDistance(player.getLocation(), loc) || player.getLocation().distance(loc) > 50) {
            return;
        }

        // Particelle diverse in base al tipo e stato
        Particle particle;
        if (spawner.getSpawnerType() == SpawnerType.VANILLA) {
            particle = Particle.FLAME;
        } else {
            particle = Particle.DRAGON_BREATH;
        }

        // Colore diverso in base allo stato
        switch (spawner.getState()) {
            case INACTIVE:
                particle = Particle.SMOKE_NORMAL;
                break;
            case WAITING:
                particle = Particle.VILLAGER_HAPPY;
                break;
            case SPAWNED:
                particle = Particle.HEART;
                break;
            case STOPPED:
                particle = Particle.BARRIER;
                break;
        }

        // Cerchio di particelle
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            double x = loc.getX() + Math.cos(angle) * 1.5;
            double z = loc.getZ() + Math.sin(angle) * 1.5;
            double y = loc.getY() + 1;

            Location particleLoc = new Location(loc.getWorld(), x, y, z);
            player.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }

        // Particella centrale
        player.spawnParticle(particle, loc.clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
    }

    /**
     * Verifica se è possibile calcolare la distanza tra due location
     */
    private boolean canCalculateDistance(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) {
            return false;
        }

        if (loc1.getWorld() == null || loc2.getWorld() == null) {
            return false;
        }

        return loc1.getWorld().getName().equals(loc2.getWorld().getName());
    }

    /**
     * Attiva i mob vicini al giocatore (logica originale mantenuta)
     */
    public void activateMobsNearPlayer(Player player) {
        double activationDistance = plugin.getConfigManager().getActivationDistance();
        Location playerLocation = player.getLocation();

        // Pulizia preliminare
        Set<Entity> invalidMobs = new HashSet<>();
        for (Entity mob : activeMobs) {
            if (mob.isDead() || !mob.isValid()) {
                invalidMobs.add(mob);
            }
        }
        activeMobs.removeAll(invalidMobs);

        for (Entity mob : new HashSet<>(activeMobs)) {
            try {
                Location mobLocation = mob.getLocation();

                if (!canCalculateDistance(playerLocation, mobLocation)) {
                    continue;
                }

                double distance = playerLocation.distance(mobLocation);

                if (distance <= activationDistance) {
                    if (mob instanceof org.bukkit.entity.LivingEntity) {
                        org.bukkit.entity.LivingEntity livingEntity = (org.bukkit.entity.LivingEntity) mob;
                        if (!livingEntity.hasAI()) {
                            livingEntity.setAI(true);
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Errore attivazione mob " + mob.getType() + ": " + e.getMessage());
                activeMobs.remove(mob);
            }
        }
    }

    // Metodi di gestione file e spawner (mantenuti dal codice originale ma aggiornati)

    public void addSpawner(CustomSpawner spawner) {
        spawners.put(spawner.getName(), spawner);

        if (!debugPlayers.isEmpty()) {
            createHologramForSpawner(spawner);
        }
    }

    public CustomSpawner getSpawner(String name) {
        return spawners.get(name);
    }

    public boolean removeSpawner(String name) {
        CustomSpawner removed = spawners.remove(name);

        if (removed != null) {
            String hologramId = "spawner_debug_" + name;
            if (debugHolograms.containsKey(hologramId)) {
                DHAPI.removeHologram(hologramId);
                debugHolograms.remove(hologramId);
            }
            return true;
        }

        return false;
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
            section.set("spawnerType", spawner.getSpawnerType().name());
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

            SpawnerType spawnerType = SpawnerType.MYTHICMOB;
            String spawnerTypeStr = section.getString("spawnerType");
            if (spawnerTypeStr != null) {
                try {
                    spawnerType = SpawnerType.valueOf(spawnerTypeStr);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Tipo spawner non valido '" + spawnerTypeStr + "' per lo spawner '" + name + "'");
                }
            }

            Location location = new Location(world, x, y, z);
            CustomSpawner spawner = new CustomSpawner(name, location, mobType, quantity, spawnerType);

            spawners.put(name, spawner);
        }

        plugin.getLogger().info("Caricati " + spawners.size() + " spawner dal file.");

        if (!debugPlayers.isEmpty()) {
            removeAllDebugHolograms();
            createDebugHolograms();
        }
    }

    private void loadSpawnersForWorld(World world) {
        spawnersConfig = YamlConfiguration.loadConfiguration(spawnersFile);

        ConfigurationSection spawnersSection = spawnersConfig.getConfigurationSection("spawners");
        if (spawnersSection == null) {
            return;
        }

        int loadedCount = 0;
        for (String name : spawnersSection.getKeys(false)) {
            ConfigurationSection section = spawnersSection.getConfigurationSection(name);
            if (section == null) continue;

            String worldName = section.getString("world");

            if (!world.getName().equals(worldName)) {
                continue;
            }

            boolean alreadyExists = spawners.values().stream()
                    .anyMatch(existing -> existing.getName().equals(name) &&
                            existing.getLocation().getWorld().getName().equals(worldName));

            if (alreadyExists) {
                continue;
            }

            try {
                double x = section.getDouble("x");
                double y = section.getDouble("y");
                double z = section.getDouble("z");
                String mobType = section.getString("mobType");
                int quantity = section.getInt("quantity");

                SpawnerType spawnerType = SpawnerType.MYTHICMOB;
                String spawnerTypeStr = section.getString("spawnerType");
                if (spawnerTypeStr != null) {
                    try {
                        spawnerType = SpawnerType.valueOf(spawnerTypeStr);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Tipo spawner non valido '" + spawnerTypeStr + "'");
                    }
                }

                Location location = new Location(world, x, y, z);
                CustomSpawner spawner = new CustomSpawner(name, location, mobType, quantity, spawnerType);

                spawners.put(name, spawner);

                if (!debugPlayers.isEmpty()) {
                    createHologramForSpawner(spawner);
                }

                loadedCount++;
            } catch (Exception e) {
                plugin.getLogger().severe("Errore nel caricamento dello spawner '" + name + "': " + e.getMessage());
            }
        }

        plugin.getLogger().info("Caricati " + loadedCount + " spawner per il mondo '" + world.getName() + "'");
    }

    public void clearActiveMobs() {
        for (Entity mob : activeMobs) {
            if (!mob.isDead() && mob.isValid()) {
                mob.remove();
            }
        }
        activeMobs.clear();
        worldMobs.clear();
    }

    public void reloadAllSpawners() {
        plugin.getLogger().info("Ricaricamento completo di tutti gli spawner...");
        spawners.clear();
        loadSpawners();
        plugin.getLogger().info("Ricaricati tutti gli spawner dal file. Totale in memoria: " + spawners.size());
    }

    public Collection<CustomSpawner> getAllSpawners() {
        return spawners.values();
    }

    /**
     * Restituisce i mondi con spawner attivi
     */
    public Set<String> getActiveWorlds() {
        return activeWorlds.keySet();
    }

    /**
     * Ferma tutti i task quando il plugin si disabilita
     */
    public void stopAllTasks() {
        if (debugTask != null && !debugTask.isCancelled()) {
            debugTask.cancel();
        }

        if (spawnerCheckTask != null && !spawnerCheckTask.i