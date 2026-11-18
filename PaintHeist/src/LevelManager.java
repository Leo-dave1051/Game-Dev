import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public final class LevelManager {
    public static final String[] OUTFIT_NAMES = {
        "Classic Thief", "Museum Staff Disguise", "Formal Suit", "Dark Stealth Outfit", "Casual Outfit"
    };
    public static final int[] OUTFIT_COSTS = {0, 20, 30, 45, 60};

    public static final String[] ACCESSORY_NAMES = {
        "None", "Burglar Mask", "Gold Monocle", "Velvet Beret", "Tactical Visor", "Canvas Satchel"
    };
    public static final int[] ACCESSORY_COSTS = {0, 15, 25, 30, 40, 50};
    public static final String UPGRADE_FAST_COPY = "fastCopy";
    public static final String UPGRADE_LONG_THROW = "longThrow";
    public static final String UPGRADE_LONG_SWITCH = "longSwitch";

    public static final String EQUIP_SMOKE_BOMB = "smokeBomb";
    public static final String EQUIP_DISTRACTION_DEVICE = "distractionDevice";
    public static final String EQUIP_LOCKPICK_KIT = "lockpickKit";
    public static final String EQUIP_SILENT_SHOES = "silentShoes";
    public static final String EQUIP_QUICK_DRAW_BRUSH = "quickDrawBrush";

    public static final String[] ALL_EQUIPMENT = {
        EQUIP_SMOKE_BOMB,
        EQUIP_DISTRACTION_DEVICE,
        EQUIP_LOCKPICK_KIT,
        EQUIP_SILENT_SHOES,
        EQUIP_QUICK_DRAW_BRUSH
    };
    public static final String[] EQUIPMENT_NAMES = {
        "Smoke Bomb",
        "Distraction Device",
        "Lockpick Kit",
        "Silent Shoes",
        "Quick-Draw Brush"
    };
    public static final String[] EQUIPMENT_DESCRIPTIONS = {
        "Dense smoke cloud blinds guards & cameras for 7s [G/1]",
        "Enhanced noisemaker: +35% throw distance & lure radius",
        "Skeleton key: +50% laser/switch bypass offline time",
        "Padded soles: halves footstep noise & sprint vibration",
        "Sable hair brush: +35s canvas time in Studio & finer lines"
    };
    public static final int[] EQUIPMENT_COSTS = {
        25, 20, 25, 30, 25
    };
    public static final int MAX_EQUIPPED = 2;

    private static final Path SAVE_FILE = Paths.get(System.getProperty("user.home"), ".paintheist-save.properties");
    private final int[] bestScores = new int[MuseumLevel.all().size()];
    private final int[] bestTimes = new int[MuseumLevel.all().size()];
    private final Set<String> ownedUpgrades = new HashSet<>();
    private final Set<String> equippedItems = new HashSet<>();
    private int unlockedLevels = 1;
    private int totalRewards;
    private int unlockedOutfitsMask = 1;
    private int selectedOutfit;
    private int unlockedAccessoriesMask = 1;
    private int selectedAccessory;
    private boolean lightingEnabled = true;
    private boolean visionConesEnabled = true;
    private boolean promptsEnabled = true;

    public LevelManager() {
        for (int i = 0; i < bestTimes.length; i++) bestTimes[i] = -1;
        load();
    }

    public boolean isUnlocked(int levelIndex) {
        return levelIndex >= 0 && levelIndex < unlockedLevels;
    }

    public int getUnlockedLevels() {
        return unlockedLevels;
    }

    public int getBestScore(int levelIndex) {
        return bestScores[levelIndex];
    }

    public int getBestTime(int levelIndex) {
        return bestTimes[levelIndex];
    }

    public int getTotalRewards() {
        return totalRewards;
    }

    public boolean isOutfitUnlocked(int index) {
        return index >= 0 && index < OUTFIT_NAMES.length && (unlockedOutfitsMask & (1 << index)) != 0;
    }

    public int getSelectedOutfit() {
        return selectedOutfit;
    }

    public boolean selectOrBuyOutfit(int index) {
        if (index < 0 || index >= OUTFIT_NAMES.length) return false;
        if (isOutfitUnlocked(index)) {
            selectedOutfit = index;
            save();
            return true;
        }
        int cost = OUTFIT_COSTS[index];
        if (totalRewards < cost) return false;
        totalRewards -= cost;
        unlockedOutfitsMask |= (1 << index);
        selectedOutfit = index;
        save();
        return true;
    }

    public boolean isAccessoryUnlocked(int index) {
        return index >= 0 && index < ACCESSORY_NAMES.length && (unlockedAccessoriesMask & (1 << index)) != 0;
    }

    public int getSelectedAccessory() {
        return selectedAccessory;
    }

    public boolean selectOrBuyAccessory(int index) {
        if (index < 0 || index >= ACCESSORY_NAMES.length) return false;
        if (isAccessoryUnlocked(index)) {
            selectedAccessory = index;
            save();
            return true;
        }
        int cost = ACCESSORY_COSTS[index];
        if (totalRewards < cost) return false;
        totalRewards -= cost;
        unlockedAccessoriesMask |= (1 << index);
        selectedAccessory = index;
        save();
        return true;
    }

    public boolean hasUpgrade(String key) {
        return ownedUpgrades.contains(key);
    }

    public boolean buyUpgrade(String key, int cost) {
        if (ownedUpgrades.contains(key) || totalRewards < cost) return false;
        totalRewards -= cost;
        ownedUpgrades.add(key);
        // Auto-equip if slot is free
        if (equippedItems.size() < MAX_EQUIPPED) {
            equippedItems.add(key);
        }
        save();
        return true;
    }

    public boolean isEquipped(String key) {
        return equippedItems.contains(key);
    }

    public boolean toggleEquip(String key) {
        if (!hasUpgrade(key)) return false;
        if (equippedItems.contains(key)) {
            equippedItems.remove(key);
            save();
            return true;
        } else {
            if (equippedItems.size() < MAX_EQUIPPED) {
                equippedItems.add(key);
                save();
                return true;
            }
            return false;
        }
    }

    public int getEquippedCount() {
        return equippedItems.size();
    }

    public Set<String> getEquippedItems() {
        return java.util.Collections.unmodifiableSet(equippedItems);
    }

    public boolean isLightingEnabled() {
        return lightingEnabled;
    }

    public void setLightingEnabled(boolean enabled) {
        lightingEnabled = enabled;
        save();
    }

    public boolean isVisionConesEnabled() {
        return visionConesEnabled;
    }

    public void setVisionConesEnabled(boolean enabled) {
        visionConesEnabled = enabled;
        save();
    }

    public boolean isPromptsEnabled() {
        return promptsEnabled;
    }

    public void setPromptsEnabled(boolean enabled) {
        promptsEnabled = enabled;
        save();
    }

    public void saveSettings() {
        save();
    }

    public Result recordCompletion(GameWorld world) {
        if (!world.isWon()) throw new IllegalArgumentException("Only a completed heist can be scored.");
        int level = world.getLevelIndex();
        int remaining = world.getTimeLeft();
        int elapsed = world.getElapsedSeconds();

        int totalQuality = 0;
        int totalColor = 0;
        int totalShape = 0;
        int totalPosition = 0;
        int stolenCount = 0;
        if (world.getMap() != null) {
            for (MuseumMap.Painting p : world.getMap().getPaintings()) {
                if (p.isStolen()) {
                    stolenCount++;
                    totalQuality += p.getQuality();
                    if (p.getBreakdown() != null) {
                        totalColor += p.getBreakdown().getColorScore();
                        totalShape += p.getBreakdown().getShapeScore();
                        totalPosition += p.getBreakdown().getPositionScore();
                    } else {
                        totalColor += p.getQuality();
                        totalShape += p.getQuality();
                        totalPosition += p.getQuality();
                    }
                }
            }
        }
        int avgQuality = stolenCount > 0 ? (int) Math.round((double) totalQuality / stolenCount) : 80;
        int avgColor = stolenCount > 0 ? (int) Math.round((double) totalColor / stolenCount) : avgQuality;
        int avgShape = stolenCount > 0 ? (int) Math.round((double) totalShape / stolenCount) : avgQuality;
        int avgPosition = stolenCount > 0 ? (int) Math.round((double) totalPosition / stolenCount) : avgQuality;

        int bonusLoot = world.getBonusLootMoney();
        int stolenLoot = world.getStolenLootCount();
        int totalLoot = world.getMap() != null ? world.getMap().getLoot().size() : 0;

        // Three-star rating system:
        // 1 Star: Successfully completed heist
        // 2 Stars: Acceptable replica accuracy (>= 70%) AND at least 50% valuables stolen (if any)
        // 3 Stars: High accuracy (>= 85%) AND minimal detection (<= 1 incident) AND all valuables stolen (if any)
        int stars = 1;
        boolean goodLoot = totalLoot == 0 || stolenLoot >= (totalLoot + 1) / 2;
        boolean allLoot = totalLoot == 0 || stolenLoot >= totalLoot;
        if (avgQuality >= 70 && goodLoot) stars = 2;
        if (avgQuality >= 85 && world.getDetectionIncidents() <= 1 && allLoot) stars = 3;

        // Rewards breakdown
        int baseReward = 15;
        int accuracyBonus = avgQuality >= 85 ? 15 : avgQuality >= 70 ? 8 : 0;
        int stealthBonus = world.getDetectionIncidents() == 0 ? 15 : world.getDetectionIncidents() <= 1 ? 5 : 0;
        int timeBonus = Math.max(0, remaining / 15);
        int lootReward = bonusLoot / 10;
        int totalReward = baseReward + accuracyBonus + stealthBonus + timeBonus + lootReward;

        int score = remaining * 15 + stolenCount * 400
            + Math.max(0, 100 - world.getSuspicion()) * 5
            + (world.getDetectionIncidents() == 0 ? 500 : 0)
            + avgQuality * 10
            + bonusLoot * 2;

        if (score > bestScores[level]) bestScores[level] = score;
        if (bestTimes[level] < 0 || elapsed < bestTimes[level]) {
            bestTimes[level] = elapsed;
        }
        unlockedLevels = Math.max(unlockedLevels, Math.min(bestScores.length, level + 2));
        totalRewards += totalReward;
        save();
        return new Result(score, stars, totalReward, baseReward, accuracyBonus, stealthBonus, timeBonus, lootReward,
            remaining, elapsed, world.getDetectionIncidents(), world.getSuspicion(),
            stolenCount, world.getTargetCount(), avgQuality, avgColor, avgShape, avgPosition,
            bonusLoot, stolenLoot, totalLoot);
    }

    private void load() {
        if (!Files.isRegularFile(SAVE_FILE)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(SAVE_FILE)) {
            props.load(in);
            unlockedLevels = Math.max(1, Math.min(bestScores.length,
                parseInt(props.getProperty("unlockedLevels"), 1)));
            totalRewards = Math.max(0, parseInt(props.getProperty("totalRewards"), 0));
            unlockedOutfitsMask = Math.max(1, parseInt(props.getProperty("unlockedOutfits"), 1));
            int savedOutfit = parseInt(props.getProperty("selectedOutfit"), 0);
            selectedOutfit = isOutfitUnlocked(savedOutfit) ? savedOutfit : 0;
            unlockedAccessoriesMask = Math.max(1, parseInt(props.getProperty("unlockedAccessories"), 1));
            int savedAccessory = parseInt(props.getProperty("selectedAccessory"), 0);
            selectedAccessory = isAccessoryUnlocked(savedAccessory) ? savedAccessory : 0;
            lightingEnabled = !"false".equalsIgnoreCase(props.getProperty("lightingEnabled", "true"));
            visionConesEnabled = !"false".equalsIgnoreCase(props.getProperty("visionConesEnabled", "true"));
            promptsEnabled = !"false".equalsIgnoreCase(props.getProperty("promptsEnabled", "true"));
            if ("true".equalsIgnoreCase(props.getProperty("muted", "false")) != SoundEffects.isMuted()) {
                SoundEffects.toggleMute();
            }
            for (String key : ALL_EQUIPMENT) {
                if ("true".equalsIgnoreCase(props.getProperty("upgrade." + key, "false"))) {
                    ownedUpgrades.add(key);
                }
                if ("true".equalsIgnoreCase(props.getProperty("equipped." + key, "false"))) {
                    if (equippedItems.size() < MAX_EQUIPPED) {
                        equippedItems.add(key);
                    }
                }
            }
            // Backward compatibility with legacy upgrade keys
            if ("true".equalsIgnoreCase(props.getProperty("upgrade.fastCopy", "false"))) {
                ownedUpgrades.add(EQUIP_QUICK_DRAW_BRUSH);
            }
            if ("true".equalsIgnoreCase(props.getProperty("upgrade.longThrow", "false"))) {
                ownedUpgrades.add(EQUIP_DISTRACTION_DEVICE);
            }
            if ("true".equalsIgnoreCase(props.getProperty("upgrade.longSwitch", "false"))) {
                ownedUpgrades.add(EQUIP_LOCKPICK_KIT);
            }
            for (int i = 0; i < bestScores.length; i++) {
                bestScores[i] = Math.max(0, parseInt(props.getProperty("bestScore." + i), 0));
                bestTimes[i] = parseInt(props.getProperty("bestTime." + i), -1);
            }
        } catch (IOException ignored) {
            // Keep default in-memory progression if save file cannot be read.
        }
    }

    private void save() {
        Properties props = new Properties();
        props.setProperty("unlockedLevels", Integer.toString(unlockedLevels));
        props.setProperty("totalRewards", Integer.toString(totalRewards));
        props.setProperty("unlockedOutfits", Integer.toString(unlockedOutfitsMask));
        props.setProperty("selectedOutfit", Integer.toString(selectedOutfit));
        props.setProperty("unlockedAccessories", Integer.toString(unlockedAccessoriesMask));
        props.setProperty("selectedAccessory", Integer.toString(selectedAccessory));
        props.setProperty("lightingEnabled", Boolean.toString(lightingEnabled));
        props.setProperty("visionConesEnabled", Boolean.toString(visionConesEnabled));
        props.setProperty("promptsEnabled", Boolean.toString(promptsEnabled));
        props.setProperty("muted", Boolean.toString(SoundEffects.isMuted()));
        for (String key : ALL_EQUIPMENT) {
            props.setProperty("upgrade." + key, Boolean.toString(ownedUpgrades.contains(key)));
            props.setProperty("equipped." + key, Boolean.toString(equippedItems.contains(key)));
        }
        for (int i = 0; i < bestScores.length; i++) {
            props.setProperty("bestScore." + i, Integer.toString(bestScores[i]));
            props.setProperty("bestTime." + i, Integer.toString(bestTimes[i]));
        }
        try (OutputStream out = Files.newOutputStream(SAVE_FILE)) {
            props.store(out, "PaintHeist progress");
        } catch (IOException ignored) {
            // Non-fatal if user home directory is read-only.
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static final class Result {
        private final int score;
        private final int stars;
        private final int reward;
        private final int baseReward;
        private final int accuracyBonus;
        private final int stealthBonus;
        private final int timeBonus;
        private final int lootReward;
        private final int timeRemaining;
        private final int elapsedSeconds;
        private final int detectionIncidents;
        private final int suspicion;
        private final int paintingsStolen;
        private final int targetPaintings;
        private final int averageAccuracy;
        private final int colorAccuracy;
        private final int shapeAccuracy;
        private final int positionAccuracy;
        private final int bonusLoot;
        private final int stolenLoot;
        private final int totalLoot;

        private Result(int score, int stars, int reward, int baseReward, int accuracyBonus,
                       int stealthBonus, int timeBonus, int lootReward, int timeRemaining, int elapsedSeconds,
                       int detectionIncidents, int suspicion, int paintingsStolen,
                       int targetPaintings, int averageAccuracy, int colorAccuracy,
                       int shapeAccuracy, int positionAccuracy,
                       int bonusLoot, int stolenLoot, int totalLoot) {
            this.score = score;
            this.stars = stars;
            this.reward = reward;
            this.baseReward = baseReward;
            this.accuracyBonus = accuracyBonus;
            this.stealthBonus = stealthBonus;
            this.timeBonus = timeBonus;
            this.lootReward = lootReward;
            this.timeRemaining = timeRemaining;
            this.elapsedSeconds = elapsedSeconds;
            this.detectionIncidents = detectionIncidents;
            this.suspicion = suspicion;
            this.paintingsStolen = paintingsStolen;
            this.targetPaintings = targetPaintings;
            this.averageAccuracy = averageAccuracy;
            this.colorAccuracy = colorAccuracy;
            this.shapeAccuracy = shapeAccuracy;
            this.positionAccuracy = positionAccuracy;
            this.bonusLoot = bonusLoot;
            this.stolenLoot = stolenLoot;
            this.totalLoot = totalLoot;
        }

        public int getScore() { return score; }
        public int getStars() { return stars; }
        public int getReward() { return reward; }
        public int getBaseReward() { return baseReward; }
        public int getAccuracyBonus() { return accuracyBonus; }
        public int getStealthBonus() { return stealthBonus; }
        public int getTimeBonus() { return timeBonus; }
        public int getLootReward() { return lootReward; }
        public int getTimeRemaining() { return timeRemaining; }
        public int getElapsedSeconds() { return elapsedSeconds; }
        public int getDetectionIncidents() { return detectionIncidents; }
        public int getSuspicion() { return suspicion; }
        public int getPaintingsStolen() { return paintingsStolen; }
        public int getTargetPaintings() { return targetPaintings; }
        public int getAverageAccuracy() { return averageAccuracy; }
        public int getColorAccuracy() { return colorAccuracy; }
        public int getShapeAccuracy() { return shapeAccuracy; }
        public int getPositionAccuracy() { return positionAccuracy; }
        public int getBonusLoot() { return bonusLoot; }
        public int getStolenLoot() { return stolenLoot; }
        public int getTotalLoot() { return totalLoot; }
    }
}
