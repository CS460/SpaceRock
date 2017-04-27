package operator.commands;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.Random;

/**
 * Created by magik on 2/5/2017. Modified by David R., 2/12/2017
 * <p>
 * The Asteroid class describes an immutable data object that _should_
 * be roughly isomorphic to what will be sent by the SpaceRock satellite code.
 * It implements {@code AsteroidData} which should help interface between
 * disparate code bases.
 */
public class Asteroid implements AsteroidData {

    private static final String[] asteroid_images = {"asteroid_1.png", "asteroid_2.png", "asteroid_3.png",
        "asteroid_4.png", "asteroid_5.png"};

    public final Point2D location;
    private BufferedImage image;
    public final double size;
    public long id;
    public Instant timestamp;


    public Asteroid(Point2D location, long id, double size, Instant timestamp) {
        this.location = location;
        this.id = id;
        this.size = size;
        this.timestamp = timestamp;
        setRandomImage();
    }


    @Override
    public String toString() {
        return String.format("Asteroid ID=%d at (%.3f, %.3f)",
                             id, location.getX(), location.getY());
    }


    @Override
    public Point2D getLoc() {
        return location;
    }


    @Override
    public double getSize() {
        return size;
    }


    @Override
    public long getID() {
        return id;
    }

    private void setRandomImage() {
        Random rand = new Random();
        String asteroid_image = asteroid_images[rand.nextInt(asteroid_images.length)];
        //System.out.format("setting imageView to: %s\n", asteroid_image);
        try {

            this.image = ImageIO.read(sensor.Asteroid.class.getResource("asteroids/" + asteroid_image));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public BufferedImage getImage()
    {
        return image;
    }
}
