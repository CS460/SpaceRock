package operator.ui;


import debrisProcessingSubsystem.Scheduler;
import debrisProcessingSubsystem.cameraComponent.Camera;
import debrisProcessingSubsystem.debrisCollection.DebrisCollection;
import debrisProcessingSubsystem.operatorComponent.OperatorTesting;
import debrisProcessingSubsystem.updateSystem.CameraUpdate;
import debrisProcessingSubsystem.updateSystem.OperatorUpdate;
import debrisProcessingSubsystem.updateSystem.UpdateType;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import operator.commands.Asteroid;
import operator.commands.AsteroidData;
import operator.commands.IncomingListener;
import operator.network.Connection;
import operator.network.DummySat;
import operator.processing.DebrisProcessor;
import sensor.ZoomLevel;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sahba and Kathrina
 */
public class SpaceRockGUI extends Application implements IncomingListener
{
  private static final long NANOSECS_AUTO_MODE_POLLRATE = 1_000_000_000;
  private long prevTime = 0;

  private static final int CAMERA_ZOOM_COEF = 150;
  private static final int MAIN_PANE_H = 400;
  private static final double MAIN_PANE_W = 600;
  private static final int DEFAULT_SECTOR_WIDTH = 100;
  private static final int DEFAULT_SECTOR_HEIGHT = 100;
  private int sectorWidth = 100;
  private int sectorHeight = 100;
  private final DebrisProcessor processor = new DebrisProcessor();
  private final Connection netLink = new Connection();
  private final DummySat satellite = new DummySat();
  private TextArea terminalText;
  private PerspectiveCamera viewCamera;
  private Group rockGroup = new Group();
  private Asteroid[] lastFrame = null;
  private boolean newData = false;
  private double x0 = 0;
  private double y0 = 0;
  private Slider zoomSlider = new Slider(-5, 5, 0);
  private boolean onOff = false;
  private boolean prevOnOff = false;
  private boolean manualAuto = true;
  private boolean prevManualAuto = true;
  private int overlap = 32;
  private int zoom = 0;
  private debrisProcessingSubsystem.cameraComponent.Camera camera;
  private OperatorTesting operator;
  private DebrisCollection collection;
  private Scheduler scheduler;
  private int previousZoomLevel = 0;
  private AnimationTimer autoModeTimer;
  private SubScene view;


  private AnimationTimer timer = new AnimationTimer()
  {
    @Override
    public void handle(long now)
    {
      if (newData)
      {
        ObservableList<Node> children = rockGroup.getChildren();
        children.clear();
        children.add(viewCamera);
        children.addAll(getAsteroidNodes());
        newData = false;
      }
    }
  };


  public static void main(String[] args)
  {
    launch(args);
  }


  @Override
  public void start(Stage stage) throws Exception
  {
    this.camera = new Camera();
    this.operator = new OperatorTesting();
    this.collection = new DebrisCollection();

    this.scheduler = new Scheduler(collection, operator, camera);
    //this starts the constant polling of the scheduler over the debriscollection, operator, and camera

    view = createView();
    satellite.start();
    netLink.addIncomingListener(this);
    netLink.connectToDummySat();
    netLink.sendCameraSpec(0, DEFAULT_SECTOR_HEIGHT, DEFAULT_SECTOR_WIDTH, false, false);//starting the camera off and in automatic mode
    timer.start();
    BorderPane mainPane = new BorderPane(view);
    mainPane.setMaxHeight(600);
    mainPane.setMaxWidth(800);
    mainPane.setPadding(new Insets(0, 10, 10, 10));

    mainPane.setRight(createRightPane());
    mainPane.setBottom(createButton());
    Scene scene = new Scene(mainPane);
    stage.setScene(scene);
    stage.setTitle("Space Rock Control Center");
    stage.setResizable(false);
    //set textarea here
    view.setOnScroll((ScrollEvent event) ->
      zoomSlider.adjustValue(zoomSlider.getValue() + event
        .getDeltaY() /
        100));
    view.setOnMousePressed((MouseEvent ev) ->
    {
      x0 = ev.getX();
      y0 = ev.getY();
    });

    view.setOnMouseDragged(e ->
    {
      final double baseDist = (MAIN_PANE_H / 2) /
        Math.tan(Math.toRadians(viewCamera.getFieldOfView() / 2));
      final double curDist = baseDist - viewCamera.getTranslateZ();
      final double viewLen = curDist *
        Math.tan(Math.toRadians(viewCamera.getFieldOfView() / 2));
      final double factorH = viewLen / (MAIN_PANE_H / 2);
      final double factorW = viewLen / (MAIN_PANE_W / 2);

      viewCamera.setTranslateX(viewCamera.getTranslateX() + (x0 - e.getX()) * factorW);
      viewCamera.setTranslateY(viewCamera.getTranslateY() + (y0 - e.getY()) * factorH);
      x0 = e.getX();
      y0 = e.getY();
    });
    stage.show();
  }


