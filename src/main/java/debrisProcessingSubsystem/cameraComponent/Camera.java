package debrisProcessingSubsystem.cameraComponent;

import debrisProcessingSubsystem.debrisCollection.DebrisRecord;
import debrisProcessingSubsystem.schedulerTester.TestableComponent;
import debrisProcessingSubsystem.updateSystem.*;
import fpga.memory.EmptyRegisterException;
import fpga.memory.MemoryMap;
import fpga.memory.NoSuchRegisterFoundException;
import fpga.memory.UnavailbleRegisterException;
import fpga.objectdetection.Debris;
import javafx.scene.Node;
import sensor.Asteroid;
import sensor.Universe.AltUniverseThread;
import sensor.ZoomLevel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Team 01 will implement the Operator
 * This will be the interface implemented by the Camera object shown
 * in the SADD.
 * This is a preliminary placeholder and very subject to change.
 * Created by dsr on 3/4/17.
 */

public class Camera implements Updatable, TestableComponent {

  LinkedList<Update> outgoing_updates;
  private boolean DEBUG = true;
  private CameraStatusReport cameraStatusModel;
  private MemoryMapAccessor memoryMap;
  //private UniverseThread universe;
  private AltUniverseThread universe;
  private BufferedImage currentImage = null;
  private Asteroid[] currentFrameAsteroids;
  private int sectionSize = 100;
  private int overlap = 32;

  public Camera() {
    universe = new AltUniverseThread(10);
    universe.setDaemon(true);
    universe.start();
    cameraStatusModel = new CameraStatusReport();
    //TODO plug in fake memory map.
    memoryMap = new TestingFPGAComs();
    this.outgoing_updates = new LinkedList<>();
  }

  /**
   * Turn on the camera.
   */
  private void on() {
    if(DEBUG){
      System.out.println("Turn on camera.");
    }
    if(memoryMap.on()){
      cameraStatusModel.setIsOn(true);
    }
    OperatorUpdate outgoingUpdate = new OperatorUpdate(UpdateType.OPERATOR);
    outgoingUpdate.setCameraStatus(new CameraStatusReport(cameraStatusModel));
    outgoing_updates.add(outgoingUpdate);
  }

  /**
   * Turn the camera off.
   */
  private void off() {
    if(DEBUG){
      System.out.println("Turn camera off.");
    }
    if(memoryMap.on()){
      cameraStatusModel.setIsOn(false);
    }
    OperatorUpdate outgoingUpdate = new OperatorUpdate(UpdateType.OPERATOR);
    outgoingUpdate.setCameraStatus(new CameraStatusReport(cameraStatusModel));
    outgoing_updates.add(outgoingUpdate);
  }

  /**
   * Reset the camera.
   * TODO this needs to power off, power on, and reset zoom level.
   */
  private void reset() {
    if(DEBUG){
      System.out.println("Reset Camera.");
    }
    OperatorUpdate outgoingUpdate = new OperatorUpdate(UpdateType.OPERATOR);
    cameraStatusModel = new CameraStatusReport();
    outgoingUpdate.setCameraStatus(cameraStatusModel);
    outgoing_updates.add(outgoingUpdate);
  }

