# Helidude

A small 3D runner game written in Java with JOGL (OpenGL), originally made in 2017.

A helicopter-hatted dude runs down a brick corridor collecting speed orbs and dodging traps and pillars.
At the end he finds a space shuttle and flies it through warp gates.

## Requirements

- JDK 21 or newer (developed and tested on JDK 27).
- VS Code with the **Extension Pack for Java**.
- Windows x64. The JOGL jars in `lib/` include Windows natives only.

The JOGL 2.6.0 jars are committed in `lib/`, so there is nothing else to download.

## Running

Open this folder in VS Code and press **F5**, or pick **Run Helidude** in the Run and Debug panel.

The launch configuration sets the working directory to the project root so `resources/` is found.
It also passes the JVM flag JOGL needs on modern Java:

```
--enable-native-access=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED
```

From a terminal in the project root:

```
javac -d out -cp "lib/*" src/animation/*.java src/models/*.java src/objects/*.java
java --enable-native-access=ALL-UNNAMED --add-opens java.desktop/sun.awt=ALL-UNNAMED -cp "out;lib/*" animation.GameRenderer
```

If the JDK path in `.vscode/settings.json` doesn't match your install, update it.

## Controls

| Key | Corridor (scene 1) | Ship (scene 3) |
| --- | --- | --- |
| A / Left | Strafe left | Roll left |
| D / Right | Strafe right | Roll right |
| W / Up | | Climb |
| S / Down | | Dive |
| Space | Jump (helicopter hat) | |
| Enter | Toggle first/third person | |
| Mouse drag | Free look in first person | |
| M | Toggle fog mode | |
| T | Toggle slow run | |
| Z | Toggle anisotropic filtering | |

In scene 2, press **W / Up** to walk to the shuttle. Game messages print to the console.