  private Node createRightPane()
  {
    /*Terminal design section-- design status labels, console and button*/
    VBox connectionStatusVbox = new VBox(10);

    connectionStatusVbox.setPadding(new Insets(0, 5, 0, 0));
    Label statusLabel = new Label("               Connection Status    ");
    statusLabel
      .setStyle("-fx-font-size: 14pt; -fx-font-family: calibri; -fx-font-weight: bold");
    Button statusButton = new Button("Active");


    BorderPane statusBox = new BorderPane();
    statusBox.setCenter(statusButton);
    statusButton.setStyle("-fx-background-color: #1ccc31;-fx-font-size:large");

    HBox labelBox = new HBox();
    HBox indicatorBox = new HBox();
    indicatorBox.setPadding(new Insets(0, 0, 0, 100));
    HBox terminalBox = new HBox();
    HBox buttonBox = new HBox();
    buttonBox.setPadding(new Insets(0, 100, 0, 85));

    terminalText = new TextArea("$>System Initialized\n");
    terminalText.setPrefColumnCount(20);
    terminalText.setPrefRowCount(10);
    terminalText.setEditable(false);

    Button clearButton = new Button("Clear Terminal");
    clearButton.setOnAction(event -> terminalText.setText("$>"));


    labelBox.getChildren().addAll(statusLabel);
    labelBox.setStyle("-fx-border-color: black");

    indicatorBox.getChildren().add(statusButton);
    terminalBox.getChildren().addAll(terminalText);
    buttonBox.getChildren().addAll(clearButton);

    connectionStatusVbox.getChildren().addAll(labelBox, indicatorBox, terminalBox, buttonBox);




    /* camera controls section*/
    VBox camLabelsVbox = new VBox(10);
    VBox box = new VBox(10);
    box.setPadding(new Insets(5, 5, 5, 5));


    Label camControlLabel = new Label("  Camera Controls  ");

    HBox camLabelBox = new HBox();
    camLabelBox.setStyle("-fx-border-color: black");
    camLabelBox.setPadding(new Insets(0, 0, 5, 55));

    camControlLabel
      .setStyle("-fx-font-size: 14pt; -fx-font-family: calibri; -fx-font-weight: bold");
    camLabelBox.getChildren().add(camControlLabel);
    Label imgDetailLabel = new Label("Image Details ");
    imgDetailLabel.setUnderline(true);
    imgDetailLabel
      .setStyle("-fx-font-size: 11pt; -fx-font-family: calibri; -fx-font-weight: bold");
    HBox camZoomBox = new HBox(5);
    HBox secOverlapBox = new HBox(5);
    HBox secSizeBox = new HBox(5);
    VBox imgDetailBox = new VBox(5);
    imgDetailBox.setPadding(new Insets(10, 5, 5, 15));

    Label camZoomLabel = new Label("Zoom");
    camZoomLabel.setStyle("-fx-font-size:9pt");

    Slider camZoomSlider = new Slider(0, 3, 0);
    camZoomSlider.setShowTickLabels(true);
    camZoomSlider.setShowTickMarks(true);
    camZoomSlider.setMajorTickUnit(1);
    camZoomSlider.setMinorTickCount(0);
    camZoomSlider.setSnapToTicks(true);
    camZoomSlider.setLabelFormatter(new StringConverter<Double>()
    {
      @Override
      public String toString(Double n)
      {
        if (n < 1) return "x0";
        if (n < 2) return "x2";
        if (n < 3) return "x4";

        return "x8";
      }

      @Override
      public Double fromString(String s)
      {
        switch (s)
        {
          case "x0":
            return 0d;
          case "x2":
            return 1d;
          case "x4":
            return 2d;
          case "x8":
            return 3d;

          default:
            return 3d;
        }
      }
    });

    Label overlapLabel = new Label("Section Overlap");
    overlapLabel.setStyle("-fx-font-size:9pt");
    TextField overlapTextField = new TextField();
    overlapTextField.setPrefWidth(50);
    overlapTextField.setText("32");

    Label pxLabel = new Label("px");
    Label pxLabel2 = new Label("px");
    Label secSizeLabel = new Label("Section Size");
    secSizeLabel.setStyle("-fx-font-size:9pt");

    TextField secTextField = new TextField();
    secTextField.setPrefWidth(50);
    secTextField.setText("100");
    secSizeLabel.setPadding(new Insets(0, 0, 20, 0));


    camZoomBox.getChildren().addAll(camZoomLabel, camZoomSlider);
    secOverlapBox.getChildren().addAll(overlapLabel, overlapTextField, pxLabel);
    secSizeBox.getChildren().addAll(secSizeLabel, secTextField, pxLabel2);
    imgDetailBox.getChildren().addAll(camZoomBox, secOverlapBox, secSizeBox);

    ////////////////////////////////
    Label modeLabel = new Label("Image Capture Mode:");
    modeLabel.setStyle("-fx-font-size: 11pt; -fx-font-family: calibri; -fx-font-weight: bold");
    modeLabel.setUnderline(true);
        /* style radio buttons*/
    ToggleGroup modeGroup = new ToggleGroup();
    RadioButton autoMode = new RadioButton("Automatic");
    autoMode.setStyle("-fx-font-size:9pt");
    RadioButton manualMode = new RadioButton("Manual");
    manualMode.setStyle("-fx-font-size:9pt");
    autoMode.setSelected(true);
    modeGroup.getToggles().addAll(autoMode, manualMode);

    Label onOffLabel = new Label("Camera On/Off:");
    onOffLabel.setStyle("-fx-font-size: 11pt; -fx-font-family: calibri; -fx-font-weight: bold");
    onOffLabel.setUnderline(true);
    ToggleGroup onOffGroup = new ToggleGroup();
    RadioButton cameraOn = new RadioButton("On");
    cameraOn.setStyle("-fx-font-size:9pt");
    RadioButton cameraOff = new RadioButton("Off");
    cameraOff.setStyle("-fx-font-size:9pt");
    cameraOff.setSelected(true);
    onOffGroup.getToggles().addAll(cameraOn, cameraOff);

    Button takePicture = new Button("Take Picture");
    takePicture.setDisable(true);
    takePicture.setOnAction(e ->
    {
      CameraUpdate camUpdate = new CameraUpdate(UpdateType.CAMERA);
      camUpdate.setTakePicture();
      scheduler.sendUpdate(camUpdate);

      try
      {
        netLink.sendCameraSpec(zoom, sectorHeight, sectorWidth, onOff, manualAuto);
      }
      catch (IOException e1)
      {
        e1.printStackTrace();
      }
    });

    autoModeTimer = new AnimationTimer()
    {
      @Override
      public void handle(long now)
      {
        if (now - prevTime > NANOSECS_AUTO_MODE_POLLRATE)
        {
          CameraUpdate camUpdate = new CameraUpdate(UpdateType.CAMERA);
          camUpdate.setTakePicture();
          scheduler.sendUpdate(camUpdate);

          prevTime = now;
        }
      }
    };

    HBox modeBox = new HBox();
    modeBox.setPadding(new Insets(5, 5, 5, 90));
    Button modeSubmitButton = new Button("submit");
    modeSubmitButton.setOnAction(e ->
    {
      String terminalString = "";
      if(onOff != ((RadioButton) onOffGroup.getSelectedToggle()).getText().equals("On"))
      {
        terminalString += "S> State On: " + onOff + " changed to " + !onOff + "\n";
        prevOnOff = onOff;
      }
      onOff = ((RadioButton) onOffGroup.getSelectedToggle()).getText().equals("On");
      manualAuto = ((RadioButton) modeGroup.getSelectedToggle()).getText().equals("Manual");
      zoom = (int) camZoomSlider.getValue();
      //zoom = (int)zoomSlider.getValue();

      sectorHeight = Integer.parseInt(secTextField.getText());
      if(sectorHeight != sectorWidth)
      {
        terminalString += "S> Section Size: " + sectorWidth + " changed to " + sectorHeight + "\n";
      }
      sectorWidth = Integer.parseInt(secTextField.getText());
      try
      {
        netLink.sendCameraSpec(zoom, sectorHeight, sectorWidth, onOff, manualAuto);
      }
      catch (IOException e1)
      {
        e1.printStackTrace();
      }
      if(!onOff) { viewCamera.setTranslateZ(500); }
      else { viewCamera.setTranslateZ(-500); }
      takePicture.setDisable(!manualAuto || !onOff);

      System.out.println("GUI transmitted:\n\tZoom Level: " + zoom + "\n\tSection size: " + secTextField.getText() +
          "\n\tPower status: " + (onOff ? "ON" : "OFF") + "\n\tCamera mode: " + (manualMode.isSelected() ? "MANUAL" : "AUTOMATIC"));

      //if the zoom has changed
      if(zoom != previousZoomLevel)
      {
        terminalString += "S> Zoom level: " + previousZoomLevel + " changed to " + zoom + "\n";
        notifySchedulerOfZoom(zoom);
      }
      //now changed to automatic mode from manual mode, or we just turned on from previously being off
      if(manualAuto != prevManualAuto || (onOff && !prevOnOff))
      {
        terminalString += "S> Automatic Mode: " + !prevManualAuto + " changed to " + !manualAuto + "\n";
        prevManualAuto = manualAuto;

        //if not manual mode, and the system is on
        if (!manualAuto && onOff)
        {
          autoModeTimer.start();
        }
        else
        {
          autoModeTimer.stop();
        }
      }
      //disable the auto image polling if the camera is off
      if(!onOff)
      {
        autoModeTimer.stop();
      }

      //Building Console output string.
      terminalText.setText(terminalString);
    });

    Button modeResetButton = new Button("reset");
    modeResetButton.setOnAction(event ->
    {
      manualAuto = true;
      overlap = 32;
      zoom = 1;
      onOff = true;

      onOffGroup.selectToggle(cameraOn);
      modeGroup.selectToggle(manualMode);
      overlapTextField.setText("32");
      camZoomSlider.setValue(0);
      secTextField.setText("100");
      try
      {
        netLink.sendCameraSpec(zoom, DEFAULT_SECTOR_HEIGHT, DEFAULT_SECTOR_WIDTH, onOff, manualAuto);
      }
      catch (IOException e1)
      {
        e1.printStackTrace();
      }
      viewCamera.setTranslateZ(-500);
      takePicture.setDisable(false);
      CameraUpdate cameraUpdate = new CameraUpdate(UpdateType.CAMERA);
      cameraUpdate.setResetCamera();
      scheduler.sendUpdate(cameraUpdate);

      terminalText.setText("S> System Reset");
    });

    modeBox.getChildren().addAll(modeSubmitButton, modeResetButton);


    VBox modeVbox = new VBox(5);
    modeVbox.setPadding(new Insets(0, 5, 5, 15));
    modeVbox.getChildren().addAll(modeLabel, autoMode, manualMode, takePicture, onOffLabel, cameraOn, cameraOff, modeBox);
    ////////////////////////


    // add all components to right pane
    //box.setMaxWidth(275);
    camLabelsVbox.getChildren().addAll(imgDetailLabel, imgDetailBox);
    box.getChildren()
      .addAll(connectionStatusVbox, camLabelBox, camLabelsVbox, modeVbox);
    box.setStyle("-fx-border-color: black");


    box.setPrefHeight(600);
    return box;
  }


