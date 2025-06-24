package it.samuconfaa.spawnerRaid;

import org.bukkit.Location;

public class CustomSpawner {

    private final String name;
    private final Location location;
    private final String mobType;
    private final int quantity;
    private final SpawnerType spawnerType; // Nuovo campo per il tipo di spawner

    public enum SpawnerType {
        VANILLA,
        MYTHICMOB
    }

    public CustomSpawner(String name, Location location, String mobType, int quantity, SpawnerType spawnerType) {
        this.name = name;
        this.location = location;
        this.mobType = mobType;
        this.quantity = quantity;
        this.spawnerType = spawnerType;
    }

    // Costruttore per compatibilità con versioni precedenti (default a MYTHICMOB)
    public CustomSpawner(String name, Location location, String mobType, int quantity) {
        this(name, location, mobType, quantity, SpawnerType.MYTHICMOB);
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location.clone(); // Restituisce una copia per evitare modifiche
    }

    public String getMobType() {
        return mobType;
    }

    public int getQuantity() {
        return quantity;
    }

    public SpawnerType getSpawnerType() {
        return spawnerType;
    }

    @Override
    public String toString() {
        return "CustomSpawner{" +
                "name='" + name + '\'' +
                ", location=" + location +
                ", mobType='" + mobType + '\'' +
                ", quantity=" + quantity +
                ", spawnerType=" + spawnerType +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        CustomSpawner that = (CustomSpawner) obj;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}