  /**
   * Tell camera to take a picture.
   */
  private void takePicture()
  {
    //@author Denver

    //right here is where the camera should save a BufferedImage of what it sees of the universe
    //it should also tell the debris collection system all the information about the debris within it's view
    //(that means generating an update to be sent to debris collection)

    //this also means determining the raw image that we should be sending to the debris collection for each asteroid
    //shouldn't be too hard - just calculate the "box" it should be in based on section sizes, and grab that box
    //from the camera's overall saved image view
    currentFrameAsteroids = universe.getAsteroids();

    currentImage = new BufferedImage(4000, 4000, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = currentImage.createGraphics();
    g.setColor(java.awt.Color.black);
    g.fillRect(0, 0, currentImage.getWidth(), currentImage.getHeight());

    for(Asteroid ast : currentFrameAsteroids)
    {
      if(ast.current_location[0] - ast.current_radius > 0 && ast.current_location[1] - ast.current_radius > 0)
      g.drawImage(ast.getRefImage(), ast.current_location[0] - ast.current_radius, ast.current_location[1] - ast.current_radius, ast.current_radius, ast.current_radius, null);
    }

    for(Asteroid ast : currentFrameAsteroids)
    {
      if(ast.current_location[0]- ast.current_radius > 0 && ast.current_location[1] - ast.current_radius > 0)
      ast.setImage(this.getSubregion(ast));
    }


    memoryMap.takePicture();
  }

  private BufferedImage getSubregion(Asteroid ast)
  {
    int minX = ast.current_location[0] - ast.current_radius, minY = ast.current_location[1] - ast.current_radius;

    return currentImage.getSubimage(minX - (minX % sectionSize) , minY - (minY % sectionSize), sectionSize, sectionSize);
  }

  /* Raw frame will be returned with every debris object.
  // Sean Hanely
  private void getRawFrame() {
    outgoing_updates.add(new OperatorUpdate(UpdateType.OPERATOR));
  }
  */

  // Corey
  private void setZoomLevel(ZoomLevel zoomLevel) {
    if(DEBUG){
      System.out.println("Setting zoom level to: " + zoomLevel);
    }
    if(memoryMap.setZoomLevel(zoomLevel)){
      cameraStatusModel.setZoomLevel(zoomLevel);
    }
    for(Asteroid ast: currentFrameAsteroids)
    {
      ast.setRad(zoomLevel.value+1);
    }
    OperatorUpdate outgoingUpdate = new OperatorUpdate(UpdateType.OPERATOR);
    outgoingUpdate.setCameraStatus(cameraStatusModel);
  }


  // Divya
  /* Camera should call this internally when the image is finished.
  private void process_image() {
    try {
      Debris debris = MemoryMap.read(Debris.class, "debris");
      if (debris != null) {
        DebrisCollectorUpdate debris_update = new DebrisCollectorUpdate(UpdateType.DEBRIS_COLLECTOR);
        debris_update.setAddDebris(true);
        debris_update.setDebrisObject(debris);
        outgoing_updates.add(debris_update);
        MemoryMap.write("debris", null);
      } else {
        //return new CameraUpdate(UpdateType.DONE);
        // TODO request next frame
      }
    } catch (NoSuchRegisterFoundException e) {
      e.printStackTrace();
    } catch (EmptyRegisterException e) {
      e.printStackTrace();
    } catch (UnavailbleRegisterException e) {
      e.printStackTrace();
    }
  }*/


  public Update updateComponent(Update theUpdate) {
    CameraUpdate camera_update = (CameraUpdate)theUpdate;
    camera_update.getParamMap().forEach((param,value) -> {
      switch(param) {
        case TURN_ON_CAMERA:
          on();
          if (DEBUG) System.out.println("Received TURN_ON_CAMERA update.");
          break;
        case TURN_OFF_CAMERA:
          off();
          if (DEBUG) System.out.println("Received TURN_OFF_CAMERA update.");
          break;
        case RESET_CAMERA:
          reset();
          if (DEBUG) System.out.println("Received RESET_CAMERA update.");
          break;
        case TAKE_PICTURE:
          takePicture();
          if (DEBUG) System.out.println("Received TAKE_PICTURE update.");
          break;
        case SET_ZOOM:
          setZoomLevel((ZoomLevel)value);
          if (DEBUG) System.out.println("Received SET_ZOOM update.");
          break;
        case PROCESS_IMAGE:
          //process_image();
          if (DEBUG) System.out.println("Received PROCESS_IMAGE update.");
          break;
        case SECTION_SIZE:
          sectionSize = (int)value;
          if (DEBUG) System.out.println("Received SECTION_SIZE update.");
          break;
        case OVERLAP_SIZE:
          overlap = (int)value;
          if (DEBUG) System.out.println("Received OVERLAP_SIZE update.");
          break;
        default:
          throw new RuntimeException("I don't understand what you want me to do.");
      }
    });
    return null;
  }

  public Update pollComponent() {
    Update updateFromMemoryMap = memoryMap.checkMap();
    outgoing_updates.addLast(updateFromMemoryMap);
    if (outgoing_updates.isEmpty()) {
      return null;
    } else {
      return outgoing_updates.removeFirst();
    }
  }

  public void addUpdateForScheduler(Update update){
    outgoing_updates.addLast(update);
  }

  /**
   * Add a debris record to the memory map.
   * @param update the DebrisRecord to add
   */
  public void addDebrisRecord(DebrisRecord update){
    memoryMap.addDebrisToRegister(update);
  }
  public List<Node> getAsteroidNodes()
  {
    return universe.getAsteroidNodes();
  }

}
