# Changelog

## v1.6

### Neu

- Arenaauswahl in den Arenaeinstellungen hinzugefügt
- Zweite Arena **„Ancient City“** hinzugefügt

### Verbessert

- Arena-Scoreboard überarbeitet und wieder funktionsfähig
- Arena-Reset überarbeitet: Spieler werden nun zurück zur Wave-Auswahl teleportiert, um dort neu zu starten
- Spawn-Konfiguration und Arena-Spawnpunkte überarbeitet
- Weltenstruktur an Paper/Minecraft **26.x** angepasst
- Ready-System intern von einzelnen Spielern auf Teams umgestellt

### Geändert

- Funktion zum Neustarten der Waves deaktiviert

### Wichtig

- Version **1.6** hat nun **Stable-Status**

## v1.5

### Neu

- Kistenrandomizer hinzugefügt
- Spawnreihenfolge entspricht nun der Klick-Reihenfolge
- Auf Paper/API 26.2 alpha aktualisiert
- Template-Datei-System eingeführt

### Verbessert

- Startcountdown überarbeitet
- Startlogik verbessert

### Behoben

- Mobspawning in Lobby und Arena entfernt
- Arena und Lobby umgestaltet

---

## v1.4

### Hinzugefügt

- Arena-Welt-Reset hinzugefügt
- Team-Scoreboard unterstützt nun Offline-Spieler
- Neue Reset-Befehle hinzugefügt:
  - `/reset arena`
  - `/reset lobby`
  - `/reset teamworld`
  - `/reset playerdata`

### Geändert

- Dateimanager neu strukturiert
- Textausgaben im Spiel überarbeitet
- Konsolenausgaben überarbeitet
- Interne Projektstruktur verbessert

---

## v1.3

### Neu
- World-Arena von der Lobby getrennt
- Vorbereitung für spätere zusätzliche Arenen hinzugefügt
- Team-Equipment-GUI für Event-Ausrüstung hinzugefügt

### Verbessert
- System auf getrennte Lobby- und Arena-Welten umgestellt
- Teleport-Logik für Lobby und Arena überarbeitet
- Resume-Funktion an die getrennten Welten angepasst
- Respawn-Ablauf verbessert
- Sound-Handling überarbeitet

### Behoben
- Mehrere Bugs im Zusammenhang mit Sounds behoben
- Fehler bei Teleports zwischen Lobby und Arena behoben
- Probleme mit Resume behoben
- Fehler beim Respawn behoben
- Weitere kleinere Bugs beseitigt

### Wichtig
- Die Arena-Welt ist ab Version 1.3 von der Lobby-Welt getrennt
- Diese Änderung dient als Grundlage für spätere neue Arenen

--- 

## v1.2

### Neu
- Welteneinstellungen hinzugefügt
- Arena-Kompass-Option erweitert

### Verbessert
- Arena-Reset verbessert
- GUIs verbessert
- Funktionen übersichtlicher neu angeordnet
- Code angepasst und für Paper/Minecraft 26.1 aktualisiert

### Behoben
- Mehrere Bugs behoben
- Kleinere Fehler im Ablauf und bei den Einstellungen beseitigt

### Wichtig
- Ab Version 1.2 wird Paper/Minecraft 26.1.2 benötigt
  und Java 25 wird vorausgesetzt

### Kompatibilität
- Benötigt **Paper 26.1.2**
- Benötigt **Java 25**
- Nicht kompatibel mit reinem Spigot/Bukkit

---

## v1.1

### Neu
- Willkommensnachricht hinzugefügt
- Weitere abbaubare Items in der Arena hinzugefügt

### Verbessert
- KeepInventory funktioniert nun auch bei Welt-Neugenerierung korrekt
- Spawnpunkte überarbeitet und verbessert
- Resume-Funktion erweitert und an die Spawnpunkte angepasst
- Reset-Game überarbeitet
- Neue, stabilere Scoreboard-Logik
- Konsolen-Logausgabe erweitert und verbessert

### Behoben
- Fehler bei der Erzeugung von Netherportalen behoben
- Verschiedene kleinere Bugs behoben

### Kompatibilität
- Benötigt **Paper 1.21.5**
- Benötigt **Java 21**
- getestet bis 1.21.11
- Nicht kompatibel mit reinem Spigot/Bukkit