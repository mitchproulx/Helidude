# Helidude Refactor Plan

Three reviewers read the whole codebase independently. Each reviewed from a different angle:
- **Architecture:** structure and dependencies.
- **Readability:** clarity and duplication.
- **Extensibility:** how easily new content can be added.

This plan keeps what all three agreed on. Section 7 lists the few points where they differed, with a recommendation for each.

**Goal:** a pure refactor. The game should look and play exactly the same afterwards, but be split into small, clearly named parts that the next person can reuse. The ROADMAP features then have an obvious place to go.

---

## 1. Why refactor: the problems all three found

- **Two classes do almost everything.** `GameRenderer` (about 930 lines) handles the window, the game loop, input, the camera, fog, textures, obstacle generation, all world drawing and scene switching. `Dude` (about 950 lines) is the character model, the ship, the flames, the explosion, movement, collisions, scene transitions and score messages.
- **Everything reaches into everything.** `Dude` reads and writes about 25 public fields on `GameRenderer`, including `currScene`, `fogMode` and `hitWall`.
- **The packages depend on each other in a cycle.** `models` imports `animation`, and `animation` imports `models`.
- **There is heavy copy and paste:**
  - the textured-quad block, 5 times;
  - the cylinder drawing block, about 12 times;
  - the "rainbow colour if boosted" block, 7 times;
  - the 10 flame blocks, about 180 lines.
- **Per-scene `if (currScene == ...)` chains repeat in about 12 methods.**
- **Obstacles are stored as about 15 parallel float arrays**, plus magic texture indices and magic numbers.

---

## 2. Agreed package structure

Everything moves under one root package, `helidude`.

```
src/helidude/
├── Main.java                 window, canvas, listeners, game loop
├── GameConfig.java           all tuning constants (speeds, sizes, timers, corridor length)
│
├── core/                     game flow
│   ├── Game.java             owns everything; update order; scene transitions
│   ├── Scene.java            enum of the 3 scenes, with per-scene settings
│   └── Messages.java         all console text (the HUD replaces it later)
│
├── input/
│   ├── InputHandler.java     keyboard and mouse listeners
│   └── PlayerInput.java      intent flags: left, right, up, down, jump
│
├── player/
│   ├── Player.java           position and state only, with resetRunner() and resetShip()
│   ├── RunnerMotion.java     scene 1 movement: strafe, jump, run speed, limb swing
│   ├── ShipMotion.java       scene 3 movement: strafe, roll, pitch, speed
│   └── TiltAxis.java         one shared helper for roll and pitch, which are identical
│
├── world/
│   ├── World.java            obstacles, orb score, crash flag, corridor progress
│   ├── Obstacle.java         type, x, y, z, hit
│   ├── ObstacleType.java     enum with hitbox sizes: BOOST, TRAP, PILLAR, SHORT_PILLAR, GATE
│   ├── ObstacleLayout.java   builds the obstacle list (today's random layout for now)
│   └── Collisions.java       one overlap test; returns what was hit instead of setting fields
│
├── render/
│   ├── Renderer.java         GLEventListener: init, display, reshape, dispose
│   ├── Camera.java
│   ├── Fog.java
│   ├── Textures.java         loading and anisotropy, with a TextureId enum (BRICK, WORMHOLE, BOOST, SKULL, SPACE)
│   ├── GlDraw.java           texturedQuad(), cylinder(), sphere() using quadrics created once
│   ├── ColorCycle.java       replaces the hand-rolled boostColorPos and flameCount counters
│   ├── Palette.java          RAINBOW and FIRE colour tables
│   ├── CorridorRenderer.java floor, roof, side walls, end wall
│   ├── ObstacleRenderer.java one draw method per obstacle type
│   ├── PropRenderer.java     floating crystals and the parked ship in scene 2
│   ├── DudeModel.java        head, body, arms, hands, legs; tint() helper
│   ├── ShipModel.java        ship body
│   ├── Flames.java           table-driven: 10 offsets and one loop
│   └── Explosion.java
│
└── geometry/
    ├── Mesh.java             was Shape: vertices, faces, outline, draw()
    ├── BoxMesh.java          the cube constructor and setColor()
    ├── Face.java
    ├── ObjLoader.java        was ShapeParser: a static load() that returns a Mesh
    └── Models.java           loads each .obj once and shares it
```

**Dependency rule:** `core` uses the other packages. `render` reads `player` and `world`. `player` and `world` don't know about rendering or `Game`. `geometry` depends on nothing in the game.

---

## 3. Where today's code goes

### GameRenderer.java is split up and then removed

