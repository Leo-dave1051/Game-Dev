import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MuseumLevel {
    private static final List<MuseumLevel> LEVELS = createLevels();

    private final int id;
    private final String name;
    private final String difficulty;
    private final String challenge;
    private final Color floorA;
    private final Color floorB;
    private final List<Rectangle> walls;
    private final List<Rectangle> cases;
    private final List<Rectangle> benches;
    private final List<Rectangle> statues;
    private final List<Rectangle> lasers;
    private final List<Rectangle> switches;
    private final List<Point> exits;
    private final List<Point> paintingPositions;
    private final List<String> paintingNames;
    private final List<GuardRoute> guards;
    private final List<HideSpotData> hideSpots;
    private final List<LaserBarrier.Data> laserPatterns;
    private final List<MovableObject.Data> movableObjects;
    private final List<SecretPassage.Data> secretPassages;
    private final Point playerStart;
    private final int missionSeconds;

    private final int mapWidth;
    private final int mapHeight;
    private final List<MuseumLoot> loot;

    private MuseumLevel(int id, String name, String difficulty, String challenge,
                        Color floorA, Color floorB, int missionSeconds, int mapWidth, int mapHeight,
                        Point playerStart, List<Rectangle> walls, List<Rectangle> cases,
                        List<Rectangle> benches, List<Rectangle> statues, List<Rectangle> lasers,
                        List<Rectangle> switches, List<Point> exits, List<Point> paintingPositions,
                        List<String> paintingNames, List<GuardRoute> guards, List<HideSpotData> hideSpots,
                        List<MuseumLoot> loot) {
        this.id = id;
        this.name = name;
        this.difficulty = difficulty;
        this.challenge = challenge;
        this.floorA = floorA;
        this.floorB = floorB;
        this.missionSeconds = missionSeconds;
        this.mapWidth = mapWidth > 0 ? mapWidth : 960;
        this.mapHeight = mapHeight > 0 ? mapHeight : 600;
        this.playerStart = new Point(playerStart);
        this.walls = copyRectangles(walls);
        this.cases = copyRectangles(cases);
        this.benches = copyRectangles(benches);
        this.statues = copyRectangles(statues);
        this.lasers = copyRectangles(lasers);
        this.switches = copyRectangles(switches);
        this.exits = copyPoints(exits);
        this.paintingPositions = copyPoints(paintingPositions);
        this.paintingNames = Collections.unmodifiableList(new ArrayList<>(paintingNames));
        this.guards = Collections.unmodifiableList(new ArrayList<>(guards));
        this.hideSpots = Collections.unmodifiableList(new ArrayList<>(hideSpots));
        this.loot = loot != null ? Collections.unmodifiableList(new ArrayList<>(loot)) : Collections.emptyList();
        this.laserPatterns = createLaserPatterns(id, this.lasers);
        this.movableObjects = createMovableObjects(id);
        this.secretPassages = createSecretPassages(id);
    }

    private MuseumLevel(int id, String name, String difficulty, String challenge,
                        Color floorA, Color floorB, int missionSeconds,
                        Point playerStart, List<Rectangle> walls, List<Rectangle> cases,
                        List<Rectangle> benches, List<Rectangle> statues, List<Rectangle> lasers,
                        List<Rectangle> switches, List<Point> exits, List<Point> paintingPositions,
                        List<String> paintingNames, List<GuardRoute> guards, List<HideSpotData> hideSpots) {
        this(id, name, difficulty, challenge, floorA, floorB, missionSeconds, 960, 600,
            playerStart, walls, cases, benches, statues, lasers, switches, exits,
            paintingPositions, paintingNames, guards, hideSpots, Collections.emptyList());
    }

    public static List<MuseumLevel> all() {
        return LEVELS;
    }

    public static MuseumLevel get(int index) {
        if (index < 0 || index >= LEVELS.size()) throw new IllegalArgumentException("Invalid museum level: " + index);
        return LEVELS.get(index);
    }

    private static List<MuseumLevel> createLevels() {
        List<MuseumLevel> levels = new ArrayList<>();

        levels.add(new MuseumLevel(
            0, "The Classic Gallery", "EASY", "Explore the rooms, secure the clock & donation box, and forge the Mona Lisa",
            new Color(125, 91, 70), new Color(114, 81, 65), 240, 1360, 800,
            p(55, 680),
            rects(
                r(24, 460, 280, 20), r(400, 460, 280, 20),
                r(700, 70, 20, 200), r(700, 360, 20, 400),
                r(980, 70, 20, 280), r(980, 460, 20, 300),
                r(240, 190, 20, 150), r(480, 190, 20, 150)
            ),
            rects(r(120, 280, 90, 40), r(360, 280, 100, 40), r(820, 220, 90, 40)),
            rects(r(80, 560, 60, 24), r(480, 560, 60, 24), r(820, 560, 60, 24)),
            rects(r(350, 490, 26, 36)),
            rects(), rects(),
            points(p(42, 700), p(1280, 700)),
            points(p(340, 110)),
            strings("Mona Lisa"),
            routes(
                route("Gallery Guard", 188, 180, 72, p(188, 180), p(520, 180), p(520, 380), p(188, 380)),
                route("Workshop Guard", 840, 480, 70, p(840, 480), p(1220, 480), p(1220, 640), p(840, 640))
            ),
            hides(hide("Gallery wardrobe", 220, 380, 46, 52), hide("Workshop cabinet", 880, 380, 46, 52), hide("Storage locker", 1180, 620, 46, 52)),
            lootItems(
                new MuseumLoot(1, "Museum Donation Box", MuseumLoot.LootType.DONATION_BOX, 240, 640, 180),
                new MuseumLoot(2, "Antique Silver Clock", MuseumLoot.LootType.CLOCK, 1080, 180, 250)
            )
        ));

        levels.add(new MuseumLevel(
            1, "The Grand Museum", "MEDIUM", "Interconnected exhibition wings, cameras, and the starry night masterpiece",
            new Color(132, 127, 119), new Color(112, 111, 108), 280, 1560, 900,
            p(55, 780),
            rects(
                r(420, 70, 20, 260), r(420, 440, 20, 420),
                r(1120, 70, 20, 260), r(1120, 440, 20, 420),
                r(24, 420, 180, 20), r(270, 420, 150, 20),
                r(1140, 420, 180, 20), r(1380, 420, 150, 20),
                r(680, 220, 160, 20)
            ),
            rects(r(120, 220, 90, 40), r(580, 240, 100, 40), r(880, 240, 100, 40), r(1260, 220, 90, 40),
                r(160, 600, 100, 40), r(620, 640, 120, 40), r(820, 640, 120, 40), r(1260, 600, 100, 40)),
            rects(r(80, 340, 60, 24), r(1380, 340, 60, 24), r(740, 760, 80, 24)),
            rects(r(760, 460, 36, 48), r(540, 520, 28, 36), r(980, 520, 28, 36)),
            rects(), rects(),
            points(p(42, 780), p(1480, 780)),
            points(p(740, 110), p(220, 110), p(1320, 110)),
            strings("The Starry Night", "Marble Garden", "Portrait in Blue"),
            routes(
                route("West Wing Guard", Guard.GuardType.REGULAR, 160, 260, 75, p(160, 260), p(340, 260), p(340, 680), p(160, 680)),
                route("East Wing Guard", Guard.GuardType.REGULAR, 1220, 260, 72, p(1220, 260), p(1440, 260), p(1440, 680), p(1220, 680)),
                route("Grand Hall Supervisor", Guard.GuardType.SUPERVISOR, 560, 360, 74, p(560, 360), p(980, 360), p(980, 740), p(560, 740))
            ),
            hides(hide("West wardrobe", 220, 340, 46, 52), hide("Restoration cabinet", 1280, 340, 46, 52),
                hide("East locker", 1420, 560, 44, 50)),
            lootItems(
                new MuseumLoot(3, "Gilded Chalice", MuseumLoot.LootType.CHALICE, 240, 540, 350),
                new MuseumLoot(4, "Antique Gold Clock", MuseumLoot.LootType.CLOCK, 1340, 220, 280),
                new MuseumLoot(5, "Atrium Donation Vault", MuseumLoot.LootType.DONATION_BOX, 760, 800, 180)
            )
        ));

        levels.add(new MuseumLevel(
            2, "The Royal Exhibition", "HARD", "Throne treasury, timed lasers, secret passages, and the pearl earring",
            new Color(108, 66, 64), new Color(82, 50, 57), 320, 1680, 960,
            p(55, 840),
            rects(
                r(380, 70, 20, 320), r(380, 520, 20, 380),
                r(1240, 70, 20, 320), r(1240, 520, 20, 380),
                r(24, 460, 160, 20), r(240, 460, 140, 20),
                r(740, 70, 20, 280), r(740, 480, 20, 420),
                r(760, 480, 480, 20)
            ),
            rects(r(100, 220, 100, 40), r(480, 220, 120, 40), r(920, 220, 120, 40), r(1380, 220, 100, 40),
                r(120, 620, 100, 40), r(500, 680, 120, 40), r(900, 680, 120, 40), r(1400, 620, 100, 40)),
            rects(r(80, 380, 60, 24), r(1480, 380, 60, 24)),
            rects(r(560, 380, 32, 42), r(1020, 380, 32, 42)),
            rects(r(1040, 490, 8, 140)),
            rects(r(1020, 680, 38, 28)),
            points(p(42, 840), p(1600, 840)),
            points(p(560, 110), p(980, 110), p(220, 110), p(1440, 110)),
            strings("Girl with a Pearl Earring", "Gilded Promise", "Queen at Dusk", "The Royal Seal"),
            routes(
                route("West Sentinel", Guard.GuardType.ELITE, 180, 220, 80, p(180, 220), p(320, 220), p(320, 740), p(180, 740)),
                route("Royal Guard", Guard.GuardType.REGULAR, 1320, 220, 75, p(1320, 220), p(1540, 220), p(1540, 740), p(1320, 740)),
                route("Throne Room Supervisor", Guard.GuardType.SUPERVISOR, 520, 440, 78, p(520, 440), p(680, 440), p(680, 780), p(520, 780)),
                route("Curator Vance", Guard.GuardType.CURATOR, 860, 220, 70, p(860, 220), p(1140, 220), p(1140, 420), p(860, 420))
            ),
            hides(hide("Royal wardrobe", 220, 380, 44, 50), hide("Servants' cabinet", 840, 520, 44, 48), hide("East locker", 1440, 480, 44, 50)),
            lootItems(
                new MuseumLoot(6, "Royal Sapphire Diamond", MuseumLoot.LootType.DIAMOND, 1120, 540, 600),
                new MuseumLoot(7, "Gilded Chalice", MuseumLoot.LootType.CHALICE, 240, 260, 380),
                new MuseumLoot(8, "Velvet Donation Box", MuseumLoot.LootType.DONATION_BOX, 620, 820, 200)
            )
        ));

        levels.add(new MuseumLevel(
            3, "The Museum of Ancient Art", "HARD", "Pharaoh's tomb, antiquities pillar hall, moving urns, and the great wave",
            new Color(123, 108, 83), new Color(105, 91, 72), 340, 1780, 1020,
            p(55, 900),
            rects(
                r(440, 70, 20, 340), r(440, 540, 20, 420),
                r(1320, 70, 20, 340), r(1320, 540, 20, 420),
                r(24, 480, 180, 20), r(260, 480, 180, 20),
                r(880, 70, 20, 300), r(880, 500, 20, 460)
            ),
            rects(r(120, 220, 100, 40), r(560, 220, 120, 40), r(1020, 220, 120, 40), r(1460, 220, 100, 40),
                r(140, 660, 100, 40), r(580, 700, 120, 40), r(1040, 700, 120, 40), r(1480, 660, 100, 40)),
            rects(r(80, 380, 60, 24), r(1560, 380, 60, 24)),
            rects(r(620, 400, 34, 46), r(1100, 400, 34, 46), r(760, 620, 34, 46)),
            rects(r(1140, 480, 8, 120)),
            rects(r(1120, 680, 38, 26)),
            points(p(42, 900), p(1680, 900)),
            points(p(640, 110), p(1080, 110), p(220, 110), p(1520, 110)),
            strings("The Great Wave off Kanagawa", "The Bronze Oracle", "Amber Dynasty", "River of Kings", "Hidden Pharaoh"),
            routes(
                route("Antiquities Guard", Guard.GuardType.REGULAR, 200, 240, 70, p(200, 240), p(380, 240), p(380, 780), p(200, 780)),
                route("Pillar Hall Elite", Guard.GuardType.ELITE, 580, 240, 78, p(580, 240), p(820, 240), p(820, 800), p(580, 800)),
                route("Lower Gallery Supervisor", Guard.GuardType.SUPERVISOR, 940, 340, 74, p(940, 340), p(1240, 340), p(1240, 800), p(940, 800)),
                route("East Sentinel", Guard.GuardType.ELITE, 1400, 240, 78, p(1400, 240), p(1640, 240), p(1640, 780), p(1400, 780))
            ),
            hides(hide("Dig storeroom", 260, 380, 46, 52), hide("Artifact cabinet", 980, 520, 44, 52), hide("Tomb locker", 1520, 480, 44, 52)),
            lootItems(
                new MuseumLoot(9, "Golden Chalice", MuseumLoot.LootType.CHALICE, 1200, 520, 420),
                new MuseumLoot(10, "Ancient Papyrus Scroll", MuseumLoot.LootType.SCROLL, 280, 640, 320),
                new MuseumLoot(11, "Carved Jade Clock", MuseumLoot.LootType.CLOCK, 1540, 260, 300)
            )
        ));

        levels.add(new MuseumLevel(
            4, "The National Art Museum", "VERY HARD", "High-security matrix, sweeping cameras, multiple alarms, and the scream",
            new Color(94, 111, 119), new Color(76, 91, 101), 380, 1920, 1120,
            p(55, 1000),
            rects(
                r(480, 70, 20, 360), r(480, 580, 20, 480),
                r(1440, 70, 20, 360), r(1440, 580, 20, 480),
                r(24, 520, 200, 20), r(280, 520, 200, 20),
                r(960, 70, 20, 340), r(960, 540, 20, 520),
                r(980, 540, 460, 20)
            ),
            rects(r(140, 240, 100, 40), r(620, 240, 120, 40), r(1120, 240, 120, 40), r(1600, 240, 100, 40),
                r(160, 720, 100, 40), r(640, 760, 120, 40), r(1140, 760, 120, 40), r(1620, 720, 100, 40)),
            rects(r(80, 420, 60, 24), r(1720, 420, 60, 24)),
            rects(r(680, 420, 36, 48), r(1220, 420, 36, 48)),
            rects(r(1280, 540, 8, 140)),
            rects(r(1260, 760, 38, 28)),
            points(p(42, 1000), p(1820, 1000)),
            points(p(720, 110), p(1200, 110), p(240, 110), p(1680, 110)),
            strings("The Scream", "New City", "After the Rain", "Glass Horizon", "The Last Original"),
            routes(
                route("North Elite", Guard.GuardType.ELITE, 220, 240, 80, p(220, 240), p(420, 240), p(420, 860), p(220, 860)),
                route("Modern East Sentinel", Guard.GuardType.ELITE, 1500, 240, 82, p(1500, 240), p(1780, 240), p(1780, 860), p(1500, 860)),
                route("Sculpture Hall Supervisor", Guard.GuardType.SUPERVISOR, 600, 380, 80, p(600, 380), p(900, 380), p(900, 860), p(600, 860)),
                route("Upper Security Elite", Guard.GuardType.ELITE, 1040, 240, 82, p(1040, 240), p(1360, 240), p(1360, 480), p(1040, 480)),
                route("West Wing Guard", Guard.GuardType.REGULAR, 80, 600, 74, p(80, 600), p(380, 600), p(380, 960), p(80, 960))
            ),
            hides(hide("Staff locker", 280, 440, 44, 52), hide("Maintenance cabinet", 1060, 440, 44, 52), hide("Vault locker", 1680, 520, 44, 52)),
            lootItems(
                new MuseumLoot(12, "Flawless Diamond Exhibit", MuseumLoot.LootType.DIAMOND, 1340, 580, 750),
                new MuseumLoot(13, "Imperial Gold Clock", MuseumLoot.LootType.CLOCK, 340, 280, 400),
                new MuseumLoot(14, "Platinum Chalice", MuseumLoot.LootType.CHALICE, 1680, 280, 500),
                new MuseumLoot(15, "Executive Donation Vault", MuseumLoot.LootType.DONATION_BOX, 740, 880, 250)
            )
        ));
        return Collections.unmodifiableList(levels);
    }

    private static Point p(int x, int y) { return new Point(x, y); }
    private static Rectangle r(int x, int y, int width, int height) { return new Rectangle(x, y, width, height); }
    private static List<Rectangle> rects(Rectangle... values) { return Arrays.asList(values); }
    private static List<Point> points(Point... values) { return Arrays.asList(values); }
    private static List<String> strings(String... values) { return Arrays.asList(values); }
    private static List<GuardRoute> routes(GuardRoute... values) { return Arrays.asList(values); }
    private static List<HideSpotData> hides(HideSpotData... values) { return Arrays.asList(values); }
    private static List<MuseumLoot> lootItems(MuseumLoot... values) { return Arrays.asList(values); }

    private static List<LaserBarrier.Data> createLaserPatterns(int levelId, List<Rectangle> lasers) {
        List<LaserBarrier.Data> patterns = new ArrayList<>();
        for (int i = 0; i < lasers.size(); i++) {
            double period;
            double warning;
            double active;
            double offset;
            if (levelId == 2) {
                period = 9.0;
                warning = 2.0;
                active = 3.0;
                offset = 0;
            } else if (levelId == 3) {
                period = i == 0 ? 8.5 : 10.5;
                warning = i == 0 ? 1.8 : 2.2;
                active = i == 0 ? 2.8 : 3.3;
                offset = i == 0 ? 0 : 2.4;
            } else {
                period = i == 0 ? 8.0 : 10.0;
                warning = i == 0 ? 1.6 : 2.0;
                active = i == 0 ? 2.8 : 3.2;
                offset = i == 0 ? 0 : 3.0;
            }
            patterns.add(new LaserBarrier.Data("Laser Grid " + (i + 1), lasers.get(i),
                period, warning, active, offset));
        }
        return Collections.unmodifiableList(patterns);
    }

    private static List<MovableObject.Data> createMovableObjects(int levelId) {
        switch (levelId) {
            case 0:
                return Collections.singletonList(
                    new MovableObject.Data("Gallery crate", r(220, 184, 24, 24), true, false, 130));
            case 1:
                return Arrays.asList(
                    new MovableObject.Data("Maintenance cart", r(520, 208, 30, 24), true, true, 210),
                    new MovableObject.Data("Marble plinth", r(566, 416, 28, 30), false, false, 0));
            case 2:
                return Arrays.asList(
                    new MovableObject.Data("Royal service cart", r(540, 204, 30, 24), true, true, 230),
                    new MovableObject.Data("Gilded display stand", r(314, 416, 30, 28), false, false, 0));
            case 3:
                return Arrays.asList(
                    new MovableObject.Data("Ancient urn", r(452, 235, 22, 24), true, false, 165),
                    new MovableObject.Data("Stone sarcophagus", r(545, 456, 42, 30), false, false, 0));
            default:
                return Arrays.asList(
                    new MovableObject.Data("Exhibition trolley", r(540, 260, 32, 24), true, true, 245),
                    new MovableObject.Data("Sculpture plinth", r(705, 444, 30, 28), false, false, 0));
        }
    }

    private static List<SecretPassage.Data> createSecretPassages(int levelId) {
        if (levelId == 2) {
            return Collections.singletonList(new SecretPassage.Data("Servants' route",
                r(406, 316, 28, 28), r(526, 316, 28, 28)));
        }
        if (levelId == 3) {
            return Collections.singletonList(new SecretPassage.Data("Pillar gallery passage",
                r(448, 336, 28, 28), r(570, 336, 28, 28)));
        }
        if (levelId == 4) {
            return Collections.singletonList(new SecretPassage.Data("Conservation corridor",
                r(548, 214, 28, 28), r(612, 214, 28, 28)));
        }
        return Collections.emptyList();
    }

    private static List<Rectangle> copyRectangles(List<Rectangle> input) {
        List<Rectangle> result = new ArrayList<>();
        for (Rectangle rectangle : input) result.add(new Rectangle(rectangle));
        return Collections.unmodifiableList(result);
    }
    private static List<Point> copyPoints(List<Point> input) {
        List<Point> result = new ArrayList<>();
        for (Point point : input) result.add(new Point(point));
        return Collections.unmodifiableList(result);
    }

    private static GuardRoute route(String name, int x, int y, int speed, Point... points) {
        return new GuardRoute(name, Guard.GuardType.REGULAR, new Point(x, y), speed, Arrays.asList(points));
    }

    private static GuardRoute route(String name, Guard.GuardType type, int x, int y, int speed, Point... points) {
        return new GuardRoute(name, type, new Point(x, y), speed, Arrays.asList(points));
    }

    private static HideSpotData hide(String name, int x, int y, int width, int height) {
        return new HideSpotData(name, new Rectangle(x, y, width, height));
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDifficulty() { return difficulty; }
    public String getChallenge() { return challenge; }
    public Color getFloorA() { return floorA; }
    public Color getFloorB() { return floorB; }
    public int getMissionSeconds() { return missionSeconds; }
    public Point getPlayerStart() { return new Point(playerStart); }
    public List<Rectangle> getWalls() { return walls; }
    public List<Rectangle> getCases() { return cases; }
    public List<Rectangle> getBenches() { return benches; }
    public List<Rectangle> getStatues() { return statues; }
    public List<Rectangle> getLasers() { return lasers; }
    public List<Rectangle> getSwitches() { return switches; }
    public List<Point> getExits() { return copyPoints(exits); }
    public List<Point> getPaintingPositions() { return copyPoints(paintingPositions); }
    public List<String> getPaintingNames() { return paintingNames; }
    public List<GuardRoute> getGuards() { return guards; }
    public List<HideSpotData> getHideSpots() { return hideSpots; }
    public List<LaserBarrier.Data> getLaserPatterns() { return laserPatterns; }
    public List<MovableObject.Data> getMovableObjects() { return movableObjects; }
    public List<SecretPassage.Data> getSecretPassages() { return secretPassages; }
    public int getPaintingCount() { return paintingPositions.size(); }
    public int getMapWidth() { return mapWidth; }
    public int getMapHeight() { return mapHeight; }
    public List<MuseumLoot> getLoot() { return loot; }
    public List<CameraData> getCameras() {
        switch (id) {
            case 0:
                return Collections.singletonList(
                    new CameraData("Gallery Entry Camera", 272, 127, 180, 38, 205, 0.65, 1.15));
            case 1:
                return Collections.singletonList(
                    new CameraData("Grand Hall Camera", 470, 96, 90, 50, 235, 0.72, 0.95));
            case 2:
                return Arrays.asList(
                    new CameraData("Royal North Camera", 350, 96, 90, 38, 250, 0.7, 0.9),
                    new CameraData("Throne Room Camera", 835, 272, 180, 34, 230, 0.9, 0.75));
            case 3:
                return Collections.singletonList(
                    new CameraData("Antiquities Camera", 468, 96, 90, 40, 245, 0.68, 0.9));
            case 4:
                return Arrays.asList(
                    new CameraData("West Security Camera", 166, 274, 90, 48, 260, 0.8, 0.8),
                    new CameraData("Central Security Camera", 548, 96, 90, 42, 290, 0.65, 0.65),
                    new CameraData("East Security Camera", 910, 290, 180, 42, 270, 0.9, 0.7));
            default:
                return Collections.emptyList();
        }
    }

    public static final class CameraData {
        private final String name;
        private final int x;
        private final int y;
        private final double facing;
        private final double sweep;
        private final double radius;
        private final double speed;
        private final double detectionSeconds;
        private CameraData(String name, int x, int y, double facing, double sweep, double radius, double speed) {
            this(name, x, y, facing, sweep, radius, speed, 0.9);
        }
        private CameraData(String name, int x, int y, double facing, double sweep, double radius, double speed,
                          double detectionSeconds) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.facing = facing;
            this.sweep = sweep;
            this.radius = radius;
            this.speed = speed;
            this.detectionSeconds = detectionSeconds;
        }
        public String getName() { return name; }
        public int getX() { return x; }
        public int getY() { return y; }
        public double getFacing() { return facing; }
        public double getSweep() { return sweep; }
        public double getRadius() { return radius; }
        public double getSpeed() { return speed; }
        public double getDetectionSeconds() { return detectionSeconds; }
    }

    public static final class GuardRoute {
        private final String name;
        private final Guard.GuardType guardType;
        private final Point start;
        private final int speed;
        private final List<Point> waypoints;

        private GuardRoute(String name, Guard.GuardType guardType, Point start, int speed, List<Point> waypoints) {
            this.name = name;
            this.guardType = guardType != null ? guardType : Guard.GuardType.REGULAR;
            this.start = new Point(start);
            this.speed = speed;
            this.waypoints = copyPoints(waypoints);
        }

        public String getName() { return name; }
        public Guard.GuardType getGuardType() { return guardType; }
        public Point getStart() { return new Point(start); }
        public int getSpeed() { return speed; }
        public List<Point> getWaypoints() { return waypoints; }
    }

    public static final class HideSpotData {
        private final String name;
        private final Rectangle bounds;
        private HideSpotData(String name, Rectangle bounds) {
            this.name = name;
            this.bounds = new Rectangle(bounds);
        }
        public String getName() { return name; }
        public Rectangle getBounds() { return new Rectangle(bounds); }
    }
}
