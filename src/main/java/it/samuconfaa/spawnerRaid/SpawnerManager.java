package it.samuconfaa.spawnerRaid;

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

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpawnerManager {

    private final SpawnerRaid plugin;
    private final Map<String, CustomSpawner> spawners;
    private final Set<Entity> activeMobs;
    private final Set<Player> debugPlayers; // Giocatori che hanno il debug attivo
    private BukkitRunnable debugTask; // Task per mostrare le particelle
    private File spawnersFile;
    private YamlConfiguration spawnersConfig;

    public SpawnerManager(SpawnerRaid plugin) {
        this.plugin = plugin;
        this.spawners = new HashMap<>();
        this.activeMobs = new HashSet<>();
        this.debugPlayers = new HashSet<>();

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

        // Inizializza il task per il debug visivo
        startDebugTask();
    }

    /**
     * Attiva/disattiva il debug visivo per un giocatore
     * @param player Il giocatore
     * @return true se il debug è ora attivo, false se è disattivato
     */
    public boolean toggleDebugMode(Player player) {
        if (debugPlayers.contains(player)) {
            debugPlayers.remove(player);
            return false;
        } else {
            debugPlayers.add(player);
            return true;
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
                    return; // Non fare nulla se nessuno ha il debug attivo
                }

                for (Player player : new HashSet<>(debugPlayers)) {
                    if (!player.isOnline()) {
                        debugPlayers.remove(player);
                        continue;
                    }

                    // Mostra particelle solo per gli spawner nel mondo del giocatore
                    for (CustomSpawner spawner : spawners.values()) {
                        if (spawner.getLocation().getWorld().getName().equals(player.getWorld().getName())) {
                            showDebugParticles(player, spawner);
                        }
                    }
                }
            }
        };

        // Esegui ogni 20 tick (1 secondo)
        debugTask.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Mostra le particelle di debug per uno spawner
     */
    private void showDebugParticles(Player player, CustomSpawner spawner) {
        Location loc = spawner.getLocation();

        // Verifica che il giocatore sia abbastanza vicino per vedere le particelle (max 50 blocchi)
        if (player.getLocation().distance(loc) > 50) {
            return;
        }

        // Particelle diverse in base al tipo di spawner
        Particle particle;
        if (spawner.getSpawnerType() == SpawnerType.VANILLA) {
            particle = Particle.FLAME; // Fiamme rosse per mob vanilla
        } else {
            particle = Particle.DRAGON_BREATH; // Particelle viola per MythicMobs
        }

        // Crea un cerchio di particelle attorno alla posizione dello spawner
        for (int i = 0; i < 8; i++) {
            double angle = 2 * Math.PI * i / 8;
            double x = loc.getX() + Math.cos(angle) * 1.5;
            double z = loc.getZ() + Math.sin(angle) * 1.5;
            double y = loc.getY() + 1;

            Location particleLoc = new Location(loc.getWorld(), x, y, z);
            player.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
        }

        // Particella centrale più grande
        player.spawnParticle(particle, loc.clone().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);

        // Particelle che salgono verso l'alto
        player.spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 2, 0.1, 0.5, 0.1, 0.02);
    }

    /**
     * Ferma il task di debug quando il plugin si disabilita
     */
    public void stopDebugTask() {
        if (debugTask != null && !debugTask.isCancelled()) {
            debugTask.cancel();
        }
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
            section.set("spawnerType", spawner.getSpawnerType().name()); // Salva il tipo di spawner
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

            // Carica il tipo di spawner, default a MYTHICMOB per compatibilità con versioni precedenti
            SpawnerType spawnerType = SpawnerType.MYTHICMOB;
            String spawnerTypeStr = section.getString("spawnerType");
            if (spawnerTypeStr != null) {
                try {
                    spawnerType = SpawnerType.valueOf(spawnerTypeStr);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Tipo spawner non valido '" + spawnerTypeStr + "' per lo spawner '" + name + "'. Utilizzando MYTHICMOB come default.");
                }
            }

            Location location = new Location(world, x, y, z);
            CustomSpawner spawner = new CustomSpawner(name, location, mobType, quantity, spawnerType);

            spawners.put(name, spawner);
        }

        plugin.getLogger().info("Caricati " + spawners.size() + " spawner dal file.");
    }

    public int activateSpawnersInWorld(World world) {
        plugin.getLogger().info("Tentativo di attivazione spawner nel mondo: " + world.getName());

        // Prima controlla se ci sono spawner in memoria per questo mondo (confronta per NOME, non per oggetto)
        long spawnersInWorldCount = spawners.values().stream()
                .filter(spawner -> spawner.getLocation().getWorld().getName().equals(world.getName()))
                .count();

        plugin.getLogger().info("Spawner in memoria per il mondo '" + world.getName() + "': " + spawnersInWorldCount);

        // Se gli spawner esistono ma hanno un riferimento al mondo vecchio, aggiornali
        boolean needsWorldUpdate = false;
        for (CustomSpawner spawner : new ArrayList<>(spawners.values())) {
            if (spawner.getLocation().getWorld().getName().equals(world.getName()) &&
                    !spawner.getLocation().getWorld().equals(world)) {
                plugin.getLogger().info("Spawner '" + spawner.getName() + "' ha riferimento mondo obsoleto, aggiorno...");
                needsWorldUpdate = true;

                // Rimuovi il vecchio e aggiungi con il nuovo riferimento mondo
                spawners.remove(spawner.getName());
                Location newLocation = new Location(world,
                        spawner.getLocation().getX(),
                        spawner.getLocation().getY(),
                        spawner.getLocation().getZ());
                CustomSpawner updatedSpawner = new CustomSpawner(
                        spawner.getName(),
                        newLocation,
                        spawner.getMobType(),
                        spawner.getQuantity(),
                        spawner.getSpawnerType()); // Mantieni il tipo di spawner
                spawners.put(updatedSpawner.getName(), updatedSpawner);
                plugin.getLogger().info("Spawner '" + spawner.getName() + "' aggiornato con nuovo riferimento mondo");
            }
        }

        // Se non ci sono spawner in memoria per questo mondo, prova a ricaricare dal file
        if (spawnersInWorldCount == 0 && !needsWorldUpdate) {
            plugin.getLogger().info("Nessuno spawner trovato in memoria per il mondo '" + world.getName() + "', ricarico dal file...");
            loadSpawnersForWorld(world);
        }

        // Ricontrolla dopo eventuali aggiornamenti
        spawnersInWorldCount = spawners.values().stream()
                .filter(spawner -> spawner.getLocation().getWorld().getName().equals(world.getName()))
                .count();

        plugin.getLogger().info("Spawner finali per il mondo '" + world.getName() + "': " + spawnersInWorldCount);

        int activated = 0;
        for (CustomSpawner spawner : spawners.values()) {
            // Confronta per nome mondo, non per oggetto World
            if (spawner.getLocation().getWorld().getName().equals(world.getName())) {
                plugin.getLogger().info("Attivando spawner: " + spawner.getName() + " (" + spawner.getSpawnerType() + ") nel mondo: " + world.getName());
                spawnMobs(spawner);
                activated++;
            }
        }

        plugin.getLogger().info("Totale spawner attivati nel mondo '" + world.getName() + "': " + activated);
        return activated;
    }

    // Metodo migliorato per caricare solo gli spawner di un mondo specifico
    private void loadSpawnersForWorld(World world) {
        plugin.getLogger().info("Tentativo di caricamento spawner per il mondo: " + world.getName());

        // Ricarica sempre il file per essere sicuri di avere i dati più recenti
        spawnersConfig = YamlConfiguration.loadConfiguration(spawnersFile);

        ConfigurationSection spawnersSection = spawnersConfig.getConfigurationSection("spawners");
        if (spawnersSection == null) {
            plugin.getLogger().warning("Sezione 'spawners' non trovata nel file spawners.yml");
            return;
        }

        plugin.getLogger().info("Spawner trovati nel file: " + spawnersSection.getKeys(false).size());

        int loadedCount = 0;
        for (String name : spawnersSection.getKeys(false)) {
            plugin.getLogger().info("Processando spawner: " + name);

            ConfigurationSection section = spawnersSection.getConfigurationSection(name);
            if (section == null) {
                plugin.getLogger().warning("Sezione spawner '" + name + "' è null");
                continue;
            }

            String worldName = section.getString("world");
            plugin.getLogger().info("Spawner '" + name + "' è nel mondo: " + worldName + " (cercando: " + world.getName() + ")");

            // Carica solo gli spawner del mondo specificato
            if (!world.getName().equals(worldName)) {
                plugin.getLogger().info("Spawner '" + name + "' saltato perché in mondo diverso");
                continue;
            }

            // Controlla se lo spawner esiste già (confronta per nome mondo, non oggetto)
            boolean alreadyExists = spawners.values().stream()
                    .anyMatch(existing -> existing.getName().equals(name) &&
                            existing.getLocation().getWorld().getName().equals(worldName));

            if (alreadyExists) {
                plugin.getLogger().info("Spawner '" + name + "' già presente in memoria per questo mondo, saltato");
                continue;
            }

            try {
                double x = section.getDouble("x");
                double y = section.getDouble("y");
                double z = section.getDouble("z");
                String mobType = section.getString("mobType");
                int quantity = section.getInt("quantity");

                // Carica il tipo di spawner
                SpawnerType spawnerType = SpawnerType.MYTHICMOB;
                String spawnerTypeStr = section.getString("spawnerType");
                if (spawnerTypeStr != null) {
                    try {
                        spawnerType = SpawnerType.valueOf(spawnerTypeStr);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Tipo spawner non valido '" + spawnerTypeStr + "' per lo spawner '" + name + "'. Utilizzando MYTHICMOB come default.");
                    }
                }

                plugin.getLogger().info("Caricamento spawner '" + name + "' - Posizione: " + x + "," + y + "," + z +
                        " - Mob: " + mobType + " - Quantità: " + quantity + " - Tipo: " + spawnerType);

                Location location = new Location(world, x, y, z);
                CustomSpawner spawner = new CustomSpawner(name, location, mobType, quantity, spawnerType);

                spawners.put(name, spawner);
                loadedCount++;
                plugin.getLogger().info("Spawner '" + name + "' caricato con successo per il mondo '" + worldName + "'");
            } catch (Exception e) {
                plugin.getLogger().severe("Errore nel caricamento dello spawner '" + name + "': " + e.getMessage());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Caricati " + loadedCount + " spawner per il mondo '" + world.getName() + "'");
    }

    private void spawnMobs(CustomSpawner spawner) {
        Location location = spawner.getLocation();

        for (int i = 0; i < spawner.getQuantity(); i++) {
            // Offset casuale per evitare che i mob si sovrappongano
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetZ = (Math.random() - 0.5) * 2;
            Location spawnLoc = location.clone().add(offsetX, 0, offsetZ);

            Entity entity = null;

            if (spawner.getSpawnerType() == SpawnerType.VANILLA) {
                // Spawn di mob vanilla
                try {
                    EntityType entityType = EntityType.valueOf(spawner.getMobType().toUpperCase());
                    entity = spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);
                    plugin.getLogger().info("Spawnato mob vanilla: " + entityType + " alla posizione " + spawnLoc);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Tipo di entità vanilla non valido: " + spawner.getMobType());
                    continue;
                } catch (Exception e) {
                    plugin.getLogger().warning("Errore nel spawn del mob vanilla " + spawner.getMobType() + ": " + e.getMessage());
                    continue;
                }
            } else {
                // Spawn di mob MythicMobs usando la nuova API
                try {
                    entity = MythicBukkit.inst().getMobManager()
                            .spawnMob(spawner.getMobType(), BukkitAdapter.adapt(spawnLoc))
                            .getEntity().getBukkitEntity();
                    plugin.getLogger().info("Spawnato mob MythicMobs: " + spawner.getMobType() + " alla posizione " + spawnLoc);
                } catch (Exception e) {
                    plugin.getLogger().warning("Errore nel spawn del mob MythicMobs " + spawner.getMobType() + ": " + e.getMessage());
                    continue;
                }
            }

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
        }
    }

    public void activateMobsNearPlayer(Player player) {
        double activationDistance = plugin.getConfigManager().getActivationDistance();

        for (Entity mob : new HashSet<>(activeMobs)) {
            if (mob.isDead() || !mob.isValid()) {
                activeMobs.remove(mob);
                continue;
            }

            // Verifica che il mob sia nello stesso mondo del giocatore (confronta per nome)
            if (!mob.getWorld().getName().equals(player.getWorld().getName())) {
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

    public void reloadAllSpawners() {
        plugin.getLogger().info("Ricaricamento completo di tutti gli spawner...");
        spawners.clear();
        loadSpawners();
        plugin.getLogger().info("Ricaricati tutti gli spawner dal file. Totale in memoria: " + spawners.size());

        // Debug: elenca tutti gli spawner caricati
        for (CustomSpawner spawner : spawners.values()) {
            plugin.getLogger().info("Spawner in memoria: " + spawner.getName() + " (" + spawner.getSpawnerType() + ") nel mondo " +
                    spawner.getLocation().getWorld().getName());
        }
    }

    public Collection<CustomSpawner> getAllSpawners() {
        return spawners.values();
    }
}