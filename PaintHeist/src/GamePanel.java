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
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
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
        MENU, LEVEL_SELECT, SHOP, CUSTOMIZE, SETTINGS, GALLERY, PLAYING, FORGERY, PAUSED, VICTORY, GAME_OVER, CURATOR_INSPECTION
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
    private static final int PAUSE_BTN_CONTINUE_Y = 236;
    private static final int PAUSE_BTN_RESTART_Y = 296;
    private static final int PAUSE_BTN_SELECT_Y = 356;
    private static final int GAMEOVER_BTN_RETRY_Y = 370;
    private static final int GAMEOVER_BTN_SELECT_Y = 430;
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
    private int shopTab = 0;
    private int gallerySelectedWing = 0;
    private String galleryInspectedPainting = null;
    private int customizePreviewDirection = 0;
    private int customizeTab = 0;
    private double cameraX;
    private double cameraY;
    private MuseumMap.Painting forgeryPainting;
    private BufferedImage forgeryCanvas;
    private final java.util.List<BufferedImage> forgeryUndoList = new java.util.ArrayList<>();
    private final java.util.List<BufferedImage> forgeryRedoList = new java.util.ArrayList<>();
    private Color forgerySelectedColor = Color.BLACK;
    private Color forgeryPreviousColor = Color.WHITE;
    private int forgeryBrushSize = 7;
    private boolean forgeryFillMode;
    private boolean forgeryZoomed;
    private double forgeryTimeRemaining = 300.0;
    private double forgeryMaxTime = 300.0;
    private Point forgeryLastDrawPoint;
    private int forgeryAccuracyPreview;
    private ForgeryEvaluator.EvaluationBreakdown forgeryBreakdownPreview;
    private boolean forgeryIsDrawing;
    private double brushSoundCooldown;

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
                clickable = shopItemAt(hoverX, hoverY) >= 0 || buttonBounds(390, 508).contains(hoverX, hoverY);
            } else if (screen == Screen.CUSTOMIZE) {
                clickable = customizeCardAt(hoverX, hoverY) >= 0
                    || customizeTabBounds(0).contains(hoverX, hoverY)
                    || customizeTabBounds(1).contains(hoverX, hoverY)
                    || customizeRotateLeftBounds().contains(hoverX, hoverY)
                    || customizeRotateRightBounds().contains(hoverX, hoverY)
                    || buttonBounds(390, 508).contains(hoverX, hoverY);
            } else if (screen == Screen.SETTINGS) {
                clickable = settingsRowAt(hoverX, hoverY) >= 0 || buttonBounds(390, 508).contains(hoverX, hoverY);
            } else if (screen == Screen.GALLERY) {
                if (galleryInspectedPainting != null) {
                    clickable = galleryInspectCloseBounds().contains(hoverX, hoverY)
                        || !new Rectangle(150, 35, 660, 530).contains(hoverX, hoverY);
                } else {
                    clickable = galleryWingTabAt(hoverX, hoverY) >= 0
                        || galleryPaintingAt(hoverX, hoverY) != null
                        || buttonBounds(390, 508).contains(hoverX, hoverY);
                }
            } else if (screen == Screen.PAUSED) {
                clickable = buttonBounds(390, PAUSE_BTN_CONTINUE_Y).contains(hoverX, hoverY)
                    || buttonBounds(390, PAUSE_BTN_RESTART_Y).contains(hoverX, hoverY)
                    || buttonBounds(390, PAUSE_BTN_SELECT_Y).contains(hoverX, hoverY);
            } else if (screen == Screen.CURATOR_INSPECTION) {
                clickable = buttonBounds(390, 508).contains(hoverX, hoverY);
            } else if (screen == Screen.VICTORY) {
                clickable = buttonBounds(390, VICTORY_NEXT_Y).contains(hoverX, hoverY)
                    || buttonBounds(390, VICTORY_REPLAY_Y).contains(hoverX, hoverY)
                    || buttonBounds(390, VICTORY_SELECT_Y).contains(hoverX, hoverY);
            } else if (screen == Screen.GAME_OVER) {
                clickable = buttonBounds(390, GAMEOVER_BTN_RETRY_Y).contains(hoverX, hoverY)
                    || buttonBounds(390, GAMEOVER_BTN_SELECT_Y).contains(hoverX, hoverY);
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
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> {
            if (!isShowing()) return false;
            int id = event.getID();
            int code = event.getKeyCode();
            if (id == KeyEvent.KEY_PRESSED) {
                handleRawKeyPressed(code);
            } else if (id == KeyEvent.KEY_RELEASED) {
                handleRawKeyReleased(code);
            }
            return false;
        });

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
            else if (screen == Screen.CURATOR_INSPECTION) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.VICTORY;
                updateCursor();
            } else if (screen == Screen.VICTORY || screen == Screen.GAME_OVER) startGame(selectedLevel);
            repaint();
        });
        bindAction("SPACE", "space", () -> {
            if (screen == Screen.MENU) screen = Screen.LEVEL_SELECT;
            else if (screen == Screen.LEVEL_SELECT) startSelectedLevel();
            else if (screen == Screen.FORGERY) finishForgery();
            else if (screen == Screen.PAUSED) resumeGame();
            else if (screen == Screen.CURATOR_INSPECTION) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.VICTORY;
                updateCursor();
            }
            repaint();
        });
        bindAction("Z", "undo", () -> {
            if (screen == Screen.FORGERY) undoForgeryStroke();
        });
        bindAction("Y", "redo", () -> {
            if (screen == Screen.FORGERY) redoForgeryStroke();
        });
        bindAction("X", "zoomTool", () -> {
            if (screen == Screen.FORGERY) {
                forgeryZoomed = !forgeryZoomed;
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
            }
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

    private void handleRawKeyPressed(int code) {
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            if (screen == Screen.LEVEL_SELECT) moveLevelSelection(levelSelectionDelta("up"));
            pressedKeys.add("up");
        } else if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            if (screen == Screen.LEVEL_SELECT) moveLevelSelection(levelSelectionDelta("down"));
            pressedKeys.add("down");
        } else if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
            if (screen == Screen.LEVEL_SELECT) moveLevelSelection(levelSelectionDelta("left"));
            pressedKeys.add("left");
        } else if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
            if (screen == Screen.LEVEL_SELECT) moveLevelSelection(levelSelectionDelta("right"));
            pressedKeys.add("right");
        } else if (code == KeyEvent.VK_SHIFT) {
            pressedKeys.add("sprint");
        } else if (code == KeyEvent.VK_R) {
            if (screen == Screen.PLAYING) {
                pressedKeys.add("sprint");
            }
        } else if (code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_C) {
            pressedKeys.add("sneak");
        } else if (code == KeyEvent.VK_E) {
            if (!pressedKeys.contains("interact")) worldInputEdge("interact");
            pressedKeys.add("interact");
        } else if (code == KeyEvent.VK_F) {
            if (!pressedKeys.contains("steal")) worldInputEdge("steal");
            pressedKeys.add("steal");
        } else if (code == KeyEvent.VK_Q) {
            if (!pressedKeys.contains("distract")) worldInputEdge("distract");
            pressedKeys.add("distract");
        } else if (code == KeyEvent.VK_G || code == KeyEvent.VK_1) {
            if (screen == Screen.PLAYING && world != null) {
                world.deploySmokeBomb();
            }
        } else if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
            if (screen == Screen.CURATOR_INSPECTION) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.VICTORY;
                updateCursor();
                repaint();
            }
        }
    }

    private void handleRawKeyReleased(int code) {
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) pressedKeys.remove("up");
        else if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) pressedKeys.remove("down");
        else if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) pressedKeys.remove("left");
        else if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) pressedKeys.remove("right");
        else if (code == KeyEvent.VK_SHIFT) pressedKeys.remove("sprint");
        else if (code == KeyEvent.VK_R) { if (screen == Screen.PLAYING) pressedKeys.remove("sprint"); }
        else if (code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_C) pressedKeys.remove("sneak");
        else if (code == KeyEvent.VK_E) pressedKeys.remove("interact");
        else if (code == KeyEvent.VK_F) pressedKeys.remove("steal");
        else if (code == KeyEvent.VK_Q) pressedKeys.remove("distract");
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
        world.applyEquipmentLoadout(levelManager.getEquippedItems());
        MuseumMap map = world.getMap();
        if (map != null) {
            cameraX = Math.max(0, Math.min(map.getWidth() - WIDTH, world.getPlayerCenterX() - WIDTH / 2.0));
            cameraY = Math.max(0, Math.min(map.getHeight() - HEIGHT, world.getPlayerCenterY() - HEIGHT / 2.0));
        } else {
            cameraX = 0;
            cameraY = 0;
        }
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
        if (screen == Screen.GALLERY && galleryInspectedPainting != null) {
            galleryInspectedPainting = null;
            SoundEffects.play(SoundEffects.Cue.CLICK);
            updateCursor();
            repaint();
            return;
        }
        if (screen == Screen.PLAYING) togglePause();
        else if (screen == Screen.LEVEL_SELECT || screen == Screen.SHOP || screen == Screen.CUSTOMIZE
            || screen == Screen.SETTINGS || screen == Screen.GALLERY) {
            uiToast = "";
            screen = Screen.MENU;
            updateCursor();
            repaint();
        } else if (screen == Screen.PAUSED || screen == Screen.VICTORY || screen == Screen.GAME_OVER || screen == Screen.CURATOR_INSPECTION) {
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
            } else if (buttonBounds(390, 508).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        } else if (screen == Screen.CUSTOMIZE) {
            if (customizeRotateLeftBounds().contains(x, y)) {
                customizePreviewDirection = (customizePreviewDirection + 3) % 4;
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
            } else if (customizeRotateRightBounds().contains(x, y)) {
                customizePreviewDirection = (customizePreviewDirection + 1) % 4;
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
            } else if (customizeTabBounds(0).contains(x, y)) {
                customizeTab = 0;
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
            } else if (customizeTabBounds(1).contains(x, y)) {
                customizeTab = 1;
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
            } else {
                int card = customizeCardAt(x, y);
                if (card >= 0) {
                    if (customizeTab == 0) {
                        handleCustomizeClick(card);
                    } else {
                        handleCustomizeAccessoryClick(card);
                    }
                } else if (buttonBounds(390, 508).contains(x, y)) {
                    SoundEffects.play(SoundEffects.Cue.CLICK);
                    handleEscape();
                }
            }
        } else if (screen == Screen.SETTINGS) {
            int row = settingsRowAt(x, y);
            if (row >= 0) {
                handleSettingsClick(row);
            } else if (buttonBounds(390, 508).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                handleEscape();
            }
        } else if (screen == Screen.GALLERY) {
            if (galleryInspectedPainting != null) {
                if (galleryInspectCloseBounds().contains(x, y) || !new Rectangle(150, 35, 660, 530).contains(x, y)) {
                    galleryInspectedPainting = null;
                    SoundEffects.play(SoundEffects.Cue.CLICK);
                    updateCursor();
                    repaint();
                }
            } else {
                int wing = galleryWingTabAt(x, y);
                if (wing >= 0) {
                    gallerySelectedWing = wing;
                    SoundEffects.play(SoundEffects.Cue.CLICK);
                    updateCursor();
                    repaint();
                } else {
                    String clickedPainting = galleryPaintingAt(x, y);
                    if (clickedPainting != null) {
                        galleryInspectedPainting = clickedPainting;
                        SoundEffects.play(SoundEffects.Cue.CLICK);
                        updateCursor();
                        repaint();
                    } else if (buttonBounds(390, 508).contains(x, y)) {
                        SoundEffects.play(SoundEffects.Cue.CLICK);
                        handleEscape();
                    }
                }
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
            if (buttonBounds(390, PAUSE_BTN_CONTINUE_Y).contains(x, y)) resumeGame();
            else if (buttonBounds(390, PAUSE_BTN_RESTART_Y).contains(x, y)) startGame();
            else if (buttonBounds(390, PAUSE_BTN_SELECT_Y).contains(x, y)) handleEscape();
        } else if (screen == Screen.CURATOR_INSPECTION) {
            if (buttonBounds(390, 508).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                screen = Screen.VICTORY;
                updateCursor();
                repaint();
            }
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
            if (buttonBounds(390, GAMEOVER_BTN_RETRY_Y).contains(x, y)) {
                SoundEffects.play(SoundEffects.Cue.CLICK);
                startGame(selectedLevel);
            } else if (buttonBounds(390, GAMEOVER_BTN_SELECT_Y).contains(x, y)) {
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
            screen = Screen.CURATOR_INSPECTION;
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
        int panelW = getWidth();
        int panelH = getHeight();

        // 1. Edge-to-edge dark museum background prevents any raw Swing gray on any resolution
        graphics.setColor(new Color(12, 11, 16));
        graphics.fillRect(0, 0, panelW, panelH);

        // 2. Full-bleed aspect-ratio COVER background for Main Menu & Submenus (no stretching, zero gray borders)
        if ((screen == Screen.MENU || screen == Screen.LEVEL_SELECT || screen == Screen.SETTINGS
                || screen == Screen.SHOP || screen == Screen.CUSTOMIZE || screen == Screen.GALLERY)
                && uiAssets != null && uiAssets.menuBg != null) {
            int bgW = uiAssets.menuBg.getWidth();
            int bgH = uiAssets.menuBg.getHeight();
            double coverScale = Math.max((double) panelW / bgW, (double) panelH / bgH);
            int drawW = (int) Math.ceil(bgW * coverScale);
            int drawH = (int) Math.ceil(bgH * coverScale);
            int drawX = (panelW - drawW) / 2;
            int drawY = (panelH - drawH) / 2;
            Graphics2D bgG = (Graphics2D) graphics.create();
            bgG.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            bgG.drawImage(uiAssets.menuBg, drawX, drawY, drawW, drawH, null);
            bgG.setColor(new Color(8, 10, 15, screen == Screen.MENU ? 85 : 180));
            bgG.fillRect(0, 0, panelW, panelH);
            bgG.dispose();
        }

        Graphics2D g = (Graphics2D) graphics.create();
        double scale = Math.min(panelW / (double) WIDTH, panelH / (double) HEIGHT);
        double offsetX = (panelW - WIDTH * scale) / 2.0;
        double offsetY = (panelH - HEIGHT * scale) / 2.0;
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
            else if (screen == Screen.CURATOR_INSPECTION) drawCuratorInspection(g);
            else if (screen == Screen.VICTORY) drawVictory(g);
            else if (screen == Screen.GAME_OVER) drawOverlay(g, "HEIST BUSTED", world == null ? "" : world.getEndReason(), "TRY AGAIN", "", "LEVEL SELECT");
        }
        g.dispose();
    }

    private void drawGame(Graphics2D g, GameWorld game) {
        MuseumMap map = game.getMap();
        int mapW = map.getWidth();
        int mapH = map.getHeight();

        double targetCamX = Math.max(0, Math.min(mapW - WIDTH, game.getPlayerCenterX() - WIDTH / 2.0));
        double targetCamY = Math.max(0, Math.min(mapH - HEIGHT, game.getPlayerCenterY() - HEIGHT / 2.0));

        cameraX += (targetCamX - cameraX) * 0.15;
        cameraY += (targetCamY - cameraY) * 0.15;
        int camX = (int) Math.round(Math.max(0, Math.min(mapW - WIDTH, cameraX)));
        int camY = (int) Math.round(Math.max(0, Math.min(mapH - HEIGHT, cameraY)));

        Graphics2D worldG = (Graphics2D) g.create();
        worldG.translate(-camX, -camY);

        map.draw(worldG, game.getPlayerCenterX(), game.getPlayerCenterY());
        if (levelManager.isVisionConesEnabled()) {
            drawGuardVision(worldG, game.getGuards());
        }
        drawCameraVision(worldG, game.getCameras());
        drawNoiseIndicators(worldG, game.getNoiseSystem().getEvents());

        for (MuseumLoot item : map.getLoot()) {
            item.draw(worldG, game.isNearLoot(item), animationFrame);
        }

        for (MuseumMap.Painting painting : map.getPaintings()) {
            drawPainting(worldG, painting, painting == game.getActivePainting());
        }
        for (MuseumMap.HideSpot spot : map.getHideSpots()) {
            drawHideSpot(worldG, spot, game.isNear(spot), game.getHidingIn() == spot);
        }
        for (Guard guard : game.getGuards()) drawGuard(worldG, guard);
        drawPlayer(worldG, game);
        drawDistractionEffect(worldG, game);
        drawSmokeScreen(worldG, game);
        if (levelManager.isLightingEnabled()) {
            drawLighting(worldG, game.getPlayerCenterX(), game.getPlayerCenterY(), mapW, mapH);
        }
        worldG.dispose();

        if (screen == Screen.PLAYING) {
            drawHud(g, game);
            drawMinimap(g, game, camX, camY);
            if (levelManager.isPromptsEnabled()) {
                drawPrompt(g, game);
            }
        }
    }

    private void drawGuardVision(Graphics2D g, java.util.List<Guard> guards) {
        for (Guard guard : guards) {
            double angle = guard.getFacingAngle();
            double radius = guard.getVisionRadius();
            double fovDegrees = guard.getVisionAngleDegrees();
            Path2D.Double cone = new Path2D.Double();
            cone.moveTo(guard.centerX(), guard.centerY());
            for (int step = 0; step <= 24; step++) {
                double a = angle - Math.toRadians(fovDegrees) + Math.toRadians(fovDegrees * 2) * step / 24.0;
                java.awt.geom.Point2D.Double endpoint = world.getMap().getVisionEndpoint(
                    guard.centerX(), guard.centerY(), a, radius);
                cone.lineTo(endpoint.x, endpoint.y);
            }
            cone.closePath();

            Guard.DetectionStage stage = guard.getDetectionStage();
            Color fillTint;
            Color borderTint;

            if (guard.getState() == Guard.State.CHASE || stage == Guard.DetectionStage.CHASE) {
                fillTint = new Color(225, 45, 45, 68);
                borderTint = new Color(255, 65, 65, 175);
            } else if (stage == Guard.DetectionStage.ALERT || guard.getState() == Guard.State.ALERT) {
                fillTint = new Color(245, 80, 40, 58);
                borderTint = new Color(255, 105, 50, 160);
            } else if (stage == Guard.DetectionStage.INVESTIGATING || guard.getState() == Guard.State.INSPECT) {
                fillTint = new Color(245, 140, 30, 48);
                borderTint = new Color(255, 165, 45, 145);
            } else if (stage == Guard.DetectionStage.SUSPICIOUS) {
                fillTint = new Color(245, 195, 45, 38);
                borderTint = new Color(255, 215, 65, 130);
            } else {
                fillTint = new Color(238, 205, 110, 24);
                borderTint = new Color(238, 205, 110, 85);
            }

            g.setColor(fillTint);
            g.fill(cone);
            g.setColor(borderTint);
            g.draw(cone);

            // Subtle center flashlight sightline to clearly show looking direction
            java.awt.geom.Point2D.Double centerBeam = world.getMap().getVisionEndpoint(
                guard.centerX(), guard.centerY(), angle, radius * 0.95);
            g.setColor(new Color(borderTint.getRed(), borderTint.getGreen(), borderTint.getBlue(), 60));
            g.drawLine((int) guard.centerX(), (int) guard.centerY(), (int) centerBeam.x, (int) centerBeam.y);
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

    private void drawSmokeScreen(Graphics2D g, GameWorld game) {
        if (!game.isSmokeActive()) return;
        double cx = game.getSmokeX();
        double cy = game.getSmokeY();
        double timer = game.getSmokeActiveTimer();
        int baseRadius = 82;

        // Render soft multi-layered swirling smoke cloud puffs
        double t = animationFrame * 0.08;
        for (int i = 0; i < 9; i++) {
            double angle = (i * 2.0 * Math.PI / 9.0) + Math.sin(t + i) * 0.4;
            double dist = 24.0 + Math.cos(t * 0.8 + i * 1.3) * 28.0;
            int px = (int) Math.round(cx + Math.cos(angle) * dist);
            int py = (int) Math.round(cy + Math.sin(angle) * dist);
            int puffR = 38 + (int) (Math.sin(t + i * 2) * 8);

            int alpha = (int) (65 + 30 * Math.sin(t + i));
            g.setColor(new Color(215, 222, 235, Math.min(140, Math.max(30, alpha))));
            g.fillOval(px - puffR, py - puffR, puffR * 2, puffR * 2);
            g.setColor(new Color(110, 120, 138, Math.min(100, Math.max(20, alpha - 25))));
            g.drawOval(px - puffR, py - puffR, puffR * 2, puffR * 2);
        }

        // Inner dense core
        int coreAlpha = Math.min(180, (int) (110 + 40 * Math.sin(t * 1.5)));
        g.setColor(new Color(230, 235, 245, coreAlpha));
        g.fillOval((int) Math.round(cx - 36), (int) Math.round(cy - 36), 72, 72);

        // Outer boundary ring
        g.setColor(new Color(200, 220, 245, 90));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[]{6f, 6f}, (float) (animationFrame * 0.5)));
        g.drawOval((int) Math.round(cx - baseRadius), (int) Math.round(cy - baseRadius), baseRadius * 2, baseRadius * 2);
        g.setStroke(new BasicStroke(1.0f));

        // Floating tactical smoke countdown label
        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(235, 245, 255));
        drawCentered(g, "☁ SMOKE BLIND (" + String.format("%.1f", timer) + "s)", (int) Math.round(cx), (int) Math.round(cy - baseRadius - 6));
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

        if (near || occupied) {
            int bob = (int) Math.round(Math.sin(animationFrame * 0.16) * 3);
            int badgeW = occupied ? 64 : 62;
            int badgeH = 18;
            int bx = r.x + r.width / 2 - badgeW / 2;
            int by = r.y - 22 + bob;

            g.setColor(new Color(12, 15, 22, 230));
            g.fillRoundRect(bx, by, badgeW, badgeH, 5, 5);
            g.setColor(occupied ? new Color(119, 232, 160) : new Color(255, 215, 0));
            g.drawRoundRect(bx, by, badgeW, badgeH, 5, 5);

            int kx = bx + 3;
            int ky = by + 2;
            g.setColor(new Color(210, 165, 60));
            g.fillRoundRect(kx, ky, 14, 14, 3, 3);
            g.setColor(new Color(255, 238, 150));
            g.fillRoundRect(kx + 1, ky + 1, 12, 10, 2, 2);
            g.setColor(new Color(35, 25, 10));
            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            g.drawString("E", kx + 3, ky + 9);

            g.setFont(new Font("Monospaced", Font.BOLD, 8));
            g.setColor(new Color(255, 240, 200));
            g.drawString(occupied ? "EXIT" : "HIDE", kx + 18, by + 12);
        }
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
        int headY = y + Guard.HEIGHT - GUARD_SPRITE_HEIGHT;

        // --- Overhead Suspicion & Detection Stage Indicator ---
        Guard.DetectionStage stage = guard.getDetectionStage();
        double suspicion = guard.getSuspicion();

        if (!guard.isInspectingArt() && (stage != Guard.DetectionStage.UNAWARE || suspicion > 5.0 || chasing)) {
            int meterW = 32;
            int meterH = 5;
            int meterX = (int) guard.centerX() - meterW / 2;
            int meterY = headY - 10;

            // Background pill
            g.setColor(new Color(15, 12, 20, 220));
            g.fillRoundRect(meterX - 1, meterY - 1, meterW + 2, meterH + 2, 4, 4);

            // Meter fill
            int fillW = (int) Math.round(meterW * (Math.min(100.0, suspicion) / 100.0));
            Color meterColor = (suspicion >= 92.0 || chasing) ? new Color(240, 55, 55)
                : (suspicion >= 60.0) ? new Color(245, 145, 35)
                : (suspicion >= 25.0) ? new Color(250, 205, 55)
                : new Color(180, 220, 120);

            g.setColor(meterColor);
            g.fillRoundRect(meterX, meterY, fillW, meterH, 3, 3);
            g.setColor(new Color(70, 60, 50));
            g.drawRoundRect(meterX - 1, meterY - 1, meterW + 2, meterH + 2, 4, 4);

            // Detection Stage Icon above the meter
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            if (chasing || stage == Guard.DetectionStage.CHASE) {
                g.setColor(new Color(255, 60, 60));
                drawCentered(g, "!", (int) guard.centerX(), meterY - 4);
            } else if (stage == Guard.DetectionStage.ALERT) {
                g.setColor(new Color(255, 95, 45));
                drawCentered(g, "!", (int) guard.centerX(), meterY - 4);
            } else if (stage == Guard.DetectionStage.INVESTIGATING) {
                g.setColor(new Color(255, 160, 40));
                drawCentered(g, "?", (int) guard.centerX(), meterY - 4);
            } else if (stage == Guard.DetectionStage.SUSPICIOUS) {
                g.setColor(new Color(255, 215, 75));
                drawCentered(g, "?", (int) guard.centerX(), meterY - 4);
            }
        }
        if (guard.isInspectingArt()) {
            int bw = 72;
            int bh = 14;
            boolean showBelow = (headY - 26 < 88);
            int bx = (int) guard.centerX() - bw / 2;
            bx = Math.max(8, Math.min(getWidth() - bw - 8, bx));
            int by = showBelow ? ((int) (guard.getY() + Guard.HEIGHT) + 6) : (headY - 24);

            g.setColor(new Color(18, 16, 24, 230));
            g.fillRoundRect(bx, by, bw, bh, 4, 4);
            g.setColor(new Color(245, 215, 120));
            g.drawRoundRect(bx, by, bw, bh, 4, 4);

            g.setFont(new Font("Monospaced", Font.BOLD, 8));
            g.setColor(new Color(255, 230, 160));
            drawCentered(g, "🔍 INSPECTING", bx + bw / 2, by + 10);

            // Progress bar below badge
            int barY = by + 16;
            int barW = 60;
            int barH = 4;
            int barX = (int) guard.centerX() - barW / 2;
            barX = Math.max(8, Math.min(getWidth() - barW - 8, barX));
            g.setColor(new Color(30, 26, 38));
            g.fillRect(barX, barY, barW, barH);
            g.setColor(new Color(255, 215, 0));
            int fillW = (int) (barW * guard.getInspectProgress());
            g.fillRect(barX, barY, fillW, barH);
            g.setColor(new Color(140, 110, 50));
            g.drawRect(barX, barY, barW, barH);
        } else if (guard.getSpeechBubbleText() != null) {
            String speech = guard.getSpeechBubbleText();
            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            int sw = g.getFontMetrics().stringWidth(speech);
            int bw = sw + 16;
            int bh = 18;
            boolean showBelow = (headY - 24 < 88);
            int bx = (int) guard.centerX() - bw / 2;
            bx = Math.max(8, Math.min(getWidth() - bw - 8, bx));
            int by = showBelow ? ((int) (guard.getY() + Guard.HEIGHT) + 6) : (headY - 24);

            g.setColor(new Color(16, 14, 22, 235));
            g.fillRoundRect(bx, by, bw, bh, 5, 5);
            g.setColor(guard.getSpeechBubbleColor());
            g.drawRoundRect(bx, by, bw, bh, 5, 5);

            // Pointer triangle pointing to guard
            int cx = Math.max(bx + 6, Math.min(bx + bw - 6, (int) guard.centerX()));
            g.setColor(new Color(16, 14, 22, 235));
            if (showBelow) {
                g.fillPolygon(new int[]{cx - 3, cx + 3, cx}, new int[]{by, by, by - 4}, 3);
                g.setColor(guard.getSpeechBubbleColor());
                g.drawLine(cx - 3, by, cx, by - 4);
                g.drawLine(cx + 3, by, cx, by - 4);
            } else {
                g.fillPolygon(new int[]{cx - 3, cx + 3, cx}, new int[]{by + bh, by + bh, by + bh + 4}, 3);
                g.setColor(guard.getSpeechBubbleColor());
                g.drawLine(cx - 3, by + bh, cx, by + bh + 4);
                g.drawLine(cx + 3, by + bh, cx, by + bh + 4);
            }

            drawCentered(g, speech, bx + bw / 2, by + 12);
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
        if (game.isHidden()) {
            return;
        }
        int x = (int) Math.round(game.getPlayerCenterX() - SPRITE_WIDTH / 2.0);
        int y = (int) Math.round(game.getPlayerBottomY() - SPRITE_HEIGHT);
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(x + 8, y + SPRITE_HEIGHT - 8, SPRITE_WIDTH - 16, 7);
        BufferedImage frame = getTintedPlayerFrame(getPlayerSpriteFrame(game), levelManager.getSelectedOutfit());
        if (game.isSneaking()) {
            Composite previous = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
            g.drawImage(frame, x, y, SPRITE_WIDTH, SPRITE_HEIGHT, null);
            drawPlayerAccessory(g, x, y, SPRITE_WIDTH, SPRITE_HEIGHT, levelManager.getSelectedAccessory(), playerDirectionRow);
            g.setComposite(previous);
        } else {
            g.drawImage(frame, x, y, SPRITE_WIDTH, SPRITE_HEIGHT, null);
            drawPlayerAccessory(g, x, y, SPRITE_WIDTH, SPRITE_HEIGHT, levelManager.getSelectedAccessory(), playerDirectionRow);
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

    private void drawLighting(Graphics2D g, double x, double y, int mapW, int mapH) {
        java.awt.Rectangle playArea = new java.awt.Rectangle(0, 0, mapW, mapH);
        g.setColor(new Color(8, 10, 18, 68));
        g.fill(playArea);
        float radius = 520f;
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
        g.setFont(new Font("Monospaced", Font.BOLD, 17));
        g.setColor(new Color(255, 222, 135));
        g.drawString("PAINTHEIST", 16, 25);

        String levelName = world != null && world.getLevel() != null ? world.getLevel().getName().toUpperCase() : "GALLERY";
        if (levelName.length() > 20) levelName = levelName.substring(0, 18) + "..";
        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(175, 185, 205));
        g.drawString(levelName, 16, 39);

        // Subtle infiltration status badge (compact: 110px wide, well clear of pill at x=175)
        g.setColor(new Color(22, 26, 36, 210));
        g.fillRoundRect(16, 45, 110, 16, 4, 4);
        g.setColor(new Color(45, 55, 75));
        g.drawRoundRect(16, 45, 110, 16, 4, 4);
        Color statusDotColor = game.getSuspicion() == 0 ? new Color(46, 204, 113)
            : game.getSuspicion() < 60 ? new Color(241, 196, 15) : new Color(231, 76, 60);
        g.setColor(statusDotColor);
        g.fillOval(22, 50, 6, 6);
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        g.setColor(new Color(215, 225, 235));
        String statusText = game.getSuspicion() == 0 ? "INFILTRATING"
            : game.getSuspicion() < 60 ? "INVESTIGATING" : "HIGH ALERT";
        g.drawString(statusText, 32, 57);

        // 2. HEIST OBJECTIVE PILL
        int objX = 175;
        int objY = 12;
        int objW = 185;
        int objH = 44;
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
            g.drawString("ESCAPE TO THE EXIT!", objX + 10, objY + 34);
        } else {
            g.setColor(Color.WHITE);
            g.drawString("FORGE & SWAP ARTWORK", objX + 10, objY + 34);
        }

        // 3. TARGET ART SATCHEL (Visual Mini Frames)
        int satchelX = 368;
        int satchelY = 12;
        int satchelW = 122;
        int satchelH = 44;
        g.setColor(new Color(24, 27, 38, 220));
        g.fillRoundRect(satchelX, satchelY, satchelW, satchelH, 8, 8);
        g.setColor(new Color(55, 65, 85));
        g.drawRoundRect(satchelX, satchelY, satchelW, satchelH, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(180, 190, 205));
        g.drawString("LOOT SATCHEL (" + game.getStolenCount() + "/" + game.getTargetCount() + ")", satchelX + 8, satchelY + 16);

        // Draw mini frames for each painting
        int targetCount = Math.min(5, game.getTargetCount());
        for (int i = 0; i < targetCount; i++) {
            int fx = satchelX + 9 + i * 22;
            int fy = satchelY + 22;
            boolean stolen = i < game.getStolenCount();
            g.setColor(new Color(12, 14, 20));
            g.fillRect(fx, fy, 18, 14);
            if (stolen) {
                g.setColor(new Color(38, 166, 91));
                g.fillRect(fx + 2, fy + 2, 14, 10);
                g.setColor(new Color(255, 215, 0));
                g.drawRect(fx, fy, 18, 14);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                g.drawString("✓", fx + 5, fy + 11);
            } else {
                g.setColor(new Color(95, 80, 60));
                g.drawRect(fx, fy, 18, 14);
                g.setColor(new Color(140, 130, 120));
                g.setFont(new Font("Monospaced", Font.BOLD, 8));
                g.drawString("?", fx + 6, fy + 11);
            }
        }

        // 4. MISSION CHRONOMETER
        int timeX = 498;
        int timeY = 12;
        int timeW = 104;
        int timeH = 44;
        boolean lowTime = game.getTimeLeft() < 30;
        Color timeBg = lowTime ? new Color(48, 18, 20, 230) : new Color(18, 22, 32, 230);
        Color timeBorder = lowTime ? new Color(230, 60, 60) : new Color(55, 68, 90);
        g.setColor(timeBg);
        g.fillRoundRect(timeX, timeY, timeW, timeH, 8, 8);
        g.setColor(timeBorder);
        g.drawRoundRect(timeX, timeY, timeW, timeH, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(lowTime ? new Color(255, 140, 140) : new Color(175, 185, 205));
        g.drawString("MISSION TIME", timeX + 10, timeY + 16);

        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(lowTime ? new Color(255, 85, 75) : new Color(140, 235, 165));
        String timeStr = String.format("%02d:%02d", game.getTimeLeft() / 60, game.getTimeLeft() % 60);
        g.drawString(timeStr, timeX + 10, timeY + 35);

        // 5. SECURITY SUSPICION & LED GAUGE
        int suspX = 610;
        int suspY = 12;
        int suspW = 180;
        int suspH = 44;
        g.setColor(new Color(20, 23, 33, 230));
        g.fillRoundRect(suspX, suspY, suspW, suspH, 8, 8);
        g.setColor(new Color(55, 65, 85));
        g.drawRoundRect(suspX, suspY, suspW, suspH, 8, 8);

        String alert = game.getAlertStatus();
        Color alertColor = alert.equals("CALM") ? new Color(46, 204, 113)
            : alert.equals("CHASE") ? new Color(231, 76, 60) : new Color(243, 156, 18);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(180, 190, 205));
        g.drawString("SUSPICION " + game.getSuspicion() + "%", suspX + 10, suspY + 16);

        g.setColor(alertColor);
        drawCentered(g, "[" + alert + "]", suspX + suspW - 32, suspY + 16);

        // 10-Segment LED Meter centered inside pill
        int segments = 10;
        int lit = (int) Math.round(game.getSuspicion() / 10.0);
        int segW = 14;
        int segH = 11;
        int segGap = 2;
        int totalMeterW = segments * (segW + segGap) - segGap;
        int barStartX = suspX + (suspW - totalMeterW) / 2;
        int barY = suspY + 24;
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

        // 6. GADGETS: COIN DISTRACTION & EQUIPMENT
        int gadgetX = 798;
        int getY = 12;
        int getW = 146;
        int getH = 44;
        g.setColor(new Color(20, 23, 33, 230));
        g.fillRoundRect(gadgetX, getY, getW, getH, 8, 8);
        g.setColor(new Color(55, 65, 85));
        g.drawRoundRect(gadgetX, getY, getW, getH, 8, 8);

        double distCd = game.getDistractionCooldown();
        boolean coinReady = distCd <= 0;
        int coinSize = 18;
        int cx = gadgetX + 8;
        int cy = getY + 6;
        g.setColor(coinReady ? new Color(255, 215, 0) : new Color(130, 135, 145));
        g.fillOval(cx, cy, coinSize, coinSize);
        g.setColor(coinReady ? new Color(210, 165, 20) : new Color(90, 95, 105));
        g.drawOval(cx, cy, coinSize, coinSize);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.drawString("¢", cx + 6, cy + 13);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(coinReady ? new Color(130, 240, 160) : new Color(200, 190, 205));
        String coinStr = coinReady ? "Q: COIN READY" : "Q: " + (int) Math.ceil(distCd) + "s CD";
        g.drawString(coinStr, gadgetX + 32, getY + 18);

        if (game.hasSmokeBomb()) {
            boolean active = game.isSmokeActive();
            int bombs = game.getSmokeBombsRemaining();
            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            if (active) {
                g.setColor(new Color(110, 235, 255));
                g.drawString("1/G: SMOKE " + String.format("%.1f", game.getSmokeActiveTimer()) + "s", gadgetX + 8, getY + 36);
            } else if (bombs > 0) {
                g.setColor(new Color(245, 215, 120));
                g.drawString("1/G: SMOKE (x" + bombs + ")", gadgetX + 8, getY + 36);
            } else {
                g.setColor(new Color(150, 140, 145));
                g.drawString("1/G: SMOKE (EMPTY)", gadgetX + 8, getY + 36);
            }
        } else {
            int noises = game.getNoiseSystem().getEvents().size();
            g.setFont(new Font("Monospaced", Font.BOLD, 8));
            g.setColor(noises == 0 ? new Color(140, 150, 165) : new Color(255, 180, 90));
            g.drawString(noises == 0 ? "NOISE: QUIET" : "NOISE: " + noises + " HEARD", gadgetX + 32, getY + 36);
        }

        // Tactical passive loadout chips (under top bar)
        int chipX = 16;
        int chipY = 72;
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        if (game.hasSilentShoes()) {
            drawTacticalChip(g, "👟 SILENT SOLES", chipX, chipY, new Color(46, 204, 113));
            chipX += 105;
        }
        if (game.hasLockpickKit()) {
            drawTacticalChip(g, "🔑 LOCKPICK KIT", chipX, chipY, new Color(241, 196, 15));
            chipX += 105;
        }
        if (game.hasQuickDrawBrush()) {
            drawTacticalChip(g, "🖌 QUICK-DRAW", chipX, chipY, new Color(52, 152, 219));
            chipX += 95;
        }
        if (game.hasDistractionDevice()) {
            drawTacticalChip(g, "🎯 LONG PITCH", chipX, chipY, new Color(155, 89, 182));
            chipX += 95;
        }

        // Valuables HUD indicator on the bottom left
        int totalLoot = game.getMap() != null ? game.getMap().getLoot().size() : 0;
        if (totalLoot > 0) {
            int lootX = 16;
            int lootY = HEIGHT - 46;
            int lootW = 165;
            int lootH = 34;
            g.setColor(new Color(18, 22, 32, 230));
            g.fillRoundRect(lootX, lootY, lootW, lootH, 6, 6);
            g.setColor(new Color(215, 175, 75));
            g.drawRoundRect(lootX, lootY, lootW, lootH, 6, 6);

            // Gold chalice mini icon
            g.setColor(new Color(255, 215, 65));
            g.fillOval(lootX + 8, lootY + 8, 14, 10);
            g.fillRect(lootX + 13, lootY + 16, 4, 8);
            g.fillRect(lootX + 9, lootY + 22, 12, 3);

            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            g.setColor(new Color(245, 215, 120));
            g.drawString("VALUABLES: $" + game.getBonusLootMoney(), lootX + 28, lootY + 15);
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g.setColor(new Color(180, 190, 205));
            g.drawString("LOOTED " + game.getStolenLootCount() + "/" + totalLoot, lootX + 28, lootY + 27);
        }
    }

    private void drawTacticalChip(Graphics2D g, String label, int x, int y, Color accent) {
        int w = g.getFontMetrics().stringWidth(label) + 12;
        int h = 16;
        g.setColor(new Color(14, 18, 26, 210));
        g.fillRoundRect(x, y, w, h, 4, 4);
        g.setColor(accent);
        g.drawRoundRect(x, y, w, h, 4, 4);
        g.setColor(new Color(240, 245, 255));
        g.drawString(label, x + 6, y + 11);
    }

    private void drawMinimap(Graphics2D g, GameWorld game, int camX, int camY) {
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

        MuseumMap map = game.getMap();
        if (map == null) return;
        int mapW = map.getWidth();
        int mapH = map.getHeight();

        // Coordinate scaling
        double sx = (double) (mmW - 12) / mapW;
        double sy = (double) (mmH - 18) / mapH;
        int offX = mmX + 6;
        int offY = mmY + 14;

        // Draw Solids (Walls)
        g.setColor(new Color(40, 90, 150, 180));
        for (Rectangle solid : map.getSolids()) {
            int bx = offX + (int) (solid.x * sx);
            int by = offY + (int) (solid.y * sy);
            int bw = Math.max(2, (int) (solid.width * sx));
            int bh = Math.max(2, (int) (solid.height * sy));
            g.fillRect(bx, by, bw, bh);
        }

        // Draw Exits
        g.setColor(new Color(46, 204, 113));
        for (Rectangle exit : map.getExits()) {
            int ex = offX + (int) (exit.x * sx);
            int ey = offY + (int) (exit.y * sy);
            g.fillRect(ex, ey, Math.max(3, (int) (exit.width * sx)), Math.max(3, (int) (exit.height * sy)));
        }

        // Draw Loot (Valuables)
        for (MuseumLoot item : map.getLoot()) {
            if (!item.isCollected()) {
                int lx = offX + (int) ((item.getX() + item.getWidth() / 2.0) * sx);
                int ly = offY + (int) ((item.getY() + item.getHeight() / 2.0) * sy);
                g.setColor(new Color(255, 230, 90));
                g.fillRect(lx - 1, ly - 1, 2, 2);
            }
        }

        // Draw Paintings
        for (MuseumMap.Painting p : map.getPaintings()) {
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

        // Camera Viewport box on Radar
        g.setColor(new Color(120, 200, 255, 130));
        int cvx = offX + (int) (camX * sx);
        int cvy = offY + (int) (camY * sy);
        int cvw = Math.max(4, (int) (WIDTH * sx));
        int cvh = Math.max(4, (int) (HEIGHT * sy));
        g.drawRect(cvx, cvy, cvw, cvh);

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
        if (uiAssets != null && uiAssets.menuBg != null) {
            // Ornate gold filigree frame framing the interactive menu content
            g.setColor(new Color(177, 133, 78, 175));
            g.drawRect(22, 22, WIDTH - 44, HEIGHT - 44);
            g.setColor(new Color(225, 181, 100, 85));
            g.drawRect(26, 26, WIDTH - 52, HEIGHT - 52);
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

    private Rectangle shopTabEquipmentBounds() {
        return new Rectangle(180, 72, 280, 26);
    }

    private Rectangle shopTabDisguisesBounds() {
        return new Rectangle(500, 72, 280, 26);
    }

    private Rectangle shopItemBounds(int index) {
        int col = index / 3;
        int row = index % 3;
        return new Rectangle(64 + col * 432, 108 + row * 122, 400, 112);
    }

    private int shopItemAt(int x, int y) {
        if (shopTabEquipmentBounds().contains(x, y)) return 100;
        if (shopTabDisguisesBounds().contains(x, y)) return 101;
        for (int i = 0; i < 6; i++) {
            if (shopItemBounds(i).contains(x, y)) return i;
        }
        return -1;
    }

    private void handleShopClick(int item) {
        SoundEffects.play(SoundEffects.Cue.CLICK);
        if (item == 100) {
            shopTab = 0;
            repaint();
            return;
        }
        if (item == 101) {
            shopTab = 1;
            repaint();
            return;
        }

        if (shopTab == 0) {
            // Equipment Tab
            if (item >= 0 && item < 5) {
                String key = LevelManager.ALL_EQUIPMENT[item];
                String name = LevelManager.EQUIPMENT_NAMES[item];
                int cost = LevelManager.EQUIPMENT_COSTS[item];
                if (!levelManager.hasUpgrade(key)) {
                    if (levelManager.buyUpgrade(key, cost)) {
                        uiToast = "UNLOCKED & EQUIPPED " + name.toUpperCase() + "!";
                        SoundEffects.play(SoundEffects.Cue.SWAP);
                    } else {
                        uiToast = "NEED " + cost + " CREDITS FOR " + name.toUpperCase() + ".";
                    }
                } else {
                    if (levelManager.isEquipped(key)) {
                        levelManager.toggleEquip(key);
                        uiToast = "UNEQUIPPED " + name.toUpperCase() + " FROM LOADOUT.";
                        SoundEffects.play(SoundEffects.Cue.CLICK);
                    } else {
                        if (levelManager.getEquippedCount() < LevelManager.MAX_EQUIPPED) {
                            levelManager.toggleEquip(key);
                            uiToast = "EQUIPPED " + name.toUpperCase() + " (" + levelManager.getEquippedCount() + "/2)!";
                            SoundEffects.play(SoundEffects.Cue.SWAP);
                        } else {
                            uiToast = "LOADOUT FULL (MAX 2). UNEQUIP AN ITEM TO EQUIP THIS.";
                        }
                    }
                }
            } else if (item == 5) {
                uiToast = "TACTICAL LOADOUT: Max 2 items armed for museum infiltration.";
            }
        } else {
            // Disguises Tab
            if (item >= 0 && item < 5) {
                int outfitIndex = item;
                boolean wasUnlocked = levelManager.isOutfitUnlocked(outfitIndex);
                if (levelManager.selectOrBuyOutfit(outfitIndex)) {
                    uiToast = (wasUnlocked ? "EQUIPPED " : "PURCHASED & EQUIPPED ")
                        + LevelManager.OUTFIT_NAMES[outfitIndex].toUpperCase() + "!";
                    SoundEffects.play(SoundEffects.Cue.SWAP);
                } else {
                    uiToast = "NEED " + LevelManager.OUTFIT_COSTS[outfitIndex] + " CREDITS FOR "
                        + LevelManager.OUTFIT_NAMES[outfitIndex].toUpperCase() + ".";
                }
            } else {
                screen = Screen.CUSTOMIZE;
                updateCursor();
                repaint();
                return;
            }
        }
        updateCursor();
        repaint();
    }

    private void drawGadgetIcon(Graphics2D g, int id, int cx, int cy, int size) {
        if (id == 0) {
            // Smoke Bomb: Tactical canister with grenade pin & smoke wisps
            g.setColor(new Color(55, 65, 52));
            g.fillRoundRect(cx - 10, cy - 8, 20, 26, 4, 4);
            g.setColor(new Color(120, 140, 115));
            g.drawRoundRect(cx - 10, cy - 8, 20, 26, 4, 4);
            g.setColor(new Color(35, 42, 32));
            g.fillRect(cx - 6, cy - 14, 12, 6);
            g.setColor(new Color(245, 215, 120));
            g.drawOval(cx - 8, cy - 20, 8, 8); // Pin
            // Smoke cloud wisp
            g.setColor(new Color(220, 230, 245, 160));
            g.fillOval(cx + 4, cy - 18, 14, 12);
            g.fillOval(cx + 10, cy - 14, 10, 10);
        } else if (id == 1) {
            // Distraction Device: Weighted acoustic coin with sonic rings
            int cr = size / 3;
            g.setColor(new Color(110, 220, 240, 180));
            g.drawArc(cx - 20, cy - 12, 40, 28, 40, 150);
            g.drawArc(cx - 15, cy - 8, 30, 22, 40, 140);
            g.setColor(new Color(255, 215, 0));
            g.fillOval(cx - cr + 4, cy - cr - 2, cr * 2, cr * 2);
            g.setColor(new Color(180, 130, 20));
            g.drawOval(cx - cr + 4, cy - cr - 2, cr * 2, cr * 2);
            g.setColor(new Color(60, 40, 10));
            g.setFont(new Font("Monospaced", Font.BOLD, 11));
            g.drawString("¢", cx + 1, cy + 4);
        } else if (id == 2) {
            // Lockpick Kit: Crossed tension wrench and skeleton pick
            g.setColor(new Color(205, 215, 230));
            g.setStroke(new BasicStroke(2.0f));
            g.drawLine(cx - 16, cy + 12, cx + 14, cy - 14); // Pick shaft
            g.drawLine(cx + 14, cy - 14, cx + 18, cy - 10); // Pick hook
            g.drawLine(cx - 12, cy - 12, cx + 16, cy + 14); // Wrench shaft
            g.drawLine(cx + 16, cy + 14, cx + 16, cy + 8); // Wrench lip
            g.setStroke(new BasicStroke(1.0f));
            g.setColor(new Color(230, 190, 80));
            g.fillRoundRect(cx - 18, cy + 10, 8, 8, 2, 2);
            g.fillRoundRect(cx - 14, cy - 16, 8, 8, 2, 2);
        } else if (id == 3) {
            // Silent Shoes: Stealth low-profile infiltrator boot with padded sole
            g.setColor(new Color(24, 26, 36));
            int[] sx = {cx - 16, cx - 12, cx - 4, cx + 12, cx + 16, cx + 16, cx - 16};
            int[] sy = {cy + 6, cy - 10, cy - 8, cy - 2, cy + 4, cy + 8, cy + 8};
            g.fillPolygon(sx, sy, 7);
            g.setColor(new Color(65, 80, 105));
            g.drawPolygon(sx, sy, 7);
            // Mint acoustic foam sole
            g.setColor(new Color(110, 235, 170));
            g.fillRect(cx - 16, cy + 8, 32, 4);
            g.setColor(new Color(40, 50, 60));
            for (int k = cx - 12; k < cx + 14; k += 6) {
                g.drawLine(k, cy + 8, k, cy + 12);
            }
        } else if (id == 4) {
            // Quick-Draw Brush: Master sable hair brush
            g.setColor(new Color(145, 65, 40));
            g.setStroke(new BasicStroke(3.0f));
            g.drawLine(cx - 16, cy + 16, cx + 8, cy - 8); // Handle
            g.setStroke(new BasicStroke(1.0f));
            // Brass ferrule
            g.setColor(new Color(255, 215, 95));
            g.fillRect(cx + 6, cy - 10, 6, 6);
            // Sable hair tip
            g.setColor(new Color(45, 32, 25));
            int[] tx = {cx + 10, cx + 18, cx + 14};
            int[] ty = {cy - 12, cy - 18, cy - 8};
            g.fillPolygon(tx, ty, 3);
            // Gold wet pigment drop
            g.setColor(new Color(255, 225, 110));
            g.fillOval(cx + 16, cy - 20, 4, 4);
        } else {
            // Operative Satchel
            g.setColor(new Color(115, 80, 45));
            g.fillRoundRect(cx - 16, cy - 12, 32, 24, 4, 4);
            g.setColor(new Color(175, 130, 80));
            g.drawRoundRect(cx - 16, cy - 12, 32, 24, 4, 4);
            // Flap and brass clasp
            g.setColor(new Color(85, 55, 30));
            g.fillRect(cx - 16, cy - 12, 32, 10);
            g.setColor(new Color(255, 215, 100));
            g.fillRect(cx - 3, cy - 4, 6, 6);
        }
    }

    private void drawDisguiseMannequin(Graphics2D g, int outfitIndex, int cx, int cy) {
        drawDisguiseMannequin(g, outfitIndex, 0, cx, cy);
    }

    private void drawDisguiseMannequin(Graphics2D g, int outfitIndex, int accessoryIndex, int cx, int cy) {
        g.setColor(new Color(10, 10, 14, 180));
        g.fillOval(cx - 18, cy + 18, 36, 10);
        g.setColor(new Color(140, 110, 60));
        g.drawOval(cx - 16, cy + 19, 32, 8);

        if (playerSpriteFrames != null && playerSpriteFrames.length > 0 && playerSpriteFrames[0].length > 0) {
            int frame = (int) (animationFrame / 8 % SPRITE_STRIP_FRAME_COUNT);
            BufferedImage base = playerSpriteFrames[ACTION_ROW_START[WALK_ACTION]][frame];
            BufferedImage tinted = getTintedPlayerFrame(base, outfitIndex);
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(tinted, cx - 24, cy - 30, 48, 56, null);
            drawPlayerAccessory(g, cx - 24, cy - 30, 48, 56, accessoryIndex, 0);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
        }
    }

    private void drawShop(Graphics2D g) {
        drawBackdrop(g);

        // Header Title
        g.setFont(new Font("Monospaced", Font.BOLD, 22));
        g.setColor(new Color(255, 222, 140));
        drawCentered(g, "BLACK MARKET FENCE & ATELIER", WIDTH / 2, 46);

        // Heist Credits Badge Pill (Top-Right)
        int credW = 200, credH = 22;
        int credX = WIDTH - credW - 54, credY = 32;
        g.setColor(new Color(24, 22, 32, 230));
        g.fillRoundRect(credX, credY, credW, credH, 8, 8);
        g.setColor(new Color(180, 140, 70));
        g.drawRoundRect(credX, credY, credW, credH, 8, 8);
        g.setColor(new Color(255, 215, 0));
        g.fillOval(credX + 8, credY + 3, 16, 16);
        g.setColor(new Color(40, 30, 10));
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.drawString("¢", credX + 13, credY + 15);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(255, 235, 175));
        g.drawString("CREDITS: " + levelManager.getTotalRewards() + " CR", credX + 30, credY + 16);

        // TAB BAR
        Rectangle tab0 = shopTabEquipmentBounds();
        Rectangle tab1 = shopTabDisguisesBounds();
        boolean hoverTab0 = hoverX >= 0 && hoverY >= 0 && tab0.contains(hoverX, hoverY);
        boolean hoverTab1 = hoverX >= 0 && hoverY >= 0 && tab1.contains(hoverX, hoverY);

        g.setColor(shopTab == 0 ? new Color(60, 48, 30) : hoverTab0 ? new Color(36, 32, 42) : new Color(22, 20, 28));
        g.fillRoundRect(tab0.x, tab0.y, tab0.width, tab0.height, 6, 6);
        g.setColor(shopTab == 0 ? GOLD : hoverTab0 ? new Color(180, 160, 130) : new Color(65, 58, 52));
        g.drawRoundRect(tab0.x, tab0.y, tab0.width, tab0.height, 6, 6);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(shopTab == 0 ? new Color(255, 235, 175) : new Color(175, 170, 185));
        drawCentered(g, "⚙ TACTICAL EQUIPMENT (" + levelManager.getEquippedCount() + "/" + LevelManager.MAX_EQUIPPED + " ARMED)",
            tab0.x + tab0.width / 2, tab0.y + 17);

        g.setColor(shopTab == 1 ? new Color(60, 48, 30) : hoverTab1 ? new Color(36, 32, 42) : new Color(22, 20, 28));
        g.fillRoundRect(tab1.x, tab1.y, tab1.width, tab1.height, 6, 6);
        g.setColor(shopTab == 1 ? GOLD : hoverTab1 ? new Color(180, 160, 130) : new Color(65, 58, 52));
        g.drawRoundRect(tab1.x, tab1.y, tab1.width, tab1.height, 6, 6);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(shopTab == 1 ? new Color(255, 235, 175) : new Color(175, 170, 185));
        drawCentered(g, "🎭 OPERATIVE CLOAKS & ATELIER", tab1.x + tab1.width / 2, tab1.y + 17);

        if (shopTab == 0) {
            // --- TACTICAL EQUIPMENT TAB ---
            for (int i = 0; i < 6; i++) {
                Rectangle card = shopItemBounds(i);
                boolean hovered = hoverX >= 0 && hoverY >= 0 && card.contains(hoverX, hoverY);

                if (i < 5) {
                    String key = LevelManager.ALL_EQUIPMENT[i];
                    String name = LevelManager.EQUIPMENT_NAMES[i];
                    String desc = LevelManager.EQUIPMENT_DESCRIPTIONS[i];
                    int cost = LevelManager.EQUIPMENT_COSTS[i];
                    boolean owned = levelManager.hasUpgrade(key);
                    boolean equipped = levelManager.isEquipped(key);

                    g.setColor(equipped ? new Color(32, 45, 36) : hovered ? new Color(42, 38, 50) : new Color(28, 26, 35));
                    g.fillRoundRect(card.x, card.y, card.width, card.height, 10, 10);
                    g.setColor(equipped ? new Color(85, 225, 140) : hovered ? GOLD : owned ? new Color(110, 160, 120) : new Color(90, 75, 65));
                    g.drawRoundRect(card.x, card.y, card.width, card.height, 10, 10);

                    // Left icon box
                    int ibx = card.x + 10, iby = card.y + 10, ibw = 80, ibh = 92;
                    g.setColor(new Color(16, 14, 20));
                    g.fillRoundRect(ibx, iby, ibw, ibh, 6, 6);
                    g.setColor(new Color(60, 50, 45));
                    g.drawRoundRect(ibx, iby, ibw, ibh, 6, 6);
                    drawGadgetIcon(g, i, ibx + ibw / 2, iby + ibh / 2, 48);

                    // Right content
                    int tx = card.x + 100;
                    g.setFont(new Font("Monospaced", Font.BOLD, 13));
                    g.setColor(new Color(255, 225, 155));
                    g.drawString(name.toUpperCase(), tx, card.y + 24);

                    g.setFont(new Font("Monospaced", Font.BOLD, 9));
                    g.setColor(new Color(110, 230, 180));
                    g.drawString("[TACTICAL GADGET]", tx, card.y + 38);

                    g.setFont(new Font("Monospaced", Font.PLAIN, 10));
                    g.setColor(new Color(195, 190, 205));
                    g.drawString(desc, tx, card.y + 55);

                    // Interactive Button Pill
                    int btnX = tx;
                    int btnY = card.y + 72;
                    int btnW = 185;
                    int btnH = 26;

                    if (equipped) {
                        g.setColor(new Color(25, 68, 38));
                        g.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                        g.setColor(new Color(65, 215, 120));
                        g.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                        g.setFont(new Font("Monospaced", Font.BOLD, 10));
                        g.setColor(new Color(130, 255, 170));
                        drawCentered(g, "✔ ARMED (CLICK TO UNEQUIP)", btnX + btnW / 2, btnY + 17);
                    } else if (owned) {
                        g.setColor(hovered ? new Color(75, 62, 42) : new Color(52, 42, 28));
                        g.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                        g.setColor(new Color(235, 195, 105));
                        g.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                        g.setFont(new Font("Monospaced", Font.BOLD, 10));
                        g.setColor(new Color(255, 235, 170));
                        drawCentered(g, "✦ EQUIP TO LOADOUT", btnX + btnW / 2, btnY + 17);
                    } else {
                        boolean canAfford = levelManager.getTotalRewards() >= cost;
                        if (canAfford) {
                            g.setColor(hovered ? new Color(130, 95, 30) : new Color(100, 72, 20));
                            g.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                            g.setColor(new Color(255, 215, 100));
                            g.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                            g.setFont(new Font("Monospaced", Font.BOLD, 10));
                            g.setColor(new Color(255, 245, 210));
                            drawCentered(g, "💰 UNLOCK - " + cost + " CR", btnX + btnW / 2, btnY + 17);
                        } else {
                            g.setColor(new Color(28, 26, 32));
                            g.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                            g.setColor(new Color(70, 65, 75));
                            g.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                            g.setFont(new Font("Monospaced", Font.BOLD, 10));
                            g.setColor(new Color(145, 140, 150));
                            drawCentered(g, "🔒 NEED " + cost + " CR", btnX + btnW / 2, btnY + 17);
                        }
                    }
                } else {
                    // Card 5: Operative Satchel Status Summary
                    g.setColor(new Color(24, 26, 35));
                    g.fillRoundRect(card.x, card.y, card.width, card.height, 10, 10);
                    g.setColor(new Color(180, 140, 75));
                    g.drawRoundRect(card.x, card.y, card.width, card.height, 10, 10);

                    int ibx = card.x + 10, iby = card.y + 10, ibw = 80, ibh = 92;
                    g.setColor(new Color(16, 14, 20));
                    g.fillRoundRect(ibx, iby, ibw, ibh, 6, 6);
                    g.setColor(new Color(60, 50, 45));
                    g.drawRoundRect(ibx, iby, ibw, ibh, 6, 6);
                    drawGadgetIcon(g, 5, ibx + ibw / 2, iby + ibh / 2, 48);

                    int tx = card.x + 100;
                    g.setFont(new Font("Monospaced", Font.BOLD, 13));
                    g.setColor(new Color(255, 225, 155));
                    g.drawString("HEIST LOADOUT ARSENAL", tx, card.y + 24);

                    g.setFont(new Font("Monospaced", Font.BOLD, 9));
                    g.setColor(new Color(245, 185, 90));
                    g.drawString("[INFILTRATION GEAR]", tx, card.y + 38);

                    g.setFont(new Font("Monospaced", Font.PLAIN, 10));
                    g.setColor(new Color(195, 190, 205));
                    g.drawString("Armed: " + levelManager.getEquippedCount() + "/2 tactical items equipped.", tx, card.y + 55);

                    int btnX = tx;
                    int btnY = card.y + 72;
                    int btnW = 185;
                    int btnH = 26;
                    g.setColor(new Color(20, 52, 32));
                    g.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                    g.setColor(new Color(65, 215, 120));
                    g.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                    g.setFont(new Font("Monospaced", Font.BOLD, 9));
                    g.setColor(new Color(130, 255, 170));
                    drawCentered(g, "✔ LOADOUT READY FOR HEIST", btnX + btnW / 2, btnY + 17);
                }
            }
        } else {
            // --- THIEF CLOAKS ATELIER TAB ---
            String[] names = {
                LevelManager.OUTFIT_NAMES[0].toUpperCase(),
                LevelManager.OUTFIT_NAMES[1].toUpperCase(),
                LevelManager.OUTFIT_NAMES[2].toUpperCase(),
                LevelManager.OUTFIT_NAMES[3].toUpperCase(),
                LevelManager.OUTFIT_NAMES[4].toUpperCase(),
                "SYNDICATE MIRROR"
            };
            String[] descs = {
                "Standard issue dark wool infiltrator suit",
                "Curator uniform. Pass through halls in plain sight",
                "Formal crimson velvet suit tailored for midnight heists",
                "Dark stealth shadow weave for silent ghost runs",
                "Casual street rogue outfit with gold trim accents",
                "Operative disguise and headwear vanity atelier"
            };
            int[] costs = {
                0,
                LevelManager.OUTFIT_COSTS[1],
                LevelManager.OUTFIT_COSTS[2],
                LevelManager.OUTFIT_COSTS[3],
                LevelManager.OUTFIT_COSTS[4],
                0
            };
            boolean[] owned = {
                true,
                levelManager.isOutfitUnlocked(1),
                levelManager.isOutfitUnlocked(2),
                levelManager.isOutfitUnlocked(3),
                levelManager.isOutfitUnlocked(4),
                true
            };

            for (int i = 0; i < 6; i++) {
                Rectangle card = shopItemBounds(i);
                boolean hovered = hoverX >= 0 && hoverY >= 0 && card.contains(hoverX, hoverY);
                boolean equipped = i < 5 && levelManager.getSelectedOutfit() == i;

                g.setColor(equipped ? new Color(52, 42, 34) : hovered ? new Color(42, 38, 50) : new Color(28, 26, 35));
                g.fillRoundRect(card.x, card.y, card.width, card.height, 10, 10);
                g.setColor(equipped ? new Color(245, 215, 120) : hovered ? GOLD : owned[i] ? new Color(110, 160, 120) : new Color(90, 75, 65));
                g.drawRoundRect(card.x, card.y, card.width, card.height, 10, 10);

                // Left icon / showcase box
                int ibx = card.x + 10, iby = card.y + 10, ibw = 80, ibh = 92;
                g.setColor(new Color(16, 14, 20));
                g.fillRoundRect(ibx, iby, ibw, ibh, 6, 6);
                g.setColor(new Color(60, 50, 45));
                g.drawRoundRect(ibx, iby, ibw, ibh, 6, 6);

                if (i < 5) {
                    drawDisguiseMannequin(g, i, ibx + ibw / 2, iby + ibh / 2 + 10);
                } else {
                    drawGadgetIcon(g, 5, ibx + ibw / 2, iby + ibh / 2, 48);
                }

                // Right content
                int tx = card.x + 100;
                g.setFont(new Font("Monospaced", Font.BOLD, 13));
                g.setColor(new Color(255, 225, 155));
                g.drawString(names[i], tx, card.y + 24);

                g.setFont(new Font("Monospaced", Font.BOLD, 9));
                g.setColor(i < 5 ? new Color(245, 175, 80) : new Color(110, 230, 180));
                g.drawString(i < 5 ? "[THIEF DISGUISE]" : "[SPECIAL FACILITY]", tx, card.y + 38);

                g.setFont(new Font("Monospaced", Font.PLAIN, 10));
                g.setColor(new Color(195, 190, 205));
                g.drawString(descs[i], tx, card.y + 55);

                // Interactive Button Pill
                int btnX = tx;
                int btnY = card.y + 72;
                int btnW = 185;
                int btnH = 26;

                if (i < 5) {
                    if (equipped) {
                        g.setColor(new Color(25, 65, 38));
                        g.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                        g.setColor(new Color(65, 215, 120));
                        g.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                        g.setFont(new Font("Monospaced", Font.BOLD, 10));
                        g.setColor(new Color(130, 255, 170));
                        drawCentered(g, "✔ CURRENTLY WORN", btnX + btnW / 2, btnY + 17);
                    } else if (owned[i]) {
                        g.setColor(hovered ? new Color(85, 68, 45) : new Color(60, 48, 32));
                        g.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                        g.setColor(new Color(235, 195, 105));
                        g.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                        g.setFont(new Font("Monospaced", Font.BOLD, 10));
                        g.setColor(new Color(255, 235, 170));
                        drawCentered(g, "✦ EQUIP DISGUISE", btnX + btnW / 2, btnY + 17);
                    } else {
                        boolean canAfford = levelManager.getTotalRewards() >= costs[i];
                        if (canAfford) {
                            g.setColor(hovered ? new Color(130, 95, 30) : new Color(100, 72, 20));
                            g.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                            g.setColor(new Color(255, 215, 100));
                            g.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                            g.setFont(new Font("Monospaced", Font.BOLD, 10));
                            g.setColor(new Color(255, 245, 210));
                            drawCentered(g, "💰 UNLOCK - " + costs[i] + " CR", btnX + btnW / 2, btnY + 17);
                        } else {
                            g.setColor(new Color(28, 26, 32));
                            g.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                            g.setColor(new Color(70, 65, 75));
                            g.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                            g.setFont(new Font("Monospaced", Font.BOLD, 10));
                            g.setColor(new Color(145, 140, 150));
                            drawCentered(g, "🔒 NEED " + costs[i] + " CR", btnX + btnW / 2, btnY + 17);
                        }
                    }
                } else {
                    g.setColor(new Color(22, 28, 38));
                    g.fillRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                    g.setColor(new Color(65, 95, 135));
                    g.drawRoundRect(btnX, btnY, btnW, btnH, 6, 6);
                    g.setFont(new Font("Monospaced", Font.BOLD, 9));
                    g.setColor(new Color(175, 205, 245));
                    drawCentered(g, "✦ OPEN CUSTOMIZE ATELIER", btnX + btnW / 2, btnY + 17);
                }
            }
        }

        if (!uiToast.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 225, 140));
            drawCentered(g, uiToast, WIDTH / 2, 492);
        }
        drawButton(g, "BACK", 390, 508, false);
    }

    private Rectangle customizeRotateLeftBounds() {
        return new Rectangle(100, 371, 104, 30);
    }

    private Rectangle customizeRotateRightBounds() {
        return new Rectangle(214, 371, 104, 30);
    }

    private Rectangle customizeTabBounds(int tab) {
        return new Rectangle(350 + tab * 145, 106, 135, 28);
    }

    private Rectangle customizeCardBounds(int index) {
        int col = index % 2;
        int row = index / 2;
        return new Rectangle(350 + col * 270, 142 + row * 108, 260, 100);
    }

    private int customizeCardAt(int x, int y) {
        int count = customizeTab == 0 ? LevelManager.OUTFIT_NAMES.length : LevelManager.ACCESSORY_NAMES.length;
        for (int i = 0; i < count; i++) {
            if (customizeCardBounds(i).contains(x, y)) return i;
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

    private void handleCustomizeAccessoryClick(int accessory) {
        SoundEffects.play(SoundEffects.Cue.CLICK);
        boolean wasUnlocked = levelManager.isAccessoryUnlocked(accessory);
        if (levelManager.selectOrBuyAccessory(accessory)) {
            uiToast = (wasUnlocked ? "EQUIPPED " : "UNLOCKED & EQUIPPED ")
                + LevelManager.ACCESSORY_NAMES[accessory].toUpperCase() + "!";
        } else {
            uiToast = "LOCKED: NEED " + LevelManager.ACCESSORY_COSTS[accessory] + " CREDITS.";
        }
        updateCursor();
        repaint();
    }

    private void drawCustomize(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 26));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, "SAFEHOUSE ATELIER: CUSTOMIZE OPERATIVE", WIDTH / 2, 60);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(195, 185, 205));
        drawCentered(g, "Rotate operative in mirror. Choose your stealth disguise & headwear.   CREDITS: "
            + levelManager.getTotalRewards() + " CR", WIDTH / 2, 78);

        int selectedOutfit = levelManager.getSelectedOutfit();
        int selectedAccessory = levelManager.getSelectedAccessory();

        // 1. Safehouse Dressing Mirror Frame (Left)
        int mx = 84, my = 106, mw = 246, mh = 356;
        g.setColor(new Color(24, 22, 30));
        g.fillRoundRect(mx, my, mw, mh, 10, 10);
        g.setColor(GOLD);
        g.drawRoundRect(mx, my, mw, mh, 10, 10);

        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(231, 190, 105));
        drawCentered(g, "SAFEHOUSE MIRROR", mx + mw / 2, my + 24);

        // Circular Pedestal Rug
        g.setColor(new Color(45, 36, 48));
        g.fillOval(mx + 43, my + 175, 160, 36);
        g.setColor(new Color(180, 140, 75));
        g.drawOval(mx + 47, my + 178, 152, 30);

        // Animated character in the mirror
        if (playerSpriteFrames != null && playerSpriteFrames.length > 0 && playerSpriteFrames[0].length > 0) {
            int frame = (int) (animationFrame / 7 % SPRITE_STRIP_FRAME_COUNT);
            int actionRow = ACTION_ROW_START[WALK_ACTION] + customizePreviewDirection;
            BufferedImage frameImg = getTintedPlayerFrame(playerSpriteFrames[actionRow][frame], selectedOutfit);
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(frameImg, mx + 68, my + 55, 110, 146, null);
            drawPlayerAccessory(g, mx + 68, my + 55, 110, 146, selectedAccessory, customizePreviewDirection);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
        }

        // Active outfit and accessory badges
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(255, 230, 165));
        drawCentered(g, LevelManager.OUTFIT_NAMES[selectedOutfit].toUpperCase(), mx + mw / 2, my + 234);

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(195, 215, 245));
        drawCentered(g, "GEAR: " + LevelManager.ACCESSORY_NAMES[selectedAccessory].toUpperCase(), mx + mw / 2, my + 249);

        g.setColor(new Color(120, 235, 160));
        drawCentered(g, "● WORN IN HEISTS", mx + mw / 2, my + 263);

        // Rotation Buttons below mirror
        Rectangle rotL = customizeRotateLeftBounds();
        Rectangle rotR = customizeRotateRightBounds();
        boolean rotLHover = hoverX >= 0 && hoverY >= 0 && rotL.contains(hoverX, hoverY);
        boolean rotRHover = hoverX >= 0 && hoverY >= 0 && rotR.contains(hoverX, hoverY);

        g.setColor(rotLHover ? new Color(75, 60, 42) : new Color(42, 36, 48));
        g.fillRoundRect(rotL.x, rotL.y, rotL.width, rotL.height, 5, 5);
        g.setColor(new Color(210, 175, 95));
        g.drawRoundRect(rotL.x, rotL.y, rotL.width, rotL.height, 5, 5);
        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(255, 235, 185));
        drawCentered(g, "◄ ROTATE", rotL.x + rotL.width / 2, rotL.y + 19);

        g.setColor(rotRHover ? new Color(75, 60, 42) : new Color(42, 36, 48));
        g.fillRoundRect(rotR.x, rotR.y, rotR.width, rotR.height, 5, 5);
        g.setColor(new Color(210, 175, 95));
        g.drawRoundRect(rotR.x, rotR.y, rotR.width, rotR.height, 5, 5);
        drawCentered(g, "ROTATE ►", rotR.x + rotR.width / 2, rotR.y + 19);

        // 2. Atelier Category Tabs (Right)
        String[] tabLabels = {"✦ HEIST DISGUISES (5)", "★ HEADWEAR & GEAR (6)"};
        for (int t = 0; t < 2; t++) {
            Rectangle tb = customizeTabBounds(t);
            boolean tabHover = hoverX >= 0 && hoverY >= 0 && tb.contains(hoverX, hoverY);
            boolean tabActive = customizeTab == t;
            g.setColor(tabActive ? new Color(60, 48, 36) : tabHover ? new Color(42, 38, 50) : new Color(26, 24, 32));
            g.fillRoundRect(tb.x, tb.y, tb.width, tb.height, 6, 6);
            g.setColor(tabActive ? GOLD : tabHover ? new Color(210, 175, 95) : new Color(75, 68, 75));
            g.drawRoundRect(tb.x, tb.y, tb.width, tb.height, 6, 6);
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(tabActive ? new Color(255, 235, 170) : new Color(185, 180, 195));
            drawCentered(g, tabLabels[t], tb.x + tb.width / 2, tb.y + 18);
        }

        // 3. Selection Cards
        if (customizeTab == 0) {
            String[][] outfitFlavors = {
                {"Midnight operative cloak.", "Blends into gallery shadows."},
                {"Museum staff disguise.", "Walk unnoticed past curatorial eyes."},
                {"Formal crimson velvet suit.", "Tailored for midnight gala heists."},
                {"Dark stealth shadow weave.", "Engineered for silent ghost runs."},
                {"Casual street rogue jacket.", "Nimble rogue urban mobility."}
            };

            for (int i = 0; i < LevelManager.OUTFIT_NAMES.length; i++) {
                Rectangle card = customizeCardBounds(i);
                boolean unlocked = levelManager.isOutfitUnlocked(i);
                boolean equipped = selectedOutfit == i;
                boolean hovered = hoverX >= 0 && hoverY >= 0 && card.contains(hoverX, hoverY);

                g.setColor(equipped ? new Color(52, 42, 34) : hovered ? new Color(42, 38, 50) : new Color(28, 26, 35));
                g.fillRoundRect(card.x, card.y, card.width, card.height, 8, 8);
                g.setColor(equipped ? GOLD : hovered ? new Color(210, 175, 95) : unlocked ? new Color(110, 150, 115) : new Color(75, 68, 75));
                g.drawRoundRect(card.x, card.y, card.width, card.height, 8, 8);

                // Left mini portrait box
                int pbx = card.x + 8, pby = card.y + 8, pbw = 58, pbh = 84;
                g.setColor(new Color(16, 14, 20));
                g.fillRoundRect(pbx, pby, pbw, pbh, 5, 5);
                g.setColor(new Color(55, 48, 58));
                g.drawRoundRect(pbx, pby, pbw, pbh, 5, 5);
                drawDisguiseMannequin(g, i, selectedAccessory, pbx + pbw / 2, pby + pbh / 2 + 10);

                // Right title & perk
                int tx = card.x + 74;
                g.setFont(new Font("Monospaced", Font.BOLD, 11));
                g.setColor(new Color(255, 225, 155));
                g.drawString(LevelManager.OUTFIT_NAMES[i].toUpperCase(), tx, card.y + 20);

                g.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g.setColor(new Color(185, 180, 195));
                g.drawString(outfitFlavors[i][0], tx, card.y + 35);
                g.drawString(outfitFlavors[i][1], tx, card.y + 48);

                // Action Button Pill
                int btnX = tx;
                int btnY = card.y + 60;
                int btnW = card.width - 82;
                int btnH = 28;

                if (equipped) {
                    g.setColor(new Color(25, 65, 38));
                    g.fillRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setColor(new Color(65, 215, 120));
                    g.drawRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setFont(new Font("Monospaced", Font.BOLD, 10));
                    g.setColor(new Color(130, 255, 170));
                    drawCentered(g, "✔ CURRENTLY WORN", btnX + btnW / 2, btnY + 18);
                } else if (unlocked) {
                    g.setColor(hovered ? new Color(85, 68, 45) : new Color(55, 45, 32));
                    g.fillRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setColor(new Color(235, 195, 105));
                    g.drawRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setFont(new Font("Monospaced", Font.BOLD, 10));
                    g.setColor(new Color(255, 235, 170));
                    drawCentered(g, "✦ EQUIP DISGUISE", btnX + btnW / 2, btnY + 18);
                } else {
                    boolean canAfford = levelManager.getTotalRewards() >= LevelManager.OUTFIT_COSTS[i];
                    g.setColor(canAfford ? (hovered ? new Color(110, 85, 25) : new Color(75, 55, 18)) : new Color(28, 26, 32));
                    g.fillRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setColor(canAfford ? new Color(255, 215, 100) : new Color(70, 65, 75));
                    g.drawRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setFont(new Font("Monospaced", Font.BOLD, 9));
                    g.setColor(canAfford ? new Color(255, 245, 210) : new Color(145, 140, 150));
                    drawCentered(g, canAfford ? "💰 UNLOCK - " + LevelManager.OUTFIT_COSTS[i] + " CR" : "🔒 LOCKED - " + LevelManager.OUTFIT_COSTS[i] + " CR", btnX + btnW / 2, btnY + 18);
                }
            }
        } else {
            String[][] accessoryFlavors = {
                {"Bare operative silhouette.", "No accessory worn on head."},
                {"Black bandit eye mask.", "Conceals identity in shadows."},
                {"Polished gold monocle.", "Inspect fine art details."},
                {"Tilted Parisian beret.", "Aesthetic flair of an art auteur."},
                {"Cyan tactical visor.", "Night-vision stealth optic HUD."},
                {"Canvas cross-body bag.", "Roomy storage for museum loot."}
            };

            for (int i = 0; i < LevelManager.ACCESSORY_NAMES.length; i++) {
                Rectangle card = customizeCardBounds(i);
                boolean unlocked = levelManager.isAccessoryUnlocked(i);
                boolean equipped = selectedAccessory == i;
                boolean hovered = hoverX >= 0 && hoverY >= 0 && card.contains(hoverX, hoverY);

                g.setColor(equipped ? new Color(52, 42, 34) : hovered ? new Color(42, 38, 50) : new Color(28, 26, 35));
                g.fillRoundRect(card.x, card.y, card.width, card.height, 8, 8);
                g.setColor(equipped ? GOLD : hovered ? new Color(210, 175, 95) : unlocked ? new Color(110, 150, 115) : new Color(75, 68, 75));
                g.drawRoundRect(card.x, card.y, card.width, card.height, 8, 8);

                // Left mini portrait box
                int pbx = card.x + 8, pby = card.y + 8, pbw = 58, pbh = 84;
                g.setColor(new Color(16, 14, 20));
                g.fillRoundRect(pbx, pby, pbw, pbh, 5, 5);
                g.setColor(new Color(55, 48, 58));
                g.drawRoundRect(pbx, pby, pbw, pbh, 5, 5);
                drawDisguiseMannequin(g, selectedOutfit, i, pbx + pbw / 2, pby + pbh / 2 + 10);

                // Right title & perk
                int tx = card.x + 74;
                g.setFont(new Font("Monospaced", Font.BOLD, 11));
                g.setColor(new Color(255, 225, 155));
                g.drawString(LevelManager.ACCESSORY_NAMES[i].toUpperCase(), tx, card.y + 20);

                g.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g.setColor(new Color(185, 180, 195));
                g.drawString(accessoryFlavors[i][0], tx, card.y + 35);
                g.drawString(accessoryFlavors[i][1], tx, card.y + 48);

                // Action Button Pill
                int btnX = tx;
                int btnY = card.y + 60;
                int btnW = card.width - 82;
                int btnH = 28;

                if (equipped) {
                    g.setColor(new Color(25, 65, 38));
                    g.fillRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setColor(new Color(65, 215, 120));
                    g.drawRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setFont(new Font("Monospaced", Font.BOLD, 10));
                    g.setColor(new Color(130, 255, 170));
                    drawCentered(g, "✔ CURRENTLY WORN", btnX + btnW / 2, btnY + 18);
                } else if (unlocked) {
                    g.setColor(hovered ? new Color(85, 68, 45) : new Color(55, 45, 32));
                    g.fillRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setColor(new Color(235, 195, 105));
                    g.drawRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setFont(new Font("Monospaced", Font.BOLD, 10));
                    g.setColor(new Color(255, 235, 170));
                    drawCentered(g, "✦ EQUIP GEAR", btnX + btnW / 2, btnY + 18);
                } else {
                    boolean canAfford = levelManager.getTotalRewards() >= LevelManager.ACCESSORY_COSTS[i];
                    g.setColor(canAfford ? (hovered ? new Color(110, 85, 25) : new Color(75, 55, 18)) : new Color(28, 26, 32));
                    g.fillRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setColor(canAfford ? new Color(255, 215, 100) : new Color(70, 65, 75));
                    g.drawRoundRect(btnX, btnY, btnW, btnH, 5, 5);
                    g.setFont(new Font("Monospaced", Font.BOLD, 9));
                    g.setColor(canAfford ? new Color(255, 245, 210) : new Color(145, 140, 150));
                    drawCentered(g, canAfford ? "💰 UNLOCK - " + LevelManager.ACCESSORY_COSTS[i] + " CR" : "🔒 LOCKED - " + LevelManager.ACCESSORY_COSTS[i] + " CR", btnX + btnW / 2, btnY + 18);
                }
            }
        }

        if (!uiToast.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 225, 140));
            drawCentered(g, uiToast, WIDTH / 2, 492);
        }
        drawButton(g, "BACK", 390, 508, false);
    }

    private Rectangle settingsRowBounds(int row) {
        return new Rectangle(160, 94 + row * 45, 640, 39);
    }

    private int settingsRowAt(int x, int y) {
        for (int i = 0; i < 5; i++) {
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
        } else if (row == 3) {
            SoundEffects.play(SoundEffects.Cue.CLICK);
            levelManager.setVisionConesEnabled(!levelManager.isVisionConesEnabled());
            uiToast = levelManager.isVisionConesEnabled() ? "GUARD VISION CONES VISIBLE." : "GUARD VISION CONES HIDDEN.";
        } else if (row == 4) {
            SoundEffects.play(SoundEffects.Cue.CLICK);
            levelManager.setPromptsEnabled(!levelManager.isPromptsEnabled());
            uiToast = levelManager.isPromptsEnabled() ? "ACTION PROMPTS VISIBLE." : "ACTION PROMPTS HIDDEN.";
        }
        updateCursor();
        repaint();
    }

    private void drawSettings(Graphics2D g) {
        drawBackdrop(g);
        g.setFont(new Font("Monospaced", Font.BOLD, 26));
        g.setColor(new Color(255, 220, 144));
        drawCentered(g, "SETTINGS & CONTROLS", WIDTH / 2, 54);
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(195, 185, 205));
        drawCentered(g, "Click any option below to toggle audio, display, and stealth preferences.", WIDTH / 2, 72);

        String[] labels = {
            "SOUND EFFECTS", "DISPLAY MODE", "GALLERY LIGHTING", "VISION CONES", "ACTION PROMPTS"
        };
        String[] descs = {
            "Master environmental and sneaking audio cues",
            "Toggle between windowed and borderless fullscreen",
            "Atmospheric dark gallery shadows vs high visibility",
            "Show directional guard flashlight sight beams",
            "Display key indicators above loot, targets and hiding spots"
        };
        boolean[] states = {
            !SoundEffects.isMuted(),
            Main.isFullScreen(),
            levelManager.isLightingEnabled(),
            levelManager.isVisionConesEnabled(),
            levelManager.isPromptsEnabled()
        };
        String[] stateLabels = {
            SoundEffects.isMuted() ? "MUTED" : "ENABLED",
            Main.isFullScreen() ? "FULL SCREEN" : "WINDOWED",
            levelManager.isLightingEnabled() ? "VIGNETTE" : "HIGH VISIBILITY",
            levelManager.isVisionConesEnabled() ? "VISIBLE" : "HIDDEN",
            levelManager.isPromptsEnabled() ? "VISIBLE" : "HIDDEN"
        };

        for (int i = 0; i < 5; i++) {
            Rectangle row = settingsRowBounds(i);
            boolean hovered = hoverX >= 0 && hoverY >= 0 && row.contains(hoverX, hoverY);

            g.setColor(hovered ? new Color(45, 40, 52) : new Color(28, 26, 35));
            g.fillRoundRect(row.x, row.y, row.width, row.height, 6, 6);
            g.setColor(hovered ? GOLD : new Color(110, 95, 80));
            g.drawRoundRect(row.x, row.y, row.width, row.height, 6, 6);

            // Left labels
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 225, 155));
            g.drawString(labels[i], row.x + 18, row.y + 17);

            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g.setColor(new Color(175, 170, 185));
            g.drawString(descs[i], row.x + 18, row.y + 31);

            // Right Toggle Switch Pill
            int swW = 140, swH = 26;
            int swX = row.x + row.width - swW - 14, swY = row.y + (row.height - swH) / 2;

            boolean on = states[i];
            g.setColor(on ? new Color(25, 65, 38) : new Color(45, 35, 38));
            g.fillRoundRect(swX, swY, swW, swH, 5, 5);
            g.setColor(on ? new Color(65, 215, 120) : new Color(180, 90, 90));
            g.drawRoundRect(swX, swY, swW, swH, 5, 5);

            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(on ? new Color(130, 255, 170) : new Color(240, 140, 140));
            drawCentered(g, (on ? "● " : "○ ") + stateLabels[i], swX + swW / 2, swY + 17);
        }

        // Tactile Keybindings Console
        int kbx = 120, kby = 330, kbw = 720, kbh = 145;
        g.setColor(new Color(22, 20, 28));
        g.fillRoundRect(kbx, kby, kbw, kbh, 8, 8);
        g.setColor(new Color(85, 75, 68));
        g.drawRoundRect(kbx, kby, kbw, kbh, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(231, 190, 105));
        drawCentered(g, "KEYBOARD & HEIST CONTROLS", kbx + kbw / 2, kby + 20);

        // Cheatsheet items
        String[][] controls = {
            {"[WASD / ARROWS]", "Move Operative"},
            {"[SHIFT / R]", "Sprint (Fast, Loud)"},
            {"[CTRL / C]", "Sneak (Silent Footsteps)"},
            {"[E]", "Forge, Swap, Steal & Hide"},
            {"[Q]", "Throw Distraction Coin"},
            {"[G]", "Deploy Smoke Screen"},
            {"[P / ESC]", "Pause / Back"},
            {"[F11]", "Toggle Fullscreen"}
        };

        for (int c = 0; c < controls.length; c++) {
            int col = c / 4;
            int r = c % 4;
            int cx = kbx + 30 + col * 350;
            int cy = kby + 44 + r * 23;

            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(new Color(255, 220, 140));
            g.drawString(controls[c][0], cx, cy);

            g.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g.setColor(new Color(195, 190, 205));
            g.drawString(controls[c][1], cx + 130, cy);
        }

        if (!uiToast.isEmpty()) {
            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 225, 140));
            drawCentered(g, uiToast, WIDTH / 2, 492);
        }
        drawButton(g, "BACK", 390, 508, false);
    }

    private Rectangle galleryWingTabBounds(int wing) {
        int w = 158;
        int gap = 8;
        int startX = (WIDTH - (5 * w + 4 * gap)) / 2;
        return new Rectangle(startX + wing * (w + gap), 82, w, 34);
    }

    private int galleryWingTabAt(int x, int y) {
        for (int i = 0; i < 5; i++) {
            if (galleryWingTabBounds(i).contains(x, y)) return i;
        }
        return -1;
    }

    private Rectangle galleryPaintingBounds(int index, int total) {
        if (total == 1) {
            return new Rectangle(120, 126, 720, 240);
        }
        int col = index % 2;
        int row = index / 2;
        return new Rectangle(64 + col * 428, 124 + row * 174, 404, 162);
    }

    private String galleryPaintingAt(int x, int y) {
        MuseumLevel level = MuseumLevel.get(gallerySelectedWing);
        java.util.List<String> list = level.getPaintingNames();
        for (int i = 0; i < list.size(); i++) {
            if (galleryPaintingBounds(i, list.size()).contains(x, y)) {
                return list.get(i);
            }
        }
        return null;
    }

    private Rectangle galleryInspectCloseBounds() {
        return new Rectangle(370, 485, 220, 36);
    }

    private void drawPictureFrame(Graphics2D g, int fx, int fy, int fw, int fh, String pName, boolean isCollected) {
        // Baroque gold frame
        g.setColor(new Color(135, 100, 45));
        g.fillRoundRect(fx, fy, fw, fh, 6, 6);
        g.setColor(new Color(235, 195, 105));
        g.drawRoundRect(fx, fy, fw, fh, 6, 6);
        g.setColor(new Color(75, 50, 20));
        g.drawRoundRect(fx + 2, fy + 2, fw - 4, fh - 4, 4, 4);

        drawCornerRivet(g, fx + 5, fy + 5);
        drawCornerRivet(g, fx + fw - 5, fy + 5);
        drawCornerRivet(g, fx + 5, fy + fh - 5);
        drawCornerRivet(g, fx + fw - 5, fy + fh - 5);

        // Canvas inset
        int cx = fx + 8, cy = fy + 8, cw = fw - 16, ch = fh - 16;
        g.setColor(new Color(12, 11, 15));
        g.fillRect(cx, cy, cw, ch);

        if (isCollected) {
            BufferedImage art = MasterpieceCatalog.getMasterpiece(pName);
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(art, cx, cy, cw, ch, null);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);

            // Overhead soft spotlight cone
            g.setPaint(new GradientPaint(cx + cw / 2, cy - 8, new Color(255, 240, 180, 50), cx + cw / 2, cy + ch, new Color(255, 240, 180, 0)));
            int[] spX = {cx + cw / 2 - 14, cx + cw / 2 + 14, cx + cw, cx};
            int[] spY = {cy, cy, cy + ch, cy + ch};
            g.fillPolygon(spX, spY, 4);
        } else {
            // Velvet shadow drape
            g.setColor(new Color(22, 20, 28));
            g.fillRect(cx, cy, cw, ch);
            g.setColor(new Color(36, 32, 44));
            for (int ly = cy + 6; ly < cy + ch; ly += 10) g.drawLine(cx + 4, ly, cx + cw - 4, ly);

            int lcx = cx + cw / 2, lcy = cy + ch / 2 - 6;
            g.setColor(new Color(210, 165, 60));
            g.fillRoundRect(lcx - 10, lcy, 20, 16, 4, 4);
            g.setColor(new Color(255, 230, 140));
            g.drawRoundRect(lcx - 10, lcy, 20, 16, 4, 4);
            g.setColor(new Color(40, 28, 10));
            g.fillOval(lcx - 2, lcy + 5, 4, 4);
            g.drawLine(lcx, lcy + 7, lcx, lcy + 11);

            g.setColor(new Color(210, 165, 60));
            g.setStroke(new BasicStroke(2.0f));
            g.drawArc(lcx - 6, lcy - 10, 12, 14, 0, 180);
            g.setStroke(new BasicStroke(1.0f));

            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            g.setColor(new Color(185, 175, 165));
            drawCentered(g, "UNCLAIMED HEIST", cx + cw / 2, lcy + 28);
        }
    }

    private void drawMuseumDossierPlaque(Graphics2D g, int px, int py, int pw, int ph, String pName, int wing, boolean isCollected, int score, int time) {
        g.setColor(new Color(32, 28, 38));
        g.fillRoundRect(px, py, pw, ph, 8, 8);
        g.setColor(new Color(190, 150, 80));
        g.drawRoundRect(px, py, pw, ph, 8, 8);
        g.setColor(new Color(75, 55, 30));
        g.drawRoundRect(px + 2, py + 2, pw - 4, ph - 4, 6, 6);

        int tx = px + 20;
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(210, 175, 100));
        g.drawString("MUSEUM ACCESSION DOSSIER", tx, py + 26);

        g.setFont(new Font("Monospaced", Font.BOLD, 17));
        g.setColor(new Color(255, 230, 155));
        g.drawString(pName.toUpperCase(), tx, py + 48);

        g.setFont(new Font("Monospaced", Font.ITALIC, 11));
        g.setColor(new Color(205, 195, 215));
        g.drawString(MasterpieceCatalog.getArtist(pName) + "  •  Oil on Belgian Linen", tx, py + 68);

        int estVal = 250000 + (wing * 100000);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(245, 215, 120));
        g.drawString("ESTIMATED BLACK MARKET VALUE: $" + String.format("%,d", estVal), tx, py + 95);

        // Lore description
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(185, 180, 195));
        g.drawString("Acquired under high-security surveillance in the Classic Gallery.", tx, py + 118);
        g.drawString("The museum staff remains baffled by the swapped counterfeit.", tx, py + 132);

        // Status ribbon
        if (isCollected) {
            g.setColor(new Color(22, 60, 35));
            g.fillRoundRect(tx, py + 150, pw - 40, 32, 6, 6);
            g.setColor(new Color(65, 215, 120));
            g.drawRoundRect(tx, py + 150, pw - 40, 32, 6, 6);
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(new Color(130, 255, 170));
            g.drawString("✔ SECURED IN VAULT  |  RECORD: " + time / 60 + ":" + String.format("%02d", time % 60) + " (" + score + " PTS)", tx + 14, py + 170);
        } else {
            g.setColor(new Color(50, 28, 30));
            g.fillRoundRect(tx, py + 150, pw - 40, 32, 6, 6);
            g.setColor(new Color(220, 80, 80));
            g.drawRoundRect(tx, py + 150, pw - 40, 32, 6, 6);
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(new Color(255, 140, 140));
            g.drawString("🔒 HANGING IN THE MUSEUM  |  INFILTRATE & FORGE TO CLAIM", tx + 14, py + 170);
        }

        // Inspect Button
        int bx = tx, by = py + 192, bw = pw - 40, bh = 26;
        g.setColor(new Color(60, 48, 34));
        g.fillRoundRect(bx, by, bw, bh, 5, 5);
        g.setColor(new Color(230, 190, 100));
        g.drawRoundRect(bx, by, bw, bh, 5, 5);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(255, 235, 175));
        drawCentered(g, "🔍 CLICK TO INSPECT MASTERPIECE CANVAS", bx + bw / 2, by + 17);
    }

    private void drawArtInspectorModal(Graphics2D g, String paintingName) {
        g.setColor(new Color(6, 8, 14, 235));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        int mx = 150, my = 35, mw = 660, mh = 530;
        g.setColor(new Color(22, 20, 28));
        g.fillRoundRect(mx, my, mw, mh, 12, 12);
        g.setColor(new Color(215, 175, 90));
        g.drawRoundRect(mx, my, mw, mh, 12, 12);
        g.setColor(new Color(80, 60, 30));
        g.drawRoundRect(mx + 3, my + 3, mw - 6, mh - 6, 10, 10);

        // Header
        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(new Color(255, 225, 145));
        drawCentered(g, "MASTERPIECE DOSSIER: " + paintingName.toUpperCase(), mx + mw / 2, my + 26);

        // Large 2x Canvas (448 x 272)
        int cx = mx + (mw - 448) / 2;
        int cy = my + 44;

        // Gold frame
        g.setColor(new Color(140, 105, 45));
        g.fillRoundRect(cx - 8, cy - 8, 448 + 16, 272 + 16, 8, 8);
        g.setColor(new Color(245, 215, 120));
        g.drawRoundRect(cx - 8, cy - 8, 448 + 16, 272 + 16, 8, 8);

        BufferedImage art = MasterpieceCatalog.getMasterpiece(paintingName);
        Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(art, cx, cy, 448, 272, null);
        if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);

        // Lower Dossier Info
        int dy = cy + 272 + 20;
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(255, 225, 155));
        g.drawString(paintingName, cx, dy + 15);

        g.setFont(new Font("Monospaced", Font.ITALIC, 11));
        g.setColor(new Color(205, 195, 215));
        g.drawString(MasterpieceCatalog.getArtist(paintingName) + "  •  Oil on Belgian Linen", cx, dy + 32);

        // Color Palette Swatches
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(230, 195, 120));
        g.drawString("AUTHENTIC PIGMENTS:", cx, dy + 56);

        Color[] pal = MasterpieceCatalog.getPalette(paintingName);
        for (int p = 0; p < pal.length; p++) {
            int sx = cx + 140 + p * 24;
            g.setColor(pal[p]);
            g.fillRect(sx, dy + 44, 18, 16);
            g.setColor(new Color(240, 220, 160));
            g.drawRect(sx, dy + 44, 18, 16);
        }

        // Close Button
        Rectangle closeBtn = galleryInspectCloseBounds();
        boolean closeHover = hoverX >= 0 && hoverY >= 0 && closeBtn.contains(hoverX, hoverY);
        g.setColor(closeHover ? new Color(90, 42, 42) : new Color(60, 32, 32));
        g.fillRoundRect(closeBtn.x, closeBtn.y, closeBtn.width, closeBtn.height, 6, 6);
        g.setColor(new Color(230, 120, 120));
        g.drawRoundRect(closeBtn.x, closeBtn.y, closeBtn.width, closeBtn.height, 6, 6);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(255, 220, 220));
        drawCentered(g, "✕ CLOSE INSPECTION (ESC)", closeBtn.x + closeBtn.width / 2, closeBtn.y + 22);
    }

    private void drawGallery(Graphics2D g) {
        drawBackdrop(g);

        // Header Title
        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.setColor(new Color(255, 222, 140));
        drawCentered(g, "STOLEN MASTERPIECES VAULT", WIDTH / 2, 56);

        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(195, 185, 205));
        drawCentered(g, "Private underground reserve. Click any acquired masterpiece to inspect.", WIDTH / 2, 72);

        // Wing Selection Tabs (5 wings)
        String[] shortNames = {"1. CLASSIC", "2. GRAND", "3. ROYAL", "4. ANCIENT", "5. NATIONAL"};
        int totalTabs = MuseumLevel.all().size();
        for (int i = 0; i < totalTabs; i++) {
            Rectangle tab = galleryWingTabBounds(i);
            boolean selected = i == gallerySelectedWing;
            boolean hovered = hoverX >= 0 && hoverY >= 0 && tab.contains(hoverX, hoverY);
            boolean unlocked = levelManager.isUnlocked(i);
            int bestScore = levelManager.getBestScore(i);
            boolean collected = bestScore > 0;

            g.setColor(selected ? new Color(60, 48, 36) : hovered ? new Color(42, 38, 50) : new Color(26, 24, 32));
            g.fillRoundRect(tab.x, tab.y, tab.width, tab.height, 6, 6);
            g.setColor(selected ? GOLD : hovered ? new Color(210, 175, 100) : collected ? new Color(110, 160, 120) : new Color(75, 65, 60));
            g.drawRoundRect(tab.x, tab.y, tab.width, tab.height, 6, 6);

            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(selected ? new Color(255, 230, 160) : unlocked ? new Color(215, 205, 220) : new Color(120, 115, 125));
            String shortName = i < shortNames.length ? shortNames[i] : ((i + 1) + ". WING " + (i + 1));
            drawCentered(g, shortName, tab.x + tab.width / 2, tab.y + 13);

            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            if (collected) {
                g.setColor(new Color(120, 235, 160));
                drawCentered(g, "★ CLAIMED", tab.x + tab.width / 2, tab.y + 25);
            } else if (unlocked) {
                g.setColor(new Color(235, 195, 110));
                drawCentered(g, "UNCLAIMED", tab.x + tab.width / 2, tab.y + 25);
            } else {
                g.setColor(new Color(130, 125, 135));
                drawCentered(g, "🔒 LOCKED", tab.x + tab.width / 2, tab.y + 25);
            }
        }

        // Active Wing Content
        MuseumLevel level = MuseumLevel.get(gallerySelectedWing);
        java.util.List<String> paintings = level.getPaintingNames();
        int wingBestScore = levelManager.getBestScore(gallerySelectedWing);
        int wingBestTime = levelManager.getBestTime(gallerySelectedWing);
        boolean isWingCollected = wingBestScore > 0;

        if (paintings.size() == 1) {
            // Grand Centerpiece layout for Level 0 (1 painting: Moonlit Orchard)
            String pName = paintings.get(0);
            int fx = 120, fy = 126, fw = 350, fh = 240;

            drawPictureFrame(g, fx, fy, fw, fh, pName, isWingCollected);

            int px = 490, py = 126, pw = 350, ph = 240;
            drawMuseumDossierPlaque(g, px, py, pw, ph, pName, gallerySelectedWing, isWingCollected, wingBestScore, wingBestTime);
        } else {
            // 2x2 Grand Exhibition layout for 3 or 4 paintings
            for (int i = 0; i < 4; i++) {
                Rectangle card = galleryPaintingBounds(i, paintings.size());
                boolean hovered = hoverX >= 0 && hoverY >= 0 && card.contains(hoverX, hoverY);

                if (i < paintings.size()) {
                    String pName = paintings.get(i);
                    g.setColor(hovered ? new Color(38, 34, 46) : new Color(25, 24, 32));
                    g.fillRoundRect(card.x, card.y, card.width, card.height, 8, 8);
                    g.setColor(hovered ? GOLD : isWingCollected ? new Color(140, 115, 75) : new Color(75, 65, 60));
                    g.drawRoundRect(card.x, card.y, card.width, card.height, 8, 8);

                    // Left frame
                    int fx = card.x + 12, fy = card.y + 12, fw = 176, fh = 138;
                    drawPictureFrame(g, fx, fy, fw, fh, pName, isWingCollected);

                    // Right plaque info
                    int tx = card.x + 200;
                    g.setFont(new Font("Monospaced", Font.BOLD, 12));
                    g.setColor(new Color(255, 225, 155));
                    g.drawString(pName, tx, card.y + 26);

                    g.setFont(new Font("Monospaced", Font.PLAIN, 10));
                    g.setColor(new Color(185, 180, 195));
                    g.drawString(MasterpieceCatalog.getArtist(pName), tx, card.y + 42);

                    int estValue = 180000 + (gallerySelectedWing * 90000) + (i * 35000);
                    g.setFont(new Font("Monospaced", Font.BOLD, 10));
                    g.setColor(new Color(245, 215, 120));
                    g.drawString("EST. VALUE: $" + String.format("%,d", estValue), tx, card.y + 64);

                    if (isWingCollected) {
                        g.setFont(new Font("Monospaced", Font.BOLD, 10));
                        g.setColor(new Color(120, 235, 160));
                        g.drawString("✔ SECURED IN VAULT", tx, card.y + 84);
                    } else {
                        g.setFont(new Font("Monospaced", Font.BOLD, 10));
                        g.setColor(new Color(230, 100, 90));
                        g.drawString("🔒 TARGET IN MUSEUM", tx, card.y + 84);
                    }

                    // Inspect Button Pill
                    int bx = tx, by = card.y + 114, bw = 185, bh = 28;
                    g.setColor(hovered ? new Color(85, 68, 42) : new Color(55, 45, 32));
                    g.fillRoundRect(bx, by, bw, bh, 5, 5);
                    g.setColor(new Color(220, 180, 90));
                    g.drawRoundRect(bx, by, bw, bh, 5, 5);
                    g.setFont(new Font("Monospaced", Font.BOLD, 10));
                    g.setColor(new Color(255, 235, 175));
                    drawCentered(g, "🔍 CLICK TO INSPECT", bx + bw / 2, by + 18);
                } else {
                    // Reserve Exhibition slot
                    g.setColor(new Color(20, 19, 25));
                    g.fillRoundRect(card.x, card.y, card.width, card.height, 8, 8);
                    g.setColor(new Color(55, 50, 58));
                    g.drawRoundRect(card.x, card.y, card.width, card.height, 8, 8);
                    g.setFont(new Font("Monospaced", Font.BOLD, 11));
                    g.setColor(new Color(125, 120, 135));
                    drawCentered(g, "VAULT RESERVE BAY", card.x + card.width / 2, card.y + 70);
                    g.setFont(new Font("Monospaced", Font.PLAIN, 9));
                    drawCentered(g, "Additional contraband displays coming soon.", card.x + card.width / 2, card.y + 90);
                }
            }
        }

        drawButton(g, "BACK", 390, 508, false);

        // Art Inspector Modal if active
        if (galleryInspectedPainting != null) {
            drawArtInspectorModal(g, galleryInspectedPainting);
        }
    }

    private void drawCuratorInspection(Graphics2D g) {
        drawBackdrop(g);

        // Header Title
        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.setColor(new Color(255, 222, 140));
        drawCentered(g, "CURATOR'S AUTHENTICATION LAB", WIDTH / 2, 54);

        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(195, 185, 205));
        drawCentered(g, "Interpol Art Division & Museum Curators examining recovered exhibits...", WIDTH / 2, 70);

        MuseumMap.Painting stolen = null;
        if (world != null && world.getMap() != null) {
            for (MuseumMap.Painting p : world.getMap().getPaintings()) {
                if (p.isStolen()) {
                    stolen = p;
                    break;
                }
            }
        }

        String pName = stolen != null ? stolen.getName() : "Moonlit Orchard";
        int quality = stolen != null ? stolen.getQuality() : 85;
        String artist = MasterpieceCatalog.getArtist(pName);
        BufferedImage originalArt = MasterpieceCatalog.getMasterpiece(pName);
        BufferedImage fakeArt = stolen != null && stolen.getFakeImage() != null ? stolen.getFakeImage() : originalArt;

        // --- LEFT FRAME: AUTHENTIC ORIGINAL ---
        int lx = 70, ly = 88, lw = 396, lh = 228;
        drawPictureFrame(g, lx, ly, lw, lh, pName, true);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(255, 220, 130));
        drawCentered(g, "🏛 ORIGINAL: " + pName.toUpperCase(), lx + lw / 2, ly + 20);

        // --- RIGHT FRAME: OPERATIVE'S RECREATED FORGERY ---
        int rx = 494, ry = 88, rw = 396, rh = 228;
        g.setColor(new Color(135, 100, 45));
        g.fillRoundRect(rx, ry, rw, rh, 6, 6);
        g.setColor(new Color(235, 195, 105));
        g.drawRoundRect(rx, ry, rw, rh, 6, 6);
        g.setColor(new Color(75, 50, 20));
        g.drawRoundRect(rx + 2, ry + 2, rw - 4, rh - 4, 4, 4);
        drawCornerRivet(g, rx + 5, ry + 5);
        drawCornerRivet(g, rx + rw - 5, ry + 5);
        drawCornerRivet(g, rx + 5, ry + rh - 5);
        drawCornerRivet(g, rx + rw - 5, ry + rh - 5);

        int rcx = rx + 8, rcy = ry + 26, rcw = rw - 16, rch = rh - 34;
        g.setColor(new Color(12, 11, 15));
        g.fillRect(rcx, rcy, rcw, rch);
        if (fakeArt != null) {
            Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(fakeArt, rcx, rcy, rcw, rch, null);
            if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
        }

        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(130, 240, 175));
        drawCentered(g, "🖌 OPERATIVE'S FORGERY (" + quality + "% MATCH)", rx + rw / 2, ry + 20);

        // Animated Magnifying Loupe Scanner on Forgery Canvas
        double sweep = (Math.sin(animationFrame * 0.05) + 1.0) / 2.0;
        int loupeX = rcx + 28 + (int) (sweep * (rcw - 56));
        int loupeY = rcy + rch / 2 + (int) (Math.sin(animationFrame * 0.08) * 22);

        g.setColor(new Color(255, 255, 255, 35));
        g.fillOval(loupeX - 22, loupeY - 22, 44, 44);
        g.setColor(new Color(255, 215, 100, 210));
        g.setStroke(new BasicStroke(2.0f));
        g.drawOval(loupeX - 22, loupeY - 22, 44, 44);
        g.setStroke(new BasicStroke(1.0f));

        g.setColor(new Color(255, 230, 140, 170));
        g.drawLine(loupeX - 10, loupeY, loupeX + 10, loupeY);
        g.drawLine(loupeX, loupeY - 10, loupeX, loupeY + 10);

        g.setColor(new Color(205, 160, 75));
        g.setStroke(new BasicStroke(3.0f));
        g.drawLine(loupeX + 16, loupeY + 16, loupeX + 30, loupeY + 30);
        g.setStroke(new BasicStroke(1.0f));

        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        g.setColor(new Color(255, 235, 170));
        drawCentered(g, "🔍 ANALYZING PIGMENT", loupeX, Math.max(ry + 32, loupeY - 26));

        // --- CENTER ASSESSMENT DOSSIER ---
        int cx = 70, cy = 328, cw = 820, ch = 162;
        g.setColor(new Color(25, 23, 33, 240));
        g.fillRoundRect(cx, cy, cw, ch, 8, 8);
        g.setColor(new Color(180, 140, 75));
        g.drawRoundRect(cx, cy, cw, ch, 8, 8);
        g.setColor(new Color(75, 55, 30));
        g.drawRoundRect(cx + 2, cy + 2, cw - 4, ch - 4, 6, 6);

        // Panel 1: Accuracy Gauge (Left, w = 220)
        int p1x = cx + 18, p1y = cy + 12;
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(210, 175, 100));
        drawCentered(g, "REPLICATION ACCURACY", p1x + 100, p1y + 12);

        g.setFont(new Font("Monospaced", Font.BOLD, 30));
        Color accColor = quality >= 80 ? new Color(110, 235, 150)
            : quality >= 60 ? new Color(245, 205, 90) : new Color(240, 90, 80);
        g.setColor(accColor);
        drawCentered(g, quality + "%", p1x + 100, p1y + 42);

        int colorScore = stolen != null && stolen.getBreakdown() != null ? stolen.getBreakdown().getColorScore() : quality;
        int shapeScore = stolen != null && stolen.getBreakdown() != null ? stolen.getBreakdown().getShapeScore() : quality;
        int posScore = stolen != null && stolen.getBreakdown() != null ? stolen.getBreakdown().getPositionScore() : quality;

        drawMiniPillarBar(g, "COLOR 35%", colorScore, p1x + 10, p1y + 54, 180);
        drawMiniPillarBar(g, "SHAPE 35%", shapeScore, p1x + 10, p1y + 80, 180);
        drawMiniPillarBar(g, "ALIGN 30%", posScore, p1x + 10, p1y + 106, 180);

        // Panel 2: Curator's Verdict (Center, w = 360)
        int p2x = cx + 240, p2y = cy + 16;
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(210, 175, 100));
        g.drawString("CURATOR'S LAB EVALUATION", p2x, p2y + 14);

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(215, 205, 220));
        if (quality >= 80) {
            g.drawString("Chief Curator: 'The pigment blend and stroke fidelity are", p2x, p2y + 36);
            g.drawString("indistinguishable from the 19th century master.'", p2x, p2y + 50);
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(new Color(110, 235, 150));
            g.drawString("✔ FOOLED MUSEUM STAFF  —  UNDETECTED ON EXHIBIT", p2x, p2y + 72);
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g.setColor(new Color(175, 170, 185));
            g.drawString("Security reported zero anomalies during shift change.", p2x, p2y + 92);
        } else if (quality >= 60) {
            g.drawString("Chief Curator: 'The canvas appears authentic at glance,", p2x, p2y + 36);
            g.drawString("though subtle color variations arouse minor suspicion.'", p2x, p2y + 50);
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(new Color(245, 205, 90));
            g.drawString("⚠️ ACCEPTED WITH INVESTIGATION  —  SECURITY ON ALERT", p2x, p2y + 72);
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g.setColor(new Color(175, 170, 185));
            g.drawString("Forensic audit scheduled in 48 hours. Operative safe.", p2x, p2y + 92);
        } else {
            g.drawString("Chief Curator: 'A grotesque forgery! The linseed oil is", p2x, p2y + 36);
            g.drawString("still tacky and the brush strokes are completely off!'", p2x, p2y + 50);
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(new Color(240, 90, 80));
            g.drawString("🚨 FORGERY EXPOSED  —  MUSEUM ALARMS BLARING", p2x, p2y + 72);
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g.setColor(new Color(175, 170, 185));
            g.drawString("Interpol was alerted immediately. Cut reduced for damage control.", p2x, p2y + 92);
        }

        // Panel 3: Black Market Bounty (Right, w = 190)
        int p3x = cx + 620, p3y = cy + 16;
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(210, 175, 100));
        g.drawString("BLACK MARKET FENCE", p3x, p3y + 14);

        int estVal = 250000 + (selectedLevel * 100000);
        int payout = (int) (estVal * (quality / 100.0));
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(255, 225, 155));
        g.drawString("VALUATION: $" + String.format("%,d", estVal), p3x, p3y + 36);
        g.setColor(new Color(110, 235, 150));
        g.drawString("FENCE CUT: $" + String.format("%,d", payout), p3x, p3y + 54);

        int rewardCredits = lastResult != null ? lastResult.getReward()
            : (quality >= 80 ? 4 : quality >= 60 ? 2 : 1);
        g.setColor(new Color(255, 215, 0));
        g.drawString("CREDITS:   +" + rewardCredits + " CR", p3x, p3y + 72);

        // Wax seal badge
        int sealW = 160, sealH = 26;
        int sealX = p3x, sealY = p3y + 88;
        g.setColor(quality >= 80 ? new Color(20, 55, 30) : quality >= 60 ? new Color(55, 42, 18) : new Color(55, 20, 20));
        g.fillRoundRect(sealX, sealY, sealW, sealH, 6, 6);
        g.setColor(accColor);
        g.drawRoundRect(sealX, sealY, sealW, sealH, 6, 6);
        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        drawCentered(g, quality >= 80 ? "✔ AUTHENTICATED" : quality >= 60 ? "⚠️ CAUTION" : "✖ REJECTED",
            sealX + sealW / 2, sealY + 17);

        // Bottom Continue Button
        drawButton(g, "READ CHRONICLE ➔", 390, 508, false);
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
        int dosH = 205;

        g.setColor(new Color(236, 230, 216));
        g.fillRoundRect(dosX, dosY, dosW, dosH, 6, 6);
        g.setColor(new Color(185, 170, 150));
        g.drawRoundRect(dosX, dosY, dosW, dosH, 6, 6);

        // 3 Gold Stars with 3D drop shadow
        int stars = lastResult == null ? 0 : lastResult.getStars();
        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.setColor(new Color(60, 45, 20, 80));
        drawCentered(g, starString(stars), dosX + dosW / 2 + 1, dosY + 26);
        g.setColor(new Color(218, 165, 32));
        drawCentered(g, starString(stars), dosX + dosW / 2, dosY + 25);

        // Star criteria legend
        g.setFont(new Font("Monospaced", Font.PLAIN, 8));
        g.setColor(new Color(110, 95, 80));
        String starCriteria = stars == 3 ? "★★★ FLAWLESS GHOST HEIST (MAX RATING)"
            : stars == 2 ? "★★ EXCELLENT FORGERY (CLEARED ≥70%)" : "★ HEIST COMPLETED (CLEARED)";
        drawCentered(g, starCriteria, dosX + dosW / 2, dosY + 38);

        // Ledger breakdown
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(35, 30, 25));
        drawCentered(g, "BLACK-MARKET FENCE LEDGER", dosX + dosW / 2, dosY + 54);

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(55, 48, 42));
        int rowY = dosY + 70;
        int stolenCount = lastResult != null ? lastResult.getPaintingsStolen() : 1;
        int targetCount = lastResult != null ? lastResult.getTargetPaintings() : 1;
        int avgAcc = lastResult != null ? lastResult.getAverageAccuracy() : 85;
        int colorAcc = lastResult != null ? lastResult.getColorAccuracy() : avgAcc;
        int shapeAcc = lastResult != null ? lastResult.getShapeAccuracy() : avgAcc;
        int posAcc = lastResult != null ? lastResult.getPositionAccuracy() : avgAcc;
        int alertVal = lastResult != null ? lastResult.getDetectionIncidents() : 0;
        int timeVal = lastResult != null ? lastResult.getTimeRemaining() : 60;
        int elapsedVal = lastResult != null ? lastResult.getElapsedSeconds() : 45;

        int stolenLoot = lastResult != null ? lastResult.getStolenLoot() : 0;
        int totalLoot = lastResult != null ? lastResult.getTotalLoot() : 0;
        int bonusLoot = lastResult != null ? lastResult.getBonusLoot() : 0;
        int lootR = lastResult != null ? lastResult.getLootReward() : 0;

        g.drawString("EXHIBITS SECURED", dosX + 14, rowY);
        g.drawString(stolenCount + "/" + targetCount + " SWAPPED", dosX + dosW - 95, rowY);
        rowY += 15;

        g.drawString("FORGERY MATCH", dosX + 14, rowY);
        g.setColor(avgAcc >= 85 ? new Color(25, 120, 50) : avgAcc >= 70 ? new Color(160, 110, 20) : new Color(160, 40, 30));
        g.drawString(avgAcc + "% [C:" + colorAcc + " S:" + shapeAcc + " P:" + posAcc + "]", dosX + dosW - 130, rowY);
        rowY += 15;

        g.setColor(new Color(55, 48, 42));
        g.drawString("VALUABLES LOOTED", dosX + 14, rowY);
        if (totalLoot > 0) {
            g.setColor(stolenLoot == totalLoot ? new Color(25, 120, 50) : new Color(180, 120, 30));
            g.drawString(stolenLoot + "/" + totalLoot + " ($" + bonusLoot + ")", dosX + dosW - 105, rowY);
        } else {
            g.drawString("NONE IN WING", dosX + dosW - 95, rowY);
        }
        rowY += 15;

        g.setColor(new Color(55, 48, 42));
        g.drawString("STEALTH STATUS", dosX + 14, rowY);
        if (alertVal == 0) {
            g.setColor(new Color(25, 120, 50));
            g.drawString("GHOST (0 ALERTS)", dosX + dosW - 110, rowY);
        } else {
            g.setColor(new Color(160, 50, 40));
            g.drawString(alertVal + " DETECTION(S)", dosX + dosW - 105, rowY);
        }
        rowY += 15;

        g.setColor(new Color(55, 48, 42));
        g.drawString("TIME ELAPSED", dosX + 14, rowY);
        g.drawString(elapsedVal + "s (" + timeVal + "s left)", dosX + dosW - 115, rowY);
        rowY += 15;

        g.drawString("BONUS BREAKDOWN", dosX + 14, rowY);
        int baseR = lastResult != null ? lastResult.getBaseReward() : 15;
        int accR = lastResult != null ? lastResult.getAccuracyBonus() : 15;
        int stR = lastResult != null ? lastResult.getStealthBonus() : 15;
        int tmR = lastResult != null ? lastResult.getTimeBonus() : 5;
        g.setColor(new Color(30, 100, 50));
        String bonusStr = "+" + baseR + "B +" + accR + "A +" + stR + "S +" + tmR + "T" + (lootR > 0 ? " +" + lootR + "L" : "");
        g.drawString(bonusStr, dosX + dosW - 145, rowY);
        rowY += 16;

        g.setColor(new Color(35, 30, 25));
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.drawString("TOTAL CREDITS", dosX + 14, rowY);
        int rewardVal = lastResult != null ? lastResult.getReward() : 50;
        g.setColor(new Color(22, 120, 48));
        g.drawString("+" + rewardVal + " CREDITS", dosX + dosW - 105, rowY);

        // Stamped Wax Appraisal Seal on Newspaper
        int sealAcc = lastResult != null ? lastResult.getAverageAccuracy() : 85;
        drawWaxAppraisalSeal(g, photoX + photoW - 14, photoY + photoH - 12, sealAcc);

        // Action Buttons
        String nextLabel = levelManager.isUnlocked(selectedLevel + 1) ? "NEXT MUSEUM" : "REPLAY HEIST";
        drawButton(g, nextLabel, 390, VICTORY_NEXT_Y, true);
        drawButton(g, "REPLAY HEIST", 390, VICTORY_REPLAY_Y, false);
        drawButton(g, "LEVEL SELECT", 390, VICTORY_SELECT_Y, false);
    }

    private void drawMiniPillarBar(Graphics2D g, String label, int score, int x, int y, int w) {
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        g.setColor(new Color(185, 180, 195));
        g.drawString(label, x, y + 8);
        g.setColor(new Color(245, 225, 150));
        String scStr = score + "%";
        int sw = g.getFontMetrics().stringWidth(scStr);
        g.drawString(scStr, x + w - sw, y + 8);

        int barY = y + 10;
        int barH = 5;
        g.setColor(new Color(18, 16, 24));
        g.fillRect(x, barY, w, barH);
        Color fillC = score >= 80 ? new Color(110, 235, 150) : score >= 60 ? new Color(245, 205, 90) : new Color(240, 90, 80);
        g.setColor(fillC);
        g.fillRect(x, barY, (int) (w * (Math.max(0, Math.min(100, score)) / 100.0)), barH);
        g.setColor(new Color(60, 50, 42));
        g.drawRect(x, barY, w, barH);
    }

    private static String starString(int stars) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 3; i++) text.append(i < stars ? "★ " : "☆ ");
        return text.toString().trim();
    }

    private void drawBackdrop(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(28, 24, 34), WIDTH, HEIGHT, new Color(10, 11, 16)));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(new Color(68, 52, 36));
        g.fillRect(18, 18, WIDTH - 36, HEIGHT - 36);

        g.setColor(new Color(18, 17, 24));
        g.fillRect(22, 22, WIDTH - 44, HEIGHT - 44);

        g.setColor(new Color(180, 138, 70));
        g.drawRect(28, 28, WIDTH - 56, HEIGHT - 56);
        g.setColor(new Color(90, 70, 40));
        g.drawRect(31, 31, WIDTH - 62, HEIGHT - 62);

        drawCornerRivet(g, 30, 30);
        drawCornerRivet(g, WIDTH - 30, 30);
        drawCornerRivet(g, 30, HEIGHT - 30);
        drawCornerRivet(g, WIDTH - 30, HEIGHT - 30);
    }

    private void drawCornerRivet(Graphics2D g, int cx, int cy) {
        g.setColor(new Color(245, 215, 120));
        g.fillOval(cx - 3, cy - 3, 6, 6);
        g.setColor(new Color(80, 60, 30));
        g.drawOval(cx - 3, cy - 3, 6, 6);
        g.setColor(Color.WHITE);
        g.fillRect(cx - 1, cy - 1, 2, 2);
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

            int dw = 520;
            int dh = 440;
            int dx = (WIDTH - dw) / 2;
            int dy = (HEIGHT - dh) / 2;

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
            drawCentered(g, "★ BUSTED BY SECURITY ★", WIDTH / 2, dy + 44);

            g.setFont(new Font("Monospaced", Font.BOLD, 12));
            g.setColor(new Color(255, 180, 175));
            drawCentered(g, "POLICE INCIDENT REPORT // ARREST DOSSIER", WIDTH / 2, dy + 68);

            // Mugshot Box on left
            int mx = dx + 30;
            int my = dy + 90;
            int mw = 124;
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
            int rx = dx + 175;
            int ry = dy + 98;
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
            drawButton(g, first.isEmpty() ? "TRY AGAIN" : first, 390, GAMEOVER_BTN_RETRY_Y, true);
            drawButton(g, third.isEmpty() ? "LEVEL SELECT" : third, 390, GAMEOVER_BTN_SELECT_Y, false);
        } else {
            // Tactical Blueprint Pause Menu
            g.setColor(new Color(8, 14, 25, 225));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            int pw = 500;
            int ph = 410;
            int px = (WIDTH - pw) / 2;
            int py = (HEIGHT - ph) / 2;

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

            g.setFont(new Font("Monospaced", Font.BOLD, 20));
            g.setColor(new Color(110, 215, 255));
            drawCentered(g, "// HEIST OPERATION PAUSED", WIDTH / 2, py + 36);

            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(new Color(190, 220, 245));
            drawCentered(g, "Museum guards and cameras frozen in position.", WIDTH / 2, py + 58);

            // Mission briefing stats box
            int bx = px + 28;
            int by = py + 72;
            int bw = pw - 56;
            int bh = 54;
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
            drawCentered(g, "ARTWORKS FORGED: " + stolen + "/" + total + "   |   TIME LEFT: " + timeL + "s   |   SUSPICION: " + susp + "%", bx + bw / 2, by + 21);
            g.setColor(new Color(170, 205, 235));
            drawCentered(g, "HEIST OBJECTIVE: FORGE TARGETS & EVADE GUARDS", bx + bw / 2, by + 40);

            if (!first.isEmpty()) drawButton(g, first, 390, PAUSE_BTN_CONTINUE_Y, true);
            if (!second.isEmpty()) drawButton(g, second, 390, PAUSE_BTN_RESTART_Y, false);
            drawButton(g, third, 390, PAUSE_BTN_SELECT_Y, false);

            // Hotkey quick-reference bar
            int kx = bx;
            int ky = py + 332;
            int kw = bw;
            int kh = 34;
            g.setColor(new Color(10, 20, 36, 180));
            g.fillRoundRect(kx, ky, kw, kh, 6, 6);
            g.setColor(new Color(45, 95, 155));
            g.drawRoundRect(kx, ky, kw, kh, 6, 6);
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            g.setColor(new Color(200, 230, 255));
            drawCentered(g, "HOTKEYS: [P] RESUME  •  [R] RESTART  •  [ESC] MENU", bx + bw / 2, ky + 21);
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
            case 1 -> new int[]{55, 110, 190};  // Museum Staff Disguise
            case 2 -> new int[]{195, 45, 65};   // Formal Suit
            case 3 -> new int[]{42, 145, 95};   // Dark Stealth Outfit
            case 4 -> new int[]{215, 170, 75};  // Casual Outfit
            default -> new int[]{226, 182, 74};
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

    private void drawPlayerAccessory(Graphics2D g, int x, int y, int width, int height, int accessoryIndex, int dirRow) {
        if (accessoryIndex <= 0) return;
        // dirRow: 0 = down, 1 = up, 2 = left, 3 = right
        int headX = x + width / 2;
        int headY = y + 16;
        switch (accessoryIndex) {
            case 1 -> { // Burglar Mask
                if (dirRow != 1) {
                    g.setColor(new Color(20, 20, 25));
                    int mx = dirRow == 2 ? headX - 10 : dirRow == 3 ? headX - 4 : headX - 8;
                    g.fillRoundRect(mx, headY + 2, 14, 5, 2, 2);
                    g.setColor(Color.WHITE);
                    if (dirRow == 0) {
                        g.fillRect(mx + 2, headY + 3, 2, 2);
                        g.fillRect(mx + 8, headY + 3, 2, 2);
                    } else if (dirRow == 2) {
                        g.fillRect(mx + 2, headY + 3, 2, 2);
                    } else {
                        g.fillRect(mx + 8, headY + 3, 2, 2);
                    }
                }
            }
            case 2 -> { // Gold Monocle
                if (dirRow == 0 || dirRow == 3) {
                    int ex = dirRow == 0 ? headX + 3 : headX + 2;
                    g.setColor(new Color(255, 215, 0));
                    g.drawOval(ex, headY + 2, 5, 5);
                    g.setColor(new Color(180, 230, 255, 180));
                    g.fillOval(ex + 1, headY + 3, 3, 3);
                    g.setColor(new Color(255, 215, 0));
                    g.drawLine(ex + 4, headY + 6, ex + 5, headY + 14);
                }
            }
            case 3 -> { // Velvet Beret
                int bx = headX - 9;
                int by = headY - 8;
                g.setColor(new Color(160, 30, 50));
                g.fillOval(bx, by, 18, 7);
                g.setColor(new Color(220, 50, 70));
                g.fillOval(bx + 3, by, 12, 4);
                g.setColor(new Color(255, 215, 0));
                g.fillRect(bx + 8, by - 2, 2, 2);
            }
            case 4 -> { // Tactical Visor
                if (dirRow != 1) {
                    int vx = dirRow == 2 ? headX - 10 : dirRow == 3 ? headX - 4 : headX - 8;
                    g.setColor(new Color(40, 45, 50));
                    g.fillRect(vx - 1, headY + 1, 16, 5);
                    g.setColor(new Color(0, 255, 210));
                    g.fillRect(vx, headY + 2, 14, 3);
                    g.setColor(new Color(200, 255, 250));
                    g.drawLine(vx + 2, headY + 3, vx + 10, headY + 3);
                }
            }
            case 5 -> { // Canvas Satchel
                g.setColor(new Color(110, 75, 45));
                g.setStroke(new BasicStroke(2.0f));
                g.drawLine(headX - 6, headY + 8, headX + 6, headY + 22);
                g.setStroke(new BasicStroke(1.0f));
                g.setColor(new Color(145, 95, 55));
                int sx = dirRow == 2 ? headX - 10 : headX + 2;
                g.fillRoundRect(sx, headY + 18, 10, 9, 2, 2);
                g.setColor(new Color(255, 215, 0));
                g.fillRect(sx + 3, headY + 20, 4, 3);
            }
        }
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

    private static final int FORGERY_CANVAS_W = 340;
    private static final int FORGERY_CANVAS_H = 208;
    private static final int ORIGINAL_W = 340;
    private static final int ORIGINAL_H = 208;
    private static final int ORIGINAL_X = 86;
    private static final int ORIGINAL_Y = 114;
    private static final int FORGERY_CANVAS_X = 534;
    private static final int FORGERY_CANVAS_Y = 114;

    private Rectangle getForgeryCanvasScreenBounds() {
        return new Rectangle(FORGERY_CANVAS_X, FORGERY_CANVAS_Y, FORGERY_CANVAS_W, FORGERY_CANVAS_H);
    }

    private Rectangle getOriginalScreenBounds() {
        return new Rectangle(ORIGINAL_X, ORIGINAL_Y, ORIGINAL_W, ORIGINAL_H);
    }

    private Rectangle getForgeryUndoButtonBounds() {
        return new Rectangle(528, 344, 55, 28);
    }

    private Rectangle getForgeryRedoButtonBounds() {
        return new Rectangle(587, 344, 55, 28);
    }

    private Rectangle getForgeryClearButtonBounds() {
        return new Rectangle(646, 344, 55, 28);
    }

    private Rectangle getForgeryZoomButtonBounds() {
        return new Rectangle(705, 344, 55, 28);
    }

    private Rectangle getForgeryBrushButtonBounds() {
        return new Rectangle(764, 344, 58, 28);
    }

    private Rectangle getForgeryFillButtonBounds() {
        return new Rectangle(826, 344, 54, 28);
    }

    private Rectangle getForgeryMixButtonBounds() {
        return new Rectangle(528, 378, 55, 28);
    }

    private Rectangle getForgeryTintButtonBounds() {
        return new Rectangle(587, 378, 55, 28);
    }

    private Rectangle getForgeryShadeButtonBounds() {
        return new Rectangle(646, 378, 55, 28);
    }

    private Rectangle getBrushSizeBounds(int sizeIndex) {
        int[] widths = {38, 38, 40, 44};
        int[] xs = {708, 750, 792, 836};
        return new Rectangle(xs[sizeIndex], 378, widths[sizeIndex], 28);
    }

    private Rectangle getPaletteSwatchBounds(int index) {
        int col = index % 6;
        int row = index / 6;
        return new Rectangle(82 + col * 58, 346 + row * 34, 52, 30);
    }

    private Rectangle getForgeryCancelButtonBounds() {
        return new Rectangle(524, 498, 144, 56);
    }

    private Rectangle getForgeryReplaceButtonBounds() {
        return new Rectangle(678, 498, 206, 56);
    }

    private boolean isForgeryClickable(int x, int y) {
        if (getForgeryReplaceButtonBounds().contains(x, y)) return true;
        if (getForgeryCancelButtonBounds().contains(x, y)) return true;
        if (getForgeryUndoButtonBounds().contains(x, y)) return true;
        if (getForgeryRedoButtonBounds().contains(x, y)) return true;
        if (getForgeryClearButtonBounds().contains(x, y)) return true;
        if (getForgeryZoomButtonBounds().contains(x, y)) return true;
        if (getForgeryBrushButtonBounds().contains(x, y)) return true;
        if (getForgeryFillButtonBounds().contains(x, y)) return true;
        if (getForgeryMixButtonBounds().contains(x, y)) return true;
        if (getForgeryTintButtonBounds().contains(x, y)) return true;
        if (getForgeryShadeButtonBounds().contains(x, y)) return true;
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
        forgeryRedoList.clear();
        forgeryUndoList.add(cloneCanvas(forgeryCanvas));

        Color[] pal = painting.getPalette();
        this.forgerySelectedColor = (pal != null && pal.length > 0) ? pal[0] : Color.BLACK;
        this.forgeryPreviousColor = Color.WHITE;
        this.forgeryBrushSize = 7;
        this.forgeryFillMode = false;
        this.forgeryZoomed = false;
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
        updateForgeryEvaluation();

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

    private void updateForgeryEvaluation() {
        if (forgeryPainting != null && forgeryPainting.getOriginalImage() != null && forgeryCanvas != null) {
            forgeryBreakdownPreview = ForgeryEvaluator.evaluateBreakdown(
                forgeryPainting.getOriginalImage(), forgeryCanvas, forgeryPainting.getPalette()
            );
            forgeryAccuracyPreview = forgeryBreakdownPreview.getOverallScore();
        }
    }

    private void finishForgery() {
        if (forgeryPainting == null || forgeryCanvas == null) {
            screen = Screen.PLAYING;
            return;
        }
        updateForgeryEvaluation();
        int accuracy = forgeryAccuracyPreview;
        world.applyCompletedForgery(forgeryPainting, forgeryCanvas, accuracy, forgeryBreakdownPreview);
        screen = Screen.PLAYING;
        forgeryPainting = null;
        forgeryCanvas = null;
        forgeryUndoList.clear();
        forgeryRedoList.clear();
        updateCursor();
        repaint();
    }

    private void abortForgery() {
        screen = Screen.PLAYING;
        forgeryPainting = null;
        forgeryCanvas = null;
        forgeryUndoList.clear();
        forgeryRedoList.clear();
        updateCursor();
        SoundEffects.play(SoundEffects.Cue.CLICK);
        repaint();
    }

    private void undoForgeryStroke() {
        if (forgeryUndoList.size() > 1) {
            BufferedImage current = forgeryUndoList.remove(forgeryUndoList.size() - 1);
            forgeryRedoList.add(current);
            BufferedImage prev = forgeryUndoList.get(forgeryUndoList.size() - 1);
            forgeryCanvas = cloneCanvas(prev);
            updateForgeryEvaluation();
            SoundEffects.play(SoundEffects.Cue.CLICK);
            repaint();
        }
    }

    private void redoForgeryStroke() {
        if (!forgeryRedoList.isEmpty()) {
            BufferedImage next = forgeryRedoList.remove(forgeryRedoList.size() - 1);
            forgeryUndoList.add(cloneCanvas(next));
            forgeryCanvas = cloneCanvas(next);
            updateForgeryEvaluation();
            SoundEffects.play(SoundEffects.Cue.CLICK);
            repaint();
        }
    }

    private void clearForgeryCanvas() {
        if (forgeryCanvas == null) return;
        forgeryUndoList.add(cloneCanvas(forgeryCanvas));
        if (forgeryUndoList.size() > 20) forgeryUndoList.remove(0);
        forgeryRedoList.clear();
        Graphics2D cg = forgeryCanvas.createGraphics();
        cg.setColor(ForgeryEvaluator.BLANK_CANVAS_COLOR);
        cg.fillRect(0, 0, MasterpieceCatalog.CANVAS_WIDTH, MasterpieceCatalog.CANVAS_HEIGHT);
        cg.dispose();
        updateForgeryEvaluation();
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
        if (getForgeryRedoButtonBounds().contains(x, y)) {
            redoForgeryStroke();
            return;
        }
        if (getForgeryClearButtonBounds().contains(x, y)) {
            clearForgeryCanvas();
            return;
        }
        if (getForgeryZoomButtonBounds().contains(x, y)) {
            forgeryZoomed = !forgeryZoomed;
            SoundEffects.play(SoundEffects.Cue.CLICK);
            repaint();
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
        if (getForgeryMixButtonBounds().contains(x, y)) {
            if (forgeryPreviousColor != null) {
                int mr = (forgerySelectedColor.getRed() + forgeryPreviousColor.getRed()) / 2;
                int mg = (forgerySelectedColor.getGreen() + forgeryPreviousColor.getGreen()) / 2;
                int mb = (forgerySelectedColor.getBlue() + forgeryPreviousColor.getBlue()) / 2;
                forgeryPreviousColor = forgerySelectedColor;
                forgerySelectedColor = new Color(mr, mg, mb);
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
            }
            return;
        }
        if (getForgeryTintButtonBounds().contains(x, y)) {
            int tr = Math.min(255, (int)(forgerySelectedColor.getRed() * 0.75 + 255 * 0.25));
            int tg = Math.min(255, (int)(forgerySelectedColor.getGreen() * 0.75 + 255 * 0.25));
            int tb = Math.min(255, (int)(forgerySelectedColor.getBlue() * 0.75 + 255 * 0.25));
            forgeryPreviousColor = forgerySelectedColor;
            forgerySelectedColor = new Color(tr, tg, tb);
            SoundEffects.play(SoundEffects.Cue.CLICK);
            repaint();
            return;
        }
        if (getForgeryShadeButtonBounds().contains(x, y)) {
            int sr = (int)(forgerySelectedColor.getRed() * 0.75);
            int sg = (int)(forgerySelectedColor.getGreen() * 0.75);
            int sb = (int)(forgerySelectedColor.getBlue() * 0.75);
            forgeryPreviousColor = forgerySelectedColor;
            forgerySelectedColor = new Color(sr, sg, sb);
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
                forgeryPreviousColor = forgerySelectedColor;
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
            forgeryPreviousColor = forgerySelectedColor;
            forgerySelectedColor = new Color(rgb, false);
            SoundEffects.play(SoundEffects.Cue.CLICK);
            repaint();
            return;
        }
        // Draw or fill on forgery canvas
        if (getForgeryCanvasScreenBounds().contains(x, y) && forgeryCanvas != null) {
            int cx, cy;
            if (forgeryZoomed) {
                int viewX = MasterpieceCatalog.CANVAS_WIDTH / 4;
                int viewY = MasterpieceCatalog.CANVAS_HEIGHT / 4;
                int viewW = MasterpieceCatalog.CANVAS_WIDTH / 2;
                int viewH = MasterpieceCatalog.CANVAS_HEIGHT / 2;
                cx = viewX + (x - FORGERY_CANVAS_X) * viewW / FORGERY_CANVAS_W;
                cy = viewY + (y - FORGERY_CANVAS_Y) * viewH / FORGERY_CANVAS_H;
            } else {
                cx = (x - FORGERY_CANVAS_X) * MasterpieceCatalog.CANVAS_WIDTH / FORGERY_CANVAS_W;
                cy = (y - FORGERY_CANVAS_Y) * MasterpieceCatalog.CANVAS_HEIGHT / FORGERY_CANVAS_H;
            }
            cx = Math.max(0, Math.min(MasterpieceCatalog.CANVAS_WIDTH - 1, cx));
            cy = Math.max(0, Math.min(MasterpieceCatalog.CANVAS_HEIGHT - 1, cy));

            forgeryUndoList.add(cloneCanvas(forgeryCanvas));
            if (forgeryUndoList.size() > 20) forgeryUndoList.remove(0);
            forgeryRedoList.clear();

            if (forgeryFillMode) {
                floodFill(forgeryCanvas, cx, cy, forgerySelectedColor);
                updateForgeryEvaluation();
                SoundEffects.play(SoundEffects.Cue.CLICK);
                repaint();
            } else {
                forgeryIsDrawing = true;
                forgeryLastDrawPoint = new Point(cx, cy);
                Graphics2D cg = forgeryCanvas.createGraphics();
                cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                cg.setColor(forgerySelectedColor);
                cg.fillOval(cx - forgeryBrushSize / 2, cy - forgeryBrushSize / 2, forgeryBrushSize, forgeryBrushSize);
                cg.dispose();
                SoundEffects.play(SoundEffects.Cue.BRUSH);
                brushSoundCooldown = 0.14;
                repaint();
            }
        }
    }

    private void handleForgeryMouseDrag(int x, int y) {
        if (!forgeryIsDrawing || forgeryCanvas == null || forgeryLastDrawPoint == null) return;
        int cx, cy;
        if (forgeryZoomed) {
            int viewX = MasterpieceCatalog.CANVAS_WIDTH / 4;
            int viewY = MasterpieceCatalog.CANVAS_HEIGHT / 4;
            int viewW = MasterpieceCatalog.CANVAS_WIDTH / 2;
            int viewH = MasterpieceCatalog.CANVAS_HEIGHT / 2;
            cx = viewX + (x - FORGERY_CANVAS_X) * viewW / FORGERY_CANVAS_W;
            cy = viewY + (y - FORGERY_CANVAS_Y) * viewH / FORGERY_CANVAS_H;
        } else {
            cx = (x - FORGERY_CANVAS_X) * MasterpieceCatalog.CANVAS_WIDTH / FORGERY_CANVAS_W;
            cy = (y - FORGERY_CANVAS_Y) * MasterpieceCatalog.CANVAS_HEIGHT / FORGERY_CANVAS_H;
        }
        cx = Math.max(0, Math.min(MasterpieceCatalog.CANVAS_WIDTH - 1, cx));
        cy = Math.max(0, Math.min(MasterpieceCatalog.CANVAS_HEIGHT - 1, cy));

        Graphics2D cg = forgeryCanvas.createGraphics();
        cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        cg.setColor(forgerySelectedColor);
        cg.setStroke(new BasicStroke(forgeryBrushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        cg.drawLine(forgeryLastDrawPoint.x, forgeryLastDrawPoint.y, cx, cy);
        cg.fillOval(cx - forgeryBrushSize / 2, cy - forgeryBrushSize / 2, forgeryBrushSize, forgeryBrushSize);
        cg.dispose();

        if (brushSoundCooldown <= 0) {
            SoundEffects.play(SoundEffects.Cue.BRUSH);
            brushSoundCooldown = 0.14;
        }

        forgeryLastDrawPoint = new Point(cx, cy);
        repaint();
    }

    private void handleForgeryMouseUp(int x, int y) {
        if (forgeryIsDrawing) {
            forgeryIsDrawing = false;
            forgeryLastDrawPoint = null;
            updateForgeryEvaluation();
            repaint();
        }
    }

    private void drawForgeryStudio(Graphics2D g) {
        if (forgeryPainting == null || forgeryCanvas == null) return;

        // Dark dim backdrop over the live museum heist
        g.setColor(new Color(6, 8, 14, 238));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        int ww = 880;
        int wh = 560;
        int wx = (WIDTH - ww) / 2;
        int wy = (HEIGHT - wh) / 2;

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
        int hx = wx + 14, hy = wy + 10, hw = ww - 28, hh = 50;
        g.setPaint(new GradientPaint(hx, hy, new Color(34, 24, 18), hx, hy + hh, new Color(18, 12, 10)));
        g.fillRoundRect(hx, hy, hw, hh, 8, 8);
        g.setColor(new Color(165, 125, 60));
        g.drawRoundRect(hx, hy, hw, hh, 8, 8);

        String pName = forgeryPainting.getName().toUpperCase();
        String pArtist = MasterpieceCatalog.getArtist(forgeryPainting.getName());
        g.setFont(new Font("Monospaced", Font.BOLD, 15));
        g.setColor(new Color(255, 225, 140));
        g.drawString("ATELIER FORGERY STUDIO: \"" + pName + "\" (" + pArtist + ")", hx + 16, hy + 22);

        String levelDiff = (world != null && world.getMap() != null && world.getMap().getLevel() != null)
            ? world.getMap().getLevel().getDifficulty() : "EASY";
        String diffLabel = MasterpieceCatalog.getDifficultyLabel(forgeryPainting.getName(), levelDiff);
        int totalSecs = (int) Math.round(forgeryMaxTime);
        String headerSub = String.format("DIFFICULTY: %s (%d:%02d)  •  CLICK ORIGINAL TO SAMPLE COLOR  •  MATCH ARTWORK TO DECEIVE GUARDS",
            diffLabel, totalSecs / 60, totalSecs % 60);

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(205, 195, 185));
        g.drawString(headerSub, hx + 16, hy + 40);

        // Timer bar & digital readout
        double ratio = Math.max(0.0, Math.min(1.0, forgeryTimeRemaining / Math.max(1.0, forgeryMaxTime)));
        int timerBarW = 180;
        int timerBarX = hx + hw - 192;
        int timerBarY = hy + 11;
        int timerBarH = 28;

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
        drawCentered(g, timerStr, timerBarX + timerBarW / 2, timerBarY + 18);

        // --- SECTION TITLES ABOVE FRAMES ---
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        g.setColor(new Color(255, 225, 140));
        drawCentered(g, "✦ ORIGINAL MASTERPIECE ✦", ORIGINAL_X + ORIGINAL_W / 2, 101);
        drawCentered(g, "✎ REPLICA CANVAS", FORGERY_CANVAS_X + FORGERY_CANVAS_W / 2, 101);

        // --- LEFT: ORIGINAL MASTERPIECE ---
        // Drop shadow behind museum frame
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(ORIGINAL_X - 6 + 4, ORIGINAL_Y - 5 + 4, ORIGINAL_W + 12, ORIGINAL_H + 10);

        // Gilded ornate museum frame
        g.setColor(new Color(26, 18, 12));
        g.fillRect(ORIGINAL_X - 6, ORIGINAL_Y - 5, ORIGINAL_W + 12, ORIGINAL_H + 10);
        g.setColor(new Color(212, 175, 75));
        g.drawRect(ORIGINAL_X - 5, ORIGINAL_Y - 4, ORIGINAL_W + 9, ORIGINAL_H + 7);
        g.setColor(new Color(145, 105, 38));
        g.drawRect(ORIGINAL_X - 3, ORIGINAL_Y - 2, ORIGINAL_W + 5, ORIGINAL_H + 3);
        g.setColor(new Color(255, 230, 140));
        g.drawRect(ORIGINAL_X - 1, ORIGINAL_Y - 1, ORIGINAL_W + 1, ORIGINAL_H + 1);

        if (forgeryPainting.getOriginalImage() != null) {
            g.drawImage(forgeryPainting.getOriginalImage(), ORIGINAL_X, ORIGINAL_Y, ORIGINAL_W, ORIGINAL_H, null);
        }

        // --- RIGHT: FORGERY CANVAS ---
        // Drop shadow behind canvas
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(FORGERY_CANVAS_X - 6 + 4, FORGERY_CANVAS_Y - 5 + 4, FORGERY_CANVAS_W + 12, FORGERY_CANVAS_H + 10);

        // Canvas wooden stretcher frame
        g.setColor(new Color(38, 28, 20));
        g.fillRect(FORGERY_CANVAS_X - 6, FORGERY_CANVAS_Y - 5, FORGERY_CANVAS_W + 12, FORGERY_CANVAS_H + 10);
        g.setColor(new Color(165, 130, 85));
        g.drawRect(FORGERY_CANVAS_X - 4, FORGERY_CANVAS_Y - 3, FORGERY_CANVAS_W + 7, FORGERY_CANVAS_H + 5);
        g.setColor(new Color(110, 85, 55));
        g.drawRect(FORGERY_CANVAS_X - 2, FORGERY_CANVAS_Y - 1, FORGERY_CANVAS_W + 3, FORGERY_CANVAS_H + 1);

        if (forgeryZoomed) {
            int viewX = MasterpieceCatalog.CANVAS_WIDTH / 4;
            int viewY = MasterpieceCatalog.CANVAS_HEIGHT / 4;
            int viewW = MasterpieceCatalog.CANVAS_WIDTH / 2;
            int viewH = MasterpieceCatalog.CANVAS_HEIGHT / 2;
            g.drawImage(forgeryCanvas, FORGERY_CANVAS_X, FORGERY_CANVAS_Y, FORGERY_CANVAS_X + FORGERY_CANVAS_W, FORGERY_CANVAS_Y + FORGERY_CANVAS_H,
                viewX, viewY, viewX + viewW, viewY + viewH, null);

            // Zoom indicator badge
            g.setColor(new Color(25, 20, 35, 220));
            g.fillRoundRect(FORGERY_CANVAS_X + 6, FORGERY_CANVAS_Y + 6, 92, 16, 4, 4);
            g.setColor(new Color(255, 215, 120));
            g.drawRoundRect(FORGERY_CANVAS_X + 6, FORGERY_CANVAS_Y + 6, 92, 16, 4, 4);
            g.setFont(new Font("Monospaced", Font.BOLD, 8));
            drawCentered(g, "🔍 ZOOM 2X ACTIVE", FORGERY_CANVAS_X + 52, FORGERY_CANVAS_Y + 17);
        } else {
            g.drawImage(forgeryCanvas, FORGERY_CANVAS_X, FORGERY_CANVAS_Y, FORGERY_CANVAS_W, FORGERY_CANVAS_H, null);
        }

        // 4 Canvas spring clips
        drawCanvasClip(g, FORGERY_CANVAS_X + 18, FORGERY_CANVAS_Y - 5);
        drawCanvasClip(g, FORGERY_CANVAS_X + FORGERY_CANVAS_W - 36, FORGERY_CANVAS_Y - 5);
        drawCanvasClip(g, FORGERY_CANVAS_X + 18, FORGERY_CANVAS_Y + FORGERY_CANVAS_H - 7);
        drawCanvasClip(g, FORGERY_CANVAS_X + FORGERY_CANVAS_W - 36, FORGERY_CANVAS_Y + FORGERY_CANVAS_H - 7);

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
        g.fillRoundRect(76, 340, 360, 76, 8, 8);
        g.setColor(new Color(145, 105, 55));
        g.drawRoundRect(76, 340, 360, 76, 8, 8);

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
        // Walnut tool tray backdrop (x=524, y=340, w=360, h=78)
        g.setColor(new Color(22, 14, 10, 240));
        g.fillRoundRect(524, 340, 360, 78, 8, 8);
        g.setColor(new Color(145, 105, 55));
        g.drawRoundRect(524, 340, 360, 78, 8, 8);

        // Row 1: Actions
        drawStudioToolButton(g, getForgeryUndoButtonBounds(), "⟲ UNDO", false);
        drawStudioToolButton(g, getForgeryRedoButtonBounds(), "↷ REDO", false);
        drawStudioToolButton(g, getForgeryClearButtonBounds(), "✕ CLEAR", false);
        drawStudioToolButton(g, getForgeryZoomButtonBounds(), forgeryZoomed ? "🔍 2X" : "🔍 1X", forgeryZoomed);
        drawStudioToolButton(g, getForgeryBrushButtonBounds(), "✎ BRUSH", !forgeryFillMode);
        drawStudioToolButton(g, getForgeryFillButtonBounds(), "⯭ FILL", forgeryFillMode);

        // Row 2: Color Mixing & Brush Sizes
        drawStudioToolButton(g, getForgeryMixButtonBounds(), "🎨 MIX", false);
        drawStudioToolButton(g, getForgeryTintButtonBounds(), "▲ TINT", false);
        drawStudioToolButton(g, getForgeryShadeButtonBounds(), "▼ SHADE", false);

        int[] sizes = {3, 7, 14, 24};
        String[] sizeLabels = {"S", "M", "L", "XL"};
        for (int i = 0; i < sizes.length; i++) {
            Rectangle bsb = getBrushSizeBounds(i);
            boolean activeSize = (forgeryBrushSize == sizes[i]);
            drawStudioBrushSizeButton(g, bsb, sizes[i], sizeLabels[i], activeSize);
        }

        // --- FORENSIC APPRAISAL STATION (Bottom Left: y = 428 to 566) ---
        int appX = 76, appY = 428, appW = 360, appH = 138;
        g.setColor(new Color(20, 15, 18, 240));
        g.fillRoundRect(appX, appY, appW, appH, 8, 8);
        g.setColor(new Color(125, 95, 55));
        g.drawRoundRect(appX, appY, appW, appH, 8, 8);

        // 3D Wax Appraisal Seal
        drawWaxAppraisalSeal(g, appX + 54, appY + appH / 2, 38, forgeryAccuracyPreview);

        // Match meter bar on right of wax seal
        int mX = appX + 112, mY = appY + 12, mW = appW - 124, mH = 26;
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
        int vX = mX, vY = appY + 44, vW = mW, vH = 82;
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
        g.drawString(verdictTitle, vX + 8, vY + 16);

        // 3-Pillar Breakdown Bars
        int colScore = forgeryBreakdownPreview != null ? forgeryBreakdownPreview.getColorScore() : 10;
        int shpScore = forgeryBreakdownPreview != null ? forgeryBreakdownPreview.getShapeScore() : 10;
        int posScore = forgeryBreakdownPreview != null ? forgeryBreakdownPreview.getPositionScore() : 10;

        drawPillarRow(g, "COLOR 35%", colScore, vX + 8, vY + 24, vW - 16);
        drawPillarRow(g, "SHAPE 35%", shpScore, vX + 8, vY + 42, vW - 16);
        drawPillarRow(g, "POS   30%", posScore, vX + 8, vY + 60, vW - 16);

        // --- HEIST BRIEFING & ACTION BUTTONS (Bottom Right: y = 428 to 566) ---
        int rightX = 524, rightY = 428, rightW = 360;

        // Strategy Briefing Card
        int infoH = 60;
        g.setColor(new Color(24, 20, 26, 230));
        g.fillRoundRect(rightX, rightY, rightW, infoH, 8, 8);
        g.setColor(new Color(110, 85, 55));
        g.drawRoundRect(rightX, rightY, rightW, infoH, 8, 8);

        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(255, 215, 120));
        g.drawString("HEIST STRATEGY BRIEFING", rightX + 12, rightY + 18);
        g.setFont(new Font("Monospaced", Font.PLAIN, 9));
        g.setColor(new Color(210, 205, 200));
        g.drawString("• Swap before timer expires to avoid detection.", rightX + 12, rightY + 34);
        g.drawString("• High match replicates deceive guards upon inspection.", rightX + 12, rightY + 48);

        // Action Buttons
        drawCustomActionButton(g, getForgeryCancelButtonBounds(), "CANCEL", "ESC", false, new Color(90, 38, 38));
        drawCustomActionButton(g, getForgeryReplaceButtonBounds(), "SWAP & REPLACE", "ENTER", true, new Color(34, 120, 65));
    }

    private void drawPillarRow(Graphics2D g, String label, int score, int x, int y, int width) {
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        g.setColor(new Color(215, 205, 195));
        g.drawString(label, x, y + 8);

        int barX = x + 58;
        int barW = width - 90;
        int barH = 5;
        g.setColor(new Color(15, 12, 18));
        g.fillRoundRect(barX, y + 2, barW, barH, 2, 2);
        g.setColor(new Color(60, 50, 40));
        g.drawRoundRect(barX, y + 2, barW, barH, 2, 2);

        int fillW = (int) (barW * Math.min(100, Math.max(0, score)) / 100.0);
        Color barCol = score >= 80 ? new Color(46, 204, 113) : score >= 60 ? new Color(243, 156, 18) : new Color(231, 76, 60);
        g.setColor(barCol);
        g.fillRoundRect(barX, y + 2, fillW, barH, 2, 2);

        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        g.drawString(score + "%", barX + barW + 6, y + 8);
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
        g.setColor(new Color(255, 230, 200));
        drawCentered(g, "★ SEAL ★", cx, cy - 13);

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        drawCentered(g, accuracy + "%", cx, cy + 2);

        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        String stamp = accuracy >= 80 ? "MASTER" : accuracy >= 60 ? "PASS" : "FAKE";
        g.setColor(accuracy >= 80 ? new Color(255, 235, 160) : new Color(255, 210, 195));
        drawCentered(g, stamp, cx, cy + 14);
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
