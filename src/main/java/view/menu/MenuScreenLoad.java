package view.menu;

import game.VERSION;
import game.save.SaveFile;
import init.constant.C;
import init.constant.Config;
import init.paths.PATH;
import init.paths.PATHS;
import init.race.RACES;
import init.sprite.UI.UI;
import script.ScriptEngine;
import script.ScriptLoad;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.DIR;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.clickable.CLICKABLE;
import snake2d.util.gui.clickable.CLICKABLE.ClickableAbs;
import snake2d.util.gui.clickable.Scrollable;
import snake2d.util.gui.clickable.Scrollable.ScrollRow;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.sprite.text.Str;
import util.colors.GCOLOR;
import util.gui.misc.GStat;
import util.gui.misc.GText;
import util.gui.table.GScrollable;
import util.info.GFORMAT;
import util.text.D;
import util.text.Dic;
import util.text.DicTime;

/**
 * MODDED: Adds an "Edit Scripts" dialog to pick scripts before loading a save.
 *
 * Notes:
 * - Script keys use the vanilla format "jar->fully.qualified.Class".
 * - If a script was saved but is unchecked, its saved bytes are skipped on load.
 * - If a script is checked but wasn't saved, it is initialized fresh.
 */
public abstract class MenuScreenLoad extends ClickableAbs {

    private final GuiSection main;
    private final GuiSection prompt = new GuiSection();
    private final GuiSection deleteOld = new GuiSection();

    // MODDED
    private final GuiSection scriptsEditor = new GuiSection();
    private java.util.List<ScriptEntry> availableScripts;
    private boolean[] scriptsChecked;

    private GuiSection current;

    private SaveFile[] saves = new SaveFile[0];
    private final RENDEROBJ info;
    private int selectedSave = -1;

    public static CharSequence ¤¤name = "¤saved game";
    static CharSequence ¤¤delete = "¤Delete Save?";
    static CharSequence ¤¤deleteAll = "¤Delete Old";
    static CharSequence ¤¤deleteAllD = "¤Delete {0} outdated saves forever? You might still be able to load them if you revert for an earlier version of the game.";

    static CharSequence ¤¤prob = "Loading it may crash the game!";
    static CharSequence ¤¤prob2 = "It can not be loaded with the current version of the game.";

    private final COLOR color;
    private SaveFile file;
    private SaveFile open;
    private double hovDS;
    private final PATH path;

    static {
        D.ts(MenuScreenLoad.class);
    }