| Today | Moves to |
| --- | --- |
| `main()`, `self()` reflection, `setup()` timer | `Main`, replacing the reflection with `new Renderer(...)` |
| `init`, `display`, `reshape`, `dispose` | `Renderer`, keeping the exact draw order |
| `drawSceneOne/Two/Three`, `currScene` | `Game` and `Scene` |
| Texture loading loop, `maxAnisotropic`, `aniso` toggle (now hidden inside `drawGroundRoof`) | `Textures` |
| `drawFog` and its fields | `Fog` |
| `updateCamera` and the camera and mouse-look fields | `Camera` |
| `keyPressed` and the mouse handlers | `InputHandler`, which writes to `PlayerInput`, `Camera` and `Fog` instead of `Dude` fields |
| `initializeObstacles` and all the `xBoosts`, `zTraps`, `xWalls2` and similar arrays, plus `orbHit` | `ObstacleLayout`, `World`, `Obstacle` |
| `drawGroundRoof`, `drawSideWalls`, `drawEndWall` | `CorridorRenderer` |
| `drawEnvironmentObjects`, `drawSpeedTiles`, `drawSlowTraps`, `drawShortCylinders`, `drawRegularCylinders` | `ObstacleRenderer` |
| `drawGhosts`, `drawShip` (the parked ship) | `PropRenderer` |
| `TRACE`, `TEST`, `MIPMAPPING`, `MAX_DIST`, `numFloorObjs`, the `*Start` thresholds, sizes | `GameConfig` |

### Dude.java is split up and then removed

| Today | Moves to |
| --- | --- |
| `xMove`, `yMove`, `forwardMove`, `turnAngle`, roll, pitch, boost, slow and jump flags, timers | `Player` |
| `turnLeft`, `turnRight`, `speedUp`, `backUp`, `jump` | `PlayerInput` |
| Scene 1 branches of `getMovement` and `adjustSpeed`, plus `jump()` and `updateLimbPosition()` | `RunnerMotion` |
| Scene 3 branches, plus `adjustRoll` and `adjustPitch` | `ShipMotion` and `TiltAxis` |
| `speedBoost()` (collision part), `didHitObstacle`, `didHitWall` | `Collisions` |
| `didReachEnd`, `getDudeStatus` (scene changes and messages) | `Game` and `Messages` |
| `drawDude`, `drawHead`, `drawBody`, `drawArms`, `drawHand`, `drawLegs` | `DudeModel` |
| `drawShip`, `drawFlames`, `drawExplosion` | `ShipModel`, `Flames`, `Explosion` |
| `boostArray`, `flameArray`, `boostColorPos`, `flameCount` | `Palette` and `ColorCycle` |

### Model classes

| Today | Change |
| --- | --- |
| `Shape` | Becomes `Mesh` and `BoxMesh`. Drop the unused `visible` and `angle` fields. Rename `line_colour` to `outlineColour`. |
| `ShapeParser` | Becomes `ObjLoader.load(...)`. Use local variables, close the file with try-with-resources, and keep the dummy vertex 0 because OBJ indices start at 1. This breaks the package cycle. |
| `ComplexShape` | Delete it. Every use is a single mesh drawn at offset (0, 2, 0). Replace it with a mesh plus that offset. |
| `Structure`, `Component` | Delete them. Their lists and `draw()` are unused. `DudeModel` holds named parts (head, arm, chest, leg) instead of `contents.get(0..3)`. |
| Duplicate loading | `Models` loads `dodecahedron.obj` and `shuttle.obj` once. Today each is parsed twice. |

---

## 4. Agreed reusable building blocks

These are what the next person reuses to add things:

| Helper | Replaces | Use it for |
| --- | --- | --- |
| `GlDraw.texturedQuad(gl, texture, x0, y0, x1, y1, z, repeat)` | 5 copied `glBegin(QUADS)` blocks | Any floor tile, wall, pad or decal |
| `GlDraw.cylinder(gl, radius, length)` and `GlDraw.sphere(gl, r)` | About 12 cylinder and 3 sphere copies, plus the delete-then-reuse quadric bug | Pillars, gates, orbs |
| `ObstacleType` and `Obstacle` | Parallel arrays and loose hitbox numbers | A new obstacle: add an enum value, a draw method and a hit effect |
| `Collisions.overlaps(...)` | 8 nested if-blocks | Any new hit test |
| `ColorCycle` and `Palette` | Hand-written modulo counters | Rainbow and fire effects |
| `DudeModel.tint(defaultRgb)` | 7 copies of the boost-colour block | Recolouring any body part |
| `Flames` offset table | 10 copied flame blocks, about 180 lines down to about 25 | Changing the ship exhaust |
| `Scene` enum settings | `if (currScene == ...)` chains | Floor and wall textures, texture repeat, tile count, speeds, strafe step per scene |
| `Messages` | `System.out` scattered everywhere | Future HUD text and popups |
| `Player.resetRunner()` and `resetShip()` | Nothing yet | ROADMAP phases 3 and 4 |

