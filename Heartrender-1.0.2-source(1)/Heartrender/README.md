# Heartrender

A mythic lifesteal sword plugin for Spigot / Paper (1.21+), built for lifesteal SMPs.

## The sword
**Heartrender** - a mythic netherite sword with Sharpness V, Fire Aspect II, Looting IV,
Sweeping Edge III, Unbreaking V and Mending.

- **Heart Siphon (passive):** 15% chance on hit to drain the target: bonus damage, a short
  Wither, and you heal 2 hearts (never above your max health).
- **Blood Rush (right-click):** crimson dash forward with no fall damage afterwards. 15s cooldown.

## Recipe (hard, but doable)
```
 Netherite Ingot | Echo Shard       | Netherite Ingot
 Diamond Block   | Nether Star      | Diamond Block
 (empty)         | Netherite Sword  | (empty)
```
Needs a Wither kill, Ancient City loot, 18 diamonds, 2 netherite ingots and a netherite sword.

## Commands (`/heartrender`, aliases `/hr` and `/heart`)
| Command | Permission | What it does |
|---|---|---|
| `/hr recipe` | revealed players / `heartrender.reveal` | Opens the recipe GUI |
| `/hr reveal <player\|all>` | `heartrender.reveal` | Shows the recipe GUI to someone, and unlocks it in their recipe book |
| `/hr hide <player\|all>` | `heartrender.reveal` | Takes the recipe back |
| `/hr give [player]` | `heartrender.admin` | Gives the sword |
| `/hr info` | `heartrender.admin` | Shows how many have been forged |
| `/hr resetcount` | `heartrender.admin` | Resets the craft counter |
| `/hr reload` | `heartrender.admin` | Reloads config.yml |

## SMP options (config.yml)
- `max-crafts: 1` - only one Heartrender can ever be forged (0 = unlimited)
- `require-reveal-to-craft: false` - set to `true` so only players you've revealed the recipe to can craft it
- `dash-cooldown-seconds`, `heart-siphon-chance`, `heart-siphon-bonus-damage`, `heart-siphon-heal` - balance the abilities

## Getting the .jar (IMPORTANT)
This zip is SOURCE CODE. Servers cannot load it. You must build it into a .jar first:

**Option A - no install (GitHub):** make a free GitHub repo, upload these files, open the
Actions tab -> "Build Heartrender" -> run it -> download the `Heartrender-jar` artifact.

**Option B - locally:** install JDK 21 + Maven, then run `build.bat` (Windows) or `./build.sh`.
The jar appears at `target/Heartrender-1.0.2.jar`.

Put ONLY the .jar in your server's `plugins/` folder and restart (not /reload).
Targets Spigot/Paper 1.21.11 (Java 21).