    protected MenuScreenLoad(CharSequence title, COLOR color, boolean edit, PATH path) {

        this.color = color;
        this.path = path;

        ScrollRow[] butts = new ScrollRow[] {
                new Savebutt(), new Savebutt(), new Savebutt(), new Savebutt(), new Savebutt(), new Savebutt(),
                new Savebutt(), new Savebutt(), new Savebutt(), new Savebutt(), new Savebutt(), new Savebutt(),
                new Savebutt(), new Savebutt(),
        };

        Scrollable s = new GScrollable(butts) {

            @Override
            public int nrOFEntries() {
                return saves.length;
            }
        };

        this.main = new GuiSection();
        main.add(s.getView());
        main.body().centerIn(MenuScreen.inner);
        main.body().moveY1(MenuScreen.inner.y1());

        info = new RENDEROBJ.RenderImp(MenuScreen.bounds.width(), 150) {

            @Override
            public void render(SPRITE_RENDERER r, float ds) {
                color.renderFrame(r, body, 8, 2);
            }
        };
        main.addRelBody(32, DIR.S, info);

        if (edit) {
            CLICKABLE load = new MenuScreen.ScreenButton(Dic.¤¤load) {
                @Override
                protected void renAction() {
                    activeSet(selectedSave >= 0 && !saves[selectedSave].spec().fubar);
                }

                @Override
                protected void clickA() {
                    if (selectedSave >= 0 && !saves[selectedSave].spec().fubar) {
                        load(saves[selectedSave]);
                    }
                    super.clickA();
                }
            };
            CLICKABLE delete = new MenuScreen.ScreenButton(Dic.¤¤delete) {
                @Override
                protected void clickA() {
                    if (selectedSave >= 0)
                        current = prompt;
                }

                @Override
                protected void renAction() {
                    activeSet(selectedSave >= 0);
                }
            };
            CLICKABLE deleteAll = new MenuScreen.ScreenButton(¤¤deleteAll) {

                @Override
                protected void clickA() {
                    current = deleteOld;
                }

                @Override
                protected void renAction() {
                    activeSet(oldSaves() > 0);
                }

            };

            // MODDED: Edit Scripts entry
            CLICKABLE scripts = new MenuScreen.ScreenButton("Edit Scripts") {
                @Override
                protected void clickA() {
                    if (selectedSave >= 0 && saves[selectedSave].specReady()) {
                        ensureScriptsModel();
                        setupScriptsCheckedFromSave(saves[selectedSave]);
                        current = scriptsEditor;
                    }
                }

                @Override
                protected void renAction() {
                    activeSet(selectedSave >= 0 && saves[selectedSave].specReady() && !saves[selectedSave].spec().fubar);
                }
            };

            MenuScreen scr = new MenuScreen(title, color) {

                @Override
                protected void back() {
                    MenuScreenLoad.this.back();
                }
            };

            scr.addButt(load);
            scr.addButt(delete);
            scr.addButt(deleteAll);
            scripts.body().moveCY(delete.body().cY());
            scripts.body().moveX1(delete.body().x2() + 275);
            main.add(scr);
            main.moveLastToBack();
            main.add(scripts);

            buildScriptsEditorUI();

        } else {
            CLICKABLE load = new MenuScreen.ScreenButton(Dic.¤¤load) {
                @Override
                protected void renAction() {
                    activeSet(selectedSave >= 0 && !saves[selectedSave].spec().fubar);

                }

                @Override
                protected void clickA() {
                    if (selectedSave >= 0 && !saves[selectedSave].spec().fubar) {
                        load(saves[selectedSave]);
                    }
                    super.clickA();
                }

            };

            MenuScreen scr = new MenuScreen(title, color) {

                @Override
                protected void back() {
                    MenuScreenLoad.this.back();
                }
            };

            scr.addButt(load);
            main.add(scr);
            main.moveLastToBack();
        }

        {
            deleteOld.add(new GText(UI.FONT().H2, ¤¤delete).color(color), 0, 0);

            deleteOld.addDownC(48, new GStat() {

                @Override
                public void update(GText text) {
                    text.add(¤¤deleteAllD);
                    text.insert(0, oldSaves());
                    text.setMaxWidth(600);
                    text.setMultipleLines(true);
                }
            }.increase().r(DIR.C));

            CLICKABLE yes = new MenuScreen.ScreenButton(Dic.¤¤Yes) {
                @Override
                protected void clickA() {
                    for (SaveFile f : saves) {
                        if (VERSION.versionMajor(f.version) < VERSION.VERSION_MAJOR)
                            PATHS.local().save().delete(f.fullName);
                    }
                    selectedSave = -1;
                    populateSaves();
                    current = main;
                }
            };
            CLICKABLE no = new MenuScreen.ScreenButton(Dic.¤¤No) {
                @Override
                protected void clickA() {
                    current = main;
                }
            };

            GuiSection ss = new GuiSection();
            ss.add(yes).addRightC(64, no);
            deleteOld.addRelBody(48, DIR.S, ss);
            deleteOld.body().centerIn(C.DIM());
        }

        {
            prompt.add(new GText(UI.FONT().H2, ¤¤delete).color(color), 0, 0);
            prompt.addDownC(16, new GStat() {

                @Override
                public void update(GText text) {
                    if (selectedSave >= 0)
                        text.add(saves[selectedSave].name);
                }
            }.increase().r(DIR.C));
            CLICKABLE yes = new MenuScreen.ScreenButton(Dic.¤¤Yes) {
                @Override
                protected void clickA() {
                    if (selectedSave >= 0) {
                        PATHS.local().save().delete(saves[selectedSave].fullName);
                        selectedSave = -1;
                        populateSaves();
                    }
                    current = main;
                }
            };
            CLICKABLE no = new MenuScreen.ScreenButton(Dic.¤¤No) {
                @Override
                protected void clickA() {
                    current = main;
                }
            };

            GuiSection ss = new GuiSection();
            ss.add(yes).addRightC(64, no);
            prompt.addRelBody(48, DIR.S, ss);
            prompt.body().centerIn(C.DIM());
        }

        current = main;

        populateSaves();
    }

    protected abstract void back();

    // MODDED: signature accepts optional scripts override.
    protected abstract void load(SaveFile f, String... newScripts);

    public void populateSaves() {
        file = null;
        open = null;
        hovDS = 0;
        saves = SaveFile.list(path);
        selectedSave = -1;
    }

    private int oldSaves() {
        if (saves == null)
            return 0;
        int i = 0;
        for (SaveFile f : saves) {
            if (VERSION.versionMajor(f.version) < VERSION.VERSION_MAJOR)
                i++;
        }
        return i;
    }

    @Override
    public boolean hover(COORDINATE mCoo) {
        if (current.hover(mCoo))
            return true;
        return false;
    }

    @Override
    public boolean click() {
        current.click();
        return false;
    }

