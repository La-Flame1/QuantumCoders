import dev.robocode.tankroyale.botapi.*;
import dev.robocode.tankroyale.botapi.events.*;
import dev.robocode.tankroyale.botapi.graphics.Color;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class QuantumCodersBot1 extends Bot {

    boolean peek; // Don't turn if there's a bot there
    double moveAmount; // How much to move
    int trigger; // Keeps track of when to move

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
        // Set colors
        setBodyColor(Color.BLACK);
        setTurretColor(Color.BLACK);
        setRadarColor(Color.DEEP_PINK);
        setBulletColor(Color.YELLOW);
        setScanColor(Color.CYAN);

        trigger = 80;

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

        // Main loop
        while (isRunning()) {
            // Peek before we turn when forward() completes.
            peek = true;
            // Move up the wall
            forward(moveAmount);
            // Don't peek now
            peek = false;
            // Turn to the next wall
            turnLeft(90);
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

    // We scanned another bot -> fire!
    @Override
    public void onScannedBot(ScannedBotEvent e) {
        fire(2);
        // Note that scan is called automatically when the bot is turning.
        // By calling it manually here, we make sure we generate another scan event if there's a bot
        // on the next wall, so that we do not start moving up it until it's gone.
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
            trigger -= 20;

            // Print out energy level
            System.out.println("Ouch, down to " + (int) (getEnergy() + .5) + " energy.");

            // Move around a bit
            turnRight(65);
            forward(100);
        }
    }
    
}    