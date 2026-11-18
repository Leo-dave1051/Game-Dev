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
        return ARTISTS.getOrDefault(name, "Master of the Heist");
    }

    public static int getReplicationTimeSeconds(String name, String levelDifficulty) {
        // Strict requirement: Minimum 5 minutes (300 seconds) for easy paintings
        int base = 300;
        String diff = levelDifficulty != null ? levelDifficulty.toUpperCase() : "";
        if (diff.contains("VERY HARD") || diff.contains("EXPERT")) {
            base = 480; // 8 minutes for Expert / National Art Museum
        } else if (diff.contains("HARD")) {
            base = 420; // 7 minutes for Hard
        } else if (diff.contains("MEDIUM")) {
            base = 360; // 6 minutes for Medium
        } else {
            base = 300; // 5 minutes for Easy
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
            case "Mona Lisa" -> {
                ARTISTS.put(name, "Leonardo da Vinci, 1503");
                Color[] pal = {
                    new Color(45, 40, 30),   // 0: Dark Umber Gown
                    new Color(75, 70, 50),   // 1: Olive Drapery
                    new Color(110, 100, 70), // 2: Tuscan Landscape Mid
                    new Color(60, 85, 95),   // 3: Hazy Aqua River & Distant Peaks
                    new Color(195, 160, 125),// 4: Soft Skin Tone
                    new Color(25, 22, 18),   // 5: Veil & Hair Charcoal
                    new Color(145, 125, 80), // 6: Warm Ochre Horizon
                    new Color(225, 195, 155) // 7: Highlight Flesh Tone
                };
                PALETTES.put(name, pal);

                // Background: Hazy Tuscan mountain vista and winding river
                g.setPaint(new GradientPaint(0, 0, pal[3], 0, 65, pal[6]));
                g.fillRect(0, 0, CANVAS_WIDTH, 65);
                g.setColor(pal[2]);
                g.fillOval(-20, 48, 120, 45);
                g.fillOval(125, 52, 120, 45);
                // Winding bridge & river
                g.setColor(pal[3]);
                g.fillRect(160, 58, 40, 6);
                g.fillOval(35, 54, 30, 8);

                // Foreground terrace balustrade
                g.setColor(pal[1]);
                g.fillRect(0, 68, CANVAS_WIDTH, 68);

                // Mona Lisa Figure - Body & Shoulders
                int cx = CANVAS_WIDTH / 2;
                g.setColor(pal[0]);
                g.fillOval(cx - 52, 54, 104, 85);
                g.fillRect(cx - 48, 78, 96, 58);

                // Folded hands in lower foreground
                g.setColor(pal[4]);
                g.fillOval(cx - 32, 108, 64, 24);
                g.setColor(pal[7]);
                g.fillOval(cx - 24, 112, 48, 16);

                // Dark hair & translucent veil draping over shoulders
                g.setColor(pal[5]);
                g.fillOval(cx - 28, 16, 56, 75);
                g.fillRect(cx - 26, 42, 52, 50);

                // Face & Enigmatic expression
                g.setColor(pal[4]);
                g.fillOval(cx - 18, 22, 36, 44);
                g.setColor(pal[7]);
                g.fillOval(cx - 14, 26, 28, 34);

                // Eyes & subtle smile
                g.setColor(pal[5]);
                g.fillRect(cx - 10, 36, 5, 2);
                g.fillRect(cx + 5, 36, 5, 2);
                g.fillRect(cx - 6, 48, 12, 2);
                // Gown neckline trim
                g.setColor(pal[6]);
                g.drawArc(cx - 18, 60, 36, 16, 180, 180);
            }
            case "The Starry Night" -> {
                ARTISTS.put(name, "Vincent van Gogh, 1889");
                Color[] pal = {
                    new Color(18, 28, 62),   // 0: Deep Cobalt Night
                    new Color(36, 68, 124),  // 1: Swirling Ultramarine
                    new Color(255, 222, 92), // 2: Luminous Crescent Moon & Stars
                    new Color(245, 245, 205),// 3: Brilliant Starburst Core
                    new Color(22, 42, 30),   // 4: Cypress Dark Olive Silhouette
                    new Color(58, 96, 72),   // 5: Rolling Alpilles Hills
                    new Color(75, 90, 120),  // 6: Village Roofs & Church Spire
                    new Color(220, 180, 50)  // 7: Swirling Sky Light
                };
                PALETTES.put(name, pal);

                // Swirling night sky backdrop
                g.setPaint(new GradientPaint(0, 0, pal[0], 0, 85, pal[1]));
                g.fillRect(0, 0, CANVAS_WIDTH, 85);

                // Sky swirl curves
                g.setColor(pal[7]);
                g.fillArc(50, 18, 130, 48, 20, 140);
                g.fillArc(80, 24, 110, 38, 200, 140);
                g.setColor(pal[1]);
                g.fillArc(56, 24, 118, 36, 20, 140);

                // Golden Crescent Moon in upper right
                g.setColor(pal[2]);
                g.fillOval(175, 12, 32, 32);
                g.setColor(pal[0]);
                g.fillOval(170, 10, 28, 28);
                // Glowing aura rings around moon
                g.setColor(new Color(255, 222, 92, 90));
                g.drawOval(168, 5, 46, 46);

                // Glowing stars
                int[][] stars = {{40, 22, 16}, {95, 14, 12}, {145, 32, 14}, {70, 48, 10}, {130, 55, 10}, {200, 50, 12}};
                for (int[] s : stars) {
                    g.setColor(pal[2]);
                    g.fillOval(s[0] - s[2]/2, s[1] - s[2]/2, s[2], s[2]);
                    g.setColor(pal[3]);
                    g.fillOval(s[0] - s[2]/4, s[1] - s[2]/4, s[2]/2, s[2]/2);
                }

                // Rolling Alpilles hills
                g.setColor(pal[5]);
                g.fillOval(20, 62, 140, 55);
                g.fillOval(110, 58, 140, 55);

                // Saint-Rémy Village base
                g.setColor(pal[6]);
                g.fillRect(45, 88, CANVAS_WIDTH - 45, 48);
                // Church with towering spire
                g.setColor(new Color(28, 34, 48));
                g.fillRect(108, 76, 22, 35);
                int[] spireX = {114, 119, 124};
                int[] spireY = {76, 42, 76};
                g.fillPolygon(spireX, spireY, 3);
                // Warm glowing cottage windows
                g.setColor(pal[2]);
                g.fillRect(68, 102, 5, 5);
                g.fillRect(86, 98, 4, 4);
                g.fillRect(142, 104, 5, 5);
                g.fillRect(168, 100, 4, 4);

                // Towering foreground Cypress tree on the left
                g.setColor(pal[4]);
                int[] cypX = {12, 28, 42, 34, 44, 28, 10};
                int[] cypY = {CANVAS_HEIGHT, 15, CANVAS_HEIGHT, 75, 45, 18, CANVAS_HEIGHT};
                g.fillPolygon(cypX, cypY, 7);
                g.fillOval(14, 25, 24, 75);
            }
            case "Girl with a Pearl Earring" -> {
                ARTISTS.put(name, "Johannes Vermeer, 1665");
                Color[] pal = {
                    new Color(15, 14, 18),   // 0: Deep Obsidian Canvas
                    new Color(28, 78, 138),  // 1: Ultramarine Turban
                    new Color(235, 185, 65), // 2: Golden Ochre Turban Band
                    new Color(245, 205, 168),// 3: Soft Alabaster Skin
                    new Color(255, 255, 255),// 4: Pearl Earring Glimmer & Collar
                    new Color(145, 95, 62),  // 5: Earth Umber Jacket
                    new Color(195, 58, 68),  // 6: Vermilion Rose Lips
                    new Color(40, 32, 25)    // 7: Deep Shadow Accent
                };
                PALETTES.put(name, pal);

                // Deep dark canvas
                g.setColor(pal[0]);
                g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

                int cx = CANVAS_WIDTH / 2;

                // Earth Umber Jacket / Shoulders turned away
                g.setColor(pal[5]);
                g.fillOval(cx - 52, 75, 105, 75);
                g.fillRect(cx - 48, 92, 96, 44);

                // White collar peeking out
                g.setColor(pal[4]);
                g.fillRect(cx - 24, 76, 32, 10);

                // Head turned over shoulder
                g.setColor(pal[3]);
                g.fillOval(cx - 22, 34, 44, 50);

                // Ultramarine & Ochre Turban
                g.setColor(pal[1]);
                g.fillOval(cx - 28, 14, 56, 40);
                g.fillRect(cx + 6, 26, 22, 54); // Turban tail hanging down
                g.setColor(pal[2]);
                g.fillRect(cx - 24, 32, 48, 12); // Golden band

                // Eyes & Features looking at viewer
                g.setColor(pal[7]);
                g.fillOval(cx - 10, 46, 6, 4);
                g.fillOval(cx + 6, 46, 6, 4);
                g.setColor(pal[6]);
                g.fillOval(cx - 4, 64, 8, 5); // Lips

                // The Legendary Glowing Pearl Earring
                g.setColor(new Color(190, 195, 210));
                g.fillOval(cx + 12, 60, 10, 12);
                g.setColor(pal[4]);
                g.fillOval(cx + 14, 61, 4, 4); // Bright reflection dot
            }
            case "The Great Wave off Kanagawa" -> {
                ARTISTS.put(name, "Katsushika Hokusai, c. 1831");
                Color[] pal = {
                    new Color(242, 235, 215),// 0: Parchment Sky
                    new Color(18, 48, 95),   // 1: Deep Prussian Blue
                    new Color(55, 115, 175), // 2: Indigo Wave Crest
                    new Color(255, 255, 255),// 3: Frothing White Sea Foam
                    new Color(175, 195, 215),// 4: Spray Droplets
                    new Color(40, 52, 68),   // 5: Mt. Fuji Slate Base
                    new Color(180, 130, 80), // 6: Wooden Oar Boats
                    new Color(90, 145, 195)  // 7: Shallow Trough Azure
                };
                PALETTES.put(name, pal);

                // Sky
                g.setColor(pal[0]);
                g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

                // Mt Fuji in center distance
                int fx = CANVAS_WIDTH / 2 - 12;
                g.setColor(pal[5]);
                int[] fujiX = {fx - 24, fx, fx + 24};
                int[] fujiY = {96, 62, 96};
                g.fillPolygon(fujiX, fujiY, 3);
                g.setColor(pal[3]); // Snow cap
                int[] snowX = {fx - 8, fx, fx + 8};
                int[] snowY = {74, 62, 74};
                g.fillPolygon(snowX, snowY, 3);

                // Wooden boats battling swells
                g.setColor(pal[6]);
                g.fillRoundRect(85, 96, 50, 8, 4, 4);
                g.fillRoundRect(140, 108, 45, 7, 4, 4);

                // Lower Rolling Wave base
                g.setColor(pal[7]);
                g.fillOval(-15, 85, 160, 80);
                g.setColor(pal[1]);
                g.fillOval(50, 92, 180, 85);
                g.fillRect(0, 112, CANVAS_WIDTH, 24);

                // The Colossal Cresting Great Wave on Left
                g.setColor(pal[1]);
                int[] waveX = {0, 0, 75, 135, 120, 85, 0};
                int[] waveY = {CANVAS_HEIGHT, 35, 18, 42, 75, 95, CANVAS_HEIGHT};
                g.fillPolygon(waveX, waveY, 7);

                g.setColor(pal[2]);
                g.fillOval(10, 24, 90, 55);

                // Frothing Wave Claws (Hokusai's signature white foam tentacles)
                g.setColor(pal[3]);
                g.fillOval(120, 36, 18, 14);
                g.fillOval(105, 26, 16, 16);
                g.fillOval(85, 16, 20, 18);
                g.fillOval(62, 14, 22, 16);
                g.fillOval(38, 20, 18, 16);
                // Spray droplets
                g.setColor(pal[4]);
                for (int i = 0; i < 18; i++) {
                    int sx = 65 + (i * 7) % 65;
                    int sy = 22 + (i * 5) % 45;
                    g.fillRect(sx, sy, 3, 3);
                }
            }
            case "The Scream" -> {
                ARTISTS.put(name, "Edvard Munch, 1893");
                Color[] pal = {
                    new Color(235, 75, 35),  // 0: Fiery Blood-Orange Sky
                    new Color(245, 165, 35), // 1: Blazing Yellow Clouds
                    new Color(25, 45, 85),   // 2: Deep Dark Fjord Waters
                    new Color(135, 65, 42),  // 3: Wooden Bridge Handrail
                    new Color(215, 205, 165),// 4: Ghostly Agonized Face
                    new Color(18, 22, 36),   // 5: Flowing Dark Garment
                    new Color(185, 125, 85), // 6: Bridge Planks
                    new Color(95, 125, 145)  // 7: Distant Hills & Horizon
                };
                PALETTES.put(name, pal);

                // Fiery undulating sunset sky
                g.setPaint(new GradientPaint(0, 0, pal[0], 0, 48, pal[1]));
                g.fillRect(0, 0, CANVAS_WIDTH, 48);
                g.setColor(pal[0]);
                g.fillArc(20, 12, 180, 30, 0, 180);
                g.fillArc(60, 22, 140, 24, 0, 180);

                // Dark Fjord & distant shoreline
                g.setColor(pal[7]);
                g.fillOval(85, 42, 145, 42);
                g.setColor(pal[2]);
                g.fillOval(70, 52, 160, 58);
                g.fillRect(80, 75, CANVAS_WIDTH - 80, 61);

                // Diagonal bridge railing receding into background
                g.setColor(pal[6]);
                int[] bridgeX = {0, 140, 140, 0};
                int[] bridgeY = {58, 115, CANVAS_HEIGHT, CANVAS_HEIGHT};
                g.fillPolygon(bridgeX, bridgeY, 4);

                g.setColor(pal[3]);
                g.drawLine(0, 72, 140, 118);
                g.drawLine(0, 84, 140, 128);

                // Distant stalking figures on the bridge
                g.setColor(pal[5]);
                g.fillRect(28, 62, 7, 18);
                g.fillRect(42, 66, 6, 17);

                // The Central Agonized Figure
                int fx = 112;
                int fy = 65;
                // Flowing dark garment
                g.setColor(pal[5]);
                g.fillOval(fx - 20, fy + 26, 40, 54);
                g.fillRect(fx - 16, fy + 48, 32, 25);

                // Skull-like face with hands on ears
                g.setColor(pal[4]);
                g.fillOval(fx - 14, fy, 28, 38);
                // Hands grasping head
                g.fillOval(fx - 18, fy + 12, 10, 18);
                g.fillOval(fx + 8, fy + 12, 10, 18);

                // Open screaming mouth and hollow eyes
                g.setColor(pal[5]);
                g.fillOval(fx - 9, fy + 8, 6, 8);  // Left eye
                g.fillOval(fx + 3, fy + 8, 6, 8);  // Right eye
                g.fillOval(fx - 5, fy + 22, 10, 14); // O-shaped mouth
            }
            default -> {
                // Procedural Masterpiece for secondary exhibits
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
                g.setColor(pal[3]);
                g.fillOval(CANVAS_WIDTH - 70, 20, 36, 36);
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
