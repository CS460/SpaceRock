package sensor.Universe;

import debrisProcessingSubsystem.Scheduler;
import debrisProcessingSubsystem.cameraComponent.Camera;
import debrisProcessingSubsystem.debrisCollection.DebrisCollection;
import debrisProcessingSubsystem.operatorComponent.OperatorTesting;
import javafx.animation.AnimationTimer;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import operator.commands.Asteroid;
import operator.commands.AsteroidData;
import operator.commands.IncomingListener;
import operator.network.Connection;
import operator.network.DummySat;
import operator.processing.DebrisProcessor;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UniverseThread extends Thread implements IncomingListener
{
  private static final int DEFAULT_SECTOR_WIDTH = 100;
  private static final int DEFAULT_SECTOR_HEIGHT = 100;
  private final DebrisProcessor processor = new DebrisProcessor();
  private final Connection netLink = new Connection();
  private final DummySat satellite = new DummySat();
  private Group rockGroup = new Group();
  private Asteroid[] lastFrame = null;
  private boolean newData = false;
  
  private boolean safeToGet = true; //thread safety measure
  
  @Override
  public void run()
  {
    try
    {
      boolean incr = true;

      //this starts the constant polling of the scheduler over the debriscollection, operator, and camera
  
      int i = 0;
      satellite.start();
      netLink.addIncomingListener(this);
      netLink.connectToDummySat();
      netLink.sendCameraSpec(0, DEFAULT_SECTOR_HEIGHT, DEFAULT_SECTOR_WIDTH, false, false);//starting the camera off and in automatic mode
      while (true)
      {
        if (incr) i++;
        if (i == 1000000)
        {
          System.out.println("POP");
          netLink.sendCameraSpec(0, DEFAULT_SECTOR_HEIGHT, DEFAULT_SECTOR_WIDTH, true, false);
          incr = false;
          i++;
        }
        if (lastFrame != null)
        {
        }
        if (newData)
        {
          safeToGet = false;
          ObservableList<Node> children = rockGroup.getChildren();
          children.clear();
          children.addAll(getAsteroidNodes());
          for (Asteroid child : lastFrame)
          {
            System.out.println("In UniverseThread: " + child);
          }
          newData = false;
          safeToGet = true;
        }
      }
    }
    catch (Exception e)
    {
      System.out.println("Exception in UniverseThread.java in run() method");
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
  private Sphere makeAsteroidSphere(Asteroid a)
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
        /* TODO: Something meaningful here */
    System.out.println("Got new image!");
  }
}
