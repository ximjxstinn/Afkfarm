package de.ximjxstinn.afkfarm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Einstellungen, gespeichert in .minecraft/config/afkfarm.json */
public class AfkConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("afkfarm.json");

    /** Automatisch essen, wenn der Hunger sinkt. */
    public boolean autoEat = true;
    /** Essen, sobald die Hungerleiste unter diesen Wert fällt (20 = voll). */
    public int eatBelow = 14;

    /** false = nur Mobs im Fadenkreuz schlagen (sicherster Modus), true = automatisch zum nächsten Mob drehen. */
    public boolean autoAim = false;
    /** Reichweite für Auto-Aim in Blöcken (max. 3.0 = Vanilla). */
    public double range = 3.0;

    /** Angriff pausieren / Waffe wechseln, wenn nur noch so viel Haltbarkeit übrig ist. */
    public int minDurability = 10;
    /** Bei niedriger Haltbarkeit auf eine andere Waffe in der Hotbar wechseln. */
    public boolean switchWeapon = true;

    /** Zufällige Extra-Verzögerung nach jedem Schlag (0 bis X Ticks), wirkt natürlicher. */
    public int extraDelayMax = 3;
    /** Mobs mit Namensschild nicht angreifen. */
    public boolean ignoreNamed = true;

    public static AfkConfig load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                AfkConfig cfg = GSON.fromJson(reader, AfkConfig.class);
                if (cfg != null) {
                    cfg.clamp();
                    return cfg;
                }
            } catch (Exception e) {
                System.err.println("[AFK Farm] Config konnte nicht gelesen werden, nutze Standardwerte: " + e.getMessage());
            }
        }
        AfkConfig cfg = new AfkConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        clamp();
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("[AFK Farm] Config konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    private void clamp() {
        eatBelow = Math.max(1, Math.min(19, eatBelow));
        range = Math.max(1.0, Math.min(3.0, range));
        minDurability = Math.max(0, Math.min(500, minDurability));
        extraDelayMax = Math.max(0, Math.min(10, extraDelayMax));
    }
}
