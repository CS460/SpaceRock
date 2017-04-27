package operator.ui;

import debrisProcessingSubsystem.Scheduler;
import debrisProcessingSubsystem.cameraComponent.Camera;
import debrisProcessingSubsystem.debrisCollection.DebrisCollection;
import debrisProcessingSubsystem.operatorComponent.OperatorTesting;
import debrisProcessingSubsystem.updateSystem.CameraUpdate;
import debrisProcessingSubsystem.updateSystem.UpdateType;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Line;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import sensor.ZoomLevel;

import java.net.URL;
import java.util.ResourceBundle;

public class GUIController implements Initializable {
  private static final boolean TESTING_FRAME_BOUNDARIES = true;
  private static final long NANOSECS_AUTO_MODE_POLLRATE = 2_000_000_000;
  private static final int CAMERA_ZOOM_COEF = 150;
  private static final int MAIN_PANE_H = 400;
  private static final double MAIN_PANE_W = 600;
  @FXML
  BorderPane borderPane;
  @FXML
  ToggleGroup modeGroup;
  @FXML
  ToggleGroup onOffGroup;
  @FXML
  RadioButton autoMode;
  @FXML
  RadioButton cameraOff;
  @FXML
  RadioButton cameraOn;
  @FXML
  RadioButton manualMode;
  @FXML
  Slider zoomSlider;
  @FXML
  Slider camZoomSlider;
  @FXML
  SubScene view;
  @FXML
  TextField secTextField;
  @FXML
  TextField overlapTextField;
  @FXML
  TextArea terminalText;
  @FXML
  Button upButton;
  @FXML
  Button downButton;
  @FXML
  Button leftButton;
  @FXML
  Button rightButton;
  @FXML
  Button takePicture;
  @FXML
  Button submitButton;
  @FXML
  Button clearButton;
  @FXML
  Button resetButton;
  @FXML
  Button statusButton;
  private Stage primaryStage;
  private long prevTime = 0;
  private int sectorWidth = 100;
  private int sectorHeight = 100;
  private PerspectiveCamera viewCamera;
  private Group rockGroup = new Group();
  private double x0 = 0;
  private double y0 = 0;
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
  private volatile boolean newData = false;
  private double navButtonSensitivity = 50;

