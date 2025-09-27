import dev.robocode.tankroyale.botapi.*;
import dev.robocode.tankroyale.botapi.events.*;
import dev.robocode.tankroyale.botapi.graphics.Color;

public class QuantumCodersBot1 extends Bot {

    boolean peek; // Don't turn if there's a bot there
    double moveAmount; // How much to move
    int trigger; // Keeps track of when to move

    // Enhanced targeting variables for longer range and stronger damage
    double lastEnemyX = 0;
    double lastEnemyY = 0;
    double lastEnemyDirection = 0;
    double lastEnemySpeed = 0;
    long lastScanTime = 0;
    double radarDirection = 1; // Radar sweep direction

    // The main method starts our bot
    public static void main(String[] args) {
        new QuantumCodersBot1().start();
    }

    // Constructor, which loads the bot config file
    QuantumCodersBot1() {
        super(BotInfo.fromFile("QuantumCodersBot1.json"));
    }

    // Called when a new round is started -> initialize and do some movement
    @Override
    public void run() {
        // Set colors - Enhanced visibility for long-range combat
        setBodyColor(Color.fromRgb(0x00, 0xFF, 0xFF)); // Quantum cyan
        setTurretColor(Color.fromRgb(0xFF, 0x00, 0xFF)); // Quantum magenta
        setRadarColor(Color.fromRgb(0xFF, 0xFF, 0x00)); // Quantum yellow
        setBulletColor(Color.fromRgb(0xFF, 0x69, 0xB4)); // Hot pink for visibility
        setScanColor(Color.fromRgb(0x00, 0xFF, 0x00)); // Bright green

        trigger = 60; // More aggressive energy management for sustained firepower

        // Enhanced radar/gun control for better long-range targeting
        setAdjustRadarForGunTurn(true);
        setAdjustGunForBodyTurn(true);

        // Add a custom event named "trigger-hit"
        addCustomEvent(new Condition("trigger-hit") {
            public boolean test() {
                return getEnergy() <= trigger;
            }
        });

        // Initialize moveAmount to the maximum possible for the arena
        moveAmount = Math.max(getArenaWidth(), getArenaHeight());
        // Initialize peek to false
        peek = false;

        // turn to face a wall.
        // getDirection() % 90 means the remainder of getDirection() divided by 90.
        turnRight(getDirection() % 90);
        forward(moveAmount);

        // Turn the gun to turn right 90 degrees.
        peek = true;
        turnGunLeft(90);
        turnLeft(90);

        // ENHANCED WALL-HUGGING MAIN LOOP - STAY ON WALLS FOR DEFENSE
        while (isRunning()) {
            // Enhanced gun control for faster target acquisition
            setGunTurnRate(20); // Optimal gun turn rate for accuracy

            // CHECK AND MAINTAIN WALL POSITION
            returnToWalls(); // Ensure we stay on walls

            // STRICT WALL MOVEMENT - Always stay on perimeter
            peek = true;
            // Move along the wall - this keeps us on the perimeter
            forward(moveAmount);
            peek = false;

            // Turn to the next wall - perfect 90-degree turns to stay on perimeter
            turnLeft(90);

            // ENHANCED RADAR SWEEP while staying on walls
            setTurnRadarRight(360 * radarDirection);

            // Occasionally change radar direction for comprehensive coverage
            if (Math.random() < 0.1) { // 10% chance
                radarDirection *= -1;
            }
        }
    }

    // We hit another bot -> move away a bit
    @Override
    public void onHitBot(HitBotEvent e) {
        // If he's in front of us, set back up a bit.
        var bearing = bearingTo(e.getX(), e.getY());
        if (bearing > -90 && bearing < 90) {
            back(100);
        } else { // else he's in back of us, so set ahead a bit.
            forward(100);
        }
    }

    // We scanned another bot -> ENHANCED LONG-RANGE TARGETING AND DAMAGE!
    @Override
    public void onScannedBot(ScannedBotEvent e) {
        // Store enemy data for predictive tracking (done first for aimAtEnemy method)
        storeEnemyData(e);

        // Calculate distance once for efficiency
        double enemyDistance = distanceTo(e.getX(), e.getY());

        // Enhanced predictive targeting
        aimAtEnemy(e, enemyDistance);

        // Fire with calculated power
        fireAtEnemy(enemyDistance);

        // Continue scanning for sustained tracking
        if (peek) {
            rescan();
        }
    }
    // A custom event occurred
    @Override
    public void onCustomEvent(CustomEvent e) {
        // Check if our custom event "trigger-hit" went off
        if (e.getCondition().getName().equals("trigger-hit")) {
            // Adjust the trigger value, or else the event will fire again and again and again...
            trigger -= 15; // More gradual adjustment for sustained combat

            // Print out energy level
            System.out.println("QuantumCoders: Energy at " + (int) (getEnergy() + .5) + " - Adjusting combat strategy!");

            // ENHANCED ENERGY CONSERVATION - STAY ON WALLS FOR DEFENSE
            if (getEnergy() < 30) {
                // Critical energy - maintain wall position for maximum defense
                System.out.println("QuantumCoders: Critical energy! Maintaining wall defense!");
                performWallEvasion(100); // Use consolidated wall movement
            } else {
                // Moderate energy - slight wall adjustment only
                System.out.println("QuantumCoders: Energy management - staying on walls!");
                forward(50); // Small movement along wall
            }
        }
    }

    // Store enemy data for predictive tracking - REMOVES DUPLICATION
    private void storeEnemyData(ScannedBotEvent e) {
        lastEnemyX = e.getX();
        lastEnemyY = e.getY();
        lastEnemyDirection = e.getDirection();
        lastEnemySpeed = e.getSpeed();
        lastScanTime = getTurnNumber();
    }

