package game;

import java.io.IOException;
import java.nio.file.Path;

import game.faction.FACTIONS;
import game.time.TIME;
import init.constant.Config;
import init.paths.PATH;
import init.paths.PATHS;
import init.type.HTYPES;
import script.ScriptEngine;
import script.ScriptLoad;
import settlement.stats.POP;
import settlement.stats.STATS;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import snake2d.util.file.Json;
import snake2d.util.sets.KeyMap;
import snake2d.util.sprite.text.Str;
import util.text.D;
import world.WORLD;
import world.region.RD;

public class GameSpec {

	/**
	 * MODDED: Marker used by "Edit Scripts" to explicitly override scripts with an
	 * empty selection.
	 * <p>
	 * Vanilla logic treats an empty varargs as "use scripts from save".
	 */
	public static final String SCRIPTS_NONE_MARKER = "__EDIT_SCRIPTS_NONE__";

	public boolean fubar;
	public final int version;
	public final double playSeconds;
	public final int population;
	public final int enemies;
	public final int regions;
	public final int regPop;
	public final CharSequence race;
	public final CharSequence city;
	public final CharSequence ruler;
	public final CharSequence desc;
	public final String[] scripts;
	public final String[] mods;
	public final int wx;
	public final int wy;
	public final String races;
	public final String rooms;
	public final String resources;
	public final String industries;

	private static String[] normalizeScripts(String[] override, String[] fallback) {
		if (override == null)
			return fallback;
		if (override.length == 1 && SCRIPTS_NONE_MARKER.equals(override[0]))
			return new String[0];
		return override.length > 0 ? override : fallback;
	}

	private GameSpec(String... scripts) {
		fubar = true;
		this.scripts = normalizeScripts(scripts, scripts);
		version = VERSION.VERSION;
		playSeconds = 0;
		population = 0;
		regions = 0;
		regPop = 0;
		enemies = 0;
		race = "?";
		city = "?";
		ruler = "?";
		desc = "?";
		mods = mods();
		wx = Config.world().WORLD_SIZE;
		wy = Config.world().WORLD_SIZE;
		races = getResources("race");
		rooms = getResources("room");
		resources = getResources("resource");
		industries = getResourcesArray("room", "INDUSTRY");
	}

	private GameSpec(FileGetter f, String... scripts) throws IOException {
		int pos = f.getPosition() + f.i() + 4;
		version = f.i();
		playSeconds = f.d();
		population = f.i();
		enemies = f.i();
		regions = f.i();
		regPop = f.i();
		wx = f.i();
		wy = f.i();

		race = f.chars();
		city = f.chars();
		ruler = f.chars();
		desc = f.chars();

		mods = f.charss();
		String[] ss = f.charss();
		this.scripts = normalizeScripts(scripts, ss);

		races = f.chars();
		rooms = f.chars();
		resources = f.chars();
		industries = f.chars();
		if (f.getPosition() != pos) {
			fubar = true;
			f.setPosition(pos);
		}
	}

	public static void save(FilePutter f) {
		int pos = f.getPosition();
		f.i(0);
		f.i(VERSION.VERSION);
		f.d((int) TIME.playedGame());
		f.i(POP.tot(null, null));
		f.i(STATS.POP().pop(HTYPES.ENEMY()));
		f.i(FACTIONS.player().realm().regions() - 1);
		f.i(RD.RACES().population.faction().get(FACTIONS.player()));
		f.i(WORLD.TWIDTH());
		f.i(WORLD.THEIGHT());

		f.chars("" + FACTIONS.player().race().info.name);
		f.chars("" + FACTIONS.player().name);
		f.chars("" + FACTIONS.player().rulerName);
		f.chars("" + FACTIONS.player().desc);

		f.charss(mods());
		f.charss(GAME.script().currentScripts());

		f.chars(getResources("race"));
		f.chars(getResources("room"));
		f.chars(getResources("resource"));
		f.chars(getResourcesArray("room", "INDUSTRY"));

		int le = f.getPosition() - pos - 4;
		f.setAtPosition(pos, le);

	}

	public static GameSpec get(String... scripts) {
		return new GameSpec(scripts);
	}

	public static GameSpec get(FileGetter f, String... scripts) {
		try {
			GameSpec s = new GameSpec(f, scripts);
			return s;
		} catch (Exception e) {
			return new GameSpec(scripts);
		}

	}

	public static GameSpec get(Path path) {
		try {
			FileGetter g = new FileGetter(path, true);
			GameSpec s = new GameSpec(g, new String[0]);
			return s;
		} catch (Exception e) {
			return new GameSpec(new String[0]);
		}
	}

	private static String[] mods() {
		String[] mods = new String[PATHS.currentMods().size()];
		for (int i = 0; i < PATHS.currentMods().size(); i++) {
			mods[i] = ("'" + PATHS.currentMods().get(i).name + "', version: " + PATHS.currentMods().get(i).version);
		}
		return mods;
	}

	private CharSequence prob;
	private CharSequence warn;
	private boolean hasCheck = false;

