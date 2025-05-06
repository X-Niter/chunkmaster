# ChunkMaster

**High-performance chunk scheduling, ticking, and worldstream optimization for modern Minecraft (NeoForge 1.21.x)**

> ⚙️ ChunkMaster is a mod that reimagines chunk ticking with parallelism, smarter scheduling, and bleeding-edge Java 21 tech like virtual threads—all in the name of performance and control.

---

![ChunkMasterLogo](https://github.com/user-attachments/assets/7ebd46da-499c-4755-a526-66d4c5b544e3)

---

## 🚀 Vision

ChunkMaster aims to reinvent how chunks are loaded and ticked by:
- Making chunk ticks **parallel**, **prioritized**, and **non-blocking**.
- Leveraging **Java 21**’s **virtual threads** to eliminate server stalls.
- Designing a **modular ECS-based tick architecture** for chunk-local systems like fluid, block, and entity simulation.
- Supporting **level-of-detail chunks**, **distance-based tick throttling**, and **sliced updates**.

---

## 🧠 Features (Now & Planned)

### ✅ Currently Implemented
- [x] Mixin-based injection into `ChunkMap` for intercepting chunk ticks
- [x] Internal scheduler system for tracking loaded chunks
- [x] Per-chunk tick proxy with support for fluid tick iteration
- [x] Basic server tick hook for driving custom logic
- [x] Logging for chunk tick operations (debugging/tracing)

### 🔜 In Progress
- [ ] Safe concurrent block & fluid ticking via copy-on-write structures
- [ ] Full ECS-style chunk tick manager (`TickRegion`, `TickChunk`, `TickEntity`)
- [ ] Smarter block tick scheduling with priority queues
- [ ] Per-chunk tick cost tracking (profiling + stats system)
- [ ] Chunk-level simulation budget enforcement
- [ ] Configurable tick frequency by dimension, biome, or chunk group

### 📅 Long-Term Goals
- [ ] Virtual-thread backed region loader (Loom)
- [ ] GPU-accelerated mesh generation (via Project Panama / JNI + Vulkan)
- [ ] Support for LOD chunks (e.g. "scenic" chunks with no block states)
- [ ] Mod integration API for scheduling chunk-local logic
- [ ] Optional compatibility with Lithium, Phosphor, and other tick mods

---

## 🏗️ Architecture

### 🔄 Chunk Tick Lifecycle

```
World Tick Start
 └─> ChunkMasterScheduler.tickAll()
       ├─> Iterate tick regions
       │     └─> Iterate tick chunks
       │          ├─> Tick block states (optional)
       │          ├─> Tick fluids
       │          └─> Tick block + fluid scheduled ticks
       └─> Post profiling/logging
```

### 🧬 Key Systems
| System                   | Purpose                                                       |
|--------------------------|---------------------------------------------------------------|
| `ChunkMasterScheduler`   | Central tick loop manager for all tracked chunks              |
| `TickChunkProxy`         | Custom tick logic per chunk, overrides vanilla ticking        |
| `ChunkMapMixin`          | Entry point for intercepting vanilla chunk load/tick events   |
| `TickContext` (Planned)  | Lightweight profiling and dependency manager for ECS updates  |

---

## 💻 Setup Instructions (Dev)

### Prerequisites
- Minecraft 1.21.x
- [NeoForge](https://neoforged.net/)
- Java 21 (Project Loom enabled)
- IntelliJ IDEA (recommended)
- Gradle 8.6+

### Steps

1. **Clone the repo**:
   ```bash
   git clone https://github.com/yourname/chunkmaster.git
   cd chunkmaster
   ```

2. **Import the project into IntelliJ** using the `build.gradle` file.

3. **Ensure Mixin is configured**:
   - `META-INF/chunkmaster.mixins.json` exists
   - Gradle has `mixin` plugin properly set up

4. **Run Client**:
   ```bash
   ./gradlew runClient
   ```

---

## 🧪 Testing Checklist

- [ ] Chunk tick logs appear correctly on server tick
- [ ] Mixins apply successfully without crash
- [ ] Player movement triggers expected chunk tick zones
- [ ] Configurable debug output can be toggled on/off
- [ ] Crash handling in block/fluid tick methods
- [ ] No memory leaks from tick queue retention

---

## 📚 Technical Deep-Dive

### 🔧 Threading Strategy
- **Fluid + block tick iteration** can be safely parallelized using thread-safe queues and iteration copies.
- **Virtual Threads** let us spawn thousands of lightweight runners for chunk I/O, prep, or simulation warm-up.

### 🧱 Chunk Data Design
- Immutable snapshot interfaces for ticking
- Copy-on-write mutation wrappers for safety
- ECS-like grouping per tick phase

### 🧠 Why It Matters
> Minecraft's tick loop is single-threaded, uniform, and linear. That’s legacy design. Modern CPUs love **data-oriented**, **vectorized**, and **asynchronous** work. ChunkMaster aims to be that shift.

---

## 🛣️ Roadmap

### ⏱️ Q2 2025
- [ ] Full ECS-style `TickRegion` system
- [ ] Parallel block and fluid ticking (read-only)
- [ ] Tick frequency profiles (low priority chunks, low freq tick)

### 🧪 Q3 2025
- [ ] Experimental LOD chunks with heightmap-only logic
- [ ] Virtual-thread chunk warmup loading
- [ ] Player proximity-based tick scheduler

### ⚙️ Q4 2025
- [ ] Full ECS migration of entities, block entities
- [ ] Compatibility layer for ticking APIs (e.g., Lithium)
- [ ] GPU mesh offloading demo (early alpha)

---

## 📖 Credits

- **Inspired by**: C2ME, Lithium, Phosphor, and the Fabric performance stack
- **Built with**: ❤️, Java 21, NeoForge, and late-night obsession
- **Maintained by**: [@xniter](https://github.com/xniter)

---

## 📄 License

MIT License. Do what you want, just don’t be a jerk.

---

> "Tick smarter, not harder." — The ChunkMaster Manifesto™
