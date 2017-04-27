package sensor.Universe;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import sensor.Asteroid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by DenverQ on 4/26/2017.
 */
public class AltUniverseThread extends Thread
{
  private Asteroid[] lastFrame = null;
  private long previousTime = 0;

  private volatile boolean safeToGet = true; //thread safety measure

  public AltUniverseThread(int numInitialAsteroids)
  {
    lastFrame = new Asteroid[numInitialAsteroids];

    for(int i = 0; i < numInitialAsteroids; i++)
    {
      lastFrame[i] = new Asteroid(new int[]{6,6,6}, 0);
    }
  }



  @Override
  public void run()
  {
    try
    {
      while (true)
      {
        long currentTime = System.currentTimeMillis();
        if(currentTime - previousTime > 500)
        {
          safeToGet = false;
          previousTime = currentTime;

          //children.addAll(getAsteroidNodes());
          for (Asteroid child : lastFrame)
          {
            child.move(1);
            System.out.println("In AltUniverseThread: " + child);
          }
          safeToGet = true;
        }
        else
        {
          Thread.sleep(10);
        }
      }
    }
    catch (Exception e)
    {
      System.out.println("Exception in AltUniverseThread.java in run() method");
    }
  }

  /* Fetches the Asteroids from the last frame only when available*/
  public Asteroid[] getAsteroids()
  {
    while (!safeToGet)
    {
      System.out.println("Waiting for lastFrame to be fully modified from another process.");
    }
    return lastFrame;
  }

  /**
   * Convert all Asteroids to renderable Spheres and return them in a List
   *
   * @return List of Asteroid Spheres
   */
/*
  private List<Node> getAsteroidNodes()
  {
    List<Node> nodeList = new ArrayList<>(lastFrame.length * 2);
    for (Asteroid a : lastFrame)
    {
      Sphere sphere = makeAsteroidSphere(a);
      nodeList.add(sphere);
    }
    return nodeList;
  }

  /**
   * Make a Sphere representing some Asteroid
   *
   * @param a Asteroid to use as a basis
   * @return a Sphere with the asteroid's ID drawn on it
   */

  /*private Sphere makeAsteroidSphere(Asteroid a)
  {
    Sphere s = new Sphere(a.size);
    PhongMaterial mat = new PhongMaterial(Color.BURLYWOOD);
    s.setTranslateX(a.getLoc().getX());
    s.setTranslateY(a.getLoc().getY());
    s.setMaterial(mat);
    s.setOnMouseClicked(mouseEvent ->
    {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("SpaceRockPopup.fxml"));
      Scene newScene;
      Parent root;
      try
      {
        root = loader.load();
        newScene = new Scene(root);
      }
      catch (IOException ex)
      {
        // TODO: handle error
        return;
      }
      SpaceRockFXMLController controller = loader.getController();

      controller.setData(a);
      Stage inputStage = new Stage();
      inputStage.setScene(newScene);
      inputStage.show();

    });
    return s;
  }


  @Override
  public void newAsteroidData(AsteroidData[] asteroids, long timestamp)
  {
    lastFrame = asteroidsFromData(asteroids);
    processor.addAndAssign(lastFrame);

    newData = true;
  }


  /* Convert an AsteroidData array to an array of Asteroids prepared for the DebrisProcessor */
  /*
  private Asteroid[] asteroidsFromData(AsteroidData[] data)
  {
    Asteroid[] asteroids = new Asteroid[data.length];
    for (int i = 0; i < data.length; i++)
    {
      AsteroidData d = data[i];
      asteroids[i] = new Asteroid(d.getLoc(), d.getID(), d.getSize(), Instant.now());
    }
    return asteroids;
  }


  @Override
  public void newImageData(java.awt.Image img, long id)
  {
    System.out.println("Got new image!");
  }
  */
  public List<Node> getAsteroidNodes()
  {
    List<Node> nodeList = new ArrayList<>(lastFrame.length * 2);
    for (Asteroid a : lastFrame)
    {
      Sphere sphere = makeAsteroidSphere(a);
      nodeList.add(sphere);
    }
    return nodeList;
  }
/*
  /**
   * Make a Sphere representing some Asteroid
   *
   * @param a Asteroid to use as a basis
   * @return a Sphere with the asteroid's ID drawn on it
   */

  private Sphere makeAsteroidSphere(Asteroid a)
  {
    Sphere s = new Sphere(a.current_radius);
    PhongMaterial mat = new PhongMaterial(Color.BURLYWOOD);
    s.setTranslateX(a.current_location[0]);
    s.setTranslateY(a.current_location[1]);
    s.setMaterial(mat);
    s.setOnMouseClicked(mouseEvent ->
    {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("SpaceRockPopup.fxml"));
      Scene newScene;
      Parent root;
      try
      {
        root = loader.load();
        newScene = new Scene(root);
      }
      catch (IOException ex)
      {
        // TODO: handle error
        return;
      }
      SpaceRockFXMLController controller = loader.getController();

      controller.setData(a);
      Stage inputStage = new Stage();
      inputStage.setScene(newScene);
      inputStage.show();

    });
    return s;
  }
}
