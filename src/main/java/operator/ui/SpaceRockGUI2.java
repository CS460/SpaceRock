package operator.ui;/**
 * Created by Neil on 4/27/2017.
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sensor.Universe.*;

import java.io.IOException;

public class SpaceRockGUI2 extends Application
{
  public GUIController controller;

  public static void main(String[] args)
  {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage)
  {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("SpaceRockGUI.fxml"));
    Scene newScene = null;
    Parent root = null;
    try
    {
      root = loader.load();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
    }
    controller = loader.getController();

    controller.setStage(primaryStage);
    newScene = new Scene(controller.getRoot());
    primaryStage.setScene(newScene);
    primaryStage.setResizable(false);
    primaryStage.sizeToScene();
    primaryStage.show();
  }
}
