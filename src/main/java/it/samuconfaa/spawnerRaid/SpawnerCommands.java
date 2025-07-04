package it.samuconfaa.spawnerRaid;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import it.samuconfaa.spawnerRaid.SpawnerRaid;
import it.samuconfaa.spawnerRaid.CustomSpawner;
import it.samuconfaa.spawnerRaid.CustomSpawner.SpawnerType;
import io.lumine.mythic.bukkit.MythicBukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnerCommands implements CommandExecutor, TabCompleter {

    private final SpawnerRaid plugin;

    public SpawnerCommands(SpawnerRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        switch (command.getName().toLowerCase()) {
            case "setspawner":
                return handleSetSpawner(sender, args);
            case "attivaspawner":
                return handleAttivaSpawner(sender, args);
            case "stopspawner":
                return handleStopSpawner(sender, args);
            case "eliminaspawner":
                return handleEliminaSpawner(sender, args);
            case "debugspawners":
                return handleDebugSpawners(sender, args);
            case "spawnerdebug":
                return handleSpawnerDebug(sender, args);
            case "renamespawner":
                return handleRenameSpawner(sender, args);
            default:
                return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        switch (command.getName().toLowerCase()) {
            case "setspawner":
                return getSetSpawnerCompletions(args);
            case "attivaspawner":
            case "stopspawner":
                return getAttivaSpawnerCompletions(args);
            case "eliminaspawner":
                return getEliminaSpawnerCompletions(args);
            case "renamespawner":
                return getRenameSpawnerCompletions(args);
            case "debugspawners":
            case "spawnerdebug":
                // Nessun argomento per debug
                return completions;
            default:
                return completions;
        }
    }

    private boolean handleRenameSpawner(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spawnerraid.rename")) {
            sender.sendMessage(ChatColor.RED + "Non hai il permesso per utilizzare questo comando!");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /renamespawner <vecchio_nome> <nuovo_nome>");
            return true;
        }

        String oldName = args[0];
        String newName = args[1];

        // Verifica se il vecchio spawner esiste
        CustomSpawner spawner = plugin.getSpawnerManager().getSpawner(oldName);
        if (spawner == null) {
            sender.sendMessage(ChatColor.RED + "Spawner '" + oldName + "' non trovato!");
            return true;
        }

        // Verifica se il nuovo nome è già in uso
        if (plugin.getSpawnerManager().getSpawner(newName) != null) {
            sender.sendMessage(ChatColor.RED + "Esiste già uno spawner con il nome '" + newName + "'!");
            return true;
        }

        // Verifica che il nuovo nome non sia vuoto o contenga caratteri speciali
        if (newName.trim().isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Il nuovo nome non può essere vuoto!");
            return true;
        }

        if (!newName.matches("^[a-zA-Z0-9_-]+$")) {
            sender.sendMessage(ChatColor.RED + "Il nuovo nome può contenere solo lettere, numeri, underscore e trattini!");
            return true;
        }

        // Esegui il rename
        boolean success = plugin.getSpawnerManager().renameSpawner(oldName, newName);

        if (success) {
            plugin.getSpawnerManager().saveSpawners();
            sender.sendMessage(ChatColor.GREEN + "Spawner rinominato con successo!");
            sender.sendMessage(ChatColor.YELLOW + "'" + oldName + "' → '" + newName + "'");
        } else {
            sender.sendMessage(ChatColor.RED + "Errore durante il rename dello spawner!");
        }

        return true;
    }

    private List<String> getRenameSpawnerCompletions(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Primo argomento: vecchio nome spawner (tab completer con spawner esistenti)
            completions = plugin.getSpawnerManager().getAllSpawners().stream()
                    .map(CustomSpawner::getName)
                    .filter(spawnerName -> spawnerName.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Secondo argomento: nuovo nome (suggerimenti)
            String oldName = args[0];
            completions.add(oldName + "_cc1");

            // Filtra i suggerimenti in base all'input
            completions = completions.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }

    private boolean handleStopSpawner(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /stopspawner <nome_mondo>");
            return true;
        }

        String worldName = args[0];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "Il mondo '" + worldName + "' non esiste!");
            return true;
        }

        int stopped = plugin.getSpawnerManager().stopSpawnersInWorld(world);

        if (stopped == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Nessuno spawner attivo trovato nel mondo '" + worldName + "'!");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Fermati " + stopped + " spawner nel mondo '" + worldName + "'!");
        }

        return true;
    }

    private List<String> getSetSpawnerCompletions(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Primo argomento: tipo di spawner
            completions.add("vanilla");
            completions.add("mythicmob");

            return completions.stream()
                    .filter(type -> type.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length == 2) {
            // Secondo argomento: nome spawner
            completions.add("spawner_" + (System.currentTimeMillis() % 1000));
            completions.add("raid_spawner_" + (int)(Math.random() * 999 + 1));
            completions.add("mob_spawner_" + (int)(Math.random() * 999 + 1));

            return completions.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length == 3) {
            // Terzo argomento: nome mob (dipende dal tipo)
            String spawnerType = args[0].toLowerCase();

            if ("vanilla".equals(spawnerType)) {
                // Suggerimenti per mob vanilla
                completions = getVanillaMobSuggestions(args[2]);
            } else if ("mythicmob".equals(spawnerType)) {
                // Suggerimenti per mob MythicMobs
                completions = getMythicMobSuggestions(args[2]);
            }

        } else if (args.length == 4) {
            // Quarto argomento: quantità
            completions.add("1");
            completions.add("5");
            completions.add("10");
            completions.add("25");
            completions.add("50");
        }

        return completions;
    }

    private List<String> getVanillaMobSuggestions(String input) {
        List<String> vanillaMobs = Arrays.asList(
                "ZOMBIE", "SKELETON", "CREEPER", "SPIDER", "ENDERMAN", "WITCH",
                "ZOMBIE_VILLAGER", "HUSK", "STRAY", "WITHER_SKELETON", "BLAZE",
                "GHAST", "MAGMA_CUBE", "SLIME", "SILVERFISH", "ENDERMITE",
                "GUARDIAN", "ELDER_GUARDIAN", "SHULKER", "VEX", "VINDICATOR",
                "EVOKER", "PILLAGER", "RAVAGER", "PHANTOM", "DROWNED",
                "PIGLIN", "PIGLIN_BRUTE", "ZOMBIFIED_PIGLIN", "HOGLIN", "ZOGLIN",
                "WARDEN", "ALLAY", "AXOLOTL", "GOAT", "GLOW_SQUID"
        );

        return vanillaMobs.stream()
                .filter(mob -> mob.toLowerCase().startsWith(input.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getMythicMobSuggestions(String input) {
        List<String> completions = new ArrayList<>();

        try {
            completions = MythicBukkit.inst().getMobManager().getMobNames()
                    .stream()
                    .filter(name -> name.toLowerCase().startsWith(input.toLowerCase()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Fallback se non riesce a ottenere i nomi dei mob
            completions.add("SkeletonKing");
            completions.add("ZombieMinion");
            completions.add("CustomBoss");
            completions.add("FireDragon");
            completions.add("IceGolem");
        }

        return completions;
    }

    private List<String> getAttivaSpawnerCompletions(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggerimenti per nomi dei mondi
            completions = Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(worldName -> worldName.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }

    private List<String> getEliminaSpawnerCompletions(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggerimenti per nomi degli spawner esistenti
            completions = plugin.getSpawnerManager().getAllSpawners().stream()
                    .map(CustomSpawner::getName)
                    .filter(spawnerName -> spawnerName.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return completions;
    }

    private boolean handleSpawnerDebug(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Questo comando può essere eseguito solo da un giocatore!");
            return true;
        }

        if (!sender.hasPermission("spawnerraid.debug")) {
            sender.sendMessage(ChatColor.RED + "Non hai il permesso per utilizzare questo comando!");
            return true;
        }

        Player player = (Player) sender;

        boolean isDebugActive = plugin.getSpawnerManager().toggleDebugMode(player);

        if (isDebugActive) {
            sender.sendMessage(ChatColor.GREEN + "✓ Debug visivo spawner ATTIVATO!");
            sender.sendMessage(ChatColor.YELLOW + "Ora puoi vedere le particelle sui tuoi spawner.");
        } else {
            sender.sendMessage(ChatColor.RED + "✗ Debug visivo spawner DISATTIVATO!");
            sender.sendMessage(ChatColor.GRAY + "Le particelle non sono più visibili.");
        }

        return true;
    }

    private boolean handleDebugSpawners(CommandSender sender, String[] args) {
        if (!sender.hasPermission("spawnerraid.debug")) {
            sender.sendMessage(ChatColor.RED + "Non hai il permesso per utilizzare questo comando!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "=== DEBUG SPAWNERS ===");

        // Mostra spawner in memoria
        sender.sendMessage(ChatColor.GREEN + "Spawner in memoria: " + plugin.getSpawnerManager().getAllSpawners().size());
        for (CustomSpawner spawner : plugin.getSpawnerManager().getAllSpawners()) {
            sender.sendMessage(ChatColor.WHITE + "- " + spawner.getName() + " nel mondo " +
                    spawner.getLocation().getWorld().getName() + " (" + spawner.getSpawnerType() +
                    " - " + spawner.getMobType() + " x" + spawner.getQuantity() + ")");
        }

        // Forza ricaricamento e mostra risultato
        sender.sendMessage(ChatColor.YELLOW + "Forzando ricaricamento...");
        plugin.getSpawnerManager().reloadAllSpawners();

        sender.sendMessage(ChatColor.GREEN + "Spawner dopo ricaricamento: " + plugin.getSpawnerManager().getAllSpawners().size());

        return true;
    }

    private boolean handleSetSpawner(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Questo comando può essere eseguito solo da un giocatore!");
            return true;
        }

        if (args.length != 4) {
            sender.sendMessage(ChatColor.RED + "Uso: /setspawner <vanilla|mythicmob> <nome> <mob> <quantità>");
            return true;
        }

        Player player = (Player) sender;
        String spawnerTypeStr = args[0].toLowerCase();
        String name = args[1];
        String mobType = args[2];
        int quantity;

        // Verifica tipo spawner
        SpawnerType spawnerType;
        if ("vanilla".equals(spawnerTypeStr)) {
            spawnerType = SpawnerType.VANILLA;
        } else if ("mythicmob".equals(spawnerTypeStr)) {
            spawnerType = SpawnerType.MYTHICMOB;
        } else {
            sender.sendMessage(ChatColor.RED + "Tipo spawner non valido! Usa 'vanilla' o 'mythicmob'");
            return true;
        }

        // Verifica quantità
        try {
            quantity = Integer.parseInt(args[3]);
            if (quantity <= 0) {
                sender.sendMessage(ChatColor.RED + "La quantità deve essere maggiore di 0!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "La quantità deve essere un numero valido!");
            return true;
        }

        // Verifica se il mob esiste
        if (spawnerType == SpawnerType.VANILLA) {
            try {
                EntityType.valueOf(mobType.toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Il mob vanilla '" + mobType + "' non esiste!");
                return true;
            }
        } else {
            if (!MythicBukkit.inst().getMobManager().getMythicMob(mobType).isPresent()) {
                sender.sendMessage(ChatColor.RED + "Il mob MythicMobs '" + mobType + "' non esiste!");
                return true;
            }
        }

        // Verifica se esiste già uno spawner con lo stesso nome
        if (plugin.getSpawnerManager().getSpawner(name) != null) {
            sender.sendMessage(ChatColor.RED + "Esiste già uno spawner con il nome '" + name + "'!");
            return true;
        }

        Location location = player.getLocation();
        CustomSpawner spawner = new CustomSpawner(name, location, mobType, quantity, spawnerType);

        plugin.getSpawnerManager().addSpawner(spawner);
        plugin.getSpawnerManager().saveSpawners();

        sender.sendMessage(ChatColor.GREEN + "Spawner '" + name + "' (" + spawnerType + ") creato con successo!");
        sender.sendMessage(ChatColor.YELLOW + "Posizione: " + location.getWorld().getName() +
                " X:" + location.getBlockX() + " Y:" + location.getBlockY() + " Z:" + location.getBlockZ());

        return true;
    }

    private boolean handleAttivaSpawner(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /attivaspawner <nome_mondo>");
            return true;
        }

        String worldName = args[0];
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            sender.sendMessage(ChatColor.RED + "Il mondo '" + worldName + "' non esiste!");
            return true;
        }

        int activated = plugin.getSpawnerManager().activateSpawnersInWorld(world);

        if (activated == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Nessuno spawner trovato nel mondo '" + worldName + "'!");
        } else {
            sender.sendMessage(ChatColor.GREEN + "Attivati " + activated + " spawner nel mondo '" + worldName + "'!");
        }

        return true;
    }

    private boolean handleEliminaSpawner(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /eliminaspawner <nome>");
            return true;
        }

        String name = args[0];

        if (plugin.getSpawnerManager().removeSpawner(name)) {
            plugin.getSpawnerManager().saveSpawners();
            sender.sendMessage(ChatColor.GREEN + "Spawner '" + name + "' eliminato con successo!");
        } else {
            sender.sendMessage(ChatColor.RED + "Spawner '" + name + "' non trovato!");
        }

        return true;
    }
}