    @Override
    protected void render(SPRITE_RENDERER r, float ds, boolean isActive, boolean isSelected, boolean isHovered) {
        if (file == null && selectedSave >= 0) {
            file = saves[selectedSave];
            open = saves[selectedSave];
        }


        current.render(r, ds);

        if (current != main) {
            file = null;
            return;
        }

        if (file == null || file != open) {
            this.hovDS = 0;
            open = file;
        }

        if (open == null)
            return;
        this.hovDS += ds;
        if (this.hovDS < 0.25 && !file.specReady())
            return;
        if (file != null) {
            renderInfo(r, file, info.body(), ds);
        }
        file = null;

    }

    private static Str version = new Str(16);
    private GText tmp = new GText(UI.FONT().H2, 64);

    protected void renderInfo(SPRITE_RENDERER r, SaveFile file, RECTANGLE body, double ds) {

        int y1 = renderInfoGen(r, file, body);
        y1 = renderInfoMod(r, file, body.x1(), y1, ds);
        renderInfoProb(r, file, body.x1(), y1);

    }

    protected int renderInfoGen(SPRITE_RENDERER r, SaveFile file, RECTANGLE body) {

        int ii = 0;
        renderPair(r, ii++, body, Dic.¤¤Capitol, file.spec().city);
        renderPair(r, ii++, body, Dic.¤¤Ruler, file.spec().ruler);
        renderPair(r, ii++, body, RACES.name(), file.spec().race);
        renderPair(r, ii++, body, Dic.¤¤Population, GFORMAT.i(tmp.clear(), file.spec().population));
        renderPair(r, ii++, body, Dic.¤¤Regions, GFORMAT.i(tmp.clear(), file.spec().regions));
        renderPair(r, ii++, body, Dic.¤¤Subjects, GFORMAT.i(tmp.clear(), file.spec().regPop));
        // Stable fallback day calculation: use secondsPerHour*hoursPerDay*16.0 (vanilla logic) without relying on missing constants.
        return renderPair(r, ii++, body, Dic.¤¤PlayTime,
                DicTime.setYears(tmp.clear(), file.spec().playSeconds / (Config.sett().secondsPerHour * Config.sett().hoursPerDay * 16.0)));


    }

    private double mi = 0;

    protected int renderInfoMod(SPRITE_RENDERER r, SaveFile file, int x1, int y1, double ds) {

        mi += ds;

        if (mi >= file.spec().mods.length)
            mi -= (int) mi;


        if (mi >= file.spec().mods.length)
            return y1 + UI.FONT().H2.height() + 6;

        color.bind();
        UI.FONT().H2.render(r, Dic.¤¤Mods, x1, y1);
        tmp.clear();
        tmp.add(1 + (int) mi).add('/').add(file.spec().mods.length).s();
        tmp.add(file.spec().mods[(int) mi]);

        UI.FONT().M.render(r, tmp, x1 + UI.FONT().H2.width(Dic.¤¤Mods) + 8, y1);
        COLOR.unbind();
        return y1 + UI.FONT().H2.height() + 6;

    }

    protected void renderInfoProb(SPRITE_RENDERER r, SaveFile file, int x1, int y1) {

        CharSequence p = file.spec().warning();
        if (p != null) {
            tmp.clear();
            tmp.color(file.spec().fubar ? COLOR.REDISH : COLOR.YELLOW100).add(p);
            tmp.s();
            tmp.add(file.spec().fubar ? ¤¤prob2 : ¤¤prob);
            tmp.setMultipleLines(true);
            tmp.setMaxWidth(C.MIN_WIDTH);
            (file.spec().fubar ? COLOR.REDISH : COLOR.YELLOW100).bind();
            UI.FONT().M.render(r, tmp, x1, y1);
            COLOR.unbind();

        }

    }


    private int renderPair(SPRITE_RENDERER r, int ii, RECTANGLE body, CharSequence title, CharSequence value) {
        int x1 = body.x1() + (ii % 3) * 350;
        int y1 = body.y1() + (ii / 3) * (UI.FONT().M.height() + 6);
        color.bind();
        UI.FONT().H2.render(r, title, x1, y1);
        x1 += UI.FONT().H2.width(title) + 8;
        COLOR.unbind();
        UI.FONT().M.render(r, value, x1, y1);
        return y1 + (UI.FONT().M.height() + 6);


    }

    protected void renderName(SPRITE_RENDERER r, SaveFile s, RECTANGLE body) {

        version.clear();
        version.add(VERSION.versionMajor(s.version));
        version.add('.');
        version.add(VERSION.versionMinor(s.version));

        UI.FONT().M.render(r, version, body.x1(), body.y1());

        UI.FONT().H2.render(r, s.name, body.x1() + 90, body.y1());

        tmp.clear().add('p').s();
        GFORMAT.i(tmp, s.pop);
        UI.FONT().M.render(r, tmp, body.x1() + 830, body.y1());

        UI.FONT().M.render(r, s.ago, body.x2() - 180, body.y1());

    }

