import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.function.Consumer;

public class LoadingPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int VIRTUAL_WIDTH = 960;
    private static final int VIRTUAL_HEIGHT = 600;
    private static final int BAR_X = 260;
    private static final int BAR_Y = 445;
    private static final int BAR_WIDTH = 440;
    private static final int BAR_HEIGHT = 16;
    private static final int FINAL_ANIMATION_TICKS = 20;

    private static final String[] HEIST_TIPS = {
        "Stay out of the guards' vision cones to keep your heist undetected.",
        "Running creates noise vibrations that guards can hear through walls.",
        "Hiding inside wardrobes and lockers breaks visual contact with pursuing guards.",
        "Valuable antique clocks, gold chalices, and gems are hidden in restricted rooms.",
        "Take your time recreating the artwork—accuracy above 80% fools the museum curator.",
        "Look for alternative emergency exits and secret passages to escape quickly.",
        "Throw distraction devices to divert guards away from target display cases.",
        "Curators and supervisors coordinate nearby guards when high alarms trigger."
    };

    private final String currentTip;
    private int tipIndex;
    private double tipTimer;

    private int progress;
    private int finalAnimationTicks;
    private long animationFrame;
    private final Timer timer;
    private final Consumer<GamePanel.Assets> onFinished;
    private GamePanel.Assets assets;
    private boolean finished;

    private static final Color GOLD = new Color(225, 181, 100);
    private static final Color GOLD_BRIGHT = new Color(255, 219, 142);
    private static final Color GOLD_DARK = new Color(130, 103, 64);
    private static final Color OBSIDIAN = new Color(12, 11, 16);

    public LoadingPanel(Consumer<GamePanel.Assets> onFinished) {
        this.onFinished = onFinished;
        this.currentTip = HEIST_TIPS[new Random().nextInt(HEIST_TIPS.length)];
        this.tipIndex = new Random().nextInt(HEIST_TIPS.length);
        setBackground(OBSIDIAN);
        setPreferredSize(new Dimension(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        setDoubleBuffered(true);

        timer = new Timer(32, event -> {
            if (progress < 100) progress++;
            animationFrame++;
            tipTimer += 0.032;
            if (tipTimer >= 3.5) {
                tipTimer = 0;
                tipIndex = (tipIndex + 1) % HEIST_TIPS.length;
            }
            if (progress == 100 && assets != null) finalAnimationTicks++;
            repaint();
            finishIfReady();
        });
        timer.start();
    }

    void setAssets(GamePanel.Assets assets) {
        this.assets = assets;
        repaint();
        finishIfReady();
    }

    private void finishIfReady() {
        if (finished || progress < 100 || assets == null
            || finalAnimationTicks < FINAL_ANIMATION_TICKS) return;
        finished = true;
        timer.stop();
        onFinished.accept(assets);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        int panelW = getWidth();
        int panelH = getHeight();

        // 1. Edge-to-edge dark museum background (prevents ANY gray borders in fullscreen/widescreen)
        Graphics2D gFull = (Graphics2D) graphics.create();
        gFull.setColor(OBSIDIAN);
        gFull.fillRect(0, 0, panelW, panelH);

        // Warm museum overhead gallery spotlight
        Point2D center = new Point2D.Float(panelW / 2.0f, panelH * 0.35f);
        float radius = Math.max(panelW, panelH) * 0.75f;
        float[] dist = {0.0f, 0.45f, 1.0f};
        Color[] colors = {new Color(36, 30, 44), new Color(18, 16, 24), OBSIDIAN};
        RadialGradientPaint spotlight = new RadialGradientPaint(center, radius, dist, colors);
        gFull.setPaint(spotlight);
        gFull.fillRect(0, 0, panelW, panelH);
        gFull.dispose();

        // 2. Letterboxed virtual coordinate drawing
        Graphics2D g = (Graphics2D) graphics.create();
        double scale = Math.min(panelW / (double) VIRTUAL_WIDTH, panelH / (double) VIRTUAL_HEIGHT);
        double offsetX = (panelW - VIRTUAL_WIDTH * scale) / 2.0;
        double offsetY = (panelH - VIRTUAL_HEIGHT * scale) / 2.0;
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Gilded museum border frame with corner accents
        g.setColor(new Color(130, 103, 64, 110));
        g.drawRect(28, 28, VIRTUAL_WIDTH - 56, VIRTUAL_HEIGHT - 56);
        drawOrnateCorner(g, 28, 28, true, true);
        drawOrnateCorner(g, VIRTUAL_WIDTH - 28, 28, false, true);
        drawOrnateCorner(g, 28, VIRTUAL_HEIGHT - 28, true, false);
        drawOrnateCorner(g, VIRTUAL_WIDTH - 28, VIRTUAL_HEIGHT - 28, false, false);

        // Title Header with drop shadow
        g.setFont(new Font("Monospaced", Font.BOLD, 36));
        g.setColor(new Color(10, 8, 14, 210));
        drawCentered(g, "PAINTHEIST", VIRTUAL_WIDTH / 2 + 2, 94);
        g.setColor(GOLD_BRIGHT);
        drawCentered(g, "PAINTHEIST", VIRTUAL_WIDTH / 2, 92);

        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(new Color(214, 172, 108));
        drawCentered(g, "TOP-DOWN STEALTH & MASTER ART FORGERY", VIRTUAL_WIDTH / 2, 115);

        // Centerpiece Artwork Display (Framed museum exhibit with velvet cord)
        drawFramedExhibit(g, VIRTUAL_WIDTH / 2, 145);

        // Animated Thief & Guard chasing above the progress bar
        drawAnimatedChase(g);

        // Progress Bar & Fill
        drawProgressBar(g);

        // Percentage Readout
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.setColor(Color.WHITE);
        drawCentered(g, "INFILTRATING MUSEUM ARCHIVES... " + progress + "%", VIRTUAL_WIDTH / 2, BAR_Y + 36);

        // Dynamic Heist Tip Box
        drawTipBox(g, VIRTUAL_WIDTH / 2, BAR_Y + 54);

        // Footer Brand
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(125, 115, 130));
        drawCentered(g, "404 TULOG PRODUCTIONS • HEIST ENGINE v2.0", VIRTUAL_WIDTH / 2, VIRTUAL_HEIGHT - 38);

        g.dispose();
    }

    private void drawFramedExhibit(Graphics2D g, int centerX, int topY) {
        int w = 150;
        int h = 100;
        int x = centerX - w / 2;
        int y = topY;

        // Hanging wires
        g.setColor(new Color(100, 85, 70));
        g.drawLine(centerX - 40, topY - 18, centerX, topY - 26);
        g.drawLine(centerX + 40, topY - 18, centerX, topY - 26);
        g.setColor(GOLD);
        g.fillOval(centerX - 3, topY - 29, 6, 6);

        // Outer ornate frame
        g.setColor(new Color(25, 20, 18));
        g.fillRect(x - 8, y - 8, w + 16, h + 16);
        g.setColor(GOLD_DARK);
        g.fillRect(x - 5, y - 5, w + 10, h + 10);
        g.setColor(GOLD);
        g.drawRect(x - 6, y - 6, w + 11, h + 11);

        // Inner Canvas: Stylized Starry Night / Moonlit Gallery
        g.setPaint(new GradientPaint(x, y, new Color(20, 32, 64), x, y + h, new Color(10, 18, 36)));
        g.fillRect(x, y, w, h);

        // Swirling gold stars & crescent moon
        g.setColor(new Color(255, 225, 120));
        g.fillOval(x + w - 38, y + 16, 22, 22);
        g.setColor(new Color(20, 32, 64));
        g.fillOval(x + w - 42, y + 14, 18, 18);

        g.setColor(new Color(245, 210, 90, 180));
        g.fillOval(x + 24, y + 26, 12, 12);
        g.fillOval(x + 60, y + 20, 14, 14);
        g.fillOval(x + 92, y + 32, 10, 10);

        // Hills & Cypress tree
        g.setColor(new Color(18, 36, 28));
        g.fillOval(x - 20, y + 62, 110, 50);
        g.fillOval(x + 50, y + 68, 120, 50);

        g.setColor(new Color(12, 24, 18));
        int[] treeX = {x + 18, x + 26, x + 34};
        int[] treeY = {y + 90, y + 30, y + 90};
        g.fillPolygon(treeX, treeY, 3);

        // Museum title plaque below painting
        g.setColor(new Color(180, 145, 85));
        g.fillRect(centerX - 35, y + h + 8, 70, 12);
        g.setColor(new Color(30, 20, 15));
        g.drawRect(centerX - 35, y + h + 8, 70, 12);
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        drawCentered(g, "TARGET EXHIBIT", centerX, y + h + 17);
    }

    private void drawAnimatedChase(Graphics2D g) {
        int travel = BAR_WIDTH - 44;
        int thiefCenterX = BAR_X + 24 + travel * progress / 100;
        int guardCenterX = thiefCenterX - 48;

        BufferedImage[] thiefFrames = assets != null ? assets.getThiefLoadingFrames() : null;
        BufferedImage[] guardFrames = assets != null ? assets.getGuardLoadingFrames() : null;

        int groundY = BAR_Y - 2;

        // Dust puffs behind thief
        if (progress > 5 && progress < 98) {
            g.setColor(new Color(180, 160, 140, 80));
            int dustOffset = (int) (animationFrame % 8) * 3;
            g.fillOval(thiefCenterX - 18 - dustOffset, groundY - 6, 8, 6);
            g.fillOval(thiefCenterX - 28 - dustOffset, groundY - 4, 6, 4);
        }

        // Drop shadows
        g.setColor(new Color(0, 0, 0, 110));
        g.fillOval(thiefCenterX - 11, groundY - 3, 22, 6);
        g.fillOval(guardCenterX - 12, groundY - 3, 24, 6);

        // Guard flashlight cone
        g.setPaint(new GradientPaint(guardCenterX, groundY - 26, new Color(255, 235, 140, 55),
            thiefCenterX, groundY - 14, new Color(255, 235, 140, 0)));
        int[] coneX = {guardCenterX + 6, thiefCenterX + 16, thiefCenterX + 12};
        int[] coneY = {groundY - 26, groundY - 38, groundY + 4};
        g.fillPolygon(coneX, coneY, 3);

        if (thiefFrames != null && thiefFrames.length > 0) {
            int frame = (int) (animationFrame / 3 % thiefFrames.length);
            drawSpriteRunner(g, thiefFrames[frame], thiefCenterX, groundY, 28, 44);
        } else {
            drawFallbackRunner(g, thiefCenterX, groundY, (int) (animationFrame / 3 % 2), true);
        }

        if (guardFrames != null && guardFrames.length > 0) {
            int frame = (int) (animationFrame / 3 % guardFrames.length);
            drawSpriteRunner(g, guardFrames[frame], guardCenterX, groundY, 32, 54);
        } else {
            drawFallbackRunner(g, guardCenterX, groundY, (int) (animationFrame / 3 % 2), false);
        }
    }

    private void drawProgressBar(Graphics2D g) {
        // Deep beveled container
        g.setColor(new Color(20, 18, 25));
        g.fillRoundRect(BAR_X - 2, BAR_Y - 2, BAR_WIDTH + 4, BAR_HEIGHT + 4, 8, 8);
        g.setColor(new Color(50, 44, 58));
        g.fillRect(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT);

        // Gold Fill with animated diagonal shimmer lines
        int fillW = BAR_WIDTH * progress / 100;
        if (fillW > 0) {
            g.setPaint(new GradientPaint(BAR_X, BAR_Y, new Color(255, 222, 138),
                BAR_X, BAR_Y + BAR_HEIGHT, new Color(194, 146, 68)));
            g.fillRect(BAR_X, BAR_Y, fillW, BAR_HEIGHT);

            // Shimmer stripes
            g.setColor(new Color(255, 245, 210, 85));
            int stripeSpacing = 20;
            int shift = (int) ((animationFrame * 2) % stripeSpacing);
            for (int sx = BAR_X - 16 + shift; sx < BAR_X + fillW; sx += stripeSpacing) {
                if (sx >= BAR_X && sx + 8 <= BAR_X + fillW) {
                    int[] px = {sx, sx + 8, sx + 4, sx - 4};
                    int[] py = {BAR_Y, BAR_Y, BAR_Y + BAR_HEIGHT, BAR_Y + BAR_HEIGHT};
                    g.fillPolygon(px, py, 4);
                }
            }
        }

        // Ornate border outline
        g.setColor(GOLD);
        g.drawRoundRect(BAR_X - 1, BAR_Y - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, 6, 6);
        g.setColor(GOLD_DARK);
        g.drawRect(BAR_X - 3, BAR_Y - 3, BAR_WIDTH + 6, BAR_HEIGHT + 6);
    }

    private void drawTipBox(Graphics2D g, int centerX, int topY) {
        int boxW = 580;
        int boxH = 34;
        int boxX = centerX - boxW / 2;

        g.setColor(new Color(24, 20, 30, 200));
        g.fillRoundRect(boxX, topY, boxW, boxH, 8, 8);
        g.setColor(new Color(130, 103, 64, 140));
        g.drawRoundRect(boxX, topY, boxW, boxH, 8, 8);

        // Tip icon
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.setColor(GOLD);
        g.drawString("TIP:", boxX + 16, topY + 21);

        // Tip text
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(228, 222, 232));
        String tip = HEIST_TIPS[tipIndex];
        g.drawString(tip, boxX + 54, topY + 21);
    }

    private void drawOrnateCorner(Graphics2D g, int x, int y, boolean left, boolean top) {
        g.setColor(GOLD);
        int dx = left ? 1 : -1;
        int dy = top ? 1 : -1;
        g.fillRect(x, y, dx * 45, dy * 3);
        g.fillRect(x, y, dx * 3, dy * 45);
        g.setColor(GOLD_BRIGHT);
        g.fillRect(x + dx * 6, y + dy * 6, dx * 6, dy * 6);
    }

    private void drawSpriteRunner(Graphics2D g, BufferedImage sprite, int centerX, int baseline,
                                  int width, int height) {
        if (sprite == null) return;
        Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(sprite, centerX - width / 2, baseline - height, width, height, null);
        if (oldHint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint);
    }

    private void drawFallbackRunner(Graphics2D g, int centerX, int groundY, int step, boolean thief) {
        int x = centerX - 9;
        Color shirt = thief ? new Color(57, 67, 111) : new Color(71, 121, 83);
        Color skin = new Color(222, 164, 119);
        Color dark = new Color(30, 27, 35);
        g.setColor(dark);
        g.fillRect(x + (step == 0 ? 2 : 5), groundY - 6, 5, 8);
        g.fillRect(x + (step == 0 ? 12 : 9), groundY - 6, 5, 8);
        g.setColor(shirt);
        g.fillRect(x + 3, groundY - 20, 14, 15);
        g.setColor(skin);
        g.fillRect(x + 5, groundY - 30, 11, 11);
        if (thief) {
            g.setColor(dark);
            g.fillRect(x + 3, groundY - 31, 15, 4);
            g.setColor(GOLD);
            g.fillRect(x + 14, groundY - 16, 7, 7);
        }
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baseline) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }
}
