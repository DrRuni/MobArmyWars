# 🛡️ MobArmyWars – PvE Battle Plugin

**MobArmyWars** is a server-side PvE battle plugin for **Paper servers**, inspired by the popular **MobArmyBattle** concept by BastiGHG.

Two teams compete against each other: **Red** and **Blue**.  
During the preparation phase, teams collect equipment, resources, drops and mobs. Collected mobs can then be configured into up to **three attack waves**.

After the preparation phase, the mob waves are sent into an automatic arena battle.  
The team that defeats the opposing mob army first wins.

---

## ⭐ Features

- Two teams: **Red** and **Blue**
- Separate lobby, team, Nether and arena worlds
- Preparation phase for collecting resources, drops and mobs
- 3 configurable attack waves per team
- Automatic arena battle system
- Shared team backpacks
- Optional block randomizer
- GUI-based settings and wave configuration
- Reset, resume and world regeneration systems
- Configurable world settings, including difficulty
- Team equipment selection
- Team scoreboard with offline players
- Optional arena monster compass
- German in-game messages and GUIs

---

## ⚙️ Compatibility

- Requires **Paper 26.1.2**
- Requires **Java 25**
- Paper only
- Not compatible with pure Spigot/Bukkit
- In-game language: **German**

---

## ⌨️ Commands

### Player commands

```text
/team join rot
/team join blau
/team leave
/mobstatus
/arenasummary
```

### Admin / OP commands

```text
/optionen
/mobarmy resume
/resume mobarmy
/setphase <lobby|teamwelt|waveauswahl|arena>
/reset <arena|lobby|teamworld|playerdata>
```

---

## 📥 Installation

1. Put the `.jar` file into your server's `/plugins` folder.
2. Start or restart the server.
3. Worlds and configuration files are generated automatically.

---

## 💬 Notes

This is my first larger plugin project.  
Feedback, bug reports and suggestions are always welcome.