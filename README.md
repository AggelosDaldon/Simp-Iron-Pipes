# Simple Iron Pipes (Forge 1.20.1)

Two blocks: **Iron Pipe** and **Iron Fitting**.

- Right-click either block to open the vanilla five-slot hopper inventory GUI.
- Place a pipe facing the intended output. It draws items from behind, pushes them forward, or ejects them into air.
- Connect multiple directional pipes through a fitting. The fitting accepts and distributes items according to neighboring pipe direction.
- Redstone disables intake on pipes and output on fittings.
- Each transfer plays the registered pipe item sounds.
- Original iron textures supplied by the project owner. Models and transport sounds are adaptations under the MIT license, with details in THIRD_PARTY_NOTICES.md.
- The **Iron Pipes** creative tab contains only these two blocks.

Java 17; Forge Minecraft 1.20.1. GitHub Actions produces experimental JAR builds. Backup worlds before testing: old versions' removed blocks do not migrate automatically.

## 0.2.2 — Item ejection stability
- Spawn outgoing items clear of the pipe nozzle to prevent edge collision jitter.
- Reduced and standardized forward velocity, with a small horizontal-launch lift.
- Preserve normal water physics and gravity; allow output into flowing water.
- Apply a brief pickup delay and retain item if world entity spawn is rejected.

Original pipe model and audio provenance is disclosed in THIRD_PARTY_NOTICES.md as required by the upstream MIT license.
