# PotionSMP

A Paper 1.21.1 Minecraft plugin that adds a slot-based custom potion ability system with 11 unique potions.

## Stack
- **Language:** Java 21
- **Build tool:** Maven 3.x
- **Target:** Paper API 1.21.1-R0.1-SNAPSHOT

## How to build

Java 21 must be on PATH. The installed GraalVM is Java 19 and won't work — use the JDK 21 from the Nix store:

```bash
export JAVA_HOME=/nix/store/3ilfkn8kxd9f6g5hgr0wpbnhghs4mq2m-openjdk-21.0.7+6
export PATH=$JAVA_HOME/bin:$PATH
cd PotionSMP && mvn package
```

The compiled jar is output to `PotionSMP/target/PotionSMP.jar` (also copied to the workspace root as `PotionSMP.jar`).

## Deploying

Drop `PotionSMP.jar` into your Paper server's `plugins/` folder and restart.

## Potions

| Potion    | Passive                       | Active                  |
|-----------|-------------------------------|-------------------------|
| Fire      | Ignite nearby on 10th hit     | Inferno Rage aura       |
| Freeze    | Stun target on 10th hit       | Arctic Prison (area)    |
| Regen     | Permanent Regen I             | Vitality Surge + dive   |
| Glitch    | Mining Fatigue I              | System Corruption       |
| Shield    | —                             | Divine Barrier          |
| Ender     | Blind target on 10th hit      | Void Domain             |
| Teleport  | —                             | Phase Shift (20 blocks) |
| Speed     | Stacking speed on hits        | Lightning Dash          |
| Feather   | —                             | Feather abilities       |
| Emerald   | —                             | Emerald abilities       |
| Strength  | —                             | Strength abilities      |

## Commands

| Command     | Description                          |
|-------------|--------------------------------------|
| `/potionsmp` | Open the potion GUI                 |
| `/slot1`    | Activate Slot 1 ability              |
| `/slot2`    | Activate Slot 2 ability              |
| `/swap`     | Swap potions between Slot 1 & Slot 2 |

## User preferences
