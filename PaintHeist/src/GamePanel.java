import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.RadialGradientPaint;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GamePanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private enum Screen {
        MENU, LEVEL_SELECT, SHOP, CUSTOMIZE, SETTINGS, GALLERY, PLAYING, FORGERY, PAUSED, VICTORY, GAME_OVER
    }

    static final class Assets {
        private final BufferedImage[][] playerSprites;
        private final BufferedImage[][][] guardSprites;
        private final BufferedImage btnPlay;
        private final BufferedImage btnCustomize;
        private final BufferedImage btnSettings;
        private final BufferedImage btnQuit;
        private final BufferedImage btnShop;
        private final BufferedImage framePrimary;
        private final BufferedImage frameSecondary;
        private final BufferedImage frameBadge;
        private final BufferedImage menuBg;

        private Assets(BufferedImage[][] playerSprites, BufferedImage[][][] guardSprites,
                       BufferedImage btnPlay, BufferedImage btnCustomize, BufferedImage btnSettings,
                       BufferedImage btnQuit, BufferedImage btnShop, BufferedImage framePrimary,
                       BufferedImage frameSecondary, BufferedImage frameBadge, BufferedImage menuBg) {
            this.playerSprites = playerSprites;
            this.guardSprites = guardSprites;
            this.btnPlay = btnPlay;
            this.btnCustomize = btnCustomize;
            this.btnSettings = btnSettings;
            this.btnQuit = btnQuit;
            this.btnShop = btnShop;
            this.framePrimary = framePrimary;
            this.frameSecondary = frameSecondary;
            this.frameBadge = frameBadge;
            this.menuBg = menuBg;
        }

        BufferedImage[] getThiefLoadingFrames() {
            BufferedImage[] strip = playerSprites[ACTION_ROW_START[WALK_ACTION] + 3];
            if (strip.length <= 4) return strip;
            BufferedImage[] four = new BufferedImage[4];
            System.arraycopy(strip, 0, four, 0, 4);
            return four;
        }

        BufferedImage[] getGuardLoadingFrames() {
            BufferedImage[] strip = guardSprites[1][3];
            if (strip.length <= 4) return strip;
            BufferedImage[] four = new BufferedImage[4];
            System.arraycopy(strip, 0, four, 0, 4);
            return four;
        }
    }

    private static final int WIDTH = MuseumMap.WIDTH;
    private static final int HEIGHT = MuseumMap.HEIGHT;
    private static final Color GOLD = new Color(231, 190, 105);
    private static final Color INK = new Color(15, 17, 24);
    private static final int SPRITE_COLUMNS = 6;
    private static final int SPRITE_ROWS = 20;
    private static final int SPRITE_STRIP_FRAME_COUNT = 4;
    private static final int SPRITE_WIDTH = 32;
    private static final int SPRITE_HEIGHT = 51;
    private static final int GUARD_SPRITE_WIDTH = 32;
    private static final int GUARD_SPRITE_HEIGHT = 64;
    private static final int[] ACTION_ROW_START = {0, 4, 8, 12, 16};
    private static final int IDLE_ACTION = 0;
    private static final int WALK_ACTION = 1;
    private static final int RUN_ACTION = 2;
    private static final int SNEAK_ACTION = 3;
    private static final int VICTORY_NEXT_Y = 335;
    private static final int VICTORY_REPLAY_Y = 395;
    private static final int VICTORY_SELECT_Y = 455;
    private static final Rectangle MENU_PLAY_BOUNDS = new Rectangle(397, 238, 166, 91);
    private static final Rectangle MENU_CUSTOMIZE_BOUNDS = new Rectangle(401, 340, 158, 57);
    private static final Rectangle MENU_SETTINGS_BOUNDS = new Rectangle(401, 403, 158, 57);
    private static final Rectangle MENU_QUIT_BOUNDS = new Rectangle(401, 466, 158, 57);
    private static final Rectangle MENU_SHOP_BOUNDS = new Rectangle(56, 52, 126, 59);
    private static final Rectangle MENU_GALLERY_BOUNDS = new Rectangle(768, 56, 136, 48);

    private final Set<String> pressedKeys = new HashSet<>();
    private final GameLoop gameLoop;
    private final LevelManager levelManager = new LevelManager();
    private final BufferedImage[][] playerSpriteFrames;
    private final BufferedImage[][][] guardSprites;
    private final Assets uiAssets;
    private final Map<BufferedImage, BufferedImage[]> tintedPlayerCache = new HashMap<>();
    private GameWorld world;
    private LevelManager.Result lastResult;
    private Screen screen = Screen.MENU;
    private int selectedLevel;
    private long animationFrame;
    private int playerDirectionRow = 1;
    private int hoverX = -1;
    private int hoverY = -1;
    private String uiToast = "";
    private MuseumMap.Painting forgeryPainting;
    private BufferedImage forgeryCanvas;
    private final java.util.List<BufferedImage> forgeryUndoList = new java.util.ArrayList<>();
    private Color forgerySelectedColor = Color.BLACK;
    private int forgeryBrushSize = 7;
    private boolean forgeryFillMode;
    private double forgeryTimeRemaining = 120.0;
    private double forgeryMaxTime = 120.0;
    private Point forgeryLastDrawPoint;
    private int forgeryAccuracyPreview;
    private boolean forgeryIsDrawing;

    public GamePanel() {
        this(loadAssets());
    }

    GamePanel(Assets assets) {
        uiAssets = assets;
        playerSpriteFrames = assets.playerSprites;
        guardSprites = assets.guardSprites;
        setPreferredSize(new java.awt.Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        setBackground(INK);
        installKeys();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                pressedKeys.clear();
            }
        });
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                Point p = toCanvasPoint(event.getX(), event.getY());
                if (p != null) {
                    if (screen == Screen.FORGERY) {
                        handleForgeryMouseDown(p.x, p.y);
                    } else {
                        handleClick(p.x, p.y);
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                Point p = toCanvasPoint(event.getX(), event.getY());
                if (p != null) {
                    hoverX = p.x;
                    hoverY = p.y;
                    if (screen == Screen.FORGERY) {
                        handleForgeryMouseDrag(p.x, p.y);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                Point p = toCanvasPoint(event.getX(), event.getY());
                if (screen == Screen.FORGERY) {
                    handleForgeryMouseUp(p == null ? -1 : p.x, p == null ? -1 : p.y);
                }
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                Point p = toCanvasPoint(event.getX(), event.getY());
                hoverX = p == null ? -1 : p.x;
                hoverY = p == null ? -1 : p.y;
                updateCursor();
                if (screen != Screen.PLAYING) repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hoverX = -1;
                hoverY = -1;
                if (screen == Screen.FORGERY) {
                    handleForgeryMouseUp(-1, -1);
                }
                setCursor(Cursor.getDefaultCursor());
                if (screen != Screen.PLAYING) repaint();
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        gameLoop = new GameLoop(this::updateGame);
    }

    private Point toCanvasPoint(int rawX, int rawY) {
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        double offsetX = (getWidth() - WIDTH * scale) / 2;
        double offsetY = (getHeight() - HEIGHT * scale) / 2;
        if (rawX < offsetX || rawX >= offsetX + WIDTH * scale
            || rawY < offsetY || rawY >= offsetY + HEIGHT * scale) {
            return null;
        }
        return new Point((int) ((rawX - offsetX) / scale), (int) ((rawY - offsetY) / scale));
    }

    private void updateCursor() {
        if (screen == Screen.FORGERY) {
            if (hoverX >= 0 && hoverY >= 0) {
                if (getForgeryCanvasScreenBounds().contains(hoverX, hoverY)) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    return;
                }
                if (getOriginalScreenBounds().contains(hoverX, hoverY) || isForgeryClickable(hoverX, hoverY)) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }
            }
            setCursor(Cursor.getDefaultCursor());
            return;
        }
        boolean clickable = false;
        if (hoverX >= 0 && hoverY >= 0) {
            if (screen == Screen.MENU) {
                clickable = MENU_PLAY_BOUNDS.contains(hoverX, hoverY)
                    || MENU_CUSTOMIZE_BOUNDS.contains(hoverX, hoverY)
                    || MENU_SETTINGS_BOUNDS.contains(hoverX, hoverY)
                    || MENU_QUIT_BOUNDS.contains(hoverX, hoverY)
                    || MENU_SHOP_BOUNDS.contains(hoverX, hoverY)
                    || MENU_GALLERY_BOUNDS.contains(hoverX, hoverY);
            } else if (screen == Screen.LEVEL_SELECT) {
                int level = levelAt(hoverX, hoverY);
                clickable = (level >= 0 && levelManager.isUnlocked(level))
                    || buttonBounds(390, 510).contains(hoverX, hoverY);
            } else if (screen == Screen.SHOP) {
                clickable = shopItemAt(hoverX, hoverY) >= 0 || buttonBounds(390, 505).contains(hoverX, hoverY);
            } else if (screen == Screen.CUSTOMIZE) {
                clickable = outfitCardAt(hoverX, hoverY) >= 0 || buttonBounds(390, 505).contains(hoverX, hoverY);
            } else if (screen == Screen.SETTINGS) {
                clickable = settingsRowAt(hoverX, hoverY) >= 0 || buttonBounds(390, 505).contains(hoverX, hoverY);
            } else if (screen == Screen.GALLERY) {
                clickable = buttonBounds(390, 505).contains(hoverX, hoverY);
            } else if (screen == Screen.PAUSED) {
                clickable = buttonBounds(390, 330).contains(hoverX, hoverY)
                    || buttonBounds(390, 390).contains(hoverX, hoverY)
                    || buttonBounds(390, 450).contains(hoverX, hoverY);
            } else if (screen == Screen.VICTORY) {
                clickable = buttonBounds(390, VICTORY_NEXT_Y).contains(hoverX, hoverY)
                    || buttonBounds(390, VICTORY_REPLAY_Y).contains(hoverX, hoverY)
                    || buttonBounds(390, VICTORY_SELECT_Y).contains(hoverX, hoverY);
            } else if (screen == Screen.GAME_OVER) {
                clickable = buttonBounds(390, 390).contains(hoverX, hoverY)
                    || buttonBounds(390, 450).contains(hoverX, hoverY);
            }
        }
        setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    static Assets loadAssets() {
        return new Assets(
            loadPlayerSpriteFrames(),
            loadGuardSprites(),
            loadOptionalUiImage("assets/ui/btn_play.png"),
            loadOptionalUiImage("assets/ui/btn_customize.png"),
            loadOptionalUiImage("assets/ui/btn_settings.png"),
            loadOptionalUiImage("assets/ui/btn_quit.png"),
            loadOptionalUiImage("assets/ui/btn_shop.png"),
            loadOptionalUiImage("assets/ui/frame_primary.png"),
            loadOptionalUiImage("assets/ui/frame_secondary.png"),
            loadOptionalUiImage("assets/ui/frame_badge.png"),
            loadOptionalUiImage("assets/ui/menu_bg.png")
        );
    }

    private static BufferedImage loadOptionalUiImage(String assetPath) {
        try {
            InputStream resource = GamePanel.class.getResourceAsStream("/" + assetPath);
            if (resource != null) {
                try (InputStream input = resource) {
                    return ImageIO.read(input);
                }
            }
            File file = findProjectAssetFrom(new File("").getAbsoluteFile().toPath(), assetPath);
            if (file == null) {
                Path classLocation = Paths.get(GamePanel.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
                if (!java.nio.file.Files.isDirectory(classLocation)) {
                    classLocation = classLocation.getParent();
                }
                file = findProjectAssetFrom(classLocation, assetPath);
            }
            return file != null && file.isFile() ? ImageIO.read(file) : null;
        } catch (IOException | URISyntaxException ignored) {
            return null;
        }
    }

    private void installKeys() {
        bindMove("W", "up", true);
        bindMove("UP", "up", true);
        bindMove("S", "down", true);
        bindMove("DOWN", "down", true);
        bindMove("A", "left", true);
        bindMove("LEFT", "left", true);
        bindMove("D", "right", true);
        bindMove("RIGHT", "right", true);
        bindMove("SHIFT", "sprint", true);
        bindMove("CONTROL", "sneak", true);
        bindMove("E", "interact", true);
        bindMove("F", "steal", true);
        bindMove("Q", "distract", true);
        bindMove("W", "up", false);
        bindMove("UP", "up", false);
        bindMove("S", "down", false);
        bindMove("DOWN", "down", false);
        bindMove("A", "left", false);
        bindMove("LEFT", "left", false);
        bindMove("D", "right", false);
        bindMove("RIGHT", "right", false);
        bindMove("SHIFT", "sprint", false);
        bindMove("CONTROL", "sneak", false);
        bindMove("E", "interact", false);
        bindMove("F", "steal", false);
        bindMove("Q", "distract", false);
        bindAction("P", "pause", () -> togglePause());
        bindAction("ESCAPE", "escape", () -> handleEscape());
        bindAction("F11", "fullScreen", Main::toggleFullScreen);
        bindAction("M", "mute", () -> {
            SoundEffects.toggleMute();
            levelManager.saveSettings();
            repaint();
        });
        bindAction("ENTER", "enter", () -> {
            if (screen == Screen.MENU) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.LEVEL_SELECT;
                updateCursor();
            } else if (screen == Screen.LEVEL_SELECT) startSelectedLevel();
            else if (screen == Screen.FORGERY) finishForgery();
            else if (screen == Screen.PAUSED) resumeGame();
            else if (screen == Screen.VICTORY || screen == Screen.GAME_OVER) startGame(selectedLevel);
            repaint();
        });
        bindAction("SPACE", "space", () -> {
            if (screen == Screen.MENU) screen = Screen.LEVEL_SELECT;
            else if (screen == Screen.LEVEL_SELECT) startSelectedLevel();
            else if (screen == Screen.FORGERY) finishForgery();
            else if (screen == Screen.PAUSED) resumeGame();
            repaint();
        });
        bindAction("Z", "undo", () -> {
            if (screen == Screen.FORGERY) undoForgeryStroke();
        });
        bindAction("B", "brushTool", () -> {
            if (screen == Screen.FORGERY) {
                forgeryFillMode = false;
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
            }
        });
        bindAction("K", "fillTool", () -> {
            if (screen == Screen.FORGERY) {
                forgeryFillMode = true;
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
            }
        });
        bindAction("R", "restart", () -> {
            if (screen == Screen.PAUSED || screen == Screen.GAME_OVER || screen == Screen.VICTORY) {
                startGame(selectedLevel);
            }
        });
    }

    private void bindMove(String key, String control, boolean down) {
        String id = control + (down ? "Down" : "Up") + key;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke((down ? "pressed " : "released ") + key), id
        );
        getActionMap().put(id, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (down) {
                    if (screen == Screen.LEVEL_SELECT && isDirectional(control)) {
                        moveLevelSelection(levelSelectionDelta(control));
                        return;
                    }
                    if (control.equals("interact") || control.equals("steal") || control.equals("distract")) {
                        if (!pressedKeys.contains(control)) {
                            worldInputEdge(control);
                        }
                    }
                    pressedKeys.add(control);
                } else {
                    pressedKeys.remove(control);
                }
            }
        });
    }

    private static boolean isDirectional(String control) {
        return control.equals("up") || control.equals("down")
            || control.equals("left") || control.equals("right");
    }

    private static int levelSelectionDelta(String control) {
        if (control.equals("left")) return -1;
        if (control.equals("right")) return 1;
        if (control.equals("up")) return -2;
        return 2;
    }

    private void worldInputEdge(String control) {
        if (screen != Screen.PLAYING || world == null) return;
        if (control.equals("interact")) world.setInteractPressed(true);
        if (control.equals("steal")) world.setStealPressed(true);
        if (control.equals("distract")) world.setDistractPressed(true);
    }

    private void bindAction(String key, String id, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed " + key), id);
        getActionMap().put(id, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private void startGame() {
        startGame(selectedLevel);
    }

    private void startGame(int levelIndex) {
        gameLoop.stop();
        pressedKeys.clear();
        selectedLevel = levelIndex;
        lastResult = null;
        world = new GameWorld(levelIndex);
        world.applyUpgrades(
            levelManager.hasUpgrade(LevelManager.UPGRADE_FAST_COPY),
            levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_THROW),
            levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_SWITCH)
        );
        screen = Screen.PLAYING;
        updateCursor();
        gameLoop.start();
        requestFocusInWindow();
        repaint();
    }

    private void resumeGame() {
        if (screen != Screen.PAUSED) return;
        screen = Screen.PLAYING;
        updateCursor();
        gameLoop.start();
        requestFocusInWindow();
        repaint();
    }

    private void togglePause() {
        if (screen == Screen.PLAYING) {
            screen = Screen.PAUSED;
            pressedKeys.clear();
            gameLoop.stop();
            updateCursor();
        } else if (screen == Screen.PAUSED) {
            resumeGame();
        }
        repaint();
    }

    private void handleEscape() {
        if (screen == Screen.FORGERY) {
            abortForgery();
            return;
        }
        if (screen == Screen.PLAYING) togglePause();
        else if (screen == Screen.LEVEL_SELECT || screen == Screen.SHOP || screen == Screen.CUSTOMIZE
            || screen == Screen.SETTINGS || screen == Screen.GALLERY) {
            uiToast = "";
            screen = Screen.MENU;
            updateCursor();
            repaint();
        } else if (screen == Screen.PAUSED || screen == Screen.VICTORY || screen == Screen.GAME_OVER) {
            gameLoop.stop();
            pressedKeys.clear();
            screen = Screen.LEVEL_SELECT;
            updateCursor();
            repaint();
        }
    }

    private void handleClick(int x, int y) {
        if (screen == Screen.MENU) {
            if (MENU_PLAY_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.LEVEL_SELECT;
                updateCursor();
                repaint();
            } else if (MENU_CUSTOMIZE_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                uiToast = "";
                screen = Screen.CUSTOMIZE;
                updateCursor();
                repaint();
            } else if (MENU_SETTINGS_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                uiToast = "";
                screen = Screen.SETTINGS;
                updateCursor();
                repaint();
            } else if (MENU_QUIT_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                System.exit(0);
            } else if (MENU_SHOP_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                uiToast = "";
                screen = Screen.SHOP;
                updateCursor();
                repaint();
            } else if (MENU_GALLERY_BOUNDS.contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                uiToast = "";
                screen = Screen.GALLERY;
                updateCursor();
                repaint();
            }
        } else if (screen == Screen.SHOP) {
            int item = shopItemAt(x, y);
            if (item >= 0) {
                handleShopClick(item);
            } else if (buttonBounds(390, 505).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        } else if (screen == Screen.CUSTOMIZE) {
            int outfit = outfitCardAt(x, y);
            if (outfit >= 0) {
                handleCustomizeClick(outfit);
            } else if (buttonBounds(390, 505).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        } else if (screen == Screen.SETTINGS) {
            int row = settingsRowAt(x, y);
            if (row >= 0) {
                handleSettingsClick(row);
            } else if (buttonBounds(390, 505).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        } else if (screen == Screen.GALLERY) {
            if (buttonBounds(390, 505).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        } else if (screen == Screen.LEVEL_SELECT) {
            int clicked = levelAt(x, y);
            if (clicked >= 0) {
                selectedLevel = clicked;
                if (levelManager.isUnlocked(clicked)) {
                    SoundEffects.play(SoundEffects.Cue.CLICK);
                    startSelectedLevel();
                } else {
                    repaint();
                }
            } else if (buttonBounds(390, 510).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.MENU;
                updateCursor();
                repaint();
            }
        } else if (screen == Screen.PAUSED) {
            if (buttonBounds(390, 330).contains(x, y)) resumeGame();
            else if (buttonBounds(390, 390).contains(x, y)) startGame();
            else if (buttonBounds(390, 450).contains(x, y)) handleEscape();
        } else if (screen == Screen.VICTORY) {
            if (buttonBounds(390, VICTORY_NEXT_Y).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                if (levelManager.isUnlocked(selectedLevel + 1)) startGame(selectedLevel + 1);
                else startGame(selectedLevel);
            } else if (buttonBounds(390, VICTORY_REPLAY_Y).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                startGame(selectedLevel);
            } else if (buttonBounds(390, VICTORY_SELECT_Y).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.LEVEL_SELECT;
                updateCursor();
                repaint();
            }
        } else if (screen == Screen.GAME_OVER) {
            if (buttonBounds(390, 390).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                startGame(selectedLevel);
            } else if (buttonBounds(390, 450).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        }
    }

    private int levelAt(int x, int y) {
        for (int i = 0; i < MuseumLevel.all().size(); i++) {
            java.awt.Rectangle bounds = levelCardBounds(i);
            if (bounds.contains(x, y)) return i;
        }
        return -1;
    }

    private static java.awt.Rectangle levelCardBounds(int index) {
        int column = index % 2;
        int row = index / 2;
        return new java.awt.Rectangle(115 + column * 375, 145 + row * 112, 350, 96);
    }

    private void moveLevelSelection(int amount) {
        if (screen != Screen.LEVEL_SELECT) return;
        int candidate = Math.max(0, Math.min(MuseumLevel.all().size() - 1, selectedLevel + amount));
        if (levelManager.isUnlocked(candidate)) selectedLevel = candidate;
        repaint();
    }

    private void startSelectedLevel() {
        if (levelManager.isUnlocked(selectedLevel)) startGame(selectedLevel);
    }

    private static java.awt.Rectangle buttonBounds(int x, int y) {
        return new java.awt.Rectangle(x, y, 180, 46);
    }

    private void updateGame(double dt) {
        if (world == null) return;
        if (screen == Screen.FORGERY) {
            updateForgery(dt);
            repaint();
            return;
        }
        if (screen != Screen.PLAYING) return;
        MuseumMap.Painting pending = world.pollForgeryRequest();
        if (pending != null) {
            startForgery(pending);
            repaint();
            return;
        }
        animationFrame++;
        GameWorld.Input input = new GameWorld.Input(
            pressedKeys.contains("up"),
            pressedKeys.contains("down"),
            pressedKeys.contains("left"),
            pressedKeys.contains("right"),
            pressedKeys.contains("sprint"),
            pressedKeys.contains("sneak"),
            pressedKeys.contains("interact"),
            pressedKeys.contains("steal"),
            false,
            false,
            false
        );
        if (input.left != input.right || input.up != input.down) {
            double dx = (input.right ? 1 : 0) - (input.left ? 1 : 0);
            double dy = (input.down ? 1 : 0) - (input.up ? 1 : 0);
            playerDirectionRow = getPlayerDirectionRow(dx, dy);
        }
        world.update(dt, input);
        if (world.isWon()) {
            lastResult = levelManager.recordCompletion(world);
            screen = Screen.VICTORY;
            gameLoop.stop();
            pressedKeys.clear();
            updateCursor();
        } else if (world.isGameOver()) {
            screen = Screen.GAME_OVER;
            gameLoop.stop();
            pressedKeys.clear();
            updateCursor();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        double offsetX = (getWidth() - WIDTH * scale) / 2;
        double offsetY = (getHeight() - HEIGHT * scale) / 2;
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (screen == Screen.MENU) drawMenu(g);
        else if (screen == Screen.LEVEL_SELECT) drawLevelSelect(g);
        else if (screen == Screen.SHOP) drawShop(g);
        else if (screen == Screen.CUSTOMIZE) drawCustomize(g);
        else if (screen == Screen.SETTINGS) drawSettings(g);
        else if (screen == Screen.GALLERY) drawGallery(g);
        else {
            if (world != null) drawGame(g, world);
            if (screen == Screen.FORGERY) drawForgeryStudio(g);
            else if (screen == Screen.PAUSED) drawOverlay(g, "HEIST PAUSED", "The guards are still on their rounds.", "CONTINUE", "RESTART HEIST", "LEVEL SELECT");
            else if (screen == Screen.VICTORY) drawVictory(g);
            else if (screen == Screen.GAME_OVER) drawOverlay(g, "HEIST BUSTED", world == null ? "" : world.getEndReason(), "TRY AGAIN", "", "LEVEL SELECT");
        }
        g.dispose();
    }

    private void drawGame(Graphics2D g, GameWorld game) {
        MuseumMap map = game.getMap();
        map.draw(g, game.getPlayerCenterX(), game.getPlayerCenterY());
        drawGuardVision(g, game.getGuards());
        drawCameraVision(g, game.getCameras());
        drawNoiseIndicators(g, game.getNoiseSystem().getEvents());
        for (MuseumMap.Painting painting : map.getPaintings()) {
            drawPainting(g, painting, painting == game.getActivePainting());
        }
        for (MuseumMap.HideSpot spot : map.getHideSpots()) {
            drawHideSpot(g, spot, game.isNear(spot), game.getHidingIn() == spot);
        }
        for (Guard guard : game.getGuards()) drawGuard(g, guard);
        drawPlayer(g, game);
        drawDistractionEffect(g, game);
        if (levelManager.isLightingEnabled()) {
            drawLighting(g, game.getPlayerCenterX(), game.getPlayerCenterY());
        }
        drawHud(g, game);
        drawMinimap(g, game);
        drawPrompt(g, game);
    }

    private void drawGuardVision(Graphics2D g, java.util.List<Guard> guards) {
        for (Guard guard : guards) {
            Point facing = guard.getFacing();
            double angle = Math.atan2(facing.y, facing.x);
            double radius = guard.getState() == Guard.State.CHASE ? 230 : 190;
            Path2D.Double cone = new Path2D.Double();
            cone.moveTo(guard.centerX(), guard.centerY());
            for (int step = 0; step <= 24; step++) {
                double a = angle - Math.toRadians(58) + Math.toRadians(116) * step / 24.0;
                java.awt.geom.Point2D.Double endpoint = world.getMap().getVisionEndpoint(
                    guard.centerX(), guard.centerY(), a, radius);
                cone.lineTo(endpoint.x, endpoint.y);
            }
            cone.closePath();
            Color tint = guard.getState() == Guard.State.CHASE
                ? new Color(210, 54, 59, 52)
                : new Color(238, 191, 101, 28);
            g.setColor(tint);
            g.fill(cone);
            g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 95));
            g.draw(cone);
        }
    }

    private void drawCameraVision(Graphics2D g, java.util.List<SecurityCamera> cameras) {
        for (SecurityCamera camera : cameras) {
            double angle = camera.getFacingAngle();
            double radius = camera.getRadius();
            Path2D.Double cone = new Path2D.Double();
            cone.moveTo(camera.getX(), camera.getY());
            for (int step = 0; step <= 24; step++) {
                double a = angle - Math.toRadians(34) + Math.toRadians(68) * step / 24.0;
                java.awt.geom.Point2D.Double endpoint = world.getMap().getVisionEndpoint(
                    camera.getX(), camera.getY(), a, radius);
                cone.lineTo(endpoint.x, endpoint.y);
            }
            cone.closePath();
            boolean disabled = world.getMap().areSecuritySystemsDisabled();
            Color tint = disabled ? new Color(90, 206, 132, 22)
                : camera.isDetecting() ? new Color(222, 44, 49, 64)
                : camera.isTracking() ? new Color(238, 181, 79, 45) : new Color(75, 181, 222, 25);
            g.setColor(tint);
            g.fill(cone);
            g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 110));
            g.draw(cone);
            int x = (int) camera.getX();
            int y = (int) camera.getY();
            g.setColor(new Color(29, 33, 42));
            g.fillRect(x - 7, y - 5, 14, 10);
            g.setColor(camera.isDetecting() ? new Color(250, 69, 64)
                : camera.isTracking() ? new Color(255, 201, 88)
                : disabled ? new Color(108, 204, 129) : new Color(120, 209, 229));
            g.fillOval(x - 3, y - 3, 6, 6);
            if (camera.isTracking()) {
                g.setColor(new Color(32, 34, 39));
                g.fillRect(x - 13, y + 8, 26, 4);
                g.setColor(camera.isDetecting() ? new Color(244, 78, 70) : new Color(247, 198, 93));
                g.fillRect(x - 12, y + 9, (int) (24 * camera.getDetectionProgress()), 2);
            }
        }
    }

    private void drawNoiseIndicators(Graphics2D g, java.util.List<NoiseSystem.NoiseEvent> events) {
        for (NoiseSystem.NoiseEvent event : events) {
            double progress = 1.0 - event.getProgress();
            int radius = 9 + (int) (Math.min(90, event.getRadius() * 0.24) * progress);
            int x = (int) Math.round(event.getX());
            int y = (int) Math.round(event.getY());
            boolean alarm = event.getSource().contains("alarm");
            Color color = alarm ? new Color(255, 88, 75) : new Color(246, 212, 139);
            int alpha = Math.max(12, 125 - (int) (progress * 90));
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            if (progress < 0.45) {
                g.fillOval(x - 3, y - 3, 6, 6);
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                g.drawString(event.getSource().toUpperCase(), x + 7, y - 6);
            }
        }
    }

    private void drawPainting(Graphics2D g, MuseumMap.Painting painting, boolean active) {
        int x = painting.getX();
        int y = painting.getY();
        int w = MuseumMap.Painting.WIDTH;
        int h = MuseumMap.Painting.HEIGHT;
        g.setColor(new Color(0, 0, 0, 95));
        g.fillRect(x - 5, y - 4, w + 10, h + 11);
        g.setColor(new Color(92, 54, 37));
        g.fillRect(x - 4, y - 4, w + 8, h + 8);
        g.setColor(new Color(205, 157, 83));
        g.fillRect(x - 2, y - 2, w + 4, h + 4);
        g.setColor(new Color(251, 218, 144));
        g.fillRect(x - 2, y - 2, w + 4, 3);
        if (!painting.isStolen()) {
            int pulse = (int) (130 + 95 * Math.sin(animationFrame * 0.12));
            g.setColor(active ? GOLD : new Color(231, 190, 105, pulse));
            g.drawRect(x - 6, y - 6, w + 12, h + 12);
        }

        // Render artwork canvas: either player's hand-painted fake or original masterpiece
        if (painting.isStolen() && painting.getFakeImage() != null) {
            g.drawImage(painting.getFakeImage(), x, y, w, h, null);
        } else if (painting.getOriginalImage() != null) {
            g.drawImage(painting.getOriginalImage(), x, y, w, h, null);
        } else {
            Color[] skies = {
                new Color(45, 93, 124), new Color(60, 106, 86), new Color(91, 67, 117)
            };
            Color sky = skies[painting.getIndex() % skies.length];
            g.setColor(sky);
            g.fillRect(x, y, w, h);
        }

        // Status badges & Guard inspection indicators
        if (painting.isStolen()) {
            if (painting.isGuardAdmired()) {
                g.setColor(new Color(38, 166, 91));
                g.fillRoundRect(x + w / 2 - 28, y - 14, 56, 12, 4, 4);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                drawCentered(g, "FOOLED " + painting.getQuality() + "%", x + w / 2, y - 5);
            } else if (painting.isGuardAlarmed() || painting.isDiscovered()) {
                g.setColor(new Color(214, 48, 49));
                g.fillRoundRect(x + w / 2 - 26, y - 14, 52, 12, 4, 4);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                drawCentered(g, "ALARM!", x + w / 2, y - 5);
            } else if (painting.isGuardSuspected()) {
                g.setColor(new Color(230, 126, 34));
                g.fillRoundRect(x + w / 2 - 28, y - 14, 56, 12, 4, 4);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                drawCentered(g, "SUSPECT", x + w / 2, y - 5);
            } else {
                g.setColor(new Color(40, 44, 55, 200));
                g.fillRoundRect(x + w / 2 - 22, y - 12, 44, 10, 4, 4);
                g.setColor(new Color(230, 230, 230));
                g.setFont(new Font("Monospaced", Font.BOLD, 7));
                drawCentered(g, "FAKE " + painting.getQuality() + "%", x + w / 2, y - 4);
            }
        } else if (active) {
            int bob = (int) Math.round(Math.sin(animationFrame * 0.16) * 3);
            int badgeW = 72;
            int badgeH = 20;
            int bx = x + w / 2 - badgeW / 2;
            int by = y - 26 + bob;

            // Translucent glowing badge container
            g.setColor(new Color(12, 15, 22, 230));
            g.fillRoundRect(bx, by, badgeW, badgeH, 6, 6);
            g.setColor(new Color(255, 215, 0));
            g.drawRoundRect(bx, by, badgeW, badgeH, 6, 6);

            // Tactile 3D Keycap for [E]
            int kx = bx + 3;
            int ky = by + 2;
            g.setColor(new Color(210, 165, 60));
            g.fillRoundRect(kx, ky, 16, 16, 3, 3);
            g.setColor(new Color(255, 238, 150));
            g.fillRoundRect(kx + 1, ky + 1, 14, 12, 2, 2);
            g.setColor(new Color(35, 25, 10));
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.drawString("E", kx + 4, ky + 11);

            // Action label
            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            g.setColor(new Color(255, 240, 200));
            g.drawString("FORGE", bx + 24, by + 14);
        }
    }

    private void drawDistractionEffect(Graphics2D g, GameWorld game) {
        if (!game.hasDistractionEffect()) return;
        double progress = 1.0 - game.getDistractionEffectProgress();
        int radius = 14 + (int) (progress * 52);
        int x = (int) Math.round(game.getDistractionX());
        int y = (int) Math.round(game.getDistractionY());
        int alpha = Math.max(0, 180 - (int) (progress * 150));
        g.setColor(new Color(246, 213, 143, alpha));
        g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        g.drawOval(x - radius / 2, y - radius / 2, radius, radius);
        g.setColor(new Color(255, 237, 191, Math.max(0, alpha - 30)));
        g.fillRect(x - 3, y - 3, 6, 6);
    }

    private void drawHideSpot(Graphics2D g, MuseumMap.HideSpot spot, boolean near, boolean occupied) {
        java.awt.Rectangle r = spot.getBounds();
        g.setColor(new Color(32, 27, 35));
        g.fillRect(r.x + 3, r.y + 5, r.width, r.height);
        g.setColor(new Color(87, 56, 45));
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(new Color(153, 101, 61));
        g.fillRect(r.x + 3, r.y + 3, r.width - 6, r.height - 6);
        g.setColor(new Color(62, 47, 43));
        g.fillRect(r.x + 7, r.y + 9, r.width - 14, r.height - 15);
        g.setColor(new Color(218, 176, 106));
        g.fillRect(r.x + 4, r.y + 5, r.width - 8, 3);
        g.setColor(occupied ? new Color(119, 232, 160) : near ? GOLD : new Color(95, 83, 74));
        g.drawRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        g.setColor(occupied ? new Color(119, 232, 160) : new Color(238, 221, 180));
        g.drawString(occupied ? "HIDDEN" : "HIDE", r.x + 5, r.y + r.height - 4);
    }

    private void drawGuard(Graphics2D g, Guard guard) {
        int x = (int) Math.round(guard.getX());
        int y = (int) Math.round(guard.getY());
        boolean chasing = guard.getState() == Guard.State.CHASE;
        Point facing = guard.getFacing();
        int direction = getGuardDirection(facing.x, facing.y);
        int action = !guard.isMoving() ? 0 : chasing ? 2 : 1;
        int ticksPerFrame = action == 0 ? 12 : action == 2 ? 4 : 7;
        int frameCount = GuardSpriteSheetExtractor.getFrameCount(action, direction);
        int frame = guard.isMoving() || action == 0
            ? (int) (animationFrame / ticksPerFrame % frameCount) : 0;
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(x + 2, y + Guard.HEIGHT - 4, Guard.WIDTH - 4, 7);
        g.drawImage(guardSprites[action][direction][frame], x - 4,
            y + Guard.HEIGHT - GUARD_SPRITE_HEIGHT,
            GUARD_SPRITE_WIDTH, GUARD_SPRITE_HEIGHT, null);
        if (guard.getState() == Guard.State.CHASE || guard.getState() == Guard.State.ALERT) {
            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(chasing ? new Color(255, 87, 76) : GOLD);
            g.drawString(chasing ? "!" : "?", x + 7, y + Guard.HEIGHT - GUARD_SPRITE_HEIGHT - 3);
        }
    }

    private static int getGuardDirection(double facingX, double facingY) {
        if (Math.abs(facingX) > Math.abs(facingY)) return facingX < 0 ? 2 : 3;
        return facingY < 0 ? 1 : 0;
    }

    private static BufferedImage[][][] loadGuardSprites() {
        try {
            BufferedImage[][][] frames = loadGuardSpriteStrips();
            if (frames != null) return frames;
        } catch (IOException exception) {
            // Fall back to the older generated atlas/sheet pipeline below.
        }

        String[] assetNames = {"idle.png", "walk.png", "run.png"};
        BufferedImage[] atlases = new BufferedImage[assetNames.length];
        try {
            for (int i = 0; i < assetNames.length; i++) {
                String assetPath = "assets/sprites/Guard/" + assetNames[i];
                InputStream resource = GamePanel.class.getResourceAsStream("/" + assetPath);
                if (resource != null) {
                    try (InputStream input = resource) {
                        atlases[i] = ImageIO.read(input);
                    }
                } else {
                    atlases[i] = ImageIO.read(findProjectAsset(assetPath));
                }
                if (atlases[i] == null) throw new IOException("Unsupported guard atlas image: " + assetPath);
            }
            return GuardSpriteSheetExtractor.loadAtlases(atlases);
        } catch (IOException exception) {
            String sheetPath = "assets/sprites/Guard/Pixel Guard Animation Sprite Sheet.png";
            try {
                InputStream resource = GamePanel.class.getResourceAsStream("/" + sheetPath);
                if (resource != null) {
                    try (InputStream input = resource) {
                        BufferedImage sheet = ImageIO.read(input);
                        if (sheet == null) throw new IOException("Unsupported guard sprite sheet image.");
                        return GuardSpriteSheetExtractor.loadFrames(sheet);
                    }
                }
                return GuardSpriteSheetExtractor.loadFrames(findProjectAsset(sheetPath));
            } catch (IOException fallbackException) {
                fallbackException.addSuppressed(exception);
                throw new IllegalStateException("Could not load guard sprites from the generated atlases "
                    + "or the original sprite sheet.", fallbackException);
            }
        }
    }

    private static BufferedImage[][][] loadGuardSpriteStrips() throws IOException {
        String[] directionFiles = {
            "Guard Front Walk.png",
            "Guard Back Walk.png",
            "Guard Left Walk.png",
            "Guard Right Walk.png"
        };

        BufferedImage idleStrip = readGuardSpriteStrip("Guard Idle.png");
        BufferedImage[] walkStrips = new BufferedImage[directionFiles.length];
        for (int i = 0; i < directionFiles.length; i++) {
            walkStrips[i] = readGuardSpriteStrip(directionFiles[i]);
        }

        BufferedImage[][][] frames = new BufferedImage[3][4][4];
        for (int direction = 0; direction < 4; direction++) {
            frames[0][direction] = splitGuardSpriteStrip(idleStrip, 4);
            frames[1][direction] = splitGuardSpriteStrip(walkStrips[direction], 4);
            frames[2][direction] = frames[1][direction].clone();
        }
        return frames;
    }

    private static BufferedImage[] splitGuardSpriteStrip(BufferedImage sheet, int frameCount) throws IOException {
        if (sheet == null) {
            throw new IOException("Guard sprite strip is missing.");
        }
        if (sheet.getWidth() <= 0 || sheet.getHeight() <= 0) {
            throw new IOException("Guard sprite strip dimensions are invalid: "
                + sheet.getWidth() + "x" + sheet.getHeight() + ".");
        }
        int frameWidth = Math.max(1, sheet.getWidth() / frameCount);
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int index = 0; index < frameCount; index++) {
            int left = index * frameWidth;
            int right = (index + 1) * frameWidth;
            frames[index] = sheet.getSubimage(left, 0, right - left, sheet.getHeight());
        }
        return frames;
    }

    private static BufferedImage readGuardSpriteStrip(String fileName) throws IOException {
        String assetPath = "assets/sprites/Guard/" + fileName;
        InputStream resource = GamePanel.class.getResourceAsStream("/" + assetPath);
        if (resource != null) {
            try (InputStream input = resource) {
                BufferedImage image = ImageIO.read(input);
                if (image == null) throw new IOException("Unsupported guard sprite image: " + assetPath);
                return image;
            }
        }

        File spriteFile = findProjectAsset(assetPath);
        BufferedImage image = ImageIO.read(spriteFile);
        if (image == null) {
            throw new IOException("Unsupported guard sprite image: " + spriteFile.getAbsolutePath());
        }
        return image;
    }

    private void drawPlayer(Graphics2D g, GameWorld game) {
        int x = (int) Math.round(game.getPlayerCenterX() - SPRITE_WIDTH / 2.0);
        int y = (int) Math.round(game.getPlayerBottomY() - SPRITE_HEIGHT);
        if (game.isHidden()) {
            g.setColor(new Color(10, 12, 16, 120));
            g.fillOval(x + 5, y + SPRITE_HEIGHT - 9, SPRITE_WIDTH - 10, 10);
            return;
        }
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(x + 8, y + SPRITE_HEIGHT - 8, SPRITE_WIDTH - 16, 7);
        BufferedImage frame = getTintedPlayerFrame(getPlayerSpriteFrame(game), levelManager.getSelectedOutfit());
        if (game.isSneaking()) {
            Composite previous = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            g.drawImage(frame, x, y, SPRITE_WIDTH, SPRITE_HEIGHT, null);
            g.setComposite(previous);
        } else {
            g.drawImage(frame, x, y, SPRITE_WIDTH, SPRITE_HEIGHT, null);
        }
    }

    private BufferedImage getPlayerSpriteFrame(GameWorld game) {
        int action;
        int ticksPerFrame;
        if (game.isSprinting()) {
            action = RUN_ACTION;
            ticksPerFrame = 4;
        } else if (game.isSneaking()) {
            action = SNEAK_ACTION;
            ticksPerFrame = 8;
        } else if (game.isMoving()) {
            action = WALK_ACTION;
            ticksPerFrame = 6;
        } else {
            action = IDLE_ACTION;
            ticksPerFrame = 8;
        }

        int frame = (int) (animationFrame / ticksPerFrame % SPRITE_STRIP_FRAME_COUNT);
        return playerSpriteFrames[ACTION_ROW_START[action] + playerDirectionRow][frame];
    }

    private static int getPlayerDirectionRow(double facingX, double facingY) {
        if (Math.abs(facingX) > Math.abs(facingY)) return facingX < 0 ? 2 : 3;
        return facingY < 0 ? 1 : 0;
    }

    private static BufferedImage[][] loadPlayerSpriteFrames() {
        try {
            BufferedImage[] idleFrames = splitSpriteStrip(readPlayerSpriteSheet("Idle.png"));
            BufferedImage[] frontWalkFrames = splitSpriteStrip(readPlayerSpriteSheet("Front Walk.png"));
            BufferedImage[] backWalkFrames = splitSpriteStrip(readPlayerSpriteSheet("Back.png"));
            BufferedImage[] leftWalkFrames = splitSpriteStrip(readPlayerSpriteSheet("Left Walk.png", "Left walk.png"));
            BufferedImage[] rightWalkFrames = splitSpriteStrip(readPlayerSpriteSheet("Right Walk.png"));

            BufferedImage[][] frames = new BufferedImage[SPRITE_ROWS][SPRITE_COLUMNS];
            BufferedImage[][] directionalWalkFrames = {
                buildAnimationFrames(frontWalkFrames),
                buildAnimationFrames(backWalkFrames),
                buildAnimationFrames(leftWalkFrames),
                buildAnimationFrames(rightWalkFrames)
            };

            for (int direction = 0; direction < 4; direction++) {
                frames[ACTION_ROW_START[IDLE_ACTION] + direction] = buildAnimationFrames(idleFrames);
            }

            for (int action = WALK_ACTION; action < ACTION_ROW_START.length; action++) {
                for (int direction = 0; direction < 4; direction++) {
                    frames[ACTION_ROW_START[action] + direction] = directionalWalkFrames[direction];
                }
            }
            return frames;
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Could not load the thief sprites. Make sure the files in assets/sprites/thief are present "
                    + "(Front Walk.png, Back.png, Left Walk.png, Right Walk.png, Idle.png).",
                exception);
        }
    }

    private static BufferedImage[] splitSpriteStrip(BufferedImage sheet) throws IOException {
        int frameCount = SPRITE_STRIP_FRAME_COUNT;
        if (sheet.getWidth() < frameCount || sheet.getHeight() <= 0) {
            throw new IOException("Unexpected thief sprite strip dimensions: "
                + sheet.getWidth() + "x" + sheet.getHeight() + ".");
        }
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int index = 0; index < frameCount; index++) {
            int left = index * sheet.getWidth() / frameCount;
            int right = (index + 1) * sheet.getWidth() / frameCount;
            frames[index] = sheet.getSubimage(left, 0, right - left, sheet.getHeight());
        }
        return frames;
    }

    private static BufferedImage[] buildAnimationFrames(BufferedImage[] frames) {
        BufferedImage[] animationFrames = new BufferedImage[SPRITE_COLUMNS];
        int sourceCount = Math.max(1, frames.length);
        for (int index = 0; index < SPRITE_COLUMNS; index++) {
            animationFrames[index] = frames[Math.min(index, sourceCount - 1)];
        }
        return animationFrames;
    }

    private static BufferedImage readPlayerSpriteSheet(String... candidateNames) throws IOException {
        IOException lastError = null;
        for (String fileName : candidateNames) {
            try {
                return readPlayerSpriteSheetSingle(fileName);
            } catch (IOException exception) {
                lastError = exception;
            }
        }
        throw lastError != null ? lastError : new IOException("No sprite sheet candidates provided.");
    }

    private static BufferedImage readPlayerSpriteSheetSingle(String fileName) throws IOException {
        String assetPath = "assets/sprites/thief/" + fileName;
        File spriteFile = findProjectAssetFrom(new File("").getAbsoluteFile().toPath(), assetPath);
        if (spriteFile == null) {
            try {
                Path classLocation = Paths.get(GamePanel.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
                if (!java.nio.file.Files.isDirectory(classLocation)) {
                    classLocation = classLocation.getParent();
                }
                spriteFile = findProjectAssetFrom(classLocation, assetPath);
            } catch (URISyntaxException exception) {
                throw new IOException("Could not locate project sprites from the game classes.", exception);
            }
        }

        BufferedImage image;
        if (spriteFile != null) {
            image = ImageIO.read(spriteFile);
            if (image == null) {
                throw new IOException("Unsupported thief sprite sheet image: " + spriteFile.getAbsolutePath());
            }
        } else {
            String resourcePath = "/" + assetPath;
            try (InputStream resource = GamePanel.class.getResourceAsStream(resourcePath)) {
                if (resource == null) throw new IOException("Game sprite not found: " + assetPath);
                image = ImageIO.read(resource);
                if (image == null) throw new IOException("Unsupported thief sprite sheet image: " + resourcePath);
            }
        }

        BufferedImage argbImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = argbImage.createGraphics();
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return argbImage;
    }

    private static File findProjectAsset(String relativePath) throws IOException {
        File asset = findProjectAssetFrom(new File("").getAbsoluteFile().toPath(), relativePath);
        if (asset != null) return asset;

        try {
            Path classLocation = Paths.get(GamePanel.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
            if (!java.nio.file.Files.isDirectory(classLocation)) {
                classLocation = classLocation.getParent();
            }
            asset = findProjectAssetFrom(classLocation, relativePath);
            if (asset != null) return asset;
        } catch (URISyntaxException exception) {
            throw new IOException("Could not locate project assets from the game classes.", exception);
        }

        throw new IOException("Game asset not found: " + relativePath
            + " (searched the working directory and game class ancestors).");
    }

    private static File findProjectAssetFrom(Path directory, String relativePath) {
        for (Path current = directory; current != null; current = current.getParent()) {
            File candidate = current.resolve(relativePath).toFile();
            if (candidate.isFile()) return candidate;
        }
        return null;
    }

    private void drawLighting(Graphics2D g, double x, double y) {
        java.awt.Rectangle playArea = new java.awt.Rectangle(0, 66, WIDTH, HEIGHT - 66);
        g.setColor(new Color(8, 10, 18, 68));
        g.fill(playArea);
        float radius = 475f;
        g.setPaint(new RadialGradientPaint(
            new Point((int) x, (int) y), radius,
            new float[]{0f, 0.32f, 0.72f, 1f},
            new Color[]{
                new Color(8, 10, 18, 0),
                new Color(8, 10, 18, 10),
                new Color(8, 10, 18, 65),
                new Color(8, 10, 18, 145)
            }
        ));
        g.fill(playArea);
    }

    private void drawHud(Graphics2D g, GameWorld game) {
        // High alert screen vignette pulse
        if (game.getSuspicion() >= 50) {
            double pulse = (Math.sin(animationFrame * 0.15) + 1.0) / 2.0;
            int alpha = (int) (18 + 45 * pulse * (game.getSuspicion() - 50) / 50.0);
            g.setColor(new Color(220, 20, 30, alpha));
            g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);
            g.drawRect(1, 1, WIDTH - 3, HEIGHT - 3);
            g.drawRect(2, 2, WIDTH - 5, HEIGHT - 5);
        }

        // Top HUD Bar background gradient with smoked glass backing
        g.setPaint(new GradientPaint(0, 0, new Color(16, 18, 26, 250), 0, 68, new Color(9, 11, 16, 252)));
        g.fillRect(0, 0, WIDTH, 68);

        // Metallic gold accent bottom border with glow
        g.setColor(new Color(45, 38, 28));
        g.fillRect(0, 66, WIDTH, 3);
        g.setPaint(new GradientPaint(0, 66, new Color(245, 215, 120), WIDTH / 2, 66, new Color(175, 130, 65)));
        g.fillRect(0, 66, WIDTH, 2);

        // 1. BRAND & MISSION (Left)
        g.setFont(new Font("Monospaced", Font.BOLD, 18));
        g.setColor(new Color(255, 222, 135));
        g.drawString("PAINTHEIST", 18, 26);

        String levelName = world != null && world.getLevel() != null ? world.getLevel().getName().toUpperCase() : "GALLERY";
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(175, 165, 180));
        g.drawString(levelName, 18, 42);

        g.setFont(new Font("Monospaced", Font.PLAIN, 9));
        g.setColor(new Color(125, 120, 135));
        g.drawString("WASD MOVE  SHIFT SPRINT  CTRL SNEAK  E FORGE  Q COIN  P PAUSE", 18, 57);

        // 2. HEIST OBJECTIVE PILL
        int objX = 175;
        int objY = 13;
        int objW = 185;
        int objH = 42;
        boolean allStolen = game.getStolenCount() == game.getTargetCount();
        Color objBg = allStolen ? new Color(18, 48, 30, 220) : new Color(24, 27, 38, 220);
        Color objBorder = allStolen ? new Color(52, 199, 89) : new Color(55, 65, 85);
        g.setColor(objBg);
        g.fillRoundRect(objX, objY, objW, objH, 8, 8);
        g.setColor(objBorder);
        g.drawRoundRect(objX, objY, objW, objH, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(allStolen ? new Color(120, 240, 160) : new Color(180, 190, 205));
        g.drawString(allStolen ? "HEIST STATUS: UNLOCKED" : "PRIMARY OBJECTIVE", objX + 10, objY + 16);

        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        if (allStolen) {
            double pulse = (Math.sin(animationFrame * 0.2) + 1.0) / 2.0;
            g.setColor(new Color(130 + (int)(125 * pulse), 255, 150));
            g.drawString("ESCAPE TO THE EXIT!", objX + 10, objY + 33);
        } else {
            g.setColor(Color.WHITE);
            g.drawString("FORGE & SWAP ARTWORK", objX + 10, objY + 33);
        }

        // 3. TARGET ART SATCHEL (Visual Mini Frames)
        int satchelX = 372;
        int satchelY = 13;
        int satchelW = 120;
        int satchelH = 42;
        g.setColor(new Color(24, 27, 38, 220));
        g.fillRoundRect(satchelX, satchelY, satchelW, satchelH, 8, 8);
        g.setColor(new Color(55, 65, 85));
        g.drawRoundRect(satchelX, satchelY, satchelW, satchelH, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(180, 190, 205));
        g.drawString("LOOT SATCHEL (" + game.getStolenCount() + "/" + game.getTargetCount() + ")", satchelX + 8, satchelY + 15);

        // Draw mini frames for each painting
        int targetCount = Math.min(5, game.getTargetCount());
        for (int i = 0; i < targetCount; i++) {
            int fx = satchelX + 10 + i * 21;
            int fy = satchelY + 20;
            boolean stolen = i < game.getStolenCount();
            g.setColor(new Color(12, 14, 20));
            g.fillRect(fx, fy, 17, 13);
            if (stolen) {
                g.setColor(new Color(38, 166, 91));
                g.fillRect(fx + 2, fy + 2, 13, 9);
                g.setColor(new Color(255, 215, 0));
                g.drawRect(fx, fy, 17, 13);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                g.drawString("✓", fx + 4, fy + 10);
            } else {
                g.setColor(new Color(95, 80, 60));
                g.drawRect(fx, fy, 17, 13);
                g.setColor(new Color(140, 130, 120));
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                g.drawString("?", fx + 6, fy + 10);
            }
        }

        // 4. MISSION CHRONOMETER
        int timeX = 502;
        int timeY = 13;
        int timeW = 105;
        int timeH = 42;
        boolean lowTime = game.getTimeLeft() < 30;
        Color timeBg = lowTime ? new Color(48, 18, 20, 230) : new Color(18, 22, 32, 230);
        Color timeBorder = lowTime ? new Color(230, 60, 60) : new Color(55, 68, 90);
        g.setColor(timeBg);
        g.fillRoundRect(timeX, timeY, timeW, timeH, 8, 8);
        g.setColor(timeBorder);
        g.drawRoundRect(timeX, timeY, timeW, timeH, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(lowTime ? new Color(255, 140, 140) : new Color(175, 185, 205));
        g.drawString("MISSION TIME", timeX + 10, timeY + 15);

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(lowTime ? new Color(255, 85, 75) : new Color(140, 235, 165));
        String timeStr = String.format("%02d:%02d", game.getTimeLeft() / 60, game.getTimeLeft() % 60);
        g.drawString(timeStr, timeX + 10, timeY + 34);

        // 5. SECURITY SUSPICION & LED GAUGE
        int suspX = 617;
        int suspY = 13;
        int suspW = 175;
        int suspH = 42;
        g.setColor(new Color(20, 23, 33, 230));
        g.fillRoundRect(suspX, suspY, suspW, suspH, 8, 8);
        g.setColor(new Color(55, 65, 85));
        g.drawRoundRect(suspX, suspY, suspW, suspH, 8, 8);

        String alert = game.getAlertStatus();
        Color alertColor = alert.equals("CALM") ? new Color(46, 204, 113)
            : alert.equals("CHASE") ? new Color(231, 76, 60) : new Color(243, 156, 18);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(180, 190, 205));
        g.drawString("SUSPICION " + game.getSuspicion() + "%", suspX + 8, suspY + 15);

        g.setColor(alertColor);
        drawCentered(g, "[" + alert + "]", suspX + suspW - 32, suspY + 15);

        // 10-Segment LED Meter
        int segments = 10;
        int lit = (int) Math.round(game.getSuspicion() / 10.0);
        int barStartX = suspX + 8;
        int barY = suspY + 22;
        int segW = 14;
        int segH = 11;
        int segGap = 2;
        for (int i = 0; i < segments; i++) {
            int sx = barStartX + i * (segW + segGap);
            if (i < lit) {
                Color c = i < 3 ? new Color(46, 204, 113) : i < 6 ? new Color(241, 196, 15) : new Color(231, 76, 60);
                g.setColor(c);
                g.fillRoundRect(sx, barY, segW, segH, 2, 2);
            } else {
                g.setColor(new Color(36, 42, 55, 140));
                g.fillRoundRect(sx, barY, segW, segH, 2, 2);
            }
            g.setColor(new Color(15, 18, 25));
            g.drawRoundRect(sx, barY, segW, segH, 2, 2);
        }

        // 6. GADGETS: COIN DISTRACTION & NOISE
        int gadgetX = 802;
        int getY = 13;
        int getW = 140;
        int getH = 42;
        g.setColor(new Color(20, 23, 33, 230));
        g.fillRoundRect(gadgetX, getY, getW, getH, 8, 8);
        g.setColor(new Color(55, 65, 85));
        g.drawRoundRect(gadgetX, getY, getW, getH, 8, 8);

        double distCd = game.getDistractionCooldown();
        boolean coinReady = distCd <= 0;
        g.setColor(coinReady ? new Color(255, 215, 0) : new Color(130, 135, 145));
        g.fillOval(gadgetX + 10, getY + 12, 18, 18);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.drawString("¢", gadgetX + 16, getY + 25);

        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(coinReady ? new Color(130, 240, 160) : new Color(200, 190, 205));
        String coinStr = coinReady ? "Q: COIN READY" : "Q: " + (int) Math.ceil(distCd) + "s COOLDOWN";
        g.drawString(coinStr, gadgetX + 34, getY + 24);

        int noises = game.getNoiseSystem().getEvents().size();
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        g.setColor(noises == 0 ? new Color(140, 150, 165) : new Color(255, 180, 90));
        g.drawString("NOISE SENSORS: " + (noises == 0 ? "QUIET" : noises + " DETECTED"), gadgetX + 12, getY + 37);
    }

    private void drawMinimap(Graphics2D g, GameWorld game) {
        int mmX = 798;
        int mmY = 78;
        int mmW = 146;
        int mmH = 92;

        // Blueprint backing
        g.setColor(new Color(10, 20, 36, 215));
        g.fillRoundRect(mmX, mmY, mmW, mmH, 8, 8);

        // Blueprint grid pattern
        g.setColor(new Color(28, 70, 115, 50));
        for (int x = mmX + 10; x < mmX + mmW; x += 12) {
            g.drawLine(x, mmY, x, mmY + mmH);
        }
        for (int y = mmY + 10; y < mmY + mmH; y += 12) {
            g.drawLine(mmX, y, mmX + mmW, y);
        }

        // Radar corner frame
        g.setColor(new Color(50, 140, 215, 160));
        g.drawRoundRect(mmX, mmY, mmW, mmH, 8, 8);

        // Mini header
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        g.setColor(new Color(110, 200, 255));
        g.drawString("RADAR // BLUEPRINT", mmX + 8, mmY + 11);

        // Coordinate scaling
        double sx = (double) (mmW - 12) / WIDTH;
        double sy = (double) (mmH - 18) / HEIGHT;
        int offX = mmX + 6;
        int offY = mmY + 14;

        // Draw Solids (Walls)
        g.setColor(new Color(40, 90, 150, 180));
        for (Rectangle solid : game.getMap().getSolids()) {
            int bx = offX + (int) (solid.x * sx);
            int by = offY + (int) (solid.y * sy);
            int bw = Math.max(2, (int) (solid.width * sx));
            int bh = Math.max(2, (int) (solid.height * sy));
            g.fillRect(bx, by, bw, bh);
        }

        // Draw Exits
        g.setColor(new Color(46, 204, 113));
        for (Rectangle exit : game.getMap().getExits()) {
            int ex = offX + (int) (exit.x * sx);
            int ey = offY + (int) (exit.y * sy);
            g.fillRect(ex, ey, Math.max(3, (int) (exit.width * sx)), Math.max(3, (int) (exit.height * sy)));
        }

        // Draw Paintings
        for (MuseumMap.Painting p : game.getMap().getPaintings()) {
            int px = offX + (int) ((p.getX() + MuseumMap.Painting.WIDTH / 2.0) * sx);
            int py = offY + (int) ((p.getY() + MuseumMap.Painting.HEIGHT / 2.0) * sy);
            if (p.isStolen()) {
                g.setColor(new Color(46, 204, 113));
                g.fillRect(px - 1, py - 1, 3, 3);
            } else {
                g.setColor(new Color(255, 215, 0));
                g.fillRect(px - 1, py - 1, 3, 3);
            }
        }

        // Draw Guards
        for (Guard guard : game.getGuards()) {
            int gx = offX + (int) (guard.centerX() * sx);
            int gy = offY + (int) (guard.centerY() * sy);
            boolean alert = guard.getState() == Guard.State.CHASE;
            g.setColor(alert ? new Color(255, 60, 60) : new Color(245, 170, 50));
            g.fillOval(gx - 2, gy - 2, 4, 4);

            // Vision cone dot
            Point facing = guard.getFacing();
            g.drawLine(gx, gy, gx + facing.x * 4, gy + facing.y * 4);
        }

        // Draw Cameras
        for (SecurityCamera cam : game.getCameras()) {
            int cx = offX + (int) (cam.getX() * sx);
            int cy = offY + (int) (cam.getY() * sy);
            g.setColor(new Color(75, 200, 240));
            g.fillOval(cx - 1, cy - 1, 3, 3);
        }

        // Draw Player Blip
        int plX = offX + (int) (game.getPlayerCenterX() * sx);
        int plY = offY + (int) (game.getPlayerCenterY() * sy);
        g.setColor(Color.WHITE);
        g.fillOval(plX - 2, plY - 2, 5, 5);

        // Sonar Radar sweep circle
        double sweep = (animationFrame * 0.08) % (Math.PI * 2);
        int sweepLen = 18;
        g.setColor(new Color(80, 210, 255, 75));
        g.drawLine(plX, plY, (int) (plX + Math.cos(sweep) * sweepLen), (int) (plY + Math.sin(sweep) * sweepLen));
    }

    private void drawPrompt(Graphics2D g, GameWorld game) {
        String prompt = game.getPrompt();
        if (prompt.isEmpty()) return;
        int w = 520;
        int x = (WIDTH - w) / 2;
        int y = HEIGHT - 54;

        // Dark frosted glass pill backing
        g.setColor(new Color(12, 14, 20, 240));
        g.fillRoundRect(x, y, w, 38, 12, 12);

        // Gold beveled border
        g.setColor(new Color(215, 170, 85));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, w, 38, 12, 12);
        g.setStroke(new BasicStroke(1.0f));

        // Floating tactile Keycap icon for [E] if present
        if (prompt.startsWith("E  ")) {
            int kx = x + 16;
            int ky = y + 8;
            g.setColor(new Color(210, 165, 60));
            g.fillRoundRect(kx, ky, 24, 22, 5, 5);
            g.setColor(new Color(255, 238, 150));
            g.fillRoundRect(kx + 1, ky + 1, 22, 18, 4, 4);
            g.setColor(new Color(35, 25, 10));
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.drawString("E", kx + 8, ky + 15);

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(new Color(255, 245, 220));
            g.drawString(prompt.substring(3).trim(), kx + 34, y + 24);
        } else {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 242, 215));
            drawCentered(g, prompt, WIDTH / 2, y + 24);
        }

        if (game.getCopyProgress() > 0 && game.getCopyProgress() < 100) {
            g.setColor(new Color(36, 40, 52));
            g.fillRoundRect(x + 14, y + 31, w - 28, 4, 2, 2);
            g.setColor(new Color(97, 202, 143));
            g.fillRoundRect(x + 14, y + 31, (w - 28) * game.getCopyProgress() / 100, 4, 2, 2);
        }
    }

    private void drawMenu(Graphics2D g) {
        if (uiAssets.menuBg != null) {
            g.drawImage(uiAssets.menuBg, 0, 0, WIDTH, HEIGHT, null);
            g.setColor(new Color(10, 12, 18, 78));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(new Color(177, 133, 78, 165));
            g.drawRect(22, 22, WIDTH - 44, HEIGHT - 44);
        } else {
            drawBackdrop(g);
            drawMuseumEmblem(g);
        }
        drawImageButton(g, uiAssets.btnShop, MENU_SHOP_BOUNDS, "SHOP", true);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(231, 190, 105));
        drawCentered(g, "CREDITS: " + levelManager.getTotalRewards(), MENU_SHOP_BOUNDS.x + MENU_SHOP_BOUNDS.width / 2, MENU_SHOP_BOUNDS.y + MENU_SHOP_BOUNDS.height + 15);
        drawBadgeButton(g, uiAssets.frameBadge, MENU_GALLERY_BOUNDS, "GALLERY");

        g.setFont(new Font("Monospaced", Font.BOLD, 44));
        g.setColor(new Color(12, 10, 16, 190));
        drawCentered(g, "PAINTHEIST", WIDTH / 2 + 2, 95);
        g.setColor(new Color(255, 219, 142));
        drawCentered(g, "PAINTHEIST", WIDTH / 2, 92);

        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(new Color(12, 10, 16, 190));
        drawCentered(g, "THE QUIETEST HEIST LEAVES THE BEST STORY", WIDTH / 2 + 1, 121);
        g.setColor(new Color(232, 224, 232));
        drawCentered(g, "THE QUIETEST HEIST LEAVES THE BEST STORY", WIDTH / 2, 120);

        drawImageButton(g, uiAssets.btnPlay, MENU_PLAY_BOUNDS, "PLAY", true);
        drawImageButton(g, uiAssets.btnCustomize, MENU_CUSTOMIZE_BOUNDS, "CUSTOMIZE", false);
        drawImageButton(g, uiAssets.btnSettings, MENU_SETTINGS_BOUNDS, "SETTINGS", false);
        drawImageButton(g, uiAssets.btnQuit, MENU_QUIT_BOUNDS, "QUIT", false);

        g.setColor(new Color(10, 12, 18, 175));
        g.fillRoundRect(WIDTH / 2 - 205, 535, 410, 22, 8, 8);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(215, 206, 212));
        drawCentered(g, "F11 TO TOGGLE FULL SCREEN   |   404Tulog Productions", WIDTH / 2, 550);
    }

    private void drawLevelSelect(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 219, 142));
        drawCentered(g, "SELECT YOUR MUSEUM", WIDTH / 2, 96);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(190, 183, 197));
        drawCentered(g, "Complete each heist to unlock the next exhibition.   CREDITS: "
            + levelManager.getTotalRewards(), WIDTH / 2, 120);
        for (int i = 0; i < MuseumLevel.all().size(); i++) {
            MuseumLevel level = MuseumLevel.get(i);
            java.awt.Rectangle card = levelCardBounds(i);
            boolean unlocked = levelManager.isUnlocked(i);
            boolean selected = i == selectedLevel;
            g.setColor(selected ? new Color(65, 53, 49) : new Color(35, 34, 41));
            g.fillRect(card.x, card.y, card.width, card.height);
            g.setColor(selected ? GOLD : unlocked ? new Color(127, 105, 78) : new Color(64, 59, 66));
            g.drawRect(card.x, card.y, card.width, card.height);
            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(unlocked ? new Color(255, 226, 167) : new Color(134, 129, 141));
            g.drawString((i + 1) + ". " + level.getName(), card.x + 14, card.y + 23);
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(unlocked ? new Color(236, 196, 124) : new Color(121, 116, 128));
            g.drawString(unlocked ? level.getDifficulty() : "LOCKED", card.x + 15, card.y + 43);
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(new Color(205, 198, 208));
            g.drawString(level.getPaintingCount() + " TARGETS   " + level.getGuards().size() + " GUARDS",
                card.x + 120, card.y + 43);
            g.setColor(new Color(158, 152, 165));
            String challenge = level.getChallenge();
            if (challenge.length() > 49) challenge = challenge.substring(0, 46) + "...";
            g.drawString(challenge, card.x + 15, card.y + 68);
            int bestScore = levelManager.getBestScore(i);
            int bestTime = levelManager.getBestTime(i);
            String best = bestScore == 0 ? "BEST --"
                : "BEST " + bestScore + "   " + bestTime / 60 + ":" + String.format("%02d", bestTime % 60);
            g.setColor(new Color(227, 211, 170));
            g.drawString(best, card.x + 15, card.y + 86);
        }
        drawButton(g, "BACK", 390, 510, false);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(165, 158, 172));
        drawCentered(g, "ARROWS / WASD SELECT   ENTER TO PLAY   ESC TO BACK", WIDTH / 2, 575);
    }

    private Rectangle shopItemBounds(int index) {
        int col = index / 3;
        int row = index % 3;
        return new Rectangle(92 + col * 404, 148 + row * 102, 372, 92);
    }

    private int shopItemAt(int x, int y) {
        for (int i = 0; i < 6; i++) {
            if (shopItemBounds(i).contains(x, y)) return i;
        }
        return -1;
    }

    private void handleShopClick(int item) {
        SoundEffects.play(SoundEffects.Cue.CLICK);
        if (item == 0) {
            if (levelManager.hasUpgrade(LevelManager.UPGRADE_FAST_COPY)) {
                uiToast = "QUICK FORGER IS ALREADY OWNED.";
            } else if (levelManager.buyUpgrade(LevelManager.UPGRADE_FAST_COPY, 20)) {
                uiToast = "UNLOCKED QUICK FORGER (+30S FORGERY TIME)!";
            } else {
                uiToast = "NEED 20 CREDITS FOR QUICK FORGER.";
            }
        } else if (item == 1) {
            if (levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_THROW)) {
                uiToast = "LONG PITCH IS ALREADY OWNED.";
            } else if (levelManager.buyUpgrade(LevelManager.UPGRADE_LONG_THROW, 15)) {
                uiToast = "UNLOCKED LONG PITCH (+35% THROW RANGE)!";
            } else {
                uiToast = "NEED 15 CREDITS FOR LONG PITCH.";
            }
        } else if (item == 2) {
            if (levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_SWITCH)) {
                uiToast = "CIRCUIT JAMMER IS ALREADY OWNED.";
            } else if (levelManager.buyUpgrade(LevelManager.UPGRADE_LONG_SWITCH, 25)) {
                uiToast = "UNLOCKED CIRCUIT JAMMER (+5s SWITCH TIME)!";
            } else {
                uiToast = "NEED 25 CREDITS FOR CIRCUIT JAMMER.";
            }
        } else {
            int outfitIndex = item - 2;
            boolean wasUnlocked = levelManager.isOutfitUnlocked(outfitIndex);
            if (levelManager.selectOrBuyOutfit(outfitIndex)) {
                uiToast = (wasUnlocked ? "EQUIPPED " : "PURCHASED & EQUIPPED ")
                    + LevelManager.OUTFIT_NAMES[outfitIndex].toUpperCase() + "!";
            } else {
                uiToast = "NEED " + LevelManager.OUTFIT_COSTS[outfitIndex] + " CREDITS FOR "
                    + LevelManager.OUTFIT_NAMES[outfitIndex].toUpperCase() + ".";
            }
        }
        updateCursor();
        repaint();
    }

    private void drawShop(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, "BLACK MARKET SHOP", WIDTH / 2, 92);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(231, 190, 105));
        drawCentered(g, "AVAILABLE HEIST CREDITS: " + levelManager.getTotalRewards(), WIDTH / 2, 118);

        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(180, 174, 188));
        g.drawString("HEIST GADGETS", 92, 139);
        g.drawString("THIEF DISGUISES", 496, 139);

        String[] names = {
            "QUICK FORGER", "LONG PITCH", "CIRCUIT JAMMER",
            LevelManager.OUTFIT_NAMES[1].toUpperCase(),
            LevelManager.OUTFIT_NAMES[2].toUpperCase(),
            LevelManager.OUTFIT_NAMES[3].toUpperCase()
        };
        String[] descs = {
            "Adds +30s bonus replication time in Forgery Studio",
            "Throw coin distractions 35% farther",
            "Laser switches stay offline 5s longer",
            "Ruby coat tint for bold midnight heists",
            "Jade coat tint for stealth specialists",
            "Gold trim coat for master art thieves"
        };
        int[] costs = {
            20, 15, 25,
            LevelManager.OUTFIT_COSTS[1], LevelManager.OUTFIT_COSTS[2], LevelManager.OUTFIT_COSTS[3]
        };
        boolean[] owned = {
            levelManager.hasUpgrade(LevelManager.UPGRADE_FAST_COPY),
            levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_THROW),
            levelManager.hasUpgrade(LevelManager.UPGRADE_LONG_SWITCH),
            levelManager.isOutfitUnlocked(1),
            levelManager.isOutfitUnlocked(2),
            levelManager.isOutfitUnlocked(3)
        };

        for (int i = 0; i < 6; i++) {
            Rectangle card = shopItemBounds(i);
            boolean hovered = hoverX >= 0 && hoverY >= 0 && card.contains(hoverX, hoverY);
            boolean equipped = i >= 3 && levelManager.getSelectedOutfit() == (i - 2);
            g.setColor(equipped ? new Color(68, 56, 46)
                : hovered ? new Color(54, 48, 60) : new Color(34, 32, 41));
            g.fillRect(card.x, card.y, card.width, card.height);
            g.setColor(equipped || hovered ? GOLD : owned[i] ? new Color(116, 168, 128) : new Color(102, 87, 74));
            g.drawRect(card.x, card.y, card.width, card.height);

            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(new Color(255, 227, 168));
            g.drawString(names[i], card.x + 14, card.y + 26);

            String status = equipped ? "EQUIPPED" : owned[i] ? "OWNED" : costs[i] + " CR";
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(equipped || owned[i] ? new Color(137, 226, 161) : new Color(240, 198, 113));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(status, card.x + card.width - 14 - fm.stringWidth(status), card.y + 26);

            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(new Color(195, 190, 202));
            g.drawString(descs[i], card.x + 14, card.y + 52);

            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(new Color(145, 140, 155));
            String hint = i < 3
                ? (owned[i] ? "ACTIVE ON ALL HEISTS" : "CLICK TO BUY FOR " + costs[i] + " CREDITS")
                : (equipped ? "CURRENTLY WORN" : owned[i] ? "CLICK TO EQUIP" : "CLICK TO UNLOCK FOR " + costs[i] + " CREDITS");
            g.drawString(hint, card.x + 14, card.y + 76);
        }

        if (!uiToast.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 224, 146));
            drawCentered(g, uiToast, WIDTH / 2, 480);
        }
        drawButton(g, "BACK", 390, 505, false);
    }

    private Rectangle outfitCardBounds(int index) {
        int col = index % 2;
        int row = index / 2;
        return new Rectangle(356 + col * 262, 154 + row * 154, 246, 138);
    }

    private int outfitCardAt(int x, int y) {
        for (int i = 0; i < LevelManager.OUTFIT_NAMES.length; i++) {
            if (outfitCardBounds(i).contains(x, y)) return i;
        }
        return -1;
    }

    private void handleCustomizeClick(int outfit) {
        SoundEffects.play(SoundEffects.Cue.CLICK);
        boolean wasUnlocked = levelManager.isOutfitUnlocked(outfit);
        if (levelManager.selectOrBuyOutfit(outfit)) {
            uiToast = (wasUnlocked ? "EQUIPPED " : "UNLOCKED & EQUIPPED ")
                + LevelManager.OUTFIT_NAMES[outfit].toUpperCase() + "!";
        } else {
            uiToast = "LOCKED: NEED " + LevelManager.OUTFIT_COSTS[outfit] + " CREDITS.";
        }
        updateCursor();
        repaint();
    }

    private void drawCustomize(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, "CUSTOMIZE THIEF", WIDTH / 2, 92);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(215, 207, 215));
        drawCentered(g, "Choose your disguise.   CREDITS: " + levelManager.getTotalRewards(), WIDTH / 2, 118);

        int selected = levelManager.getSelectedOutfit();
        g.setColor(new Color(32, 30, 39));
        g.fillRect(92, 154, 240, 292);
        g.setColor(GOLD);
        g.drawRect(92, 154, 240, 292);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(231, 190, 105));
        drawCentered(g, "LIVE PREVIEW", 212, 180);

        BufferedImage baseFrame = (playerSpriteFrames != null && playerSpriteFrames.length > 0 && playerSpriteFrames[0].length > 0)
            ? playerSpriteFrames[0][0] : null;
        if (baseFrame != null) {
            BufferedImage preview = getTintedPlayerFrame(baseFrame, selected);
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(preview, 152, 200, 120, 156, null);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
        }
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(255, 227, 168));
        drawCentered(g, LevelManager.OUTFIT_NAMES[selected].toUpperCase(), 212, 392);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(180, 174, 188));
        drawCentered(g, "ACTIVE DISGUISE", 212, 416);

        Color[] swatches = {
            new Color(74, 104, 154),
            new Color(184, 62, 68),
            new Color(62, 152, 104),
            new Color(214, 170, 74)
        };
        for (int i = 0; i < LevelManager.OUTFIT_NAMES.length; i++) {
            Rectangle card = outfitCardBounds(i);
            boolean unlocked = levelManager.isOutfitUnlocked(i);
            boolean equipped = selected == i;
            boolean hovered = hoverX >= 0 && hoverY >= 0 && card.contains(hoverX, hoverY);
            g.setColor(equipped ? new Color(65, 53, 49) : hovered ? new Color(52, 46, 58) : new Color(35, 34, 41));
            g.fillRect(card.x, card.y, card.width, card.height);
            g.setColor(equipped || hovered ? GOLD : unlocked ? new Color(127, 105, 78) : new Color(64, 59, 66));
            g.drawRect(card.x, card.y, card.width, card.height);

            g.setColor(swatches[i]);
            g.fillRect(card.x + 16, card.y + 18, 28, 28);
            g.setColor(new Color(240, 224, 186));
            g.drawRect(card.x + 16, card.y + 18, 28, 28);

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(unlocked ? new Color(255, 226, 167) : new Color(148, 142, 154));
            g.drawString(LevelManager.OUTFIT_NAMES[i], card.x + 56, card.y + 37);

            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            g.setColor(equipped ? new Color(137, 226, 161) : unlocked ? new Color(224, 198, 138) : new Color(216, 146, 110));
            String tag = equipped ? "EQUIPPED"
                : unlocked ? "UNLOCKED - CLICK TO EQUIP"
                : "LOCKED - " + LevelManager.OUTFIT_COSTS[i] + " CR";
            g.drawString(tag, card.x + 16, card.y + 82);

            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(new Color(165, 160, 174));
            g.drawString(i == 0 ? "Signature dark midnight cloak." : "Also available in the Shop.", card.x + 16, card.y + 112);
        }

        if (!uiToast.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 224, 146));
            drawCentered(g, uiToast, WIDTH / 2, 480);
        }
        drawButton(g, "BACK", 390, 505, false);
    }

    private Rectangle settingsRowBounds(int row) {
        return new Rectangle(210, 152 + row * 74, 540, 58);
    }

    private int settingsRowAt(int x, int y) {
        for (int i = 0; i < 3; i++) {
            if (settingsRowBounds(i).contains(x, y)) return i;
        }
        return -1;
    }

    private void handleSettingsClick(int row) {
        if (row == 0) {
            SoundEffects.toggleMute();
            levelManager.saveSettings();
            if (!SoundEffects.isMuted()) SoundEffects.play(SoundEffects.Cue.CLICK);
            uiToast = SoundEffects.isMuted() ? "SOUND EFFECTS MUTED." : "SOUND EFFECTS ENABLED.";
        } else if (row == 1) {
            SoundEffects.play(SoundEffects.Cue.CLICK);
            Main.toggleFullScreen();
            uiToast = Main.isFullScreen() ? "SWITCHED TO FULL SCREEN." : "SWITCHED TO WINDOWED MODE.";
        } else if (row == 2) {
            SoundEffects.play(SoundEffects.Cue.CLICK);
            levelManager.setLightingEnabled(!levelManager.isLightingEnabled());
            uiToast = levelManager.isLightingEnabled() ? "GALLERY VIGNETTE ENABLED." : "HIGH VISIBILITY LIGHTING ENABLED.";
        }
        updateCursor();
        repaint();
    }

    private void drawSettings(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, "SETTINGS", WIDTH / 2, 92);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(215, 207, 215));
        drawCentered(g, "Click any option below to toggle.", WIDTH / 2, 118);

        String[] labels = {"SOUND EFFECTS", "DISPLAY MODE", "GALLERY LIGHTING"};
        String[] values = {
            SoundEffects.isMuted() ? "MUTED" : "ON",
            Main.isFullScreen() ? "FULL SCREEN" : "WINDOWED",
            levelManager.isLightingEnabled() ? "DRAMATIC VIGNETTE" : "HIGH VISIBILITY"
        };
        for (int i = 0; i < 3; i++) {
            Rectangle row = settingsRowBounds(i);
            boolean hovered = hoverX >= 0 && hoverY >= 0 && row.contains(hoverX, hoverY);
            g.setColor(hovered ? new Color(56, 49, 62) : new Color(35, 34, 41));
            g.fillRect(row.x, row.y, row.width, row.height);
            g.setColor(hovered ? GOLD : new Color(127, 105, 78));
            g.drawRect(row.x, row.y, row.width, row.height);

            g.setFont(new Font("Monospaced", Font.BOLD, 14));
            g.setColor(new Color(255, 227, 168));
            g.drawString(labels[i], row.x + 22, row.y + 35);

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(new Color(137, 226, 161));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(values[i], row.x + row.width - 22 - fm.stringWidth(values[i]), row.y + 35);
        }

        g.setColor(new Color(30, 29, 36));
        g.fillRect(150, 384, 660, 82);
        g.setColor(new Color(92, 79, 64));
        g.drawRect(150, 384, 660, 82);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(231, 190, 105));
        drawCentered(g, "CONTROLS", WIDTH / 2, 408);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(205, 198, 208));
        drawCentered(g, "WASD / ARROWS Move   |   SHIFT Sneak   |   R Sprint   |   E Swap / Hide / Switch", WIDTH / 2, 432);
        drawCentered(g, "Q Throw Distraction   |   F Secret Passage   |   P Pause   |   M Mute   |   F11 Fullscreen", WIDTH / 2, 452);

        if (!uiToast.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 224, 146));
            drawCentered(g, uiToast, WIDTH / 2, 488);
        }
        drawButton(g, "BACK", 390, 505, false);
    }

    private void drawGallery(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 28));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, "STOLEN MASTERPIECES GALLERY", WIDTH / 2, 92);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.setColor(new Color(215, 207, 215));
        drawCentered(g, "Complete each museum heist to add its works to your private vault.", WIDTH / 2, 118);

        for (int i = 0; i < MuseumLevel.all().size(); i++) {
            MuseumLevel level = MuseumLevel.get(i);
            int y = 142 + i * 68;
            boolean unlocked = levelManager.isUnlocked(i);
            int bestScore = levelManager.getBestScore(i);
            boolean collected = bestScore > 0;
            g.setColor(collected ? new Color(46, 43, 44) : new Color(32, 31, 38));
            g.fillRect(84, y, 792, 58);
            g.setColor(collected ? GOLD : unlocked ? new Color(115, 96, 74) : new Color(64, 59, 66));
            g.drawRect(84, y, 792, 58);

            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.setColor(collected ? new Color(255, 227, 168) : unlocked ? new Color(210, 202, 214) : new Color(134, 129, 141));
            g.drawString((i + 1) + ". " + level.getName(), 100, y + 23);

            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(collected ? new Color(224, 210, 178) : new Color(148, 143, 156));
            String paintings = String.join("  |  ", level.getPaintingNames());
            if (paintings.length() > 74) paintings = paintings.substring(0, 71) + "...";
            g.drawString(paintings, 100, y + 44);

            int bestTime = levelManager.getBestTime(i);
            String badge = collected
                ? "COLLECTED  |  BEST " + bestScore + "  (" + bestTime / 60 + ":" + String.format("%02d", bestTime % 60) + ")"
                : unlocked ? "UNCLAIMED" : "LOCKED";
            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            g.setColor(collected ? new Color(137, 226, 161) : unlocked ? new Color(231, 190, 105) : new Color(134, 129, 141));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(badge, 860 - fm.stringWidth(badge), y + 34);
        }
        drawButton(g, "BACK", 390, 505, false);
    }

    private void drawVictory(Graphics2D g) {
        // Darkened museum backdrop
        g.setColor(new Color(6, 8, 12, 230));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        int nx = 195;
        int ny = 22;
        int nw = 570;
        int nh = 495;

        // Vintage newsprint paper backing
        g.setColor(new Color(244, 239, 227));
        g.fillRoundRect(nx, ny, nw, nh, 8, 8);
        g.setColor(new Color(40, 34, 28));
        g.setStroke(new BasicStroke(2.0f));
        g.drawRoundRect(nx, ny, nw, nh, 8, 8);
        g.drawRect(nx + 6, ny + 6, nw - 12, nh - 12);
        g.setStroke(new BasicStroke(1.0f));

        // Newspaper Masthead
        g.setFont(new Font("Serif", Font.BOLD, 26));
        g.setColor(new Color(25, 20, 16));
        drawCentered(g, "THE METROPOLITAN CHRONICLE", nx + nw / 2, ny + 38);

        // Dateline divider
        g.setColor(new Color(60, 50, 40));
        g.drawLine(nx + 14, ny + 46, nx + nw - 14, ny + 46);
        g.drawLine(nx + 14, ny + 49, nx + nw - 14, ny + 49);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(90, 80, 70));
        drawCentered(g, "SPECIAL CRIME REPORT  •  PRICE FIVE CENTS  •  EXTRA EDITION", nx + nw / 2, ny + 59);

        g.setColor(new Color(60, 50, 40));
        g.drawLine(nx + 14, ny + 64, nx + nw - 14, ny + 64);

        // Bold Banner Headline
        g.setFont(new Font("Serif", Font.BOLD, 17));
        g.setColor(new Color(180, 30, 25));
        drawCentered(g, "MASTERPIECE SWAPPED IN DARING MIDNIGHT HEIST!", nx + nw / 2, ny + 85);

        String museumName = (world != null && world.getLevel() != null) ? world.getLevel().getName() : "Museum Gallery";
        g.setFont(new Font("Serif", Font.ITALIC, 11));
        g.setColor(new Color(50, 45, 40));
        drawCentered(g, "Curators baffled at " + museumName + " as hand-painted forgery fools security!", nx + nw / 2, ny + 102);

        // --- LEFT: NEWSPAPER PHOTO OF FORGERY ---
        int photoX = nx + 22;
        int photoY = ny + 115;
        int photoW = 205;
        int photoH = 135;

        g.setColor(new Color(220, 215, 205));
        g.fillRect(photoX, photoY, photoW, photoH);
        g.setColor(new Color(60, 52, 42));
        g.drawRect(photoX, photoY, photoW, photoH);

        // Draw the player's actual hand-painted forgery
        BufferedImage fakePhoto = null;
        if (world != null && world.getMap() != null) {
            for (MuseumMap.Painting p : world.getMap().getPaintings()) {
                if (p.isStolen() && p.getFakeImage() != null) {
                    fakePhoto = p.getFakeImage();
                    break;
                }
            }
        }
        if (fakePhoto != null) {
            g.drawImage(fakePhoto, photoX + 4, photoY + 4, photoW - 8, photoH - 24, null);
        } else {
            g.setColor(new Color(140, 130, 115));
            g.fillRect(photoX + 4, photoY + 4, photoW - 8, photoH - 24);
        }

        g.setFont(new Font("Monospaced", Font.PLAIN, 8));
        g.setColor(new Color(80, 70, 60));
        drawCentered(g, "PHOTO: The replica hung on gallery wall", photoX + photoW / 2, photoY + photoH - 6);

        // --- RIGHT: HEIST LOOT DOSSIER ---
        int dosX = nx + 245;
        int dosY = ny + 115;
        int dosW = 295;
        int dosH = 195;

        g.setColor(new Color(236, 230, 216));
        g.fillRoundRect(dosX, dosY, dosW, dosH, 6, 6);
        g.setColor(new Color(185, 170, 150));
        g.drawRoundRect(dosX, dosY, dosW, dosH, 6, 6);

        // 3 Gold Stars with 3D drop shadow
        int stars = lastResult == null ? 0 : lastResult.getStars();
        g.setFont(new Font("Monospaced", Font.BOLD, 26));
        g.setColor(new Color(60, 45, 20, 80));
        drawCentered(g, starString(stars), dosX + dosW / 2 + 1, dosY + 31);
        g.setColor(new Color(218, 165, 32));
        drawCentered(g, starString(stars), dosX + dosW / 2, dosY + 30);

        // Ledger breakdown
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(35, 30, 25));
        drawCentered(g, "BLACK-MARKET FENCE LEDGER", dosX + dosW / 2, dosY + 56);

        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(55, 48, 42));
        int rowY = dosY + 80;
        int scoreVal = lastResult == null ? 0 : lastResult.getScore();
        int rewardVal = lastResult == null ? 0 : lastResult.getReward();
        int timeVal = lastResult == null ? 0 : lastResult.getTimeRemaining();
        int alertVal = lastResult == null ? 0 : lastResult.getDetectionIncidents();

        g.drawString("STEALTH SCORE", dosX + 18, rowY);
        g.drawString(String.format("%,d", scoreVal), dosX + dosW - 65, rowY);
        rowY += 20;

        g.drawString("HEIST REWARD", dosX + 18, rowY);
        g.setColor(new Color(38, 145, 75));
        g.drawString("+" + rewardVal + " CREDITS", dosX + dosW - 105, rowY);
        rowY += 20;

        g.setColor(new Color(55, 48, 42));
        g.drawString("TIME REMAINING", dosX + 18, rowY);
        g.drawString(timeVal + "s", dosX + dosW - 65, rowY);
        rowY += 20;

        g.drawString("GUARDS ALERTED", dosX + 18, rowY);
        g.drawString(String.valueOf(alertVal), dosX + dosW - 65, rowY);

        // Stamped Wax Appraisal Seal on Newspaper
        drawWaxAppraisalSeal(g, photoX + photoW - 14, photoY + photoH - 12, 90);

        // Action Buttons
        String nextLabel = levelManager.isUnlocked(selectedLevel + 1) ? "NEXT MUSEUM" : "REPLAY HEIST";
        drawButton(g, nextLabel, 390, VICTORY_NEXT_Y, true);
        drawButton(g, "REPLAY HEIST", 390, VICTORY_REPLAY_Y, false);
        drawButton(g, "LEVEL SELECT", 390, VICTORY_SELECT_Y, false);
    }

    private static String starString(int stars) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 3; i++) text.append(i < stars ? "★ " : "☆ ");
        return text.toString().trim();
    }

    private void drawBackdrop(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(39, 33, 42), WIDTH, HEIGHT, new Color(9, 11, 17)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(112, 82, 54));
        g.fillRect(30, 30, WIDTH - 60, HEIGHT - 60);
        g.setColor(new Color(25, 24, 31));
        g.fillRect(36, 36, WIDTH - 72, HEIGHT - 72);
        g.setColor(new Color(177, 133, 78));
        g.drawRect(43, 43, WIDTH - 86, HEIGHT - 86);
        g.setColor(new Color(54, 47, 54));
        for (int x = 70; x < WIDTH - 80; x += 56) {
            g.fillRect(x, 58, 4, HEIGHT - 116);
        }
    }

    private void drawMuseumEmblem(Graphics2D g) {
        int x = 405;
        int y = 132;
        g.setColor(new Color(78, 48, 37));
        g.fillRect(x, y, 150, 88);
        g.setColor(new Color(211, 165, 91));
        g.fillRect(x + 6, y + 6, 138, 76);
        g.setColor(new Color(38, 63, 81));
        g.fillRect(x + 13, y + 13, 124, 62);
        g.setColor(new Color(240, 198, 113));
        g.fillRect(x + 26, y + 21, 24, 22);
        g.setColor(new Color(38, 54, 65));
        g.fillRect(x + 16, y + 48, 53, 21);
        g.setColor(new Color(62, 94, 68));
        g.fillRect(x + 66, y + 40, 64, 29);
        g.setColor(new Color(224, 164, 91));
        g.fillRect(x + 83, y + 32, 14, 18);
        g.setColor(new Color(248, 220, 151));
        g.drawRect(x - 2, y - 2, 154, 92);
    }

    private void drawOverlay(Graphics2D g, String title, String subtitle, String first, String second, String third) {
        if (screen == Screen.GAME_OVER) {
            // Police detention room with alternating red/blue emergency siren light
            double strobe = (Math.sin(animationFrame * 0.2) + 1.0) / 2.0;
            g.setColor(new Color((int) (25 + 40 * strobe), 10, (int) (20 + 35 * (1.0 - strobe)), 240));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            int dx = 225;
            int dy = 55;
            int dw = 510;
            int dh = 430;

            // Police Dossier Manila Card
            g.setColor(new Color(28, 30, 38));
            g.fillRoundRect(dx, dy, dw, dh, 10, 10);
            g.setColor(new Color(210, 50, 45));
            g.setStroke(new BasicStroke(2.5f));
            g.drawRoundRect(dx, dy, dw, dh, 10, 10);
            g.setStroke(new BasicStroke(1.0f));

            // Distressed "APPREHENDED" Stamp
            g.setFont(new Font("Monospaced", Font.BOLD, 28));
            g.setColor(new Color(225, 45, 40));
            drawCentered(g, "★ BUSTED BY SECURITY ★", WIDTH / 2, dy + 42);

            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 180, 175));
            drawCentered(g, "POLICE INCIDENT REPORT // ARREST DOSSIER", WIDTH / 2, dy + 64);

            // Mugshot Box on left
            int mx = dx + 28;
            int my = dy + 85;
            int mw = 120;
            int mh = 145;
            g.setColor(new Color(15, 17, 24));
            g.fillRect(mx, my, mw, mh);
            g.setColor(new Color(75, 80, 95));
            g.drawRect(mx, my, mw, mh);

            // Height measurement lines
            g.setColor(new Color(45, 50, 65));
            for (int yl = my + 15; yl < my + mh - 20; yl += 18) {
                g.drawLine(mx + 6, yl, mx + mw - 6, yl);
            }
            // Silhouette
            g.setColor(new Color(22, 25, 34));
            g.fillOval(mx + mw / 2 - 18, my + 25, 36, 42);
            g.fillRoundRect(mx + 20, my + 65, mw - 40, 55, 12, 12);

            // Booking Placard
            g.setColor(Color.WHITE);
            g.fillRect(mx + 12, my + mh - 32, mw - 24, 20);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            drawCentered(g, "CASE #0942", mx + mw / 2, my + mh - 18);

            // Incident Details on right
            int rx = dx + 165;
            int ry = dy + 92;
            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            g.setColor(new Color(245, 215, 140));
            g.drawString("FAILURE LOG:", rx, ry);
            ry += 18;

            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(new Color(240, 235, 230));
            g.drawString(subtitle.isEmpty() ? "Detected and cornered by security guards." : subtitle, rx, ry);
            ry += 24;

            g.setColor(new Color(185, 190, 205));
            int alerts = world != null ? world.getDetectionIncidents() : 0;
            g.drawString("DETECTION ALERTS LOGGED: " + alerts, rx, ry);
            ry += 18;
            g.drawString("STATUS: SUSPECT TAKEN INTO CUSTODY", rx, ry);

            // Action Buttons
            drawButton(g, first.isEmpty() ? "TRY AGAIN" : first, 390, 390, true);
            drawButton(g, third.isEmpty() ? "LEVEL SELECT" : third, 390, 450, false);
        } else {
            // Tactical Blueprint Pause Menu
            g.setColor(new Color(8, 14, 25, 225));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            int px = 245;
            int py = 75;
            int pw = 470;
            int ph = 415;

            // Blueprint card
            g.setColor(new Color(16, 28, 48));
            g.fillRoundRect(px, py, pw, ph, 10, 10);

            // Blueprint grid
            g.setColor(new Color(30, 65, 110, 60));
            for (int x = px + 15; x < px + pw; x += 18) g.drawLine(x, py, x, py + ph);
            for (int y = py + 15; y < py + ph; y += 18) g.drawLine(px, y, px + pw, y);

            g.setColor(new Color(64, 185, 255));
            g.setStroke(new BasicStroke(2.0f));
            g.drawRoundRect(px, py, pw, ph, 10, 10);
            g.setStroke(new BasicStroke(1.0f));

            g.setFont(new Font("Monospaced", Font.BOLD, 22));
            g.setColor(new Color(110, 215, 255));
            drawCentered(g, "// HEIST OPERATION PAUSED", WIDTH / 2, py + 42);

            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(new Color(190, 220, 245));
            drawCentered(g, "Museum guards and cameras frozen in position.", WIDTH / 2, py + 68);

            // Mission briefing stats box
            int bx = px + 25;
            int by = py + 88;
            int bw = pw - 50;
            int bh = 60;
            g.setColor(new Color(10, 18, 32, 220));
            g.fillRoundRect(bx, by, bw, bh, 6, 6);
            g.setColor(new Color(45, 95, 155));
            g.drawRoundRect(bx, by, bw, bh, 6, 6);

            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(new Color(255, 215, 120));
            int stolen = world != null ? world.getStolenCount() : 0;
            int total = world != null ? world.getTargetCount() : 0;
            int timeL = world != null ? world.getTimeLeft() : 0;
            int susp = world != null ? world.getSuspicion() : 0;
            drawCentered(g, "ARTWORKS FORGED: " + stolen + "/" + total + "   |   TIME LEFT: " + timeL + "s   |   SUSPICION: " + susp + "%", bx + bw / 2, by + 24);
            g.setColor(new Color(170, 205, 235));
            drawCentered(g, "KEYBINDINGS: P RESUME  •  R RESTART  •  ESC MENU", bx + bw / 2, by + 44);

            int firstY = 330;
            if (!first.isEmpty()) drawButton(g, first, 390, firstY, true);
            if (!second.isEmpty()) drawButton(g, second, 390, 390, false);
            drawButton(g, third, 390, 450, false);
        }
    }

    private BufferedImage getTintedPlayerFrame(BufferedImage src, int outfitIndex) {
        if (src == null || outfitIndex <= 0 || outfitIndex >= LevelManager.OUTFIT_NAMES.length) {
            return src;
        }
        BufferedImage[] variants = tintedPlayerCache.computeIfAbsent(
            src, k -> new BufferedImage[LevelManager.OUTFIT_NAMES.length]);
        if (variants[outfitIndex] != null) {
            return variants[outfitIndex];
        }
        int[] targetRgb = switch (outfitIndex) {
            case 1 -> new int[]{208, 66, 72};   // Crimson Phantom
            case 2 -> new int[]{64, 178, 114};  // Emerald Forger
            default -> new int[]{226, 182, 74}; // Gilded Master
        };
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage tinted = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int argb = src.getRGB(px, py);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) continue;
                int r = (argb >>> 16) & 0xFF;
                int gr = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                boolean skinTone = r > gr + 22 && gr > b + 14 && r > 150;
                if (skinTone) {
                    tinted.setRGB(px, py, argb);
                    continue;
                }
                double lum = (0.299 * r + 0.587 * gr + 0.114 * b) / 255.0;
                double scale = Math.max(0.28, 0.35 + lum * 0.95);
                int nr = Math.min(255, (int) Math.round(r * 0.25 + targetRgb[0] * scale * 0.75));
                int ng = Math.min(255, (int) Math.round(gr * 0.25 + targetRgb[1] * scale * 0.75));
                int nb = Math.min(255, (int) Math.round(b * 0.25 + targetRgb[2] * scale * 0.75));
                tinted.setRGB(px, py, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }
        variants[outfitIndex] = tinted;
        return tinted;
    }

    private void drawImageButton(Graphics2D g, BufferedImage sprite, Rectangle bounds, String fallbackLabel, boolean highlighted) {
        boolean hovered = hoverX >= 0 && hoverY >= 0 && bounds.contains(hoverX, hoverY);
        if (sprite != null) {
            int dx = hovered ? bounds.x - 2 : bounds.x;
            int dy = hovered ? bounds.y - 2 : bounds.y;
            int dw = hovered ? bounds.width + 4 : bounds.width;
            int dh = hovered ? bounds.height + 4 : bounds.height;
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(sprite, dx, dy, dw, dh, null);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
            if (hovered) {
                g.setColor(new Color(255, 226, 150, 55));
                g.fillRoundRect(dx + 4, dy + 4, dw - 8, dh - 8, 8, 8);
                g.setColor(new Color(255, 224, 146, 190));
                g.drawRoundRect(dx + 2, dy + 2, dw - 4, dh - 4, 8, 8);
            }
            return;
        }
        g.setColor(highlighted
            ? (hovered ? new Color(145, 96, 54) : new Color(119, 79, 45))
            : (hovered ? new Color(75, 65, 78) : new Color(55, 48, 57)));
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setColor(highlighted || hovered ? new Color(231, 190, 105) : new Color(127, 105, 78));
        g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(new Color(255, 237, 199));
        drawCentered(g, fallbackLabel, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2 + 5);
    }

    private void drawBadgeButton(Graphics2D g, BufferedImage frameSprite, Rectangle bounds, String label) {
        boolean hovered = hoverX >= 0 && hoverY >= 0 && bounds.contains(hoverX, hoverY);
        if (frameSprite != null) {
            int dx = hovered ? bounds.x - 2 : bounds.x;
            int dy = hovered ? bounds.y - 1 : bounds.y;
            int dw = hovered ? bounds.width + 4 : bounds.width;
            int dh = hovered ? bounds.height + 2 : bounds.height;
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(frameSprite, dx, dy, dw, dh, null);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
            if (hovered) {
                g.setColor(new Color(255, 224, 146, 45));
                g.fillRoundRect(dx + 5, dy + 5, dw - 10, dh - 10, 6, 6);
            }
        } else {
            g.setColor(hovered ? new Color(75, 65, 78) : new Color(55, 48, 57));
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            g.setColor(hovered ? GOLD : new Color(127, 105, 78));
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(hovered ? new Color(255, 244, 212) : new Color(255, 231, 182));
        drawCentered(g, label, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2 + 4);
    }

    private void drawButton(Graphics2D g, String label, int x, int y, boolean highlighted) {
        Rectangle bounds = buttonBounds(x, y);
        boolean hovered = hoverX >= 0 && hoverY >= 0 && bounds.contains(hoverX, hoverY);
        BufferedImage frame = highlighted ? uiAssets.framePrimary : uiAssets.frameSecondary;
        if (frame != null) {
            int dx = hovered ? bounds.x - 2 : bounds.x;
            int dy = hovered ? bounds.y - 1 : bounds.y;
            int dw = hovered ? bounds.width + 4 : bounds.width;
            int dh = hovered ? bounds.height + 2 : bounds.height;
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(frame, dx, dy, dw, dh, null);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
            if (hovered) {
                g.setColor(new Color(255, 224, 146, 45));
                g.fillRoundRect(dx + 4, dy + 4, dw - 8, dh - 8, 6, 6);
            }
        } else {
            g.setColor(highlighted
                ? (hovered ? new Color(145, 96, 54) : new Color(119, 79, 45))
                : (hovered ? new Color(75, 65, 78) : new Color(55, 48, 57)));
            g.fillRect(x, y, 180, 46);
            g.setColor(highlighted || hovered ? new Color(231, 190, 105) : new Color(127, 105, 78));
            g.drawRect(x, y, 180, 46);
        }
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(hovered ? new Color(255, 246, 218) : new Color(255, 237, 199));
        drawCentered(g, label, x + 90, y + 28);
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baseline) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }

    private static final int FORGERY_CANVAS_W = 336;
    private static final int FORGERY_CANVAS_H = 204;
    private static final int ORIGINAL_W = 336;
    private static final int ORIGINAL_H = 204;
    private static final int ORIGINAL_X = 90;
    private static final int ORIGINAL_Y = 85;
    private static final int FORGERY_CANVAS_X = 510;
    private static final int FORGERY_CANVAS_Y = 85;

    private Rectangle getForgeryCanvasScreenBounds() {
        return new Rectangle(FORGERY_CANVAS_X, FORGERY_CANVAS_Y, FORGERY_CANVAS_W, FORGERY_CANVAS_H);
    }

    private Rectangle getOriginalScreenBounds() {
        return new Rectangle(ORIGINAL_X, ORIGINAL_Y, ORIGINAL_W, ORIGINAL_H);
    }

    private Rectangle getForgeryUndoButtonBounds() {
        return new Rectangle(510, 302, 75, 30);
    }

    private Rectangle getForgeryClearButtonBounds() {
        return new Rectangle(595, 302, 75, 30);
    }

    private Rectangle getForgeryBrushButtonBounds() {
        return new Rectangle(680, 302, 78, 30);
    }

    private Rectangle getForgeryFillButtonBounds() {
        return new Rectangle(768, 302, 78, 30);
    }

    private Rectangle getBrushSizeBounds(int sizeIndex) {
        int[] widths = {48, 48, 48, 58};
        int[] xs = {600, 656, 712, 768};
        return new Rectangle(xs[sizeIndex], 340, widths[sizeIndex], 26);
    }

    private Rectangle getPaletteSwatchBounds(int index) {
        int col = index % 6;
        int row = index / 6;
        return new Rectangle(90 + col * 56, 302 + row * 34, 48, 28);
    }

    private Rectangle getForgeryCancelButtonBounds() {
        return new Rectangle(480, 446, 150, 46);
    }

    private Rectangle getForgeryReplaceButtonBounds() {
        return new Rectangle(650, 446, 210, 46);
    }

    private boolean isForgeryClickable(int x, int y) {
        if (getForgeryReplaceButtonBounds().contains(x, y)) return true;
        if (getForgeryCancelButtonBounds().contains(x, y)) return true;
        if (getForgeryUndoButtonBounds().contains(x, y)) return true;
        if (getForgeryClearButtonBounds().contains(x, y)) return true;
        if (getForgeryBrushButtonBounds().contains(x, y)) return true;
        if (getForgeryFillButtonBounds().contains(x, y)) return true;
        for (int i = 0; i < 4; i++) {
            if (getBrushSizeBounds(i).contains(x, y)) return true;
        }
        Color[] availableColors = getForgeryPaletteList();
        for (int i = 0; i < availableColors.length; i++) {
            if (getPaletteSwatchBounds(i).contains(x, y)) return true;
        }
        return false;
    }

    private Color[] getForgeryPaletteList() {
        Color[] p = (forgeryPainting != null) ? forgeryPainting.getPalette() : null;
        Color[] standards = {
            Color.BLACK, Color.WHITE, ForgeryEvaluator.BLANK_CANVAS_COLOR,
            new Color(192, 57, 43), new Color(41, 128, 185), new Color(39, 174, 96),
            new Color(243, 156, 18), new Color(142, 68, 173), new Color(100, 70, 50),
            new Color(52, 73, 94), new Color(244, 208, 111), new Color(26, 188, 156)
        };
        Color[] result = new Color[12];
        int idx = 0;
        if (p != null) {
            for (Color c : p) {
                if (idx < 12 && c != null) result[idx++] = c;
            }
        }
        for (Color s : standards) {
            if (idx >= 12) break;
            boolean exists = false;
            for (int j = 0; j < idx; j++) {
                if (result[j] != null && result[j].equals(s)) { exists = true; break; }
            }
            if (!exists) result[idx++] = s;
        }
        return result;
    }

    private void startForgery(MuseumMap.Painting painting) {
        if (painting == null) return;
        this.forgeryPainting = painting;
        this.forgeryCanvas = new BufferedImage(
            MasterpieceCatalog.CANVAS_WIDTH, MasterpieceCatalog.CANVAS_HEIGHT, BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D cg = forgeryCanvas.createGraphics();
        cg.setColor(ForgeryEvaluator.BLANK_CANVAS_COLOR);
        cg.fillRect(0, 0, MasterpieceCatalog.CANVAS_WIDTH, MasterpieceCatalog.CANVAS_HEIGHT);
        cg.dispose();

        forgeryUndoList.clear();
        forgeryUndoList.add(cloneCanvas(forgeryCanvas));

        Color[] pal = painting.getPalette();
        this.forgerySelectedColor = (pal != null && pal.length > 0) ? pal[0] : Color.BLACK;
        this.forgeryBrushSize = 7;
        this.forgeryFillMode = false;
        this.forgeryIsDrawing = false;
        this.forgeryLastDrawPoint = null;

        String levelDiff = (world != null && world.getMap() != null && world.getMap().getLevel() != null)
            ? world.getMap().getLevel().getDifficulty() : "EASY";
        double baseTime = MasterpieceCatalog.getReplicationTimeSeconds(painting.getName(), levelDiff);
        if (levelManager.hasUpgrade(LevelManager.UPGRADE_FAST_COPY)) {
            baseTime += 30.0; // +30s bonus with Quick Forger upgrade
        }
        this.forgeryMaxTime = baseTime;
        this.forgeryTimeRemaining = baseTime;
        this.forgeryAccuracyPreview = ForgeryEvaluator.evaluateAccuracy(
            painting.getOriginalImage(), forgeryCanvas, painting.getPalette()
        );

        this.screen = Screen.FORGERY;
        pressedKeys.clear();
        updateCursor();
        SoundEffects.play(SoundEffects.Cue.CLICK);
    }

    private void updateForgery(double dt) {
        if (screen != Screen.FORGERY || forgeryPainting == null) return;
        forgeryTimeRemaining -= dt;
        if (forgeryTimeRemaining <= 0) {
            forgeryTimeRemaining = 0;
            finishForgery();
        }
    }

    private void finishForgery() {
        if (forgeryPainting == null || forgeryCanvas == null) {
            screen = Screen.PLAYING;
            return;
        }
        int accuracy = ForgeryEvaluator.evaluateAccuracy(
            forgeryPainting.getOriginalImage(), forgeryCanvas, forgeryPainting.getPalette()
        );
        world.applyCompletedForgery(forgeryPainting, forgeryCanvas, accuracy);
        screen = Screen.PLAYING;
        forgeryPainting = null;
        forgeryCanvas = null;
        forgeryUndoList.clear();
        updateCursor();
        repaint();
    }

    private void abortForgery() {
        screen = Screen.PLAYING;
        forgeryPainting = null;
        forgeryCanvas = null;
        forgeryUndoList.clear();
        updateCursor();
        SoundEffects.play(SoundEffects.Cue.CLICK);
        repaint();
    }

    private void undoForgeryStroke() {
        if (forgeryUndoList.size() > 1) {
            forgeryUndoList.remove(forgeryUndoList.size() - 1);
            BufferedImage prev = forgeryUndoList.get(forgeryUndoList.size() - 1);
            forgeryCanvas = cloneCanvas(prev);
            if (forgeryPainting != null && forgeryPainting.getOriginalImage() != null) {
                forgeryAccuracyPreview = ForgeryEvaluator.evaluateAccuracy(
                    forgeryPainting.getOriginalImage(), forgeryCanvas, forgeryPainting.getPalette()
                );
            }
            SoundEffects.play(SoundEffects.Cue.CLICK);
            repaint();
        }
    }

    private void clearForgeryCanvas() {
        if (forgeryCanvas == null) return;
        forgeryUndoList.add(cloneCanvas(forgeryCanvas));
        if (forgeryUndoList.size() > 15) forgeryUndoList.remove(0);
        Graphics2D cg = forgeryCanvas.createGraphics();
        cg.setColor(ForgeryEvaluator.BLANK_CANVAS_COLOR);
        cg.fillRect(0, 0, MasterpieceCatalog.CANVAS_WIDTH, MasterpieceCatalog.CANVAS_HEIGHT);
        cg.dispose();
        if (forgeryPainting != null && forgeryPainting.getOriginalImage() != null) {
            forgeryAccuracyPreview = ForgeryEvaluator.evaluateAccuracy(
                forgeryPainting.getOriginalImage(), forgeryCanvas, forgeryPainting.getPalette()
            );
        }
        SoundEffects.play(SoundEffects.Cue.CLICK);
        repaint();
    }

    private static BufferedImage cloneCanvas(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private void floodFill(BufferedImage img, int startX, int startY, Color targetColor) {
        int targetRgb = targetColor.getRGB();
        int srcRgb = img.getRGB(startX, startY);
        if (srcRgb == targetRgb) return;
        int w = img.getWidth();
        int h = img.getHeight();
        boolean[] visited = new boolean[w * h];
        int[] queue = new int[w * h];
        int head = 0, tail = 0;
        queue[tail++] = startY * w + startX;
        visited[startY * w + startX] = true;
        while (head < tail) {
            int idx = queue[head++];
            int x = idx % w;
            int y = idx / w;
            img.setRGB(x, y, targetRgb);
            if (x > 0 && !visited[idx - 1] && img.getRGB(x - 1, y) == srcRgb) {
                visited[idx - 1] = true;
                queue[tail++] = idx - 1;
            }
            if (x < w - 1 && !visited[idx + 1] && img.getRGB(x + 1, y) == srcRgb) {
                visited[idx + 1] = true;
                queue[tail++] = idx + 1;
            }
            if (y > 0 && !visited[idx - w] && img.getRGB(x, y - 1) == srcRgb) {
                visited[idx - w] = true;
                queue[tail++] = idx - w;
            }
            if (y < h - 1 && !visited[idx + w] && img.getRGB(x, y + 1) == srcRgb) {
                visited[idx + w] = true;
                queue[tail++] = idx + w;
            }
        }
    }

    private void handleForgeryMouseDown(int x, int y) {
        if (getForgeryReplaceButtonBounds().contains(x, y)) {
            finishForgery();
            return;
        }
        if (getForgeryCancelButtonBounds().contains(x, y)) {
            abortForgery();
            return;
        }
        if (getForgeryUndoButtonBounds().contains(x, y)) {
            undoForgeryStroke();
            return;
        }
        if (getForgeryClearButtonBounds().contains(x, y)) {
            clearForgeryCanvas();
            return;
        }
        if (getForgeryBrushButtonBounds().contains(x, y)) {
            forgeryFillMode = false;
            SoundEffects.play(SoundEffects.Cue.CLICK);
            repaint();
            return;
        }
        if (getForgeryFillButtonBounds().contains(x, y)) {
            forgeryFillMode = true;
            SoundEffects.play(SoundEffects.Cue.CLICK);
            repaint();
            return;
        }
        int[] sizes = {3, 7, 14, 24};
        for (int i = 0; i < sizes.length; i++) {
            if (getBrushSizeBounds(i).contains(x, y)) {
                forgeryBrushSize = sizes[i];
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
                return;
            }
        }
        Color[] availableColors = getForgeryPaletteList();
        for (int i = 0; i < availableColors.length; i++) {
            if (getPaletteSwatchBounds(i).contains(x, y)) {
                forgerySelectedColor = availableColors[i];
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
                return;
            }
        }
        // Eyedropper: Click on original masterpiece to sample its color
        if (getOriginalScreenBounds().contains(x, y) && forgeryPainting != null && forgeryPainting.getOriginalImage() != null) {
            int ox = (x - ORIGINAL_X) * MasterpieceCatalog.CANVAS_WIDTH / ORIGINAL_W;
            int oy = (y - ORIGINAL_Y) * MasterpieceCatalog.CANVAS_HEIGHT / ORIGINAL_H;
            ox = Math.max(0, Math.min(MasterpieceCatalog.CANVAS_WIDTH - 1, ox));
            oy = Math.max(0, Math.min(MasterpieceCatalog.CANVAS_HEIGHT - 1, oy));
            int rgb = forgeryPainting.getOriginalImage().getRGB(ox, oy);
            forgerySelectedColor = new Color(rgb, false);
            SoundEffects.play(SoundEffects.Cue.CLICK);
            repaint();
            return;
        }
        // Draw or fill on forgery canvas
        if (getForgeryCanvasScreenBounds().contains(x, y) && forgeryCanvas != null) {
            int cx = (x - FORGERY_CANVAS_X) * MasterpieceCatalog.CANVAS_WIDTH / FORGERY_CANVAS_W;
            int cy = (y - FORGERY_CANVAS_Y) * MasterpieceCatalog.CANVAS_HEIGHT / FORGERY_CANVAS_H;
            cx = Math.max(0, Math.min(MasterpieceCatalog.CANVAS_WIDTH - 1, cx));
            cy = Math.max(0, Math.min(MasterpieceCatalog.CANVAS_HEIGHT - 1, cy));

            forgeryUndoList.add(cloneCanvas(forgeryCanvas));
            if (forgeryUndoList.size() > 15) forgeryUndoList.remove(0);

            if (forgeryFillMode) {
                floodFill(forgeryCanvas, cx, cy, forgerySelectedColor);
                if (forgeryPainting != null && forgeryPainting.getOriginalImage() != null) {
                    forgeryAccuracyPreview = ForgeryEvaluator.evaluateAccuracy(
                        forgeryPainting.getOriginalImage(), forgeryCanvas, forgeryPainting.getPalette()
                    );
                }
                repaint();
            } else {
                forgeryIsDrawing = true;
                forgeryLastDrawPoint = new Point(cx, cy);
                Graphics2D cg = forgeryCanvas.createGraphics();
                cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                cg.setColor(forgerySelectedColor);
                cg.fillOval(cx - forgeryBrushSize / 2, cy - forgeryBrushSize / 2, forgeryBrushSize, forgeryBrushSize);
                cg.dispose();
                repaint();
            }
        }
    }

    private void handleForgeryMouseDrag(int x, int y) {
        if (!forgeryIsDrawing || forgeryCanvas == null || forgeryLastDrawPoint == null) return;
        int cx = (x - FORGERY_CANVAS_X) * MasterpieceCatalog.CANVAS_WIDTH / FORGERY_CANVAS_W;
        int cy = (y - FORGERY_CANVAS_Y) * MasterpieceCatalog.CANVAS_HEIGHT / FORGERY_CANVAS_H;
        cx = Math.max(0, Math.min(MasterpieceCatalog.CANVAS_WIDTH - 1, cx));
        cy = Math.max(0, Math.min(MasterpieceCatalog.CANVAS_HEIGHT - 1, cy));

        Graphics2D cg = forgeryCanvas.createGraphics();
        cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        cg.setColor(forgerySelectedColor);
        cg.setStroke(new BasicStroke(forgeryBrushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        cg.drawLine(forgeryLastDrawPoint.x, forgeryLastDrawPoint.y, cx, cy);
        cg.fillOval(cx - forgeryBrushSize / 2, cy - forgeryBrushSize / 2, forgeryBrushSize, forgeryBrushSize);
        cg.dispose();

        forgeryLastDrawPoint = new Point(cx, cy);
        repaint();
    }

    private void handleForgeryMouseUp(int x, int y) {
        if (forgeryIsDrawing) {
            forgeryIsDrawing = false;
            forgeryLastDrawPoint = null;
            if (forgeryPainting != null && forgeryPainting.getOriginalImage() != null && forgeryCanvas != null) {
                forgeryAccuracyPreview = ForgeryEvaluator.evaluateAccuracy(
                    forgeryPainting.getOriginalImage(), forgeryCanvas, forgeryPainting.getPalette()
                );
            }
            repaint();
        }
    }

    private void drawForgeryStudio(Graphics2D g) {
        if (forgeryPainting == null || forgeryCanvas == null) return;

        // Dark dim backdrop over the live museum heist
        g.setColor(new Color(6, 8, 14, 238));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        int wx = 55, wy = 16, ww = 850, wh = 508;

        // Mahogany wood drafting table base
        g.setPaint(new GradientPaint(wx, wy, new Color(44, 28, 20), wx, wy + wh, new Color(26, 16, 12)));
        g.fillRoundRect(wx, wy, ww, wh, 14, 14);

        // Fine horizontal wood grain lines
        g.setColor(new Color(255, 220, 180, 10));
        for (int gy = wy + 8; gy < wy + wh - 8; gy += 6) {
            g.drawLine(wx + 8, gy, wx + ww - 8, gy);
        }

        // Chamfered brass outer table rim
        g.setColor(new Color(195, 155, 75));
        g.setStroke(new BasicStroke(2.0f));
        g.drawRoundRect(wx, wy, ww, wh, 14, 14);
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(new Color(110, 80, 35));
        g.drawRoundRect(wx + 2, wy + 2, ww - 4, wh - 4, 12, 12);

        // 4 Brass corner brackets with rivets
        drawBrassBracket(g, wx + 6, wy + 6, true, true);
        drawBrassBracket(g, wx + ww - 26, wy + 6, false, true);
        drawBrassBracket(g, wx + 6, wy + wh - 26, true, false);
        drawBrassBracket(g, wx + ww - 26, wy + wh - 26, false, false);

        // --- HEADER BAR ---
        int hx = wx + 12, hy = wy + 10, hw = ww - 24, hh = 50;
        g.setPaint(new GradientPaint(hx, hy, new Color(34, 24, 18), hx, hy + hh, new Color(18, 12, 10)));
        g.fillRoundRect(hx, hy, hw, hh, 8, 8);
        g.setColor(new Color(165, 125, 60));
        g.drawRoundRect(hx, hy, hw, hh, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(new Color(255, 225, 140));
        g.drawString("ATELIER FORGERY STUDIO: " + forgeryPainting.getName().toUpperCase(), hx + 18, hy + 23);

        String levelDiff = (world != null && world.getMap() != null && world.getMap().getLevel() != null)
            ? world.getMap().getLevel().getDifficulty() : "EASY";
        String diffLabel = MasterpieceCatalog.getDifficultyLabel(forgeryPainting.getName(), levelDiff);
        int totalSecs = (int) Math.round(forgeryMaxTime);
        String headerSub = String.format("DIFFICULTY: %s (%d:%02d)  •  REPLICATE ACCURATELY TO DECEIVE GUARDS",
            diffLabel, totalSecs / 60, totalSecs % 60);

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(205, 195, 185));
        g.drawString(headerSub, hx + 18, hy + 40);

        // Timer bar & digital readout
        double ratio = Math.max(0.0, Math.min(1.0, forgeryTimeRemaining / Math.max(1.0, forgeryMaxTime)));
        int timerBarW = 190;
        int timerBarX = hx + hw - 204;
        int timerBarY = hy + 13;
        int timerBarH = 24;

        g.setColor(new Color(12, 10, 8));
        g.fillRoundRect(timerBarX - 3, timerBarY - 3, timerBarW + 6, timerBarH + 6, 6, 6);
        g.setColor(new Color(75, 58, 38));
        g.drawRoundRect(timerBarX - 3, timerBarY - 3, timerBarW + 6, timerBarH + 6, 6, 6);

        Color timerColor = forgeryTimeRemaining > 30.0 ? new Color(46, 204, 113)
            : forgeryTimeRemaining > 15.0 ? new Color(241, 196, 15) : new Color(231, 76, 60);
        g.setColor(timerColor);
        g.fillRoundRect(timerBarX, timerBarY, (int) (timerBarW * ratio), timerBarH, 4, 4);

        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(Color.WHITE);
        String timerStr;
        if (forgeryTimeRemaining >= 60.0) {
            int mins = (int) forgeryTimeRemaining / 60;
            int secs = (int) forgeryTimeRemaining % 60;
            timerStr = String.format("TIMER %d:%02d", mins, secs);
        } else {
            timerStr = String.format("TIMER %.1fs", forgeryTimeRemaining);
        }
        drawCentered(g, timerStr, timerBarX + timerBarW / 2, timerBarY + 16);

        // --- LEFT: ORIGINAL MASTERPIECE ---
        // Brass museum title placard above original
        g.setColor(new Color(36, 26, 18));
        g.fillRoundRect(ORIGINAL_X, ORIGINAL_Y - 18, ORIGINAL_W, 16, 4, 4);
        g.setColor(new Color(195, 155, 75));
        g.drawRoundRect(ORIGINAL_X, ORIGINAL_Y - 18, ORIGINAL_W, 16, 4, 4);

        String artist = MasterpieceCatalog.getArtist(forgeryPainting.getName());
        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(255, 230, 140));
        drawCentered(g, "✦ ORIGINAL EXHIBIT: " + forgeryPainting.getName().toUpperCase() + " (" + artist + ") ✦",
            ORIGINAL_X + ORIGINAL_W / 2, ORIGINAL_Y - 6);

        // Drop shadow behind museum frame
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(ORIGINAL_X - 6 + 4, ORIGINAL_Y - 6 + 4, ORIGINAL_W + 12, ORIGINAL_H + 12);

        // Gilded ornate museum frame
        g.setColor(new Color(26, 18, 12));
        g.fillRect(ORIGINAL_X - 8, ORIGINAL_Y - 8, ORIGINAL_W + 16, ORIGINAL_H + 16);
        g.setColor(new Color(212, 175, 75));
        g.drawRect(ORIGINAL_X - 6, ORIGINAL_Y - 6, ORIGINAL_W + 11, ORIGINAL_H + 11);
        g.setColor(new Color(145, 105, 38));
        g.drawRect(ORIGINAL_X - 4, ORIGINAL_Y - 4, ORIGINAL_W + 7, ORIGINAL_H + 7);
        g.setColor(new Color(255, 230, 140));
        g.drawRect(ORIGINAL_X - 2, ORIGINAL_Y - 2, ORIGINAL_W + 3, ORIGINAL_H + 3);

        if (forgeryPainting.getOriginalImage() != null) {
            g.drawImage(forgeryPainting.getOriginalImage(), ORIGINAL_X, ORIGINAL_Y, ORIGINAL_W, ORIGINAL_H, null);
        }

        // Eyedropper hint under original
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(215, 195, 170));
        g.drawString("Tip: Click any color on the original to sample with eyedropper", ORIGINAL_X, ORIGINAL_Y + ORIGINAL_H + 13);

        // --- RIGHT: FORGERY CANVAS ---
        // Header above canvas
        g.setColor(new Color(36, 26, 18));
        g.fillRoundRect(FORGERY_CANVAS_X, FORGERY_CANVAS_Y - 18, FORGERY_CANVAS_W, 16, 4, 4);
        g.setColor(new Color(195, 155, 75));
        g.drawRoundRect(FORGERY_CANVAS_X, FORGERY_CANVAS_Y - 18, FORGERY_CANVAS_W, 16, 4, 4);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(255, 230, 140));
        drawCentered(g, "✎ FORGER'S WORKBENCH — OIL ON LINEN CANVAS",
            FORGERY_CANVAS_X + FORGERY_CANVAS_W / 2, FORGERY_CANVAS_Y - 6);

        // Drop shadow behind canvas
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(FORGERY_CANVAS_X - 6 + 4, FORGERY_CANVAS_Y - 6 + 4, FORGERY_CANVAS_W + 12, FORGERY_CANVAS_H + 12);

        // Canvas wooden stretcher frame
        g.setColor(new Color(38, 28, 20));
        g.fillRect(FORGERY_CANVAS_X - 6, FORGERY_CANVAS_Y - 6, FORGERY_CANVAS_W + 12, FORGERY_CANVAS_H + 12);
        g.setColor(new Color(165, 130, 85));
        g.drawRect(FORGERY_CANVAS_X - 4, FORGERY_CANVAS_Y - 4, FORGERY_CANVAS_W + 8, FORGERY_CANVAS_H + 8);
        g.setColor(new Color(110, 85, 55));
        g.drawRect(FORGERY_CANVAS_X - 2, FORGERY_CANVAS_Y - 2, FORGERY_CANVAS_W + 4, FORGERY_CANVAS_H + 4);

        g.drawImage(forgeryCanvas, FORGERY_CANVAS_X, FORGERY_CANVAS_Y, FORGERY_CANVAS_W, FORGERY_CANVAS_H, null);

        // 4 Canvas spring clips
        drawCanvasClip(g, FORGERY_CANVAS_X - 4, FORGERY_CANVAS_Y - 4);
        drawCanvasClip(g, FORGERY_CANVAS_X + FORGERY_CANVAS_W - 14, FORGERY_CANVAS_Y - 4);
        drawCanvasClip(g, FORGERY_CANVAS_X - 4, FORGERY_CANVAS_Y + FORGERY_CANVAS_H - 8);
        drawCanvasClip(g, FORGERY_CANVAS_X + FORGERY_CANVAS_W - 14, FORGERY_CANVAS_Y + FORGERY_CANVAS_H - 8);

        // Hover cursor circle on canvas
        if (hoverX >= FORGERY_CANVAS_X && hoverX < FORGERY_CANVAS_X + FORGERY_CANVAS_W
            && hoverY >= FORGERY_CANVAS_Y && hoverY < FORGERY_CANVAS_Y + FORGERY_CANVAS_H) {
            int r = Math.max(3, (int) Math.round(forgeryBrushSize * (double) FORGERY_CANVAS_W / MasterpieceCatalog.CANVAS_WIDTH));
            g.setColor(new Color(forgerySelectedColor.getRed(), forgerySelectedColor.getGreen(), forgerySelectedColor.getBlue(), 170));
            g.fillOval(hoverX - r / 2, hoverY - r / 2, r, r);
            g.setColor(Color.WHITE);
            g.drawOval(hoverX - r / 2, hoverY - r / 2, r, r);
            g.setColor(new Color(0, 0, 0, 180));
            g.drawOval(hoverX - r / 2 - 1, hoverY - r / 2 - 1, r + 2, r + 2);
        }

        // --- PALETTE SWATCHES (Bottom Left) ---
        // Walnut palette tray backdrop
        g.setColor(new Color(22, 14, 10, 240));
        g.fillRoundRect(82, 296, 352, 74, 8, 8);
        g.setColor(new Color(145, 105, 55));
        g.drawRoundRect(82, 296, 352, 74, 8, 8);

        Color[] availableColors = getForgeryPaletteList();
        for (int i = 0; i < availableColors.length; i++) {
            Rectangle sb = getPaletteSwatchBounds(i);
            Color c = availableColors[i];
            boolean selected = c != null && c.equals(forgerySelectedColor);
            boolean hovered = hoverX >= 0 && hoverY >= 0 && sb.contains(hoverX, hoverY);

            if (c != null) {
                // Drop shadow
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRoundRect(sb.x + 1, sb.y + 2, sb.width, sb.height, 6, 6);

                // Base color fill
                g.setColor(c);
                g.fillRoundRect(sb.x, sb.y, sb.width, sb.height, 6, 6);

                // Specular gloss highlight (wet oil paint effect)
                g.setColor(new Color(255, 255, 255, 110));
                g.fillRoundRect(sb.x + 3, sb.y + 3, sb.width - 14, sb.height / 2 - 2, 4, 4);
                g.setColor(new Color(255, 255, 255, 180));
                g.fillOval(sb.x + 5, sb.y + 4, 3, 3);

                if (selected) {
                    g.setColor(new Color(255, 225, 90));
                    g.setStroke(new BasicStroke(2.5f));
                    g.drawRoundRect(sb.x - 2, sb.y - 2, sb.width + 4, sb.height + 4, 7, 7);
                    g.setStroke(new BasicStroke(1.0f));

                    // Center contrast pip
                    g.setColor((c.getRed() + c.getGreen() + c.getBlue()) > 380 ? Color.BLACK : Color.WHITE);
                    g.fillOval(sb.x + sb.width / 2 - 3, sb.y + sb.height / 2 - 3, 6, 6);
                } else if (hovered) {
                    g.setColor(Color.WHITE);
                    g.drawRoundRect(sb.x - 1, sb.y - 1, sb.width + 2, sb.height + 2, 6, 6);
                } else {
                    g.setColor(new Color(50, 40, 32));
                    g.drawRoundRect(sb.x, sb.y, sb.width, sb.height, 6, 6);
                }
            }
        }

        // --- TOOLS & BRUSHWORK (Bottom Right) ---
        // Walnut tool tray backdrop
        g.setColor(new Color(22, 14, 10, 240));
        g.fillRoundRect(502, 296, 352, 74, 8, 8);
        g.setColor(new Color(145, 105, 55));
        g.drawRoundRect(502, 296, 352, 74, 8, 8);

        drawStudioToolButton(g, getForgeryUndoButtonBounds(), "⟲ UNDO", false);
        drawStudioToolButton(g, getForgeryClearButtonBounds(), "✕ CLEAR", false);
        drawStudioToolButton(g, getForgeryBrushButtonBounds(), "✎ BRUSH", !forgeryFillMode);
        drawStudioToolButton(g, getForgeryFillButtonBounds(), "⯭ FILL", forgeryFillMode);

        // Brush Sizes
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(225, 210, 180));
        g.drawString("BRUSH SIZE:", 512, 357);

        int[] sizes = {3, 7, 14, 24};
        String[] sizeLabels = {"3px", "7px", "14px", "24px"};
        for (int i = 0; i < sizes.length; i++) {
            Rectangle bsb = getBrushSizeBounds(i);
            boolean activeSize = (forgeryBrushSize == sizes[i]);
            drawStudioBrushSizeButton(g, bsb, sizes[i], sizeLabels[i], activeSize);
        }

        // --- FORENSIC APPRAISAL STATION (Bottom Left: y = 380 to 498) ---
        int appX = 82, appY = 380, appW = 376, appH = 118;
        g.setColor(new Color(20, 15, 18, 240));
        g.fillRoundRect(appX, appY, appW, appH, 8, 8);
        g.setColor(new Color(125, 95, 55));
        g.drawRoundRect(appX, appY, appW, appH, 8, 8);

        // 3D Wax Appraisal Seal
        drawWaxAppraisalSeal(g, appX + 46, appY + appH / 2, 36, forgeryAccuracyPreview);

        // Match meter bar on right of wax seal
        int mX = appX + 96, mY = appY + 14, mW = appW - 108, mH = 26;
        g.setColor(new Color(10, 10, 14));
        g.fillRoundRect(mX, mY, mW, mH, 5, 5);
        g.setColor(new Color(65, 55, 45));
        g.drawRoundRect(mX, mY, mW, mH, 5, 5);

        int fillW = (int) (mW * Math.min(100, Math.max(0, forgeryAccuracyPreview)) / 100.0);
        Color statusColor = (forgeryAccuracyPreview >= 80) ? new Color(46, 204, 113)
            : (forgeryAccuracyPreview >= 60) ? new Color(243, 156, 18) : new Color(231, 76, 60);

        g.setPaint(new GradientPaint(mX, mY, statusColor.brighter(), mX + fillW, mY, statusColor));
        g.fillRoundRect(mX + 1, mY + 1, Math.max(0, fillW - 2), mH - 2, 4, 4);

        // 60% and 80% tolerance threshold notch lines
        int tick60 = mX + (int) (mW * 0.60);
        int tick80 = mX + (int) (mW * 0.80);
        g.setColor(new Color(255, 255, 255, 140));
        g.drawLine(tick60, mY + 2, tick60, mY + mH - 3);
        g.drawLine(tick80, mY + 2, tick80, mY + mH - 3);

        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        drawCentered(g, "ACCURACY MATCH: " + forgeryAccuracyPreview + "%", mX + mW / 2, mY + 17);

        // Threat Analysis Verdict pill under meter
        int vX = mX, vY = appY + 48, vW = mW, vH = 58;
        Color vBg = (forgeryAccuracyPreview >= 80) ? new Color(20, 50, 30, 220)
            : (forgeryAccuracyPreview >= 60) ? new Color(50, 42, 16, 220) : new Color(55, 20, 20, 220);
        Color vBorder = (forgeryAccuracyPreview >= 80) ? new Color(52, 199, 89)
            : (forgeryAccuracyPreview >= 60) ? new Color(245, 180, 40) : new Color(235, 75, 75);

        g.setColor(vBg);
        g.fillRoundRect(vX, vY, vW, vH, 6, 6);
        g.setColor(vBorder);
        g.drawRoundRect(vX, vY, vW, vH, 6, 6);

        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(statusColor);
        String verdictTitle = (forgeryAccuracyPreview >= 80) ? "✔ AUTHENTIC MASTERPIECE"
            : (forgeryAccuracyPreview >= 60) ? "⚠ PASSABLE FORGERY" : "✖ FLAWED COUNTERFEIT";
        g.drawString(verdictTitle, vX + 10, vY + 18);

        g.setFont(new Font("Monospaced", Font.PLAIN, 9));
        g.setColor(new Color(230, 225, 220));
        String verdictDesc1 = (forgeryAccuracyPreview >= 80) ? "Guards will inspect and accept without alarm."
            : (forgeryAccuracyPreview >= 60) ? "Guards may hesitate; alarms sound if suspicious."
            : "Guards detect immediately and sound the alarm!";
        g.drawString(verdictDesc1, vX + 10, vY + 34);

        g.setFont(new Font("Monospaced", Font.PLAIN, 8));
        g.setColor(new Color(175, 170, 165));
        g.drawString("Thresholds: 60% Passable  •  80% Flawless", vX + 10, vY + 48);

        // --- ACTION BUTTONS (Bottom Center/Right) ---
        drawCustomActionButton(g, getForgeryCancelButtonBounds(), "CANCEL", "ESC", false, new Color(90, 38, 38));
        drawCustomActionButton(g, getForgeryReplaceButtonBounds(), "SWAP & REPLACE", "ENTER", true, new Color(34, 120, 65));
    }

    private void drawBrassBracket(Graphics2D g, int x, int y, boolean left, boolean top) {
        int w = 20, h = 20, thick = 6;
        g.setColor(new Color(195, 155, 75));
        if (left && top) {
            g.fillRect(x, y, w, thick);
            g.fillRect(x, y, thick, h);
            drawBrassRivet(g, x + 3, y + 3);
            drawBrassRivet(g, x + w - 4, y + 3);
            drawBrassRivet(g, x + 3, y + h - 4);
        } else if (!left && top) {
            g.fillRect(x, y, w, thick);
            g.fillRect(x + w - thick, y, thick, h);
            drawBrassRivet(g, x + w - 4, y + 3);
            drawBrassRivet(g, x + 3, y + 3);
            drawBrassRivet(g, x + w - 4, y + h - 4);
        } else if (left && !top) {
            g.fillRect(x, y + h - thick, w, thick);
            g.fillRect(x, y, thick, h);
            drawBrassRivet(g, x + 3, y + h - 4);
            drawBrassRivet(g, x + w - 4, y + h - 4);
            drawBrassRivet(g, x + 3, y + 3);
        } else {
            g.fillRect(x, y + h - thick, w, thick);
            g.fillRect(x + w - thick, y, thick, h);
            drawBrassRivet(g, x + w - 4, y + h - 4);
            drawBrassRivet(g, x + 3, y + h - 4);
            drawBrassRivet(g, x + w - 4, y + 3);
        }
    }

    private void drawBrassRivet(Graphics2D g, int cx, int cy) {
        g.setColor(new Color(65, 45, 18));
        g.fillOval(cx - 3, cy - 3, 6, 6);
        g.setColor(new Color(245, 215, 125));
        g.fillOval(cx - 2, cy - 2, 4, 4);
        g.setColor(new Color(110, 80, 30));
        g.drawLine(cx - 1, cy - 1, cx + 1, cy + 1);
    }

    private void drawCanvasClip(Graphics2D g, int x, int y) {
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(x + 1, y + 2, 18, 12, 3, 3);
        g.setColor(new Color(175, 155, 125));
        g.fillRoundRect(x, y, 18, 12, 3, 3);
        g.setColor(new Color(230, 220, 200));
        g.drawRoundRect(x, y, 18, 12, 3, 3);
        g.setColor(new Color(85, 75, 65));
        g.fillOval(x + 7, y + 4, 4, 4);
    }

    private void drawWaxAppraisalSeal(Graphics2D g, int cx, int cy, int accuracy) {
        drawWaxAppraisalSeal(g, cx, cy, 32, accuracy);
    }

    private void drawWaxAppraisalSeal(Graphics2D g, int cx, int cy, int radius, int accuracy) {
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(cx - radius + 3, cy - radius + 3, radius * 2, radius * 2);

        Color waxDark = accuracy >= 80 ? new Color(110, 20, 20) : accuracy >= 60 ? new Color(130, 65, 15) : new Color(100, 25, 25);
        Color waxMid = accuracy >= 80 ? new Color(175, 35, 35) : accuracy >= 60 ? new Color(195, 115, 30) : new Color(155, 40, 40);
        Color waxLight = accuracy >= 80 ? new Color(225, 75, 75) : accuracy >= 60 ? new Color(245, 165, 60) : new Color(205, 70, 70);

        int scallops = 14;
        int scallopR = radius / 3;
        g.setColor(waxDark);
        for (int i = 0; i < scallops; i++) {
            double angle = i * (Math.PI * 2 / scallops);
            int sx = cx + (int) Math.round(Math.cos(angle) * (radius - scallopR / 2));
            int sy = cy + (int) Math.round(Math.sin(angle) * (radius - scallopR / 2));
            g.fillOval(sx - scallopR, sy - scallopR, scallopR * 2, scallopR * 2);
        }

        g.setPaint(new RadialGradientPaint(
            new Point(cx - radius / 3, cy - radius / 3),
            radius,
            new float[]{0.0f, 0.7f, 1.0f},
            new Color[]{waxLight, waxMid, waxDark}
        ));
        g.fillOval(cx - radius + 4, cy - radius + 4, (radius - 4) * 2, (radius - 4) * 2);

        g.setColor(new Color(60, 10, 10, 160));
        g.setStroke(new BasicStroke(1.5f));
        g.drawOval(cx - radius + 10, cy - radius + 10, (radius - 10) * 2, (radius - 10) * 2);
        g.setColor(new Color(255, 200, 200, 90));
        g.drawOval(cx - radius + 11, cy - radius + 11, (radius - 11) * 2, (radius - 11) * 2);
        g.setStroke(new BasicStroke(1.0f));

        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        g.setColor(new Color(255, 225, 190));
        drawCentered(g, "★ HEIST ★", cx, cy - 14);

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        drawCentered(g, accuracy + "%", cx, cy + 2);

        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        String stamp = accuracy >= 80 ? "MASTER" : accuracy >= 60 ? "PASS" : "FAKE";
        g.setColor(accuracy >= 80 ? new Color(255, 235, 160) : new Color(255, 205, 190));
        drawCentered(g, stamp, cx, cy + 15);
    }

    private void drawStudioBrushSizeButton(Graphics2D g, Rectangle bounds, int size, String label, boolean highlighted) {
        boolean hovered = hoverX >= 0 && hoverY >= 0 && bounds.contains(hoverX, hoverY);
        Color bg = highlighted ? new Color(175, 130, 60)
            : (hovered ? new Color(52, 60, 78) : new Color(32, 38, 50));
        g.setColor(bg);
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);
        g.setColor(highlighted || hovered ? new Color(255, 220, 130) : new Color(70, 80, 100));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 5, 5);

        // Brush diameter dot preview
        int dotR = Math.max(2, Math.min(6, size / 3 + 1));
        int dotX = bounds.x + 8;
        int dotY = bounds.y + bounds.height / 2;
        g.setColor(forgerySelectedColor != null ? forgerySelectedColor : Color.WHITE);
        g.fillOval(dotX - dotR, dotY - dotR, dotR * 2, dotR * 2);
        g.setColor(Color.WHITE);
        g.drawOval(dotX - dotR, dotY - dotR, dotR * 2, dotR * 2);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(highlighted ? Color.WHITE : (hovered ? new Color(255, 240, 200) : new Color(205, 215, 228)));
        g.drawString(label, bounds.x + 16 + (bounds.width - 24 - g.getFontMetrics().stringWidth(label)) / 2, bounds.y + bounds.height / 2 + 3);
    }

    private void drawStudioToolButton(Graphics2D g, Rectangle bounds, String text, boolean highlighted) {
        boolean hovered = hoverX >= 0 && hoverY >= 0 && bounds.contains(hoverX, hoverY);
        Color bg = highlighted ? new Color(175, 130, 60)
            : (hovered ? new Color(52, 60, 78) : new Color(32, 38, 50));
        g.setColor(bg);
        g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);

        // Top edge bevel
        g.setColor(new Color(255, 255, 255, highlighted ? 55 : (hovered ? 35 : 15)));
        g.fillRoundRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height / 2 - 1, 4, 4);

        g.setColor(highlighted || hovered ? new Color(255, 220, 130) : new Color(70, 80, 100));
        g.setStroke(new BasicStroke(highlighted ? 1.8f : 1.0f));
        g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 6, 6);
        g.setStroke(new BasicStroke(1.0f));

        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(highlighted ? Color.WHITE : (hovered ? new Color(255, 240, 200) : new Color(200, 210, 225)));
        drawCentered(g, text, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2 + 4);
    }

    private void drawCustomActionButton(Graphics2D g, Rectangle bounds, String text, String shortcut, boolean primary, Color baseColor) {
        boolean hovered = hoverX >= 0 && hoverY >= 0 && bounds.contains(hoverX, hoverY);
        int dx = hovered ? bounds.x - 1 : bounds.x;
        int dy = hovered ? bounds.y - 1 : bounds.y;
        int dw = hovered ? bounds.width + 2 : bounds.width;
        int dh = hovered ? bounds.height + 2 : bounds.height;

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 120));
        g.fillRoundRect(dx + 2, dy + 3, dw, dh, 8, 8);

        // Main fill
        Color bg = hovered ? baseColor.brighter() : baseColor;
        g.setColor(bg);
        g.fillRoundRect(dx, dy, dw, dh, 8, 8);

        // Bevel highlight
        g.setColor(new Color(255, 255, 255, hovered ? 60 : 35));
        g.fillRoundRect(dx + 2, dy + 2, dw - 4, dh / 2 - 2, 6, 6);

        Color border = primary ? (hovered ? new Color(255, 240, 160) : new Color(235, 195, 110))
            : (hovered ? new Color(215, 170, 170) : new Color(145, 100, 100));
        g.setColor(border);
        g.setStroke(new BasicStroke(primary ? 2.0f : 1.5f));
        g.drawRoundRect(dx, dy, dw, dh, 8, 8);
        g.setStroke(new BasicStroke(1.0f));

        // Main text & shortcut badge
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(hovered ? Color.WHITE : new Color(250, 245, 235));
        int textW = g.getFontMetrics().stringWidth(text);
        int totalW = textW + (shortcut.isEmpty() ? 0 : 42);
        int startX = bounds.x + (bounds.width - totalW) / 2;
        g.drawString(text, startX, bounds.y + bounds.height / 2 + 4);

        if (!shortcut.isEmpty()) {
            int kx = startX + textW + 8;
            int ky = bounds.y + bounds.height / 2 - 9;
            int kw = Math.max(26, g.getFontMetrics().stringWidth(shortcut) + 12);
            int kh = 18;

            g.setColor(new Color(18, 22, 30, 210));
            g.fillRoundRect(kx, ky, kw, kh, 4, 4);
            g.setColor(primary ? new Color(255, 215, 110) : new Color(200, 170, 170));
            g.drawRoundRect(kx, ky, kw, kh, 4, 4);

            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            g.setColor(Color.WHITE);
            drawCentered(g, shortcut, kx + kw / 2, ky + 12);
        }
    }
}
