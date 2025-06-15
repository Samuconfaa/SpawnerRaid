package it.samuconfaa.spawnerRaid;

import org.bukkit.Location;

public class CustomSpawner {

    private final String name;
    private final Location location;
    private final String mobType;
    private final int quantity;

    public CustomSpawner(String name, Location location, String mobType, int quantity) {
        this.name = name;
        this.location = location;
        this.mobType = mobType;
        this.quantity = quantity;
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

    @Override
    public String toString() {
        return "CustomSpawner{" +
                "name='" + name + '\'' +
                ", location=" + location +
                ", mobType='" + mobType + '\'' +
                ", quantity=" + quantity +
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