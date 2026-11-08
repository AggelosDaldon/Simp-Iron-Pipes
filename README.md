# Simple Iron Pipes

**Original iron pipe logistics mod for Minecraft 1.20.1, Forge 47.4.x and Java 17.**

This repository contains an independent implementation with new block models,
textures, item transport logic and a configurable routing interface. It is not
a port of Simple Copper Pipes, and does not include its original source or assets.

## Blocks

| Block | Purpose |
| --- | --- |
| **Iron Pipe** | Connects neighboring pipes and carries one item at a time. |
| **Iron Extractor** | Pulls items from an attached inventory and feeds the pipe network. |
| **Iron Routing Junction** | Selects an output direction based on item filters. |
| **Iron Outlet** | Dispenses items into the world when there is no inventory endpoint. |

All blocks connect in six directions. Copper, wax and oxidation are not part
of the mod. Iron textures and steel accents are original pixel-art resources.

## How to build a network

1. Put a chest or other inventory in the world.
2. Place an **Iron Extractor** against its face. The face clicked determines
   which side the extractor pulls from (it pulls through the side touching the
   block you placed it against).
3. Connect an **Iron Pipe** chain to the extractor and a destination chest,
   or attach an **Iron Outlet** to eject items. An outlet ejects in the
   direction of the face against which it was placed.
4. Add an **Iron Routing Junction** where the network splits.

### Routing filters

Right-click the **Iron Routing Junction** to open the 6-output GUI.

- Pick an item up with the mouse from your inventory, then **left-click**
  the desired direction's slot to copy it as a ghost filter.
- **Right-click** a filter slot to clear it.
- You keep the item: filters do not consume items.
- Matching items prefer their explicitly assigned exits.
- Unassigned items can use any otherwise unfiltered exit.
- Filter choices are saved with the block entity and restored after reload.

This initial implementation supports one exact-item filter per output
direction, including item NBT. A pipe only forwards when a reachable
inventory or outlet can accept its item. The graph search is bounded to 128
pipe nodes per attempt to control server cost.

### Redstone and inventory compatibility

- Applying a redstone signal directly to a pipe, extractor, junction, or
  outlet pauses that block's transport.
- Uses the Forge item handler capability to interact with supported chests,
  hoppers, barrels, modded inventories and other item handlers.
- Insertion into an ordinary pipe from compatible item handlers is supported.
- A pipe has an internal capacity of one item.

## Recipes

- 8 Iron Pipes: six iron ingots in two horizontal rows.
- Routing Junction: four iron ingots surrounding an iron pipe.
- Extractor: hopper and iron pipe combined with iron ingots.
- Outlet: dropper and iron pipe combined with iron ingots.

## Building

```bash
gradle build
```

Requires JDK 17 and Gradle 8.8. GitHub Actions compiles and publishes
a ZIP artifact containing the mod JAR. Install the same JAR on client and server.

## Status

**Experimental gameplay build:** Builds successfully in GitHub Actions.
In-world behavior and dedicated server compatibility should be tested on
a fresh world before a public stable release. GUI, routing, ejection,
saving and modded inventory compatibility require gameplay verification.

The project deliberately does not claim that it is a copy of, or a Forge
port of, another Minecraft mod.
