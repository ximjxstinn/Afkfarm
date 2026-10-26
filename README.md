# AFK Farm (Fabric 1.21.11)

Client-Mod fürs AFK-Mobfarming: schlägt Mobs am Farmplatz automatisch, isst bei Hunger und schützt deine Waffe.

## Features
- **Auto-Angriff am Platz** – schlägt nur mit vollem Angriffs-Cooldown, plus kleine zufällige Verzögerung.
  - *Fadenkreuz-Modus* (Standard): greift nur den Mob an, auf den du schaust – ideal für Mobfarmen.
  - *Auto-Aim*: dreht dich weich zum nächsten sichtbaren Mob in Reichweite (max. 3 Blöcke).
- **Auto-Essen** – nimmt das beste Essen aus der Hotbar, isst und wechselt zurück zur Waffe.
  Kein Gammelfleisch, Spinnenauge, Kugelfisch, roher Hühnchen, Chorusfrucht, keine Goldäpfel.
- **Haltbarkeits-Schutz** – wechselt auf ein anderes Schwert/eine Axt in der Hotbar oder pausiert, bevor die Waffe kaputtgeht.
- Greift nur feindliche Mobs an, keine Tiere, Villager oder Spieler. Benannte Mobs werden standardmäßig ignoriert.

## Bedienung
- **K** – AFK-Farm an/aus (änderbar unter Optionen → Steuerung → Verschiedenes)
- `/afkfarm` – an/aus
- `/afkfarm status` – aktuelle Einstellungen
- `/afkfarm aim true|false`
- `/afkfarm range <1.0–3.0>`
- `/afkfarm autoeat true|false`
- `/afkfarm eatbelow <1–19>`
- `/afkfarm durability <0–500>`
- `/afkfarm switchweapon true|false`
- `/afkfarm delay <0–10>`
- `/afkfarm ignorenamed true|false`

Einstellungen landen in `.minecraft/config/afkfarm.json`.

## Tipp
Drück **F3 + P**, damit sich beim Tabben aus dem Spiel kein Pausemenü öffnet.

## Bauen
`./gradlew build` → Jar liegt in `build/libs/afkfarm-1.0.0.jar`