  private Node createButton()
  {
    GridPane gridPane = new GridPane();
    gridPane.setHgap(0);
    gridPane.setVgap(0);
    gridPane.setGridLinesVisible(false);
    //gridPane.setGridLinesVisible(true);
    gridPane.setPadding(new Insets(0, 0, 0, 150));


    /////////////////////////////frame zoom links here/////////////
    VBox framePanelVBox = new VBox(5);
    HBox framePanelHBox = new HBox(25);
    Label gridLabel = new Label("Frame Controls");
    gridLabel.setStyle("-fx-font-size: 14pt; -fx-font-family: calibri; -fx-font-weight: bold");
    HBox frameZoomBox = new HBox();
    frameZoomBox.setAlignment(Pos.CENTER);
    HBox frameZoomElements = new HBox();
    Label zoomLabel = new Label("Frame Zoom:");
    zoomLabel.setStyle("-fx-font-size: 9pt; ");

    zoomSlider.setShowTickLabels(true);
    zoomSlider.setShowTickMarks(true);
    zoomSlider.setMajorTickUnit(1);
    zoomSlider.setMinorTickCount(1);

    //create buttons and add to grid pane
    GridPane frameButtons = new GridPane();
    Button upButton = new Button("Up");
    Button downButton = new Button("Down");
    Button leftButton = new Button("Left");
    Button rightButton = new Button("Right");
    rightButton.setPrefWidth(55);
    upButton.setPrefWidth(55);
    downButton.setPrefWidth(55);
    leftButton.setPrefWidth(55);
    leftButton.setStyle("-fx-font-size:8pt");
    upButton.setStyle("-fx-font-size:8pt");
    downButton.setStyle("-fx-font-size:8pt");
    rightButton.setStyle("-fx-font-size:8pt");

    frameButtons.add(upButton, 2, 1);
    frameButtons.add(downButton, 2, 3);
    frameButtons.add(rightButton, 3, 2);
    frameButtons.add(leftButton, 1, 2);
    frameButtons.setPadding(new Insets(1, 1, 1, 1));


    frameZoomElements.getChildren().addAll(zoomLabel, zoomSlider);
    framePanelHBox.getChildren().addAll(frameZoomElements, frameButtons);
    framePanelHBox.setPadding(new Insets(0, 0, 40, 0)); //padding from screen bottom
    framePanelVBox.getChildren().addAll(gridLabel, framePanelHBox);
    frameZoomBox.getChildren().addAll(framePanelVBox);
    gridPane.add(frameZoomBox, 1, 1);
    gridPane.setStyle("-fx-border-color: black");
    zoomSlider.valueProperty().addListener(

      (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        viewCamera.setTranslateZ(viewCamera.getTranslateZ() +
          (newValue.doubleValue() - oldValue.doubleValue()) *
            CAMERA_ZOOM_COEF));
    return gridPane;
  }


  private SubScene createView()
  {
    viewCamera = new PerspectiveCamera(false);
    rockGroup.getChildren().add(viewCamera);

    SubScene scene = new SubScene(rockGroup, MAIN_PANE_W, MAIN_PANE_H);
    scene.setFill(Color.BLACK);
    scene.setCamera(viewCamera);
    viewCamera.setTranslateZ(-500);
    return scene;
  }

  //indicates to the scheduler that it should update the camera with an update (zoom in this case)
  private void notifySchedulerOfZoom(int newZoom)
  {
    CameraUpdate cameraUpdate = new CameraUpdate(UpdateType.CAMERA);
    previousZoomLevel = newZoom;
    switch (newZoom)
    {
      case 0:
        cameraUpdate.setZoomLevel(ZoomLevel.NONE);
        break;
      case 1:
        cameraUpdate.setZoomLevel(ZoomLevel.x2);
        break;
      case 2:
        cameraUpdate.setZoomLevel(ZoomLevel.x4);
        break;
      case 3:
        cameraUpdate.setZoomLevel(ZoomLevel.x8);
        break;
    }
    scheduler.sendUpdate(cameraUpdate);
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
