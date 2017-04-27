package sensor.Universe;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;


/**
 *
 * @author keira
 */
public class SpaceRockFXMLController implements Initializable
{
  private double size;
  @FXML
  private Label timeCaptured;

  @FXML
  private Label objectID;

  @FXML
  private Label threatLevel;

  @FXML
  private Label velocity;

  @FXML
  private Button requestFrameButton;

  @FXML
  private Label diameter;
  private long ID;
  private Instant timestamp;
  private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

  public void setData(sensor.Asteroid asteroid) {
    this.size = asteroid.current_radius;
    this.ID = asteroid.getId();
    //this.timestamp = asteroid.timestamp;
    diameter.setText("" + Math.round(size));
    objectID.setText("" + ID);
    //timeCaptured.setText("Timestamp: " + timestamp.toString());
  }
  @FXML
  public void buttonPressed(ActionEvent e)
  {
    if(e.getSource() == requestFrameButton)
    {
      System.out.println("Raw frame requested!");
    }
  }


  @Override
  public void initialize(URL url, ResourceBundle rb) {

//        zoom_txt.setText(Double.toString(0.000));
//        section_size_txt.setText(Double.toString(100.0));
//        overlap_amount_txt.setText(Double.toString(0.000));
//
//        zoom_slide.valueProperty().addListener(event -> {
//            zoom_txt.setText(Double.toString(zoom_slide.getValue()));
//        });
//        section_size_slide.valueProperty().addListener(event -> {
//            section_size_txt.setText(Double.toString((section_size_slide.getValue() + 25) * 4));
//        });
//        overlap_amount_slide.valueProperty().addListener(event -> {
//            overlap_amount_txt.setText(Double.toString((overlap_amount_slide.getValue()) * 2));
//        });
  }



}