Keep it simple: no entity-component system, scene graph or plugin registry. Enums and a few small classes are enough for a game of about 2,000 lines.

---

## 5. Agreed naming, comment and dead-code cleanup

**Renames.**

| Today | New name | Why |
| --- | --- | --- |
| `forwardMove`, `xMove`, `yMove` | `z`, `x`, `y` | They are positions, not moves |
| `changeView` | `firstPerson` | `true` means first person |
| `xCubes`, `zCubes` | short pillars | They draw cylinders |
| `xWalls`, `wallWidth`, `wallHeight` | pillars, `pillarRadius` | They are cylinders and radii |
| `diamond`, `ghost` | `crystal` | Both are the same dodecahedron |
| `zBoosts2` and similar | gates | They are scene 3 warp gates |
| `sc3_texture`, `textures[0..5]` | `TextureId.SPACE` and the other names | No magic indices |
| `exp_size` | `explosionScale` | Clearer |
| `scale` ("brick density") | `textureRepeat` | That's what it is |
| Local `shipDist = 3` and `2` | `brickTilesBeforeSpace` | Its name shadows the unrelated field `shipDist = -45` |
| `GameRenderer.drawShip` and `Dude.drawShip` | `drawParkedShip` and `drawPlayerShip` | Same name, different things |
| `fogMode` 1 and 2 | a small enum | Clearer than numbers |

**Wrong comments and text to fix:**
- The Space key prints "Enter: JUMP!" and the Enter key prints "Space bar: Perspective changed". The labels are swapped.
- `i % 1 == 0 // each odd number` is always true.
- "brick density" sits on space textures.
- "bottom right" labels the top-left tile.
- "connect the helicopter blade" is a stray comment in `drawExplosion`.
- "(TEST ONLY)" sits on code that also handles the scene 2 walk.
- Typos: "FLEW THREW", "HELECOPTER", "CLYINDERS".

**Magic numbers** go to `GameConfig` with names: speeds, strafe limits, jump heights, boost and slow lifetimes, roll and pitch speed and limit, hitbox sizes, camera constants.

**Dead code to delete:**
- **Unused fields:** `t`, `reverse`, `INTERVAL`, `dReverse`, `cMove`, `direction`, `currDirection`, `numFeet`, `dudeShapes`, the duplicate `INPUT_PATH_NAME`.
- **Unused imports:** `ArrayList` in ShapeParser; `models.Dude` and `models.Shape` inside Dude.
- **The always-true check** `maxAnisotropic > 0`.
- **Unused assets:** `space3.jpg` is loaded but never used. `slots.obj` is never loaded. Keep both files, but stop loading `space3.jpg`.

---

## 6. Behaviour traps: keep these working during the refactor

All three reviewers flagged these. A pure refactor must keep them as they are. Fixing any of them is a separate, deliberate change, and some are already in the ROADMAP.

| Quirk | Why it matters during the refactor |
| --- | --- |
| **Fog is used as an event signal.** The "hit a trap" and "grabbed an orb" messages, and the orb penalty, fire on fog-mode changes, not on actual hits. Pressing M or a boost ending can trigger them. | It depends on the exact order in `display()`: fog clears the flag, then the dude sets it, then the status reads it. Keep that order until `Collisions` returns real events. |
| **The camera updates after the scene is drawn.** Each frame uses the previous frame's camera. | Keep the order, or the first frame looks different. |
| **Scene 3 has 70 pillar hitboxes but draws only 35**, with stacking offsets. There are invisible pillars, and the drawn pillars don't line up with the hitboxes. | `ObstacleRenderer` must reproduce the stacking exactly, or the visual change must be accepted. |
| **Gate height is clamped while drawing**, and only for gates that are drawn. | Move the clamp into layout generation. Gameplay stays the same because collision already reads the clamped values. |
| **`Shape.setColor` only removes half the old faces**, so each box ends up with about 11 faces. | This is invisible, because the newest faces draw last. Fixing it is safe and saves work. Recommend fixing it. |
| **`drawSideWalls` sets `minX` and `minY` to 0** for the rest of the game. | One reviewer found that only the very first frame uses the original values, and that frame is drawn before the camera is set. So the refactor can safely hard-code 0. This updates ROADMAP 0.5. |
| **`drawShortCylinders` translates twice**, drawing two stacked pillars. | Already in ROADMAP phase 5. |
| **The score ladder has `> 16` twice**, so "GREAT" is unreachable. | Fix it in `Messages`, as a tiny, visible improvement. |
| **When slow mode ends, `boostLife = 50` is set** where `slowTime` was probably meant. | Flag it; fix it in roadmap phase 2, where traps are reworked. |
| **One Up press in scene 2 walks the dude all the way to the ship**, then pitches the ship up once. | Keep it, or decide on it during roadmap work. |
| **The timer thread changes state while the GL thread draws.** | Fixed by ROADMAP 0.1 (FPSAnimator), the first job after the refactor is merged. |

---

## 7. Decisions (confirmed)

The reviewers differed on these points. Each has now been decided.

| Topic | Decision |
| --- | --- |
| **Scene names** | `CORRIDOR`, `HANGAR`, `FLIGHT`, replacing `ONE`, `TWO`, `THREE`. |
| **Per-scene behaviour** | Settings live on the existing `Scene` enum. Movement is split into `RunnerMotion` and `ShipMotion`. No scene classes or vehicle hierarchy. |
| **Obstacle rendering** | One `ObstacleRenderer` class with one draw method per obstacle type. |
| **Render package** | Flat `render/` for now. Add `render/hud/` when ROADMAP phase 1 starts. |
| **Geometry package name** | `geometry`. |
| **Collision events** | `Collisions` returns a small result describing what was hit. `Game` decides what happens. |

---

## 8. Implementation plan: 10 phases, one commit each

### How to work through the phases
- Work on a branch called `refactor`, made from `vscode-port`. Merge it when all 10 phases are done.
- Each phase is one commit, using the commit message given. Every phase leaves the game building and playable.
- Phases build on each other, so do them in order.
- ROADMAP 0.1 (FPSAnimator) is not part of this refactor. It comes first in the ROADMAP once the refactor is merged.

### Checks after every phase
1. VS Code shows no errors in the Problems panel.
2. It builds from a terminal:
   ```
   javac -d out -cp "lib/*" (all files under src)
   ```
3. Play one full game:
   - **Corridor:** collect an orb, run over a trap, crash into a pillar, then reach the end.
   - **Hangar:** walk to the ship.
   - **Flight:** fly through a gate, then crash.
4. The console messages and visuals match the previous phase. The only intended differences are listed in that phase.

---

### Phase 1: Move into the `helidude` package
Move the existing files into their final packages, without changing any code inside them yet.

| File | New location |
| --- | --- |
| `GameRenderer` | `helidude/core/` |
| `Dude` | `helidude/player/` |
| `Shape`, `Face`, `ShapeParser`, `ComplexShape`, `Structure`, `Component` | `helidude/geometry/` |

- Use `git mv` so each file keeps its history.
- Update the `package` and `import` lines.
- Update `mainClass` in `.vscode/launch.json` to `helidude.core.GameRenderer`, and update the README commands.

**Commit:** `Move sources under the helidude root package`

### Phase 2: Cleanup and renames
Make the code easier to read, with no structural change.
- Delete the dead fields, imports and checks listed in section 5.
- Fix the wrong comments, the swapped Space and Enter messages, and the typos.
- Rename the scenes to `CORRIDOR`, `HANGAR` and `FLIGHT`.
- Apply the field and method renames from section 5.
- Stop loading the unused `space3.jpg`.

**Intended differences:**
- The corrected key messages.
- The "GREAT" score rating can now appear, after fixing the duplicate `> 16` check.

**Commit:** `Clean up dead code, comments and names`

### Phase 3: Constants and textures
Give every magic number a name.
- Add `helidude/GameConfig.java` with all tuning constants: speeds, sizes, timers, corridor length, hitbox sizes and camera values.
- Add `render/Textures.java` and the `TextureId` enum: `BRICK`, `WORMHOLE`, `BOOST`, `SKULL`, `SPACE`.
- Move the texture loading and the anisotropy toggle out of `drawGroundRoof` into `Textures`.
- Replace `textures[0..5]` and `sc3_texture` with `TextureId` values.

**Commit:** `Add GameConfig constants and Textures with TextureId`

### Phase 4: Geometry
Clean up the 3D model classes.
- Turn `Shape` into `Mesh` and `BoxMesh`, and drop the unused fields.
- Fix `setColor` so it removes all the old faces. This is invisible, but stops each box growing to about 11 faces.
- Turn `ShapeParser` into a static `ObjLoader.load()` with try-with-resources.
- Add `Models`, so each `.obj` file loads once.
- Delete `ComplexShape`, `Structure` and `Component`. Replace them with a mesh plus offset, and named body parts.

