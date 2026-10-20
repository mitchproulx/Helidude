# Helidude Roadmap

The planned improvements, in the order they should be built.
Each phase leaves the game runnable, so it can be committed and played before moving on.

Phases 0 and 1 are groundwork. Phases 2 to 5 are the new gameplay features and depend on phase 1.

---

## Phase 0: Cleanup

Small fixes that make the later work easier. No visible gameplay change.

### 0.1 Replace the timer loop with JOGL's FPSAnimator
- **Now:** a `java.util.Timer` in `GameRenderer.setup()` advances `t` and `dPos`, then calls `canvas.repaint()` 60 times a second from a background thread.
- **Change:** use `com.jogamp.opengl.util.FPSAnimator(canvas, 60)` started in `main`. Move the `t` and `dPos` updates into a `tick()` method called at the top of `display()`.
- **Why:** frames are driven by the GL thread, so state updates and drawing no longer race. Frame pacing is smoother too.

### 0.2 Stop scene two from printing every frame
- `drawSceneTwo()` prints "PRESS THE UP ARROW KEY..." on every frame.
- Print it once, when the scene changes to TWO. Phase 1 replaces it with an on-screen hint anyway.

### 0.3 Create GLU quadrics once
- `orb` and `cylinder` are created in `init()`, but `gluDeleteQuadric()` is called after every draw and the same objects are reused. `Dude.drawHead()` and `Dude.drawExplosion()` create and delete a new sphere each frame.
- Create each quadric once in `init()` or the `Dude` constructor. Delete them only in `dispose()`.

### 0.4 Load resources from the classpath
- Textures use `new File("resources/...")` and `ShapeParser` uses `FileReader`, so the game only works when started from the project root.
- Add `resources` as a classpath folder. Load with `getClass().getResourceAsStream("/brick.jpg")`. Change `ShapeParser` to read from an `InputStream`.
- The launch configuration then no longer needs `cwd` to be set, and a runnable jar becomes possible later.

### 0.5 Known quirks to keep in mind
These don't need fixing now, but they will matter during the reset work in phase 1.
- `drawSideWalls()` sets `minX` and `minY` to 0. The ground and end wall then draw with those changed values on every later frame. Keep this behaviour, or the ground layout shifts.
- `didReachEnd()` resets the corridor instantly when a cylinder is hit. Phase 3 replaces this.
- `game.distance += 10` runs when `forwardMove % 5 == 0`. This is a float comparison, so it almost never fires. It should be removed or reworked during the reset work.
- In scene three, reaching the end also triggers the explosion. Phase 4 separates winning from crashing.

---

## Phase 1: Game state, reset and HUD foundation

All the new features need three things the game doesn't have yet: a clear game state, a way to reset parts of the game, and on-screen text and buttons.

### 1.1 Game state machine
Add a `GameState` enum next to `Scene` in `GameRenderer`:

| State | Meaning |
| --- | --- |
| `PLAYING` | Normal gameplay. |
| `DYING` | The runner's death animation is playing. Input is ignored. |
| `RUNNER_DEAD` | The runner retry popup is showing. |
| `SHIP_EXPLODING` | The ship explosion is playing. |
| `SHIP_ENDED` | The ship end popup is showing, after a crash or a win. |

`Dude.run()` only moves and checks collisions while the state is `PLAYING`.

### 1.2 Reset methods
Game state is currently spread across `GameRenderer` and `Dude` fields. Add three explicit resets:
- **`resetRunner()`** puts the dude at the corridor start, restores health, clears the slow and boost flags and `orbHit[]`, and rebuilds the obstacle layout.
- **`resetShip()`** puts the ship at the start of scene three. It clears `xMove`, `yMove`, `forwardMove`, roll, pitch, `explosion`, `exp_size` and `hitWall`.
- **`newGame()`** resets everything, including retry attempts and ship credits, and returns to scene one.

Each one splits into a `GameRenderer` part for the world and a `Dude.reset...()` part for the character.

### 1.3 HUD and overlay rendering
Add a new class `animation/Hud.java` that draws on top of the 3D scene at the end of `display()`:
- Switch to an orthographic projection in pixels. Disable depth test, fog and textures, then restore them afterwards.
- Draw text with `com.jogamp.opengl.util.awt.TextRenderer`, which is included in `jogl-all`.
- Provide helpers: `drawBar(x, y, w, h, fraction, colour)`, `drawPanel(...)`, `drawButton(label, rect, hovered)` and `drawDim(alpha)`.
- **Popup focus:** every popup first calls `drawDim(0.6f)`, a full-screen black quad at 60% opacity with blending on. Then it draws the panel and buttons at full brightness on top. The 3D scene keeps rendering underneath, frozen, so the dimmed game stays visible behind the popup.
- Fade the dim in over about 0.3 seconds when a popup opens, so it doesn't snap on.

### 1.4 Clickable buttons
- Each popup keeps a list of buttons with their screen rectangles.
- `mouseClicked` and `mouseMoved` hit-test those rectangles while a popup is showing. Flip Y, because OpenGL's origin is bottom-left.
- Use `drawable.getSurfaceWidth()` against `canvas.getWidth()` to scale mouse coordinates. Windows display scaling can otherwise make clicks miss.
- Every button also gets a keyboard shortcut, such as Enter for the main action and N for a new game.

**Check:** a test popup opens over a dimmed game and closes with both a click and its key, at 100% and 150% Windows display scaling.

---

## Phase 2: Runner health bar

### Behaviour
- The runner starts each attempt with **3 health**.
- Each slow trap he runs over costs **1 health**. The slow-down effect still applies.
- At 0 health he dies, which goes to phase 3.
- A health bar shows at the top-left in scene one only, as three segments that turn from green to red.