  private AnimationTimer timer = new AnimationTimer() {
    @Override
    public void handle(long now)
    {
      if (newData) {
        ObservableList<Node> children = rockGroup.getChildren();
        children.clear();
        children.add(viewCamera);
        children.addAll(camera.getAsteroidNodes());
        for (int i = 0; i <= 4000; i += sectorHeight) {
          Line line = new Line(i, 0, i, 4000);
          line.setStrokeWidth(2);
          Line line2 = new Line(0, i, 4000, i);
          line2.setStrokeWidth(2);

          if (i % 1000 == 0) {
            line.setStroke(Color.GREEN);
            line.setFill(Color.GREEN);
            line2.setStroke(Color.GREEN);
            line2.setFill(Color.GREEN);
          } else {
            line.setStroke(Color.WHITE);
            line.setFill(Color.WHITE);
            line2.setStroke(Color.WHITE);
            line2.setFill(Color.WHITE);
          }
          rockGroup.getChildren().addAll(line, line2);
        }

        if (TESTING_FRAME_BOUNDARIES) {
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

  void setStage(Stage stage)
  {
    this.primaryStage = stage;
  }

  private void createView()
  {
    viewCamera = new PerspectiveCamera(false);
    rockGroup.getChildren().add(viewCamera);

    view.setRoot(rockGroup);
    view.setFill(Color.BLACK);
    view.setCamera(viewCamera);

    viewCamera.setTranslateZ(-500);
    viewCamera.setTranslateX(197);
    viewCamera.setTranslateY(130);
  }

  private void setViewListeners()
  {
    view.setOnScroll((ScrollEvent event) ->
    {
      double cameraX = viewCamera.getTranslateX();
      double cameraY = viewCamera.getTranslateY();
      double newValue = zoomSlider.getValue() + event.getDeltaY() / 100;
      if ((cameraX >= (193 - 54 * newValue) && cameraX <= (3203 + 54 * newValue))
        && (cameraY >= (128 - 33 * newValue) && cameraY <= (3473 + 33 * newValue)))
      {
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

      if (xTranslation >= (193 - 54 * sliderVal) && xTranslation <= (3203 + 54 * sliderVal)) {
        viewCamera.setTranslateX(xTranslation);
        x0 = e.getX();
      }
      if (yTranslation >= (128 - 33 * sliderVal) && yTranslation <= (3473 + 33 * sliderVal)) {
        viewCamera.setTranslateY(yTranslation);
        y0 = e.getY();
      }
    });
  }

  private void formatCamZoomLabels()
  {
    camZoomSlider.setLabelFormatter(new StringConverter<Double>() {
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
        switch (s) {
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
  }

  void setButtonListeners()
  {
    // Listener for the "Clear Terminal" Button
    clearButton.setOnAction(event -> terminalText.setText("\\$>"));

    // Listener for the "Take Picture" Button
    takePicture.setDisable(true);
    takePicture.setOnAction(e ->
    {
      CameraUpdate camUpdate = new CameraUpdate(UpdateType.CAMERA);
      camUpdate.setTakePicture();
      scheduler.sendUpdate(camUpdate);
      newData = true;
    });

    autoModeTimer = new AnimationTimer() {
      @Override
      public void handle(long now)
      {
        if (now - prevTime > NANOSECS_AUTO_MODE_POLLRATE) {
          CameraUpdate camUpdate = new CameraUpdate(UpdateType.CAMERA);
          camUpdate.setTakePicture();
          scheduler.sendUpdate(camUpdate);
          //camera.getCurrentImage();
          newData = true;

          prevTime = now;
        }
      }
    };

    // Listener for "Submit" button - includes hooks to the entire right pane
    submitButton.setOnAction(e ->
    {
      String terminalString = "";
      if (onOff != ((RadioButton) onOffGroup.getSelectedToggle()).getText().equals("On")) {
        terminalString += "\\$> State On: " + onOff + " changed to " + !onOff + "\n";
        prevOnOff = onOff;
      }
      if (overlap != Integer.parseInt(overlapTextField.getText())) {
        prevOverlap = overlap;
        overlap = Integer.parseInt(overlapTextField.getText());
        terminalString += "\\$> Overlap Size: " + prevOverlap + " changed to " + overlap + "\n";
        CameraUpdate camUpdate = new CameraUpdate(UpdateType.CAMERA);
        camUpdate.setOverlapSize(overlap);
        scheduler.sendUpdate(camUpdate);
      }
      onOff = ((RadioButton) onOffGroup.getSelectedToggle()).getText().equals("On");
      manualAuto = ((RadioButton) modeGroup.getSelectedToggle()).getText().equals("Manual");
      zoom = (int) camZoomSlider.getValue();

      //zoom = (int)zoomSlider.getValue();

      sectorHeight = Integer.parseInt(secTextField.getText());
      if (sectorHeight != sectorWidth) {
        terminalString += "\\$> Section Size: " + sectorWidth + " changed to " + sectorHeight + "\n";
        CameraUpdate camupdate = new CameraUpdate(UpdateType.CAMERA);
        camupdate.setSectionSize(sectorHeight);
        scheduler.sendUpdate(camupdate);
      }
      sectorWidth = Integer.parseInt(secTextField.getText());
      if (!onOff) {
        viewCamera.setTranslateZ(500);
      } else {
        viewCamera.setTranslateZ(-500);
      }
      takePicture.setDisable(!manualAuto || !onOff);

      System.out.println("GUI transmitted:\n\tZoom Level: " + zoom + "\n\tSection size: " + secTextField.getText() +
        "\n\tPower status: " + (onOff ? "ON" : "OFF") + "\n\tCamera mode: " + (manualMode.isSelected() ? "MANUAL" : "AUTOMATIC"));

      //if the zoom has changed
      if (zoom != previousZoomLevel) {
        terminalString += "\\$> Zoom level: " + previousZoomLevel + " changed to " + zoom + "\n";
        notifySchedulerOfZoom(zoom);
      }
      //now changed to automatic mode from manual mode, or we just turned on from previously being off
      if (manualAuto != prevManualAuto || (onOff && !prevOnOff)) {
        terminalString += "\\$> Automatic Mode: " + !prevManualAuto + " changed to " + !manualAuto + "\n";
        prevManualAuto = manualAuto;

        //if not manual mode, and the system is on
        if (!manualAuto && onOff) {
          autoModeTimer.start();
        } else {
          autoModeTimer.stop();
        }
      }
      //disable the auto image polling if the camera is off
      if (!onOff) {
        autoModeTimer.stop();
      }

      //Building Console output string.
      terminalText.setText(terminalString);
    });

    // Listener for "Reset" button
    resetButton.setOnAction(event ->
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
  }

  void setArrowListeners() {
    upButton.setOnAction(event -> {
      double yTranslation = viewCamera.getTranslateY() - navButtonSensitivity;
      double sliderVal = zoomSlider.getValue();
      if (yTranslation >= (128 - 33 * sliderVal)) {
        viewCamera.setTranslateY(yTranslation);
      }
    });
    downButton.setOnAction(event -> {
      double yTranslation = viewCamera.getTranslateY() + navButtonSensitivity;
      double sliderVal = zoomSlider.getValue();
      if (yTranslation <= (3473 + 33 * sliderVal)) {
        viewCamera.setTranslateY(yTranslation);
      }

    });
    leftButton.setOnAction(event -> {
      double xTranslation = viewCamera.getTranslateX() - navButtonSensitivity;
      double sliderVal = zoomSlider.getValue();
      if (xTranslation >= (193 - 54 * sliderVal)) {
        viewCamera.setTranslateX(xTranslation);
      }
    });
    rightButton.setOnAction(event -> {
      double xTranslation = viewCamera.getTranslateX() + navButtonSensitivity;
      double sliderVal = zoomSlider.getValue();
      if (xTranslation <= (3203 + 54 * sliderVal)) {
        viewCamera.setTranslateX(xTranslation);
      }
    });
  }

  void setZoomSliderListener() {
    zoomSlider.valueProperty().addListener(
      (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
        viewCamera.setTranslateZ(viewCamera.getTranslateZ() + (newValue.doubleValue() - oldValue.doubleValue()) * CAMERA_ZOOM_COEF)
    );
  }

  //indicates to the scheduler that it should update the camera with an update (zoom in this case)
  private void notifySchedulerOfZoom(int newZoom)
  {
    CameraUpdate cameraUpdate = new CameraUpdate(UpdateType.CAMERA);
    previousZoomLevel = newZoom;
    switch (newZoom) {
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

  BorderPane getRoot() {
    return this.borderPane;
  }

  @Override
  public void initialize(URL location, ResourceBundle resources)
  {
    this.camera = new Camera();
    this.operator = new OperatorTesting();
    this.collection = new DebrisCollection();
    this.scheduler = new Scheduler(collection, operator, camera);
    //this starts the constant polling of the scheduler over the debriscollection, operator, and camera
    timer.start();

    createView();
    formatCamZoomLabels();
    setButtonListeners();
    setArrowListeners();
    setZoomSliderListener();
    setViewListeners();
  }
}
