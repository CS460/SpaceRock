package operator.ui;


import debrisProcessingSubsystem.Scheduler;
import debrisProcessingSubsystem.cameraComponent.Camera;
import debrisProcessingSubsystem.debrisCollection.DebrisCollection;
import debrisProcessingSubsystem.operatorComponent.OperatorTesting;
import debrisProcessingSubsystem.updateSystem.CameraUpdate;
import debrisProcessingSubsystem.updateSystem.UpdateType;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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
import javafx.scene.shape.Line;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import operator.commands.Asteroid;
import operator.commands.AsteroidData;
import operator.commands.IncomingListener;
import operator.processing.DebrisProcessor;
import sensor.ZoomLevel;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sahba and Kathrina
 */
public class SpaceRockGUI extends Application
{
  private static final boolean TESTING_FRAME_BOUNDARIES = true;

  private static final long NANOSECS_AUTO_MODE_POLLRATE = 2_000_000_000;
  private long prevTime = 0;

  private static final int CAMERA_ZOOM_COEF = 150;
  private static final int MAIN_PANE_H = 400;
  private static final double MAIN_PANE_W = 600;
  private int sectorWidth = 100;
  private int sectorHeight = 100;
  private TextArea terminalText;
  private PerspectiveCamera viewCamera;
  private Group rockGroup = new Group();
  private double x0 = 0;
  private double y0 = 0;
  private Slider zoomSlider = new Slider(-5, 5, 0);
  private boolean onOff = false;
  private boolean prevOnOff = false;
  private boolean manualAuto = true;
  private boolean prevManualAuto = true;
  private int overlap = 32;
  private int prevOverlap = overlap;
  private int zoom = 0;
  private debrisProcessingSubsystem.cameraComponent.Camera camera;
  private OperatorTesting operator;
  private DebrisCollection collection;
  private Scheduler scheduler;
  private int previousZoomLevel = 0;
  private AnimationTimer autoModeTimer;
  private SubScene view;
  private volatile boolean newData = false;
  private double navButtonSensitivity = 50;

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
        children.addAll(camera.getAsteroidNodes());
        for(int i = 0; i <= 4000; i+= sectorHeight)
        {
          Line line = new Line(i,0,i,4000);
          line.setStrokeWidth(2);
          line.setStroke(Color.WHITE);
          line.setFill(Color.WHITE);

          Line line2 = new Line(0,i,4000,i);
          line2.setStrokeWidth(2);
          line2.setStroke(Color.WHITE);
          line2.setFill(Color.WHITE);
          rockGroup.getChildren().addAll(line,line2);
        }

        if(TESTING_FRAME_BOUNDARIES)
        {
          Sphere s = new Sphere(5);
          s.setMaterial(new PhongMaterial(Color.ALICEBLUE));
          Sphere s1 = new Sphere(5);
          s.setMaterial(new PhongMaterial(Color.ALICEBLUE));
          Sphere s2 = new Sphere(5);
          s.setMaterial(new PhongMaterial(Color.ALICEBLUE));
          Sphere s3 = new Sphere(5);
          s.setMaterial(new PhongMaterial(Color.ALICEBLUE));

          s.setTranslateX(0);
          s.setTranslateY(0);
          children.add(s);

          s1.setTranslateX(4000);
          s1.setTranslateY(0);
          children.add(s1);

          s2.setTranslateY(4000);
          s2.setTranslateX(4000);
          children.add(s2);

          s3.setTranslateY(4000);
          s3.setTranslateX(0);
          children.add(s3);
        }

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
    timer.start();

    view = createView();

    //@Austin, added to make the top right coordinates of the camera (0,0) on the plane
    viewCamera.setTranslateX(197);
    viewCamera.setTranslateY(130);
    //timer.start();
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
    {
      double cameraX = viewCamera.getTranslateX();
      double cameraY = viewCamera.getTranslateY();
      double newValue = zoomSlider.getValue() + event.getDeltaY() / 100;
      if((cameraX >= (193 - 54*newValue) && cameraX <= (3203 + 54*newValue))
              && (cameraY >= (128 - 33*newValue) && cameraY <= (3473 + 33*newValue))) {
        zoomSlider.adjustValue(newValue);
      }
    });

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
      double xTranslation = viewCamera.getTranslateX() + (x0 - e.getX()) * factorW;
      double yTranslation = viewCamera.getTranslateY() + (y0 - e.getY()) * factorH;
      double sliderVal = zoomSlider.getValue();

      if(xTranslation >= (193 - 54*sliderVal) && xTranslation <= (3203 + 54*sliderVal))
      {
        viewCamera.setTranslateX(xTranslation);
        x0 = e.getX();
      }
      if(yTranslation >= (128 - 33*sliderVal) && yTranslation <= (3473 + 33*sliderVal))
      {
        viewCamera.setTranslateY(yTranslation);
        y0 = e.getY();
      }

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
      newData = true;
     // BufferedImage asteroidImage = camera.getCurrentImage();
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
          //camera.getCurrentImage();
          newData = true;

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
      if(overlap != Integer.parseInt(overlapTextField.getText()))
      {
        prevOverlap = overlap;
        overlap = Integer.parseInt(overlapTextField.getText());
        terminalString += "S> Overlap Size: " + prevOverlap + " changed to " + overlap + "\n";
        CameraUpdate camUpdate = new CameraUpdate(UpdateType.CAMERA);
        camUpdate.setOverlapSize(overlap);
        scheduler.sendUpdate(camUpdate);
      }
      onOff = ((RadioButton) onOffGroup.getSelectedToggle()).getText().equals("On");
      manualAuto = ((RadioButton) modeGroup.getSelectedToggle()).getText().equals("Manual");
      zoom = (int) camZoomSlider.getValue();

      //zoom = (int)zoomSlider.getValue();

      sectorHeight = Integer.parseInt(secTextField.getText());
      if(sectorHeight != sectorWidth)
      {
        terminalString += "S> Section Size: " + sectorWidth + " changed to " + sectorHeight + "\n";
        CameraUpdate camupdate = new CameraUpdate(UpdateType.CAMERA);
        camupdate.setSectionSize(sectorHeight);
        scheduler.sendUpdate(camupdate);
      }
      sectorWidth = Integer.parseInt(secTextField.getText());
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
      zoomSlider.setValue(0);
      secTextField.setText("100");
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

    upButton.setOnAction(event -> {
      double yTranslation = viewCamera.getTranslateY() - navButtonSensitivity;
      double sliderVal = zoomSlider.getValue();
      if(yTranslation >= (128 - 33*sliderVal))
      {
        viewCamera.setTranslateY(yTranslation);
      }
    });
    downButton.setOnAction(event -> {
      double yTranslation = viewCamera.getTranslateY() + navButtonSensitivity;
      double sliderVal = zoomSlider.getValue();
      if(yTranslation <= (3473 + 33*sliderVal))
      {
        viewCamera.setTranslateY(yTranslation);
      }

    });
    leftButton.setOnAction(event -> {
      double xTranslation = viewCamera.getTranslateX() - navButtonSensitivity;
      double sliderVal = zoomSlider.getValue();
      if(xTranslation >= (193 - 54*sliderVal))
      {
        viewCamera.setTranslateX(xTranslation);
      }
    });
    rightButton.setOnAction(event -> {
      double xTranslation = viewCamera.getTranslateX() + navButtonSensitivity;
      double sliderVal = zoomSlider.getValue();
      if(xTranslation <= (3203 + 54*sliderVal))
      {
        viewCamera.setTranslateX(xTranslation);
      }

    });

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
}
