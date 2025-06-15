package it.samuconfaa.spawnerRaid;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import it.samuconfaa.spawnerRaid.SpawnerRaid;
import it.samuconfaa.spawnerRaid.CustomSpawner;
import io.lumine.mythic.bukkit.MythicBukkit;

import java.util.ArrayList;
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
            case "eliminaspawner":
                return handleEliminaSpawner(sender, args);
            case "debugspawners":
                return handleDebugSpawners(sender, args);
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
                return getAttivaSpawnerCompletions(args);
            case "eliminaspawner":
                return getEliminaSpawnerCompletions(args);
            case "debugspawners":
                // Nessun argomento per debug
                return completions;
            default:
                return completions;
        }
    }

    private List<String> getSetSpawnerCompletions(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggerimenti per nomi spawner (3 nomi casuali)
            completions.add("spawner_" + (System.currentTimeMillis() % 1000));
            completions.add("raid_spawner_" + (int)(Math.random() * 999 + 1));
            completions.add("mob_spawner_" + (int)(Math.random() * 999 + 1));

            // Filtra in base a quello che ha già scritto l'utente
            return completions.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        } else if (args.length == 2) {
            // Suggerimenti per nomi mob MythicMobs
            try {
                completions = MythicBukkit.inst().getMobManager().getMobNames()
                        .stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                // Fallback se non riesce a ottenere i nomi dei mob
                completions.add("SkeletonKing");
                completions.add("ZombieMinion");
                completions.add("CustomBoss");
                completions.add("FireDragon");
                completions.add("IceGolem");
            }

        } else if (args.length == 3) {
            // Suggerimenti per quantità (5 quantità random)
            completions.add("1");
            completions.add("5");
            completions.add("10");
            completions.add("25");
            completions.add("50");
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
                    spawner.getLocation().getWorld().getName() + " (" + spawner.getMobType() + " x" + spawner.getQuantity() + ")");
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

        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /setspawner <nome> <mob_mythic> <quantità>");
            return true;
        }

        Player player = (Player) sender;
        String name = args[0];
        String mobType = args[1];
        int quantity;

        try {
            quantity = Integer.parseInt(args[2]);
            if (quantity <= 0) {
                sender.sendMessage(ChatColor.RED + "La quantità deve essere maggiore di 0!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "La quantità deve essere un numero valido!");
            return true;
        }

        // Verifica se il mob di MythicMobs esiste
        if (!MythicBukkit.inst().getMobManager().getMythicMob(mobType).isPresent()) {
            sender.sendMessage(ChatColor.RED + "Il mob MythicMobs '" + mobType + "' non esiste!");
            return true;
        }

        // Verifica se esiste già uno spawner con lo stesso nome
        if (plugin.getSpawnerManager().getSpawner(name) != null) {
            sender.sendMessage(ChatColor.RED + "Esiste già uno spawner con il nome '" + name + "'!");
            return true;
        }

        Location location = player.getLocation();
        CustomSpawner spawner = new CustomSpawner(name, location, mobType, quantity);

        plugin.getSpawnerManager().addSpawner(spawner);
        plugin.getSpawnerManager().saveSpawners();

        sender.sendMessage(ChatColor.GREEN + "Spawner '" + name + "' creato con successo!");
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