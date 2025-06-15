package it.samuconfaa.spawnerRaid;

import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.*;
import java.util.stream.Collectors;

public class SpawnerTabCompleter implements TabCompleter {

    private final SpawnerRaid plugin;

    // Nomi casuali per spawner
    private final List<String> randomSpawnerNames = Arrays.asList(
            "GuardianSpawner", "EliteSpawner", "BossSpawner", "DefenderSpawner", "SentinelSpawner",
            "WarriorSpawner", "MageSpawner", "ArcherSpawner", "KnightSpawner", "AssassinSpawner",
            "DragonSpawner", "DemonSpawner", "AngelSpawner", "ShadowSpawner", "FlameSpawner",
            "IceSpawner", "ThunderSpawner", "EarthSpawner", "WindSpawner", "VoidSpawner"
    );

    // Quantità casuali
    private final List<String> randomQuantities = Arrays.asList("1", "3", "5", "10", "15");

    public SpawnerTabCompleter(SpawnerRaid plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "setspawner":
                return handleSetSpawnerCompletion(args);
            case "attivaspawner":
                return handleAttivaSpawnerCompletion(args);
            case "eliminaspawner":
                return handleEliminaSpawnerCompletion(args);
            case "debugspawners":
                return new ArrayList<>(); // Nessun argomento per debug
            default:
                return new ArrayList<>();
        }
    }

    private List<String> handleSetSpawnerCompletion(String[] args) {
        switch (args.length) {
            case 1: // Nome spawner
                return getRandomSpawnerNames(args[0]);
            case 2: // Tipo mob MythicMobs
                return getMythicMobTypes(args[1]);
            case 3: // Quantità
                return getRandomQuantities(args[2]);
            default:
                return new ArrayList<>();
        }
    }

    private List<String> handleAttivaSpawnerCompletion(String[] args) {
        if (args.length == 1) {
            return getWorldNames(args[0]);
        }
        return new ArrayList<>();
    }

    private List<String> handleEliminaSpawnerCompletion(String[] args) {
        if (args.length == 1) {
            return getExistingSpawnerNames(args[0]);
        }
        return new ArrayList<>();
    }

    private List<String> getRandomSpawnerNames(String partial) {
        List<String> suggestions = new ArrayList<>();

        // Genera 3 nomi casuali che non esistono già
        Set<String> existingNames = plugin.getSpawnerManager().getAllSpawners()
                .stream()
                .map(spawner -> spawner.getName())
                .collect(Collectors.toSet());

        List<String> availableNames = randomSpawnerNames.stream()
                .filter(name -> !existingNames.contains(name))
                .collect(Collectors.toList());

        Collections.shuffle(availableNames);

        // Prendi i primi 3 (o meno se non ce ne sono abbastanza)
        for (int i = 0; i < Math.min(3, availableNames.size()); i++) {
            String name = availableNames.get(i);
            if (name.toLowerCase().startsWith(partial.toLowerCase())) {
                suggestions.add(name);
            }
        }

        // Se l'utente sta già scrivendo qualcosa, filtra in base al testo parziale
        if (!partial.isEmpty()) {
            suggestions = suggestions.stream()
                    .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Se non ci sono suggerimenti che matchano, mostra tutti i 3 casuali
        if (suggestions.isEmpty() && partial.isEmpty()) {
            for (int i = 0; i < Math.min(3, availableNames.size()); i++) {
                suggestions.add(availableNames.get(i));
            }
        }

        return suggestions;
    }

    private List<String> getMythicMobTypes(String partial) {
        try {
            Collection<String> mobTypes = null;

            // Prova diversi metodi per ottenere i mob MythicMobs
            try {
                // Metodo 1: getMobNames() (versioni più recenti)
                mobTypes = MythicBukkit.inst().getMobManager().getMobNames();
            } catch (Exception e1) {
                try {
                    // Metodo 2: getMobTypes() (versioni più vecchie)
                    mobTypes = (Collection<String>) MythicBukkit.inst().getMobManager().getClass()
                            .getMethod("getMobTypes").invoke(MythicBukkit.inst().getMobManager());
                } catch (Exception e2) {
                    // Metodo 3: Attraverso la registry
                    mobTypes = MythicBukkit.inst().getMobManager().getMythicMobNames();
                }
            }

            if (mobTypes != null) {
                return mobTypes.stream()
                        .filter(mobType -> mobType.toLowerCase().startsWith(partial.toLowerCase()))
                        .sorted()
                        .limit(20)
                        .collect(Collectors.toList());
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Errore nel recuperare i tipi di mob MythicMobs: " + e.getMessage());
        }

        // Fallback: restituisci alcuni mob di esempio se tutti i metodi falliscono
        List<String> fallbackMobs = Arrays.asList(
                "SkeletonKing", "ZombieWarrior", "SpiderQueen", "CreeperBoss", "EndermanMage",
                "BlazeGuard", "GhastLord", "WitherKnight", "GuardianElite", "DragonMinion",
                "OrcWarrior", "ElfArcher", "DwarfMiner", "TrollBerserker", "GoblinThief",
                "PhoenixBird", "IceDragon", "ShadowAssassin", "LightningMage", "EarthGolem"
        );

        return fallbackMobs.stream()
                .filter(mobType -> mobType.toLowerCase().startsWith(partial.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getRandomQuantities(String partial) {
        return randomQuantities.stream()
                .filter(quantity -> quantity.startsWith(partial))
                .collect(Collectors.toList());
    }

    private List<String> getWorldNames(String partial) {
        return Bukkit.getWorlds().stream()
                .map(World::getName)
                .filter(worldName -> worldName.toLowerCase().startsWith(partial.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> getExistingSpawnerNames(String partial) {
        return plugin.getSpawnerManager().getAllSpawners().stream()
                .map(spawner -> spawner.getName())
                .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }
}