**Commit:** `Replace Shape classes with Mesh, BoxMesh, ObjLoader and Models`

### Phase 5: Drawing helpers
Remove the copied OpenGL blocks.
- Add `render/GlDraw.java` with `texturedQuad()`, `cylinder()` and `sphere()`. The quadrics are created once and never deleted mid-game.
- Swap all 5 textured-quad copies, about 12 cylinder copies and 3 sphere copies to use these helpers.
- Keep every argument value identical.

**Commit:** `Add GlDraw helpers for quads, cylinders and spheres`

### Phase 6: Colour and ship effects
Remove the copied colour and flame blocks.
- Add `render/Palette.java` with the `RAINBOW` and `FIRE` colour tables.
- Add `render/ColorCycle.java`, replacing the `boostColorPos` and `flameCount` counters. Keep the crystal rotation reading the same rainbow counter.
- Add a `tint()` helper for the 7 "rainbow if boosted" blocks.
- Add `render/Flames.java`: an offset table and one loop, in the same draw order so the colours cycle the same way.

**Commit:** `Add Palette, ColorCycle and table-driven Flames`

### Phase 7: Window, camera, fog and input
Take the application shell out of `GameRenderer`.
- Add `helidude/Main.java`: the window, canvas and timer. Replace the reflection with a direct `new Renderer(...)`.
- Add `render/Renderer.java`: `init`, `display`, `reshape`, `dispose`. Keep the draw order exactly, with the camera last.
- Add `render/Camera.java` and `render/Fog.java`, with a small fog-mode enum.
- Add `input/InputHandler.java` and `input/PlayerInput.java`. Keys set intent flags instead of `Dude` fields.
- Update `mainClass` to `helidude.Main`.

**Commit:** `Extract Main, Renderer, Camera, Fog and InputHandler`

### Phase 8: Player split
Split `Dude` into game logic and drawing.
- Add `player/Player.java`: position and state, plus empty `resetRunner()` and `resetShip()` hooks for the ROADMAP.
- Add `player/RunnerMotion.java`, `player/ShipMotion.java` and `player/TiltAxis.java`, so roll and pitch share one class.
- Add `render/DudeModel.java`, `render/ShipModel.java` and `render/Explosion.java`.
- Keep the order of today's `run()` calls exactly. The fog-based messages depend on it.
- `Dude.java` is deleted at the end of this phase. Collision and scene-change code moves temporarily into `Game` and goes to its final home in phases 9 and 10.

**Commit:** `Split Dude into Player, motion classes and models`

### Phase 9: World, obstacles and collisions
Replace the parallel arrays.
- Add `world/Obstacle.java`, `world/ObstacleType.java` (with hitbox sizes) and `world/World.java`.
- Add `world/ObstacleLayout.java`, which reproduces today's random placement exactly. This includes the scene 3 quirk of 70 hitboxes and 35 drawn pillars. Use a seedable `Random`.
- Move the gate-height clamp from drawing into layout.
- Add `world/Collisions.java` with one `overlaps()` test. It returns a result describing what was hit.
- Add `render/ObstacleRenderer.java` (one method per type), `render/CorridorRenderer.java` and `render/PropRenderer.java`.

**Check:** layouts with the same seed match before and after.

**Commit:** `Add World, Obstacle model, layout, collisions and renderers`

### Phase 10: Game flow and wrap-up
Finish the structure and update the docs.
- Add `core/Game.java`: the update order, scene transitions, acting on collision results and the orb score.
- Move the per-scene settings onto the `Scene` enum: textures, texture repeat, tile count, speeds and strafe step. Remove the `if (currScene == ...)` chains.
- Add `core/Messages.java` for all console text.
- Make fields private and add methods.
- Delete what remains of `GameRenderer`.
- Update `ROADMAP.md` to the new class names, such as `render/hud/Hud` and `world/Obstacle`, and the corrected section 0.5. Update the README project layout.
- Do a final read-through. Check that no class reaches into another's fields, and that `geometry` depends on nothing else.

**Commit:** `Add Game and Messages, move scene settings to Scene, update docs`

---

### After the refactor
Merge `refactor`, then start ROADMAP phase 0 with FPSAnimator. The new features then have their places ready:
- **HUD:** `render/hud/`.
- **Health:** `Player`.
- **Death animation:** `DudeModel`.
- **Retries and credits:** `Game`.
- **Obstacle layout:** `ObstacleLayout`.
