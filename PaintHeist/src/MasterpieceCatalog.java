import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public final class MasterpieceCatalog {
    public static final int CANVAS_WIDTH = 224;
    public static final int CANVAS_HEIGHT = 136;

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private static final Map<String, Color[]> PALETTES = new HashMap<>();
    private static final Map<String, String> ARTISTS = new HashMap<>();

    private MasterpieceCatalog() { }

    public static synchronized BufferedImage getMasterpiece(String name) {
        return CACHE.computeIfAbsent(name, MasterpieceCatalog::renderMasterpiece);
    }

    public static Color[] getPalette(String name) {
        getMasterpiece(name); // Ensure populated
        return PALETTES.getOrDefault(name, new Color[]{
            new Color(35, 55, 75), new Color(85, 120, 150), new Color(180, 140, 90),
            new Color(240, 210, 140), new Color(40, 80, 50), new Color(160, 60, 50),
            new Color(245, 238, 220), new Color(20, 20, 25)
        });
    }

    public static String getArtist(String name) {
        getMasterpiece(name);
        return ARTISTS.getOrDefault(name, "Unknown Master");
    }

    public static int getReplicationTimeSeconds(String name, String levelDifficulty) {
        int base = 120; // At least 2 minutes (120s) for Easy paintings
        String diff = levelDifficulty != null ? levelDifficulty.toUpperCase() : "";
        if (diff.contains("VERY HARD") || diff.contains("EXPERT")) {
            base = 210; // 3.5 minutes for Very Hard
        } else if (diff.contains("HARD")) {
            base = 180; // 3.0 minutes for Hard
        } else if (diff.contains("MEDIUM")) {
            base = 150; // 2.5 minutes for Medium
        } else {
            base = 120; // 2.0 minutes for Easy
        }
        return base;
    }

    public static String getDifficultyLabel(String name, String levelDifficulty) {
        String diff = levelDifficulty != null ? levelDifficulty.toUpperCase() : "EASY";
        if (diff.contains("VERY HARD")) return "VERY HARD";
        if (diff.contains("HARD")) return "HARD";
        if (diff.contains("MEDIUM")) return "MEDIUM";
        return "EASY";
    }

    private static BufferedImage renderMasterpiece(String name) {
        BufferedImage img = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        switch (name) {
            case "Moonlit Orchard" -> {
                ARTISTS.put(name, "Vincent de Lune, 1891");
                Color[] pal = {
                    new Color(13, 27, 42), new Color(27, 38, 59), new Color(45, 90, 39),
                    new Color(27, 59, 27), new Color(224, 230, 237), new Color(244, 208, 111),
                    new Color(192, 57, 43), new Color(74, 46, 24)
                };
                PALETTES.put(name, pal);
                g.setPaint(new GradientPaint(0, 0, pal[0], 0, 85, pal[1]));
                g.fillRect(0, 0, CANVAS_WIDTH, 85);
                // Moon
                g.setColor(pal[5]);
                g.fillOval(145, 18, 38, 38);
                g.setColor(pal[4]);
                g.fillOval(149, 22, 30, 30);
                // Hill & orchard
                g.setColor(pal[3]);
                g.fillOval(-30, 70, 160, 90);
                g.fillOval(100, 65, 160, 95);
                g.setColor(pal[2]);
                g.fillRect(0, 92, CANVAS_WIDTH, 44);
                // Trees
                int[] treeX = {28, 76, 125, 180};
                for (int tx : treeX) {
                    g.setColor(pal[7]);
                    g.fillRect(tx + 10, 82, 8, 28);
                    g.setColor(pal[3]);
                    g.fillOval(tx - 6, 44, 40, 42);
                    g.setColor(pal[2]);
                    g.fillOval(tx - 2, 48, 32, 34);
                    // Ripe apples
                    g.setColor(pal[6]);
                    g.fillOval(tx + 4, 56, 5, 5);
                    g.fillOval(tx + 18, 62, 5, 5);
                    g.fillOval(tx + 10, 70, 5, 5);
                }
            }
            case "The First Light" -> {
                ARTISTS.put(name, "H. K. Sonnenfeld, 1874");
                Color[] pal = {
                    new Color(255, 211, 181), new Color(214, 90, 49), new Color(93, 58, 88),
                    new Color(46, 28, 56), new Color(246, 201, 14), new Color(28, 43, 45),
                    new Color(255, 255, 255), new Color(232, 165, 152)
                };
                PALETTES.put(name, pal);
                g.setPaint(new GradientPaint(0, 0, pal[2], 0, 75, pal[1]));
                g.fillRect(0, 0, CANVAS_WIDTH, 75);
                // Sunrise glow
                g.setColor(pal[4]);
                g.fillOval(CANVAS_WIDTH / 2 - 25, 45, 50, 50);
                g.setColor(pal[0]);
                g.fillOval(CANVAS_WIDTH / 2 - 18, 52, 36, 36);
                // Distant peaks
                int[] px1 = {0, 60, 120};
                int[] py1 = {80, 32, 80};
                g.setColor(pal[3]);
                g.fillPolygon(px1, py1, 3);
                int[] px2 = {100, 165, 224};
                int[] py2 = {80, 24, 80};
                g.fillPolygon(px2, py2, 3);
                // Snowcaps
                g.setColor(pal[6]);
                g.fillPolygon(new int[]{52, 60, 68}, new int[]{45, 32, 45}, 3);
                g.fillPolygon(new int[]{156, 165, 174}, new int[]{38, 24, 38}, 3);
                // Foreground mountain ridge
                g.setColor(pal[5]);
                g.fillPolygon(new int[]{0, 90, 180, 224, 224, 0}, new int[]{80, 58, 72, 65, 136, 136}, 6);
                g.setColor(pal[3]);
                g.fillRect(0, 105, CANVAS_WIDTH, 31);
            }
            case "Marble Garden" -> {
                ARTISTS.put(name, "Clara Bellini, 1845");
                Color[] pal = {
                    new Color(30, 63, 32), new Color(52, 88, 48), new Color(244, 244, 246),
                    new Color(165, 165, 170), new Color(194, 59, 78), new Color(72, 169, 166),
                    new Color(176, 91, 59), new Color(216, 194, 157)
                };
                PALETTES.put(name, pal);
                // Sky & garden hedge
                g.setColor(pal[5]);
                g.fillRect(0, 0, CANVAS_WIDTH, 60);
                g.setColor(pal[0]);
                g.fillRect(0, 50, CANVAS_WIDTH, 40);
                g.setColor(pal[1]);
                g.fillRect(0, 80, CANVAS_WIDTH, 56);
                // Marble balustrade & path
                g.setColor(pal[7]);
                g.fillRect(40, 100, CANVAS_WIDTH - 80, 36);
                g.setColor(pal[2]);
                g.fillRect(20, 85, CANVAS_WIDTH - 40, 8);
                // Central marble statue
                int cx = CANVAS_WIDTH / 2;
                g.setColor(pal[3]);
                g.fillRect(cx - 16, 80, 32, 22);
                g.setColor(pal[2]);
                g.fillRect(cx - 14, 82, 28, 18);
                g.fillOval(cx - 10, 35, 20, 24);
                g.fillRect(cx - 12, 56, 24, 26);
                // Rose bushes
                g.setColor(pal[4]);
                for (int rx = 25; rx < CANVAS_WIDTH - 25; rx += 36) {
                    g.fillOval(rx, 75, 8, 8);
                    g.fillOval(rx + 8, 72, 7, 7);
                }
            }
            case "Portrait in Blue" -> {
                ARTISTS.put(name, "Baron E. Vance, 1782");
                Color[] pal = {
                    new Color(31, 58, 96), new Color(15, 29, 48), new Color(229, 184, 66),
                    new Color(240, 192, 151), new Color(158, 27, 50), new Color(245, 245, 250),
                    new Color(62, 39, 35), new Color(20, 20, 25)
                };
                PALETTES.put(name, pal);
                g.setColor(pal[6]);
                g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                // Drapery
                g.setColor(pal[1]);
                g.fillRect(0, 0, 60, CANVAS_HEIGHT);
                g.fillRect(CANVAS_WIDTH - 50, 0, 50, CANVAS_HEIGHT);
                // Portrait figure
                int cx = CANVAS_WIDTH / 2;
                g.setColor(pal[0]); // Royal Blue Coat
                g.fillOval(cx - 44, 60, 88, 80);
                g.fillRect(cx - 42, 85, 84, 51);
                // Gold trim & buttons
                g.setColor(pal[2]);
                g.fillRect(cx - 3, 85, 6, 45);
                g.fillOval(cx - 2, 92, 4, 4);
                g.fillOval(cx - 2, 104, 4, 4);
                g.fillOval(cx - 2, 116, 4, 4);
                // Face & cravat
                g.setColor(pal[5]);
                g.fillRect(cx - 14, 64, 28, 22);
                g.setColor(pal[3]);
                g.fillOval(cx - 20, 22, 40, 46);
                // Hair & eyes
                g.setColor(pal[7]);
                g.fillOval(cx - 22, 18, 44, 22);
                g.fillRect(cx - 12, 40, 4, 6);
                g.fillRect(cx + 8, 40, 4, 6);
            }
            case "The Crown's Reflection" -> {
                ARTISTS.put(name, "Royal Goldsmiths Guild, 1690");
                Color[] pal = {
                    new Color(102, 7, 8), new Color(56, 4, 14), new Color(255, 186, 8),
                    new Color(0, 119, 182), new Color(0, 128, 0), new Color(211, 211, 211),
                    new Color(144, 224, 239), new Color(255, 243, 176)
                };
                PALETTES.put(name, pal);
                g.setColor(pal[1]);
                g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                // Mirror base
                g.setColor(pal[6]);
                g.fillOval(28, 70, CANVAS_WIDTH - 56, 52);
                g.setColor(pal[5]);
                g.drawOval(28, 70, CANVAS_WIDTH - 56, 52);
                // Velvet cushion
                g.setColor(pal[0]);
                g.fillRoundRect(50, 72, CANVAS_WIDTH - 100, 38, 18, 18);
                // Crown gold band
                int cx = CANVAS_WIDTH / 2;
                g.setColor(pal[2]);
                g.fillRect(cx - 40, 48, 80, 28);
                // Crown arches & points
                int[] ptsX = {cx - 40, cx - 25, cx, cx + 25, cx + 40};
                for (int px : ptsX) {
                    g.fillPolygon(new int[]{px - 6, px, px + 6}, new int[]{48, 26, 48}, 3);
                    g.setColor(pal[7]);
                    g.fillOval(px - 3, 22, 6, 6);
                    g.setColor(pal[2]);
                }
                // Jewels
                g.setColor(pal[3]);
                g.fillOval(cx - 28, 56, 8, 8);
                g.fillOval(cx + 20, 56, 8, 8);
                g.setColor(pal[4]);
                g.fillOval(cx - 4, 54, 10, 10);
            }
            case "Gilded Promise" -> {
                ARTISTS.put(name, "Marco Canaletto, 1735");
                Color[] pal = {
                    new Color(26, 83, 92), new Color(13, 43, 48), new Color(247, 178, 103),
                    new Color(247, 157, 101), new Color(244, 132, 95), new Color(160, 26, 88),
                    new Color(17, 19, 23), new Color(233, 236, 239)
                };
                PALETTES.put(name, pal);
                // Venetian sunset
                g.setPaint(new GradientPaint(0, 0, pal[5], 0, 70, pal[3]));
                g.fillRect(0, 0, CANVAS_WIDTH, 70);
                // Grand Canal water
                g.setPaint(new GradientPaint(0, 70, pal[2], 0, CANVAS_HEIGHT, pal[0]));
                g.fillRect(0, 70, CANVAS_WIDTH, 66);
                // Palazzi silhouettes
                g.setColor(pal[1]);
                g.fillRect(0, 34, 60, 40);
                g.fillRect(CANVAS_WIDTH - 70, 28, 70, 46);
                // Gondola
                g.setColor(pal[6]);
                g.fillRoundRect(80, 92, 64, 12, 6, 6);
                g.fillRect(134, 82, 3, 16);
            }
            default -> {
                // General procedural masterpiece for any other painting name
                ARTISTS.put(name, "Master of the Heist, 1888");
                Color[] pal = {
                    new Color(42, 65, 88), new Color(78, 114, 139), new Color(198, 152, 92),
                    new Color(238, 206, 130), new Color(52, 94, 68), new Color(184, 68, 62),
                    new Color(245, 240, 230), new Color(22, 24, 32)
                };
                PALETTES.put(name, pal);
                int hash = Math.abs(name.hashCode());
                Color sky = pal[hash % 3];
                g.setPaint(new GradientPaint(0, 0, sky, 0, 80, pal[3]));
                g.fillRect(0, 0, CANVAS_WIDTH, 80);
                // Celestial orb (sun or moon)
                g.setColor(pal[3]);
                g.fillOval(CANVAS_WIDTH - 70, 20, 36, 36);
                // Landscape or architecture
                g.setColor(pal[4]);
                g.fillRect(0, 75, CANVAS_WIDTH, 61);
                g.setColor(pal[2]);
                g.fillRect(30, 40, 80, 50);
                g.setColor(pal[5]);
                g.fillPolygon(new int[]{20, 70, 120}, new int[]{40, 16, 40}, 3);
                g.setColor(pal[7]);
                g.fillRect(60, 62, 20, 28);
            }
        }
        g.dispose();
        return img;
    }
}