### Implementation
- Add `health` and `MAX_HEALTH = 3` to `Dude`.
- **Count each trap once.** `didHitObstacle()` currently sets `slow = true` on every frame the dude overlaps a trap, which would drain all 3 health on a single trap. Add a `trapHit[]` array, or an `Obstacle.hit` flag after phase 5, so a trap only costs health the first time.
- Briefly flash the dude red, about 0.5 seconds, when health is lost. The `setColor` calls in `drawHead()`, `drawBody()` and the other draw methods already support this.
- Draw the bar from `Hud`.

**Check:** health drops by exactly one per trap, and the third trap triggers death.

---

## Phase 3: Runner death animation and retry popup

### Death animation (`DYING` state, about 1.5 seconds)
- Forward movement stops and the limbs stop swinging.
- The dude tips over backwards: rotate around the X axis from 0 to 90 degrees with an ease-out curve. He sinks slightly so he lies on the floor.
- The helicopter hat spins faster, lifts off and drifts upward while fading out.
- The screen tints red briefly, drawn by `Hud` as a semi-transparent full-screen quad.
- A `deathTimer` drives it, in `Dude.drawDude()` and a new `Dude.drawDeath()`.

### Retry popup (`RUNNER_DEAD` state)
- **Title:** "Helidude went down!"
- **Info:** the reason (out of health, or hit a pillar), orbs collected, and attempts left.
- **Retry** (Enter) uses one attempt and calls `resetRunner()`.
- **Start over** (N) calls `newGame()`.
- **Attempts:** 3 per game. With none left, Retry is greyed out and the title changes to "Game over".

### Pillar hits
A cylinder hit currently restarts the corridor instantly in `didReachEnd()`. It now kills the dude immediately: health goes to 0 and the same animation and popup follow. The instant reset code in `didReachEnd()` is removed for hits. Reaching the end of the corridor still goes to scene two as before.

**Check:** dying by traps and by a pillar both play the animation and then the popup. Retry decrements attempts, and the fourth death only offers Start over.

---

## Phase 4: Ship end screen with credits

### Behaviour
- **Crash:** the explosion plays once instead of looping. It grows, then fades over about 1.5 seconds, and the popup opens.
- **Win:** reaching the end no longer explodes the ship. The ship keeps flying and a "You made it!" popup opens.
- **Popup:**
  - Title: "You crashed!" or "You made it!"
  - Info: distance flown and warp gates passed.
  - **Retry ship (Credits: N/3)** (Enter) uses one credit and calls `resetShip()`. This restarts scene three without replaying the corridor.
  - **Start over** (N) calls `newGame()`.
- **Credits:** 3 per game, shown in the HUD during scene three. At 0 credits the retry button is greyed out.

### Implementation
- `Dude.drawExplosion()` stops resetting `exp_size` to 35 at the end. When it reaches its maximum, it sets the state to `SHIP_ENDED`.
- Split the end of `didReachEnd()` for scene three into a crash path and a win path.
- `resetShip()` must also reset the scene three obstacle layout and `orbHit[]`, which the warp gates reuse.

**Check:** crash, retry, crash again, and the credits count down to 0. Winning shows the win popup. Start over returns to the corridor with full credits and attempts.

---

## Phase 5: Designed obstacle layout with more spacing

### Problem
Every obstacle type, 35 of each, gets a fully random x in -3..3 and a random z in 0..-250, independently of the others. They overlap, cluster and sometimes block the whole corridor, with no spacing guarantee.

### New approach: rows, lanes and patterns
- **Lanes:** the dude strafes in 0.5 steps between -2.5 and 2.5. Use 5 lanes at x = -2, -1, 0, 1 and 2.
- **Rows:** obstacles appear in rows spaced **8 units apart** along z. The first row is at z = -20, after the safe start area, and the last is before the end wall. That gives about 28 rows instead of 140 scattered objects.
- **Patterns:** a short hand-made list of row patterns, each using 1 to 3 lanes. Examples:
  - one pillar in the middle;
  - traps on both edges;
  - a short pillar with an orb beside it;
  - a trap in front of a speed boost.
- **Rules:**
  - Every row leaves at least two adjacent free lanes, so there is always a path.
  - Two rows in a row never block the same lanes.
  - At most one trap per row. Traps cost health now, so they must be avoidable.
  - Difficulty ramps up: easy patterns in the first third, harder ones later.
- **Light randomness:** pick patterns from a seeded `java.util.Random`, so each attempt varies but stays fair. Add a small ±0.5 z jitter so rows don't look like a grid.

### Implementation
- Add `objects/Obstacle.java` with `type` (BOOST, TRAP, PILLAR, SHORT_PILLAR, ORB, GATE), `x`, `y`, `z` and `hit`.
- Add `animation/ObstacleLayout.java` to generate a `List<Obstacle>` for scene one and for scene three. Scene three uses the same idea with warp gates and pillars, plus a height for gates.
- Replace the parallel arrays (`xBoosts`, `zBoosts`, `xTraps`, `zWalls`, `xCubes` and the others) and `initializeObstacles()`. Drawing and collision code loops over the list by type.
- `drawShortCylinders()` currently translates twice inside its loop, so it draws two stacked pillars at a doubled offset. Fix this while moving it to the list.

**Check:** play several attempts and confirm there is always a clear path, nothing overlaps, and the end of the corridor is harder than the start.

---

## Decisions (confirmed)
- **Runner retry attempts:** 3 per game. When they run out, only Start over remains.
- **Pillar hits** kill the runner instantly. Only slow traps use the health bar.
- **Ship credits** only restart scene three. The runner's attempts and the ship's credits are separate counts, and both reset on Start over.
- **Popups are drawn inside the OpenGL window**, not as Swing dialogs. While a popup is open, the game behind it is dimmed so the popup stands out.
