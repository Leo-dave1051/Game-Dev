import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * 3-Pillar Painting Accuracy Evaluator:
 * - Color Similarity (35%)
 * - Shape / Contour Similarity (35%)
 * - Position / Spatial Accuracy (30%)
 */
public final class ForgeryEvaluator {
    public static final Color BLANK_CANVAS_COLOR = new Color(245, 238, 220);

    public static final class EvaluationBreakdown {
        private final int colorScore;
        private final int shapeScore;
        private final int positionScore;
        private final int overallScore;

        public EvaluationBreakdown(int color, int shape, int position, int overall) {
            this.colorScore = color;
            this.shapeScore = shape;
            this.positionScore = position;
            this.overallScore = overall;
        }

        public int getColorScore() { return colorScore; }
        public int getShapeScore() { return shapeScore; }
        public int getPositionScore() { return positionScore; }
        public int getOverallScore() { return overallScore; }
    }

    private ForgeryEvaluator() { }

    public static int evaluateAccuracy(BufferedImage original, BufferedImage fake, Color[] palette) {
        return evaluateBreakdown(original, fake, palette).getOverallScore();
    }

    public static EvaluationBreakdown evaluateBreakdown(BufferedImage original, BufferedImage fake, Color[] palette) {
        if (original == null || fake == null) {
            return new EvaluationBreakdown(10, 10, 10, 10);
        }

        int width = Math.min(original.getWidth(), fake.getWidth());
        int height = Math.min(original.getHeight(), fake.getHeight());
        if (width <= 0 || height <= 0) {
            return new EvaluationBreakdown(10, 10, 10, 10);
        }

        // 1. Check blank canvas ratio
        int blankPixels = 0;
        int totalPixels = 0;
        int blankR = BLANK_CANVAS_COLOR.getRed();
        int blankG = BLANK_CANVAS_COLOR.getGreen();
        int blankB = BLANK_CANVAS_COLOR.getBlue();

        for (int y = 0; y < height; y += 2) {
            for (int x = 0; x < width; x += 2) {
                totalPixels++;
                int rgb = fake.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (Math.abs(r - blankR) + Math.abs(g - blankG) + Math.abs(b - blankB) < 18) {
                    blankPixels++;
                }
            }
        }

        double blankRatio = (double) blankPixels / Math.max(1, totalPixels);
        if (blankRatio > 0.94) {
            int base = Math.max(5, (int) Math.round((1.0 - blankRatio) * 80.0));
            return new EvaluationBreakdown(base, base, base, base);
        }

        // --- PILLAR 1: COLOR SIMILARITY (35%) ---
        // Compares global color distribution & palette match
        long diffR = 0, diffG = 0, diffB = 0;
        int sampleCount = 0;
        for (int y = 0; y < height; y += 3) {
            for (int x = 0; x < width; x += 3) {
                int oRgb = original.getRGB(x, y);
                int fRgb = fake.getRGB(x, y);
                diffR += Math.abs(((oRgb >> 16) & 0xFF) - ((fRgb >> 16) & 0xFF));
                diffG += Math.abs(((oRgb >> 8) & 0xFF) - ((fRgb >> 8) & 0xFF));
                diffB += Math.abs((oRgb & 0xFF) - (fRgb & 0xFF));
                sampleCount++;
            }
        }
        double avgColorDist = sampleCount > 0 ? (double) (diffR + diffG + diffB) / (sampleCount * 3.0 * 255.0) : 1.0;
        double colorMatch = Math.max(0.0, 1.0 - avgColorDist * 1.6);

        // Palette usage bonus
        int paletteMatches = 0;
        int targetColors = Math.min(6, palette == null ? 0 : palette.length);
        if (palette != null && targetColors > 0) {
            for (int i = 0; i < targetColors; i++) {
                Color c = palette[i];
                int tr = c.getRed(), tg = c.getGreen(), tb = c.getBlue();
                boolean found = false;
                for (int y = 0; y < height && !found; y += 4) {
                    for (int x = 0; x < width && !found; x += 4) {
                        int rgb = fake.getRGB(x, y);
                        if (Math.abs(((rgb >> 16) & 0xFF) - tr) + Math.abs(((rgb >> 8) & 0xFF) - tg)
                            + Math.abs((rgb & 0xFF) - tb) < 35) {
                            found = true;
                            paletteMatches++;
                        }
                    }
                }
            }
        }
        double paletteScore = targetColors > 0 ? (double) paletteMatches / targetColors : 0.8;
        int colorScore = Math.max(10, Math.min(99, (int) Math.round((colorMatch * 0.75 + paletteScore * 0.25) * 100.0)));

        // --- PILLAR 2: SHAPE & CONTOUR SIMILARITY (35%) ---
        // Edge detection comparing gradient outlines and structure
        double edgeMatchSum = 0.0;
        int edgePoints = 0;
        for (int y = 2; y < height - 2; y += 3) {
            for (int x = 2; x < width - 2; x += 3) {
                // Approximate luminance gradient for original
                int oLumL = getLuminance(original.getRGB(x - 1, y));
                int oLumR = getLuminance(original.getRGB(x + 1, y));
                int oLumU = getLuminance(original.getRGB(x, y - 1));
                int oLumD = getLuminance(original.getRGB(x, y + 1));
                int oGrad = Math.abs(oLumR - oLumL) + Math.abs(oLumD - oLumU);

                // Approximate luminance gradient for fake
                int fLumL = getLuminance(fake.getRGB(x - 1, y));
                int fLumR = getLuminance(fake.getRGB(x + 1, y));
                int fLumU = getLuminance(fake.getRGB(x, y - 1));
                int fLumD = getLuminance(fake.getRGB(x, y + 1));
                int fGrad = Math.abs(fLumR - fLumL) + Math.abs(fLumD - fLumU);

                double gradDiff = Math.abs(oGrad - fGrad) / 255.0;
                edgeMatchSum += Math.max(0.0, 1.0 - gradDiff * 1.5);
                edgePoints++;
            }
        }
        double avgShapeMatch = edgePoints > 0 ? edgeMatchSum / edgePoints : 0.5;
        int shapeScore = Math.max(10, Math.min(99, (int) Math.round(avgShapeMatch * 100.0)));

        // --- PILLAR 3: POSITION / SPATIAL ACCURACY (30%) ---
        // Block-by-block regional alignment (10 columns x 6 rows = 60 blocks)
        int cols = 10;
        int rows = 6;
        int blockW = width / cols;
        int blockH = height / rows;
        double blockScoreSum = 0.0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int startX = c * blockW;
                int startY = r * blockH;
                int endX = (c == cols - 1) ? width : startX + blockW;
                int endY = (r == rows - 1) ? height : startY + blockH;

                long oR = 0, oG = 0, oB = 0;
                long fR = 0, fG = 0, fB = 0;
                int bCount = 0;

                for (int by = startY; by < endY; by += 2) {
                    for (int bx = startX; bx < endX; bx += 2) {
                        int orgb = original.getRGB(bx, by);
                        oR += (orgb >> 16) & 0xFF;
                        oG += (orgb >> 8) & 0xFF;
                        oB += orgb & 0xFF;

                        int frgb = fake.getRGB(bx, by);
                        fR += (frgb >> 16) & 0xFF;
                        fG += (frgb >> 8) & 0xFF;
                        fB += frgb & 0xFF;

                        bCount++;
                    }
                }

                if (bCount > 0) {
                    double d = (Math.abs((double) oR / bCount - (double) fR / bCount)
                        + Math.abs((double) oG / bCount - (double) fG / bCount)
                        + Math.abs((double) oB / bCount - (double) fB / bCount)) / (3.0 * 255.0);
                    blockScoreSum += Math.max(0.0, 1.0 - d * 1.6);
                }
            }
        }
        double avgPositionMatch = blockScoreSum / (cols * rows);
        int positionScore = Math.max(10, Math.min(99, (int) Math.round(avgPositionMatch * 100.0)));

        // --- WEIGHTED FORMULA: Color (35%) + Shape (35%) + Position (30%) ---
        double weighted = colorScore * 0.35 + shapeScore * 0.35 + positionScore * 0.30;
        // Subtle power curve so reasonable player efforts get satisfying scores
        double calibrated = Math.pow(weighted / 100.0, 0.94) * 100.0;
        int overallScore = Math.max(10, Math.min(99, (int) Math.round(calibrated)));

        return new EvaluationBreakdown(colorScore, shapeScore, positionScore, overallScore);
    }

    private static int getLuminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (299 * r + 587 * g + 114 * b) / 1000;
    }
}
