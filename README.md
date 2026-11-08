# Simple Iron Pipes
An **independent**, original item transport mod for **Minecraft 1.20.1 Forge**.

## Blocks
- **Iron Pipe:** six-direction connections, compact iron appearance, item-by-item inventory transport.
- **Iron Routing Junction:** configurable per-face item routes, prioritizing matching destinations.

## Configure junction filters
1. Place a routing junction and connect several pipes.
2. **Sneak + right-click a junction face with an item** to assign that item to that exit. The item is not consumed.
3. **Sneak + right-click a face with an empty hand** to clear its filter.
4. **Right-click empty-handed** to read active filters in chat.
5. Routes with matching filters take precedence; unfiltered exits handle unassigned items.

This first build implements a single item per pipe at a time and a simple round-robin transport algorithm. Advanced inventory compatibility, loops, buffering, network scalability, filter UI, sounds and dedicated-server gameplay tests remain to be refined.

## Build
Requires Java 17, Gradle 8.8 and Forge 47.4.16.
```sh
gradle build
```
The compiled mod is in `build/libs`. GitHub Actions uploads a build on each commit.

## Original implementation
No source code, textures, models, or sound assets from Simple Copper Pipes are included. The models are purpose-built and use Minecraft's built-in iron material textures. This project is not a port of Simple Copper Pipes.
