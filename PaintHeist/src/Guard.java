import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public final class Guard {
    public enum State { PATROL, INSPECT, ALERT, CHASE, SEARCH, RETURN, INSPECTING_ART }

    public enum DetectionStage {
        UNAWARE,        // 0% - 24%: Calm routine patrol
        SUSPICIOUS,     // 25% - 64%: Pauses, turns toward disturbance, yellow "?"
        INVESTIGATING,  // 65% - 94%: Walks to investigate suspicious spot, orange "?"
        ALERT,          // 95% - 99%: Intently identifying intruder, bright red "!"
        CHASE           // 100%: Confirmed intruder! Full sprint chase
    }

    public enum GuardType {
        REGULAR,
        ELITE,
        SUPERVISOR,
        CURATOR
    }

    public static final int WIDTH = 24;
    public static final int HEIGHT = 24;
    private final List<Point> patrolRoute;
    private final int patrolSpeed;
    private final String name;
    private final GuardType guardType;
    private double x;
    private double y;
    private double facingX = 1;
    private double facingY;
    private int patrolIndex;
    private State state = State.PATROL;
    private DetectionStage detectionStage = DetectionStage.UNAWARE;
    private double suspicion; // 0.0 to 100.0
    private double stateTimer;
    private double lostSightTimer;
    private double replanTimer;
    private double searchTimer;
    private double searchOffset;
    private double goalX;
    private double goalY;
    private double searchAnchorX;
    private double searchAnchorY;
    private double pathEndX;
    private double pathEndY;
    private boolean pathCalculated;
    private boolean pathReachable;
    private boolean moving;
    private List<Point> path = new ArrayList<>();
    private boolean playerVisible;

    private MuseumMap.Painting inspectingPainting;
    private double artInspectTimer;
    private String speechBubbleText;
    private Color speechBubbleColor = Color.WHITE;
    private double speechBubbleTimer;

    public Guard(String name, double x, double y, int speed, Point... route) {
        this(name, GuardType.REGULAR, x, y, speed, route);
    }

    public Guard(String name, GuardType guardType, double x, double y, int speed, Point... route) {
        this.name = name;
        this.guardType = guardType != null ? guardType : GuardType.REGULAR;
        this.x = x;
        this.y = y;
        this.patrolSpeed = speed;
        this.patrolRoute = new ArrayList<>();
        for (Point point : route) patrolRoute.add(new Point(point));
        if (!patrolRoute.isEmpty()) {
            goalX = patrolRoute.get(0).x;
            goalY = patrolRoute.get(0).y;
        }
    }

    public void update(double dt, MuseumMap map, double playerX, double playerY, boolean hidden,
                       boolean sprinting, boolean sneaking, boolean hasNoise, double noiseX, double noiseY,
                       double noiseRadius) {
        double previousX = x;
        double previousY = y;
        double playerCenterX = playerX + 10;
        double playerCenterY = playerY + 12;
        double distance = Point.distance(centerX(), centerY(), playerCenterX, playerCenterY);

        double baseVisionRadius = guardType == GuardType.ELITE ? 240 : 195;
        double visionMaxDist = state == State.CHASE ? (baseVisionRadius + 35) : (sprinting ? baseVisionRadius * 1.28 : baseVisionRadius);

        playerVisible = !hidden && distance <= visionMaxDist
            && (distance < 36 || inVisionCone(playerCenterX, playerCenterY))
            && map.hasLineOfSight(centerX(), centerY(), playerCenterX, playerCenterY);

        if (speechBubbleTimer > 0) {
            speechBubbleTimer = Math.max(0, speechBubbleTimer - dt);
        }

        // --- Suspicion & Detection Stage Evaluation ---
        DetectionStage prevStage = detectionStage;

        if (playerVisible) {
            if (state == State.INSPECTING_ART) {
                inspectingPainting = null;
            }

            // Suspicion buildup rate based on distance and player stealth: fills rapidly when in direct sight
            double rate = distance < 45 ? 145.0 : distance < 120 ? 92.0 : 62.0;
            if (guardType == GuardType.ELITE) rate *= 1.35;
            if (sprinting) rate *= 2.0;
            else if (sneaking) rate *= 0.60;

            suspicion = Math.min(100.0, suspicion + rate * dt);
            lostSightTimer = 0;
            goalX = playerX;
            goalY = playerY;

            // Turn toward the player immediately when spotted
            double dx = playerCenterX - centerX();
            double dy = playerCenterY - centerY();
            double dist = Math.hypot(dx, dy);
            if (dist > 0.001) {
                facingX = dx / dist;
                facingY = dy / dist;
            }

            if (suspicion >= 100.0) {
                detectionStage = DetectionStage.CHASE;
                if (state != State.CHASE) {
                    state = State.CHASE;
                    stateTimer = 0;
                    searchTimer = 0;
                    setSpeechBubble("HALT! INTRUDER!", new Color(255, 60, 60), 2.2);
                }
            } else if (suspicion >= 92.0) {
                detectionStage = DetectionStage.ALERT;
                if (state != State.CHASE && state != State.ALERT) {
                    state = State.ALERT;
                    stateTimer = 0;
                    setSpeechBubble("INTRUDER SPOTTED!", new Color(255, 90, 50), 1.6);
                }
            } else if (suspicion >= 60.0) {
                detectionStage = DetectionStage.INVESTIGATING;
                if (state != State.CHASE && state != State.ALERT && state != State.INSPECT) {
                    state = State.INSPECT;
                    stateTimer = 0;
                    setSpeechBubble("Hey! Who's that?!", new Color(255, 160, 40), 1.8);
                }
            } else if (suspicion >= 25.0) {
                detectionStage = DetectionStage.SUSPICIOUS;
                if (state == State.PATROL) {
                    setSpeechBubble("...?", new Color(255, 215, 80), 1.2);
                }
            }
        } else {
            // Player is not directly visible
            if (state == State.CHASE) {
                lostSightTimer += dt;
                if (lostSightTimer >= 1.8) {
                    state = State.SEARCH;
                    searchAnchorX = goalX;
                    searchAnchorY = goalY;
                    searchTimer = 0;
                    searchOffset = 0;
                    stateTimer = 0;
                    setSpeechBubble("Where did they go?!", new Color(245, 175, 50), 2.0);
                }
                double decayRate = 12.0;
                suspicion = Math.max(0.0, suspicion - dt * decayRate);
            } else if (state == State.INSPECT) {
                stateTimer += dt;
                // While investigating a sound/disturbance out of sight, the bar fills slowly
                suspicion = Math.min(62.0, suspicion + dt * 16.0);

                // If guard reaches suspicious spot and player is gone, investigate briefly then return
                if (hasReachedGoal(goalX, goalY) || stateTimer >= 4.5) {
                    state = State.RETURN;
                    stateTimer = 0;
                    setSpeechBubble("Nothing here... false alarm.", new Color(180, 210, 180), 2.0);
                }
            } else {
                // Suspicion decays smoothly when returning or back on patrol
                double decayRate = state == State.SEARCH ? 10.0 : 20.0;
                suspicion = Math.max(0.0, suspicion - dt * decayRate);
            }

            if (suspicion >= 100.0) detectionStage = DetectionStage.CHASE;
            else if (suspicion >= 92.0) detectionStage = DetectionStage.ALERT;
            else if (suspicion >= 60.0) detectionStage = DetectionStage.INVESTIGATING;
            else if (suspicion >= 25.0) detectionStage = DetectionStage.SUSPICIOUS;
            else detectionStage = DetectionStage.UNAWARE;
        }

        // --- Noise reaction ---
        if (!playerVisible && hasNoise && state != State.CHASE && state != State.SEARCH
            && state != State.INSPECTING_ART
            && Point.distance(centerX(), centerY(), noiseX, noiseY) <= noiseRadius) {
            goalX = noiseX - WIDTH / 2.0;
            goalY = noiseY - HEIGHT / 2.0;
            // Slowly enter Suspicious stage with yellow "?" instead of instantly jumping high
            if (suspicion < 26.0) {
                suspicion = 26.0;
            } else {
                suspicion = Math.min(58.0, suspicion + 8.0);
            }
            detectionStage = DetectionStage.SUSPICIOUS;
            state = State.INSPECT;
            stateTimer = 0;
            setSpeechBubble("...?", new Color(255, 215, 80), 1.8);
        }

        // --- State Machine Updates ---
        switch (state) {
            case PATROL:
                if (detectionStage == DetectionStage.SUSPICIOUS && playerVisible) {
                    // Pause briefly to inspect the disturbance
                    moving = false;
                } else {
                    updatePatrol(dt, map);
                }
                break;
            case INSPECTING_ART:
                moving = false;
                artInspectTimer += dt;
                if (inspectingPainting != null) {
                    double targetCenterX = inspectingPainting.getX() + MuseumMap.Painting.WIDTH / 2.0;
                    double targetCenterY = inspectingPainting.getY() + MuseumMap.Painting.HEIGHT / 2.0;
                    double dx = targetCenterX - centerX();
                    double dy = targetCenterY - centerY();
                    double dist = Math.hypot(dx, dy);
                    if (dist > 0.001) {
                        facingX = dx / dist;
                        facingY = dy / dist;
                    }
                }
                break;
            case ALERT:
                moving = false;
                stateTimer += dt;
                if (stateTimer >= 0.6) {
                    state = suspicion >= 95.0 ? State.CHASE : State.INSPECT;
                    stateTimer = 0;
                }
                break;
            case INSPECT:
                moveTo(dt, map, goalX, goalY, patrolSpeed + 6);
                stateTimer += dt;
                if (stateTimer >= 5.0 || hasReachedGoal(goalX, goalY)) {
                    state = State.RETURN;
                    stateTimer = 0;
                }
                break;
            case CHASE:
                double chaseSpeed = guardType == GuardType.ELITE ? 152.0 : 138.0;
                moveTo(dt, map, goalX, goalY, chaseSpeed);
                break;
            case SEARCH:
                searchTimer += dt;
                if (searchTimer < 2.5) {
                    moveTo(dt, map, goalX, goalY, patrolSpeed + 10);
                } else if (searchTimer < 5.5) {
                    if (searchTimer - searchOffset > 1.0) {
                        searchOffset = searchTimer;
                        double angle = searchTimer * 2.25;
                        setGoal(searchAnchorX + Math.cos(angle) * 55, searchAnchorY + Math.sin(angle) * 42);
                    }
                    moveTo(dt, map, goalX, goalY, patrolSpeed);
                } else {
                    state = State.RETURN;
                    stateTimer = 0;
                    setSpeechBubble("Lost them. Back to patrol.", new Color(180, 210, 180), 2.0);
                }
                break;
            case RETURN:
                if (patrolRoute.isEmpty()) {
                    state = State.PATROL;
                    stateTimer = 0;
                    break;
                }
                Point waypoint = patrolRoute.get(patrolIndex);
                moveTo(dt, map, waypoint.x, waypoint.y, patrolSpeed + 4);
                if (hasReachedGoal(waypoint.x, waypoint.y)) {
                    state = State.PATROL;
                    stateTimer = 0;
                }
                break;
        }
        moving = Math.abs(x - previousX) > 0.01 || Math.abs(y - previousY) > 0.01;
    }

    public void update(double dt, MuseumMap map, double playerX, double playerY, boolean hidden,
                       boolean sprinting, boolean hasNoise, double noiseX, double noiseY,
                       double noiseRadius) {
        update(dt, map, playerX, playerY, hidden, sprinting, false, hasNoise, noiseX, noiseY, noiseRadius);
    }

    private boolean inVisionCone(double px, double py) {
        double dx = px - centerX();
        double dy = py - centerY();
        double magnitude = Math.hypot(dx, dy);
        if (magnitude < 0.001) return true;
        double dot = (dx * facingX + dy * facingY) / magnitude;
        return dot >= Math.cos(Math.toRadians(58));
    }

    private void updatePatrol(double dt, MuseumMap map) {
        if (patrolRoute.isEmpty()) return;
        Point waypoint = patrolRoute.get(patrolIndex);
        moveTo(dt, map, waypoint.x, waypoint.y, patrolSpeed);
        if (hasReachedGoal(waypoint.x, waypoint.y)) {
            patrolIndex = (patrolIndex + 1) % patrolRoute.size();
            setGoal(patrolRoute.get(patrolIndex).x, patrolRoute.get(patrolIndex).y);
        }
    }

    private void moveTo(double dt, MuseumMap map, double targetX, double targetY, double speed) {
        replanTimer -= dt;
        if (path.isEmpty() || replanTimer <= 0) {
            path = map.findPath(x, y, targetX, targetY, WIDTH, HEIGHT);
            replanTimer = 0.75;
            pathCalculated = true;
            pathReachable = !path.isEmpty();
            if (pathReachable) {
                Point endpoint = path.get(path.size() - 1);
                pathEndX = endpoint.x;
                pathEndY = endpoint.y;
            }
            if (!path.isEmpty() && Point.distance(x, y, path.get(0).x, path.get(0).y) < 8) path.remove(0);
        }
        while (!path.isEmpty() && Point.distance(x, y, path.get(0).x, path.get(0).y) < 5) path.remove(0);
        if (path.isEmpty()) return;
        Point point = path.get(0);
        double dx = point.x - x;
        double dy = point.y - y;
        double distance = Math.hypot(dx, dy);
        if (distance < 0.01) return;
        double step = Math.min(distance, speed * dt);
        double vx = dx / distance * step;
        double vy = dy / distance * step;
        double previousX = x;
        double previousY = y;
        moveAxis(vx, 0, map);
        moveAxis(0, vy, map);
        double movedX = x - previousX;
        double movedY = y - previousY;
        double movedDistance = Math.hypot(movedX, movedY);
        if (movedDistance > 0.001) {
            facingX = movedX / movedDistance;
            facingY = movedY / movedDistance;
        }
    }

    private void moveAxis(double dx, double dy, MuseumMap map) {
        if (dx == 0 && dy == 0) return;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy)) / 3.0));
        double sx = dx / steps;
        double sy = dy / steps;
        for (int i = 0; i < steps; i++) {
            double nextX = x + sx;
            double nextY = y + sy;
            if (map.canStandAt(nextX, nextY, WIDTH, HEIGHT)) {
                x = nextX;
                y = nextY;
            } else {
                path.clear();
                return;
            }
        }
    }

    private boolean atGoal(double targetX, double targetY) {
        return Point.distance(x, y, targetX, targetY) <= 14;
    }

    private boolean hasReachedGoal(double targetX, double targetY) {
        return atGoal(targetX, targetY)
            || (pathCalculated && pathReachable && path.isEmpty()
                && Point.distance(x, y, pathEndX, pathEndY) <= 8);
    }

    private void setGoal(double x, double y) {
        goalX = x;
        goalY = y;
        path.clear();
        pathCalculated = false;
        pathReachable = false;
        replanTimer = 0;
    }

    public void setSearchTarget(double x, double y) {
        searchAnchorX = x;
        searchAnchorY = y;
        setGoal(x, y);
        searchTimer = 0;
        searchOffset = 0;
        if (state != State.CHASE) {
            state = State.SEARCH;
            stateTimer = 0;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double centerX() { return x + WIDTH / 2.0; }
    public double centerY() { return y + HEIGHT / 2.0; }
    public State getState() { return state; }
    public String getName() { return name; }
    public boolean isPlayerVisible() { return playerVisible; }
    public boolean isMoving() { return moving; }
    public Point getFacing() { return new Point((int) Math.round(facingX * 100), (int) Math.round(facingY * 100)); }

    public void startInspectingArt(MuseumMap.Painting painting) {
        this.inspectingPainting = painting;
        this.state = State.INSPECTING_ART;
        this.artInspectTimer = 0;
        this.moving = false;
        this.path.clear();
        double targetCenterX = painting.getX() + MuseumMap.Painting.WIDTH / 2.0;
        double targetCenterY = painting.getY() + MuseumMap.Painting.HEIGHT / 2.0;
        double dx = targetCenterX - centerX();
        double dy = targetCenterY - centerY();
        double dist = Math.hypot(dx, dy);
        if (dist > 0.001) {
            facingX = dx / dist;
            facingY = dy / dist;
        }
    }

    public boolean isInspectingArt() {
        return state == State.INSPECTING_ART;
    }

    public double getInspectProgress() {
        return Math.min(1.0, artInspectTimer / 2.0);
    }

    public double getArtInspectTimer() {
        return artInspectTimer;
    }

    public MuseumMap.Painting getInspectingPainting() {
        return inspectingPainting;
    }

    public void finishArtInspection(boolean continuePatrol) {
        inspectingPainting = null;
        artInspectTimer = 0;
        if (continuePatrol) {
            state = State.RETURN;
            stateTimer = 0;
        }
    }

    public void setSpeechBubble(String text, Color color, double duration) {
        this.speechBubbleText = text;
        this.speechBubbleColor = color;
        this.speechBubbleTimer = duration;
    }

    public String getSpeechBubbleText() {
        return speechBubbleTimer > 0 ? speechBubbleText : null;
    }

    public Color getSpeechBubbleColor() {
        return speechBubbleColor;
    }

    public double getSpeechBubbleTimer() {
        return speechBubbleTimer;
    }

    public double getSuspicion() {
        return suspicion;
    }

    public DetectionStage getDetectionStage() {
        return detectionStage;
    }

    public GuardType getGuardType() {
        return guardType;
    }

    public double getVisionRadius() {
        double base = guardType == GuardType.ELITE ? 240.0 : 195.0;
        return state == State.CHASE ? (base + 35.0) : base;
    }

    public double getVisionAngleDegrees() {
        return guardType == GuardType.ELITE ? 64.0 : 58.0;
    }

    public double getFacingAngle() {
        return Math.atan2(facingY, facingX);
    }
}
