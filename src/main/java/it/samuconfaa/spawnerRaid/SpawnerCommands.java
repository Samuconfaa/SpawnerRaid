package it.samuconfaa.spawnerRaid;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import it.samuconfaa.spawnerRaid.SpawnerRaid;
import it.samuconfaa.spawnerRaid.CustomSpawner;
import io.lumine.mythic.bukkit.MythicBukkit;

public class SpawnerCommands implements CommandExecutor {

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
            default:
                return false;
        }
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