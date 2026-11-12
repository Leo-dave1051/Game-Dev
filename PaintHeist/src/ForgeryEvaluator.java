import java.awt.Color;
import java.awt.image.BufferedImage;

public final class ForgeryEvaluator {
    public static final Color BLANK_CANVAS_COLOR = new Color(245, 238, 220);

    private ForgeryEvaluator() { }

    public static int evaluateAccuracy(BufferedImage original, BufferedImage fake, Color[] palette) {
        if (original == null || fake == null) return 10;

        int width = Math.min(original.getWidth(), fake.getWidth());
        int height = Math.min(original.getHeight(), fake.getHeight());
        if (width <= 0 || height <= 0) return 10;

        // 1. Check blank canvas coverage
        int blankPixels = 0;
        int totalPixels = width * height;
        int blankR = BLANK_CANVAS_COLOR.getRed();
        int blankG = BLANK_CANVAS_COLOR.getGreen();
        int blankB = BLANK_CANVAS_COLOR.getBlue();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = fake.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int dist = Math.abs(r - blankR) + Math.abs(g - blankG) + Math.abs(b - blankB);
                if (dist < 15) blankPixels++;
            }
        }

        double blankRatio = (double) blankPixels / totalPixels;
        if (blankRatio > 0.92) {
            return Math.max(5, (int) Math.round((1.0 - blankRatio) * 100.0));
        }

        // 2. Block-by-block regional color match (14 columns x 8 rows = 112 blocks)
        int cols = 14;
        int rows = 8;
        int blockW = width / cols;
        int blockH = height / rows;
        double totalBlockScore = 0.0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int startX = c * blockW;
                int startY = r * blockH;
                int endX = (c == cols - 1) ? width : startX + blockW;
                int endY = (r == rows - 1) ? height : startY + blockH;

                long origR = 0, origG = 0, origB = 0;
                long fakeR = 0, fakeG = 0, fakeB = 0;
                int count = 0;

                for (int y = startY; y < endY; y++) {
                    for (int x = startX; x < endX; x++) {
                        int oRgb = original.getRGB(x, y);
                        origR += (oRgb >> 16) & 0xFF;
                        origG += (oRgb >> 8) & 0xFF;
                        origB += oRgb & 0xFF;

                        int fRgb = fake.getRGB(x, y);
                        fakeR += (fRgb >> 16) & 0xFF;
                        fakeG += (fRgb >> 8) & 0xFF;
                        fakeB += fRgb & 0xFF;

                        count++;
                    }
                }

                if (count > 0) {
                    double avgOr = (double) origR / count;
                    double avgOg = (double) origG / count;
                    double avgOb = (double) origB / count;

                    double avgFr = (double) fakeR / count;
                    double avgFg = (double) fakeG / count;
                    double avgFb = (double) fakeB / count;

                    double dist = (Math.abs(avgOr - avgFr) + Math.abs(avgOg - avgFg) + Math.abs(avgOb - avgFb)) / (3.0 * 255.0);
                    double match = Math.max(0.0, 1.0 - dist * 1.55);
                    totalBlockScore += match;
                }
            }
        }
        double avgBlockMatch = totalBlockScore / (cols * rows);

        // 3. Palette usage score: did the player paint with the original's colors?
        int colorsUsed = 0;
        int targetColors = Math.min(6, palette == null ? 0 : palette.length);
        if (palette != null && targetColors > 0) {
            for (int i = 0; i < targetColors; i++) {
                Color target = palette[i];
                int tr = target.getRed();
                int tg = target.getGreen();
                int tb = target.getBlue();
                int matches = 0;

                for (int y = 0; y < height && matches < 30; y += 2) {
                    for (int x = 0; x < width && matches < 30; x += 2) {
                        int rgb = fake.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        if (Math.abs(r - tr) + Math.abs(g - tg) + Math.abs(b - tb) < 38) {
                            matches++;
                        }
                    }
                }
                if (matches >= 25) colorsUsed++;
            }
        }
        double paletteScore = targetColors > 0 ? (double) colorsUsed / targetColors : 0.8;

        double combined = avgBlockMatch * 0.75 + paletteScore * 0.25;
        // Non-linear gentle curve so good faith attempts hit 65-85% comfortably
        double finalScore = Math.pow(combined, 0.92);
        return Math.max(10, Math.min(99, (int) Math.round(finalScore * 100.0)));
    }
}
