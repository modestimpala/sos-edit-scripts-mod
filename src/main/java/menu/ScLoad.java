package menu;

import game.GAME;
import game.battle.state.BattleState;
import game.battle.state.BattleStateExiter;
import game.battle.state.BattleStateResult;
import game.save.GameLoader;
import game.save.SaveFile;
import init.paths.PATHS;
import init.sprite.UI.UI;
import menu.GUI.Shadower;
import snake2d.CORE;
import snake2d.CORE_STATE;
import snake2d.SPRITE_RENDERER;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.RECTANGLE;
import snake2d.util.sprite.text.Str;
import util.gui.misc.GText;
import util.text.D;
import view.menu.MenuScreenLoad;
import world.battle.spec.BATTLE_RESULT;

/**
 * MODDED: forwards optional scripts selection to {@link GameLoader}.
 */
class ScLoad implements SC {

    private static CharSequence ¤¤showCases = "¤showcases";
    private static CharSequence ¤¤custom = "¤scenarios";
    private static CharSequence ¤¤battles = "¤battles";

    static {
        D.ts(ScLoad.class);
    }

    private final MenuScreenLoad screen;
    private final Menu menu;
    public final CharSequence name;

    ScLoad(Menu menu, MenuScreenLoad screen, CharSequence name) {
        this.menu = menu;
        this.screen = screen;
        this.name = name;
    }

    @Override
    public boolean hover(COORDINATE mCoo) {
        return screen.hover(mCoo);
    }

    @Override
    public boolean click() {
        return screen.click();
    }

    @Override
    public void render(SPRITE_RENDERER r, float ds) {
        screen.render(r, ds);
        Shadower.ren(r, ds);
    }

    @Override
    public boolean back(Menu menu) {
        menu.switchScreen(menu.main);
        return true;
    }

    public boolean hasSaves() {
        return screen.saves().length != 0;
    }

    public void loadSave() {
        if (hasSaves()) {
            menu.start(new GameLoader(PATHS.local().save().get(screen.saves()[0].fullName)));
        }
    }

    public static ScLoad load(Menu menu) {
        MenuScreenLoad screen = new MenuScreenLoad(MenuScreenLoad.¤¤name, GUI.labelColor, true, PATHS.local().save()) {

            @Override
            protected void load(SaveFile f, String... newScripts) {
                menu.start(new GameLoader(f.path, newScripts));
            }

            @Override
            protected void back() {
                menu.switchScreen(menu.main);
            }
        };
        return new ScLoad(menu, screen, MenuScreenLoad.¤¤name);
    }

    public static ScLoad showcase(Menu menu) {

        MenuScreenLoad screen = new MenuScreenLoad(¤¤showCases, GUI.labelColor, false, PATHS.MISC().EXAMPLES) {

            @Override
            protected void load(SaveFile f, String... newScripts) {
                menu.start(new GameLoader(f.path, newScripts) {
                    @Override
                    public void doAfterSet() {
                        GAME.achieve(false);
                        super.doAfterSet();
                    }
                });
            }

            @Override
            protected void back() {
                menu.switchScreen(menu.main);
            }

            @Override
            protected void renderInfo(SPRITE_RENDERER r, SaveFile file, RECTANGLE body, double ds) {
                int y1 = renderInfoGen(r, file, body);
                if (file.specReady() && file.spec().fubar)
                    renderInfoProb(r, file, body.x1(), y1);
            }

            @Override
            protected void renderName(SPRITE_RENDERER r, SaveFile s, RECTANGLE body) {

                UI.FONT().H2.render(r, s.name, body.x1() + 64, body.y1());

                UI.icons().s.human.renderCY(r, body.x1() + 700, body.y1() + UI.FONT().M.height() / 2);
                Str.TMP.clear().add(s.pop);
                UI.FONT().M.render(r, Str.TMP, body.x1() + 720, body.y1());
            }

        };
        return new ScLoad(menu, screen, ¤¤showCases);
    }

    public static ScLoad scenarios(Menu menu) {
        MenuScreenLoad screen = new MenuScreenLoad(¤¤custom, GUI.labelColor, false, PATHS.MISC().CUSTOM) {

            GText t = new GText(UI.FONT().M, 128);
            {
                t.setMaxWidth(800);
                t.setMultipleLines(true);
            }

            @Override
            protected void load(SaveFile f, String... newScripts) {
                menu.start(new GameLoader(f.path, newScripts));
            }

            @Override
            protected void back() {
                menu.switchScreen(menu.main);
            }

            @Override
            protected void renderInfo(SPRITE_RENDERER r, SaveFile file, RECTANGLE body, double ds) {
                t.set(file.spec().desc);
                t.renderC(r, body);
            }

            @Override
            protected void renderName(SPRITE_RENDERER r, SaveFile s, RECTANGLE body) {
                UI.FONT().H2.render(r, s.name, body.x1() + 64, body.y1() + UI.FONT().M.height() / 2);
            }

        };
        return new ScLoad(menu, screen, ¤¤custom);

    }

    public static ScLoad battle(Menu menu) {
        MenuScreenLoad screen = new MenuScreenLoad(¤¤battles, GUI.labelColor, false, PATHS.MISC().BATTLE) {
            GText t = new GText(UI.FONT().M, 128);
            {
                t.setMaxWidth(800);
                t.setMultipleLines(true);
            }

            @Override
            protected void load(SaveFile f, String... newScripts) {
                menu.start(new GameLoader(f.path, newScripts) {
                    @Override
                    public void doAfterSet() {
                        super.doAfterSet();
                        BattleState.setLoaded(new BattleStateExiter() {

                            @Override
                            public void exit(BATTLE_RESULT res, int plosses, int elosses) {
                                CORE.setCurrentState(new CORE_STATE.Constructor() {
                                    @Override
                                    public CORE_STATE getState() {
                                        return Menu.make();
                                    }
                                });
                            }

                            @Override
                            public void afterExit(BattleStateResult res) {
                            }
                        }, f.path, true);
                    }
                });
            }

            @Override
            protected void renderInfo(SPRITE_RENDERER r, SaveFile file, RECTANGLE body, double ds) {

                t.clear().add(file.spec().population).s().add('V').add('s').s().add(file.spec().enemies);
                t.adjustWidth();
                t.renderCX(r, body.cX(), body.y1());

                t.set(file.spec().desc);
                t.renderCX(r, body.cX(), body.y1() + 32);
            }

            @Override
            protected void renderName(SPRITE_RENDERER r, SaveFile s, RECTANGLE body) {
                UI.FONT().H2.render(r, s.name, body.x1() + 64, body.y1() + UI.FONT().M.height() / 2);
            }

            @Override
            protected void back() {
                menu.switchScreen(menu.main);
            }
        };
        return new ScLoad(menu, screen, ¤¤battles);
    }

}
