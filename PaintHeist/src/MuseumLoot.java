import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public final class MuseumLoot {
    public enum LootType {
        CHALICE,
        CLOCK,
        DIAMOND,
        DONATION_BOX,
        SCROLL
    }

    private final int id;
    private final String name;
    private final LootType type;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int value;
    private boolean collected;

    public MuseumLoot(int id, String name, LootType type, int x, int y, int value) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = 24;
        this.height = 24;
        this.value = value;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public LootType getType() { return type; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getValue() { return value; }
    public boolean isCollected() { return collected; }

    public void collect() {
        this.collected = true;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public Rectangle getInteractionBounds() {
        return new Rectangle(x - 16, y - 16, width + 32, height + 32);
    }

    public void draw(Graphics2D g, boolean playerNear, long animTick) {
        if (collected) return;

        // Soft gold pedestal shadow
        g.setColor(new Color(0, 0, 0, 75));
        g.fillOval(x - 2, y + height - 4, width + 4, 8);

        // Pedestal base
        g.setColor(new Color(75, 62, 54));
        g.fillRect(x + 2, y + height - 8, width - 4, 8);
        g.setColor(new Color(195, 155, 95));
        g.drawRect(x + 2, y + height - 8, width - 4, 8);

        // Draw specific loot icon
        switch (type) {
            case CHALICE -> {
                g.setColor(new Color(255, 215, 65));
                g.fillRect(x + 8, y + 10, 8, 6);
                g.fillOval(x + 5, y + 2, 14, 10);
                g.fillRect(x + 10, y + 12, 4, 6);
                g.fillRect(x + 6, y + 16, 12, 3);
                g.setColor(new Color(255, 245, 180));
                g.fillOval(x + 7, y + 3, 4, 4);
            }
            case CLOCK -> {
                g.setColor(new Color(215, 175, 85));
                g.fillOval(x + 3, y + 2, 18, 18);
                g.setColor(new Color(245, 242, 230));
                g.fillOval(x + 6, y + 5, 12, 12);
                g.setColor(new Color(30, 25, 20));
                g.drawLine(x + 12, y + 11, x + 12, y + 7);
                g.drawLine(x + 12, y + 11, x + 15, y + 11);
            }
            case DIAMOND -> {
                g.setColor(new Color(70, 195, 255));
                int[] dx = {x + 12, x + 20, x + 12, x + 4};
                int[] dy = {y + 2, y + 10, y + 18, y + 10};
                g.fillPolygon(dx, dy, 4);
                g.setColor(new Color(220, 245, 255));
                g.fillPolygon(new int[]{x + 12, x + 16, x + 12, x + 8}, new int[]{y + 4, y + 10, y + 14, y + 10}, 4);
            }
            case DONATION_BOX -> {
                g.setColor(new Color(55, 45, 65));
                g.fillRect(x + 3, y + 4, 18, 14);
                g.setColor(new Color(225, 185, 95));
                g.drawRect(x + 3, y + 4, 18, 14);
                g.setColor(new Color(255, 215, 75));
                g.fillRect(x + 8, y + 6, 8, 2); // Coin slot
                g.fillOval(x + 10, y + 11, 4, 4);
            }
            case SCROLL -> {
                g.setColor(new Color(235, 225, 195));
                g.fillRoundRect(x + 4, y + 5, 16, 14, 4, 4);
                g.setColor(new Color(195, 60, 50));
                g.fillRect(x + 10, y + 5, 4, 14); // Ribbon
            }
        }

        // Floating sparkle glow
        if (animTick % 30 < 15) {
            g.setColor(new Color(255, 255, 200, 180));
            int spX = x + 3 + (int) ((animTick * 3) % 18);
            int spY = y + 2 + (int) ((animTick * 2) % 12);
            g.fillRect(spX, spY, 2, 2);
        }

        // Indicator when player is close
        if (playerNear) {
            g.setColor(new Color(255, 225, 110, 220));
            g.drawRect(x - 2, y - 2, width + 4, height + 4);
            g.setFont(new Font("Monospaced", Font.BOLD, 9));
            g.drawString("+$" + value, x - 2, y - 4);
        }
    }
}