    public SaveFile[] saves() {
        return saves;
    }

    private final class Savebutt extends CLICKABLE.ClickableAbs implements ScrollRow {

        private int index;
        public Savebutt() {
            body.setWidth(MenuScreen.inner.width());
            body.setHeight(28);
        }

        @Override
        public void init(int index) {
            this.index = index;
        }

        @Override
        protected void clickA() {
            selectedSave = index;
            file = null;
        }

        @Override
        protected void render(SPRITE_RENDERER r, float ds, boolean isActive, boolean isSelected, boolean isHovered) {
            SaveFile s = saves[index];

            if (index == selectedSave) {
                GCOLOR.T().SELECTED.bind();
            } else if (isHovered) {
                GCOLOR.T().HOVERED.bind();
            } else if (s.specReady() && s.spec().fubar) {
                COLOR.REDISH.bind();
            } else if (s.problem() != null | (s.specReady() && s.spec().warning() != null)) {
                COLOR.YELLOW100.bind();
            }

            // Vanilla draws the row text via renderName (version + name + pop + ago)
            renderName(r, s, body);
            COLOR.WHITE50.render(r, body.x1(), body.x2(), body.y2(), body.y2() + 1);
            COLOR.unbind();
        }

    }

    // ---------------- MODDED: scripts editor ----------------

    private static final class ScriptEntry {
        final String key;
        final String label;

        ScriptEntry(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }

    private void ensureScriptsModel() {
        if (availableScripts != null)
            return;

        java.util.ArrayList<ScriptEntry> list = new java.util.ArrayList<>();
        for (ScriptLoad l : ScriptEngine.getAll()) {
            // keep it stable & readable; label uses jar + class
            String label = l.file + " -> " + l.className;
            list.add(new ScriptEntry(l.key, label));
        }
        list.sort(java.util.Comparator.comparing(a -> a.label));
        availableScripts = java.util.Collections.unmodifiableList(list);
        scriptsChecked = new boolean[availableScripts.size()];
    }

    private void setupScriptsCheckedFromSave(SaveFile f) {
        java.util.Arrays.fill(scriptsChecked, false);
        String[] ss = f.spec().scripts;
        if (ss == null)
            return;

        java.util.HashSet<String> set = new java.util.HashSet<>();
        java.util.Collections.addAll(set, ss);
        for (int i = 0; i < availableScripts.size(); i++) {
            scriptsChecked[i] = set.contains(availableScripts.get(i).key);
        }
    }

    private void buildScriptsEditorUI() {
        ensureScriptsModel();

        scriptsEditor.add(new GText(UI.FONT().H2, "Edit Scripts"), 0, 0);

        class ScriptRow extends CLICKABLE.ClickableAbs implements ScrollRow {

            private final int ii;

            ScriptRow(int ii) {
                this.ii = ii;
                body.setWidth(MenuScreen.inner.width());
                body.setHeight(24);
            }

            @Override
            public void init(int index) {
                // fixed mapping; row index is ii
            }

            @Override
            protected void clickA() {
                scriptsChecked[ii] = !scriptsChecked[ii];
            }

            @Override
            protected void render(SPRITE_RENDERER r, float ds, boolean isActive, boolean isSelected, boolean isHovered) {
                int x1 = body.x1();
                int y1 = body.y1();

                if (isHovered)
                    GCOLOR.T().HOVERED.bind();

                UI.FONT().M.render(r, (scriptsChecked[ii] ? "[x] " : "[ ] ") + availableScripts.get(ii).label,
                        x1, y1);

                COLOR.unbind();
            }
        }

        ScrollRow[] rows = new ScrollRow[availableScripts.size()];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = new ScriptRow(i);
        }

        Scrollable s2 = new GScrollable(rows) {
            @Override
            public int nrOFEntries() {
                return rows.length;
            }
        };
        scriptsEditor.addDownC(16, s2.getView());

        CLICKABLE ok = new MenuScreen.ScreenButton(Dic.¤¤load) {
            @Override
            protected void clickA() {
                if (selectedSave >= 0) {
                    SaveFile f = saves[selectedSave];
                    java.util.ArrayList<String> keys = new java.util.ArrayList<>();
                    for (int i = 0; i < scriptsChecked.length; i++) {
                        if (scriptsChecked[i])
                            keys.add(availableScripts.get(i).key);
                    }
                    load(f, keys.toArray(new String[0]));
                }
                current = main;
            }
        };
        CLICKABLE cancel = new MenuScreen.ScreenButton(Dic.¤¤cancel) {
            @Override
            protected void clickA() {
                current = main;
            }
        };

        GuiSection ss = new GuiSection();
        ss.add(ok).addRightC(64, cancel);
        scriptsEditor.addRelBody(48, DIR.S, ss);
        scriptsEditor.body().centerIn(C.DIM());
    }

}
