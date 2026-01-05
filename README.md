
# Edit Scripts (Songs of Syx mod)

Adds an **“Edit Scripts”** button to the load screen so you can choose which scripts are enabled **before** loading a save.

## How it works

- Open Load Game.
- Select a save.
- Click **Edit Scripts**.
- Tick/untick scripts.
- Click **Load** to load the save using that script selection.

## Notes

- This only changes which scripts are enabled for the load; it doesn’t edit the save file on disk. You have to save again in-game to make the changes permanent.

## Compatibility

This mod replaces game UI classes, so it’s incompatible with any other mod that also replaces any of these files:

- `src\main\java\view\menu\MenuScreenLoad.java`
- `src\main\java\view\menu\IMenuLoad.java`
- `src\main\java\menu\ScLoad.java`