    // Fire at enemy with optimal power - REMOVES DUPLICATION
    private void fireAtEnemy(double enemyDistance) {
        double firePower = calculateFirePower(enemyDistance);
        if (getEnergy() > firePower) {
            fire(firePower);
            System.out.println("QuantumCoders: Firing " + firePower + " power at range " + (int)enemyDistance);
        }
    }

    // Wall-based evasive movement - REMOVES DUPLICATION
    private void performWallEvasion(int baseDistance) {
        if (Math.random() < 0.5) {
            forward(baseDistance + (Math.random() * 40)); // Move forward along wall
        } else {
            back(baseDistance + (Math.random() * 40)); // Move backward along wall
        }
    }

    // ENHANCED LONG-RANGE TARGETING METHOD - OPTIMIZED
    private void aimAtEnemy(ScannedBotEvent e, double enemyDistance) {
        double enemyBearing = bearingTo(e.getX(), e.getY());

        // ADVANCED PREDICTIVE TARGETING for long-range accuracy
        long timeDiff = getTurnNumber() - lastScanTime;
        if (timeDiff > 0 && lastEnemySpeed > 0 && enemyDistance > 200) {
            // Long-range prediction - more sophisticated for distant targets
            double timeToTarget = enemyDistance / 20; // Bullet speed approximation
            double predictionFactor = Math.min(2.0, enemyDistance / 300); // Stronger prediction for longer range

            double predictedX = e.getX() + Math.sin(Math.toRadians(e.getDirection())) * e.getSpeed() * timeToTarget * predictionFactor;
            double predictedY = e.getY() + Math.cos(Math.toRadians(e.getDirection())) * e.getSpeed() * timeToTarget * predictionFactor;

            // Aim at predicted position
            double targetBearing = Math.toDegrees(Math.atan2(predictedX - getX(), predictedY - getY()));
            double gunTurn = normalizeRelativeAngle(targetBearing - getGunDirection());
            turnGunRight(gunTurn);
        } else {
            // Direct aim for close range or no prediction data
            double absoluteBearing = getDirection() + enemyBearing;
            double gunTurn = normalizeRelativeAngle(absoluteBearing - getGunDirection());
            turnGunRight(gunTurn);
        }
    }

    // ENHANCED FIRE POWER CALCULATION - STRONGER DAMAGE AT ALL RANGES
    private double calculateFirePower(double distance) {
        // MAXIMUM DAMAGE STRATEGY - Much stronger than before
        if (getEnergy() > 50) {
            // High energy - use maximum power for devastating damage
            if (distance < 200) {
                return 3.0; // Maximum devastation at close range
            } else if (distance < 400) {
                return 2.8; // Very high power for medium range
            } else if (distance < 600) {
                return 2.5; // High power for long range
            } else if (distance < 800) {
                return 2.2; // Strong power for very long range
            } else {
                return 2.0; // Still strong at extreme range
            }
        } else if (getEnergy() > 20) {
            // Medium energy - still aggressive
            if (distance < 300) {
                return 2.5; // High power
            } else if (distance < 600) {
                return 2.0; // Medium-high power
            } else {
                return 1.8; // Medium power for long range
            }
        } else {
            // Low energy - conservative but still effective
            if (distance < 200) {
                return 2.0; // Still pack a punch at close range
            } else if (distance < 500) {
                return 1.5; // Moderate power
            } else {
                return 1.2; // Conservative for very long range
            }
        }
    }

    // Enhanced hit by bullet response - MAINTAIN WALL POSITION
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        System.out.println("QuantumCoders: Taking fire! Evasive wall maneuvers!");

        // WALL-BASED EVASIVE ACTION - Don't leave the walls!
        if (getEnergy() > 15) {
            performWallEvasion(80); // Use consolidated wall evasion method
        }

        // Only make small adjustments to stay on wall perimeter
        if (Math.random() < 0.3) { // 30% chance for minor adjustment
            if (Math.random() < 0.5) {
                turnRight(15); // Small turn to stay on wall
            } else {
                turnLeft(15); // Small turn to stay on wall
            }
        }
    }

    // Enhanced wall collision handling - STAY ON WALLS!
    @Override
    public void onHitWall(HitWallEvent e) {
        System.out.println("QuantumCoders: Wall contact - adjusting position to stay on perimeter!");

        // When we hit a wall, we want to continue along it, not move away
        // Turn to continue moving along the wall perimeter
        turnLeft(90); // Turn to follow the wall

        // Small adjustment to maintain wall contact
        back(20); // Back up slightly
        forward(10); // Then move forward along the wall
    }

    // Method to ensure we return to walls if somehow displaced
    private void returnToWalls() {
        double distanceToNearestWall = Math.min(
                Math.min(getX(), getArenaWidth() - getX()),
                Math.min(getY(), getArenaHeight() - getY())
        );

        // If we're too far from walls (more than 50 units), return to them
        if (distanceToNearestWall > 50) {
            System.out.println("QuantumCoders: Returning to wall defense position!");

            // Find the nearest wall and head towards it
            if (getX() < getArenaWidth() / 2) {
                // Go to left wall
                turnTowards(0, getY());
            } else {
                // Go to right wall
                turnTowards(getArenaWidth(), getY());
            }

            forward(distanceToNearestWall);
        }
    }

    // Helper method to turn towards a specific point
    private void turnTowards(double x, double y) {
        double angle = Math.toDegrees(Math.atan2(x - getX(), y - getY()));
        double turn = normalizeRelativeAngle(angle - getDirection());
        turnRight(turn);
    }
}