	private static CharSequence ¤¤version = "¤Version mismatch! Save is made with major game version: {0}. Try downgrading the game to this version. The current game version is: ";
	private static CharSequence ¤¤race = "¤The amount of races does not match the current configuration.";
	private static CharSequence ¤¤room = "¤The amount of rooms does not match the current configuration.";
	private static CharSequence ¤¤industries = "¤The amount of industries does not match the current configuration.";
	private static CharSequence ¤¤resources = "¤The amount of resources does not match the current configuration.";
	private static CharSequence ¤¤modOther = "¤The save can not be loaded as it was made with another mod configuration:";
	private static CharSequence ¤¤modNone = "¤The save can not be loaded as it was made with an un-modified game. Disable all mods in the launcher to load the game.";
	private static CharSequence ¤¤script = "¤The script: {0} that the game was saved with can not be found.";
	private static CharSequence ¤¤fubar = "The save-file is corrupt.";
	private static CharSequence ¤¤mod2 = "¤Try to enable the same mods, in the same order, in the launcher and reload the save.";
	private static CharSequence ¤¤underlaying = "¤UnderLaying problem:";

	private static CharSequence ¤¤mods = "Game was saved with other mods than those currently enabled.";

	static {
		D.ts(GameSpec.class);
	}

	public CharSequence warning() {
		if (!hasCheck) {
			hasCheck = true;
			warn = pwarning();
			prob = pproblem();
		}
		return warn;
	}

	public CharSequence crashCause() {

		if (!hasCheck) {
			hasCheck = true;
			warn = pwarning();
			prob = pproblem();
		}
		return prob;
	}

	public CharSequence pwarning() {
		if (fubar) {
			return ¤¤fubar;
		}
		if (VERSION.versionMajor(version) != VERSION.VERSION_MAJOR)
			return "" + (Str.TMP.clear().add(¤¤version).insert(0, VERSION.versionMajor(version)) + " " + +VERSION.VERSION_MAJOR);

		if (!modsEqual())
			return "" + Str.TMP.clear().add(¤¤mods);
		if (!races.equals(getResources("race"))) {
			return ¤¤race;
		}
		if (!rooms.equals(getResources("room"))) {
			modException(¤¤room);
		}
		if (!resources.equals(getResources("resource"))) {
			modException(¤¤resources);
		}
		if (industries != null && !industries.equals(getResourcesArray("room", "INDUSTRY")))
			return ¤¤industries;
		KeyMap<String> avai = new KeyMap<String>();
		for (ScriptLoad sc : ScriptEngine.getAll())
			avai.put(sc.key, sc.key);
		for (String sc : scripts) {
			if (!avai.containsKey(sc)) {
				return (modException(Str.TMP.clear().add(¤¤script).insert(0, sc)));
			}
		}

		return null;
	}

	private boolean modsEqual() {
		String[] mm = mods();
		if (mm.length != mods.length)
			return false;
		for (int i = 0; i < mods.length; i++) {
			if (!mm[i].equals(mods[i]))
				return false;
		}
		return true;
	}

	private CharSequence pproblem() {

		if (fubar) {
			return ¤¤fubar;
		}

		if (VERSION.versionMajor(version) != VERSION.VERSION_MAJOR)
			return "" + (Str.TMP.clear().add(¤¤version).insert(0, VERSION.versionMajor(version)) + " " + +VERSION.VERSION_MAJOR);

		if (!races.equals(getResources("race"))) {
			return (modException(¤¤race));
		}
		if (!rooms.equals(getResources("room"))) {
			return (modException(¤¤room));
		}
		if (!resources.equals(getResources("resource"))) {
			return (modException(¤¤resources));
		}
		if (industries != null && !industries.equals(getResourcesArray("room", "INDUSTRY")))
			return (modException(¤¤industries));

		KeyMap<String> avai = new KeyMap<String>();
		for (ScriptLoad sc : ScriptEngine.getAll())
			avai.put(sc.key, sc.key);
		for (String sc : scripts) {
			if (!avai.containsKey(sc)) {
				return (modException(Str.TMP.clear().add(¤¤script).insert(0, sc)));
			}
		}
		if (!modsEqual()) {
			Str s = Str.TMP;
			if (mods.length == 0) {
				s.add(¤¤modNone);
			} else {
				s.add(¤¤modOther);
				s.NL();
				s.NL();
				for (String ss : mods) {
					s.add(ss);
					s.NL();
				}
				s.NL();
				s.add(¤¤mod2);
			}
			return "" + s;
		}
		return null;
	}

	private CharSequence modException(CharSequence problem) {

		if (!modsEqual()) {
			Str s = Str.TMP;
			s.clear();

			if (mods.length == 0) {
				s.add(¤¤modNone);
			} else {
				s.add(¤¤modOther);
				s.NL();
				s.NL();
				for (String ss : mods) {
					s.add(ss);
					s.NL();
				}
				s.NL();
				s.add(¤¤mod2);
			}
			s.NL();
			s.NL();
			s.add(¤¤underlaying);
			s.NL();
			s.add(problem);
			return "" + s;
		} else {
			return "" + problem;
		}

	}

	private static String getResourcesArray(String init, String key) {
		String s = "";
		PATH p = PATHS.INIT().getFolder(init);
		for (String k : p.getFiles()) {
			Json j = new Json(p.get(k));
			if (j.has(key) && j.jsonsIs(key))
				s += k + j.jsons(key).length;
		}
		return s;
	}

	private static String getResources(String init) {
		String s = "";
		for (String k : PATHS.INIT().getFolder(init).getFiles())
			s += k;
		return s;
	}

}
