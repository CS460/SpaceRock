package operator.ui;/**
 * Created by Neil on 4/27/2017.
 */

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SpaceRockGUI extends Application {
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
    Parent root;
    try {
      root = loader.load();
      newScene = new Scene(root);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    controller = loader.getController();
    primaryStage.setScene(newScene);
    primaryStage.setResizable(false);
    primaryStage.sizeToScene();
    primaryStage.show();
  }
}
