package sensor.Universe;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;


/**
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

  @FXML
  private ImageView imgView;

  private long ID;
  private Instant timestamp;
  private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
  private Image image;

  public void setData(sensor.Asteroid asteroid)
  {
    this.image = asteroid.getFXImage();
    this.size = asteroid.originalRad;
    this.ID = asteroid.getId();
    //this.timestamp = asteroid.timestamp;
    diameter.setText("" + Math.round(size));
    objectID.setText("" + ID);
    //timeCaptured.setText("Timestamp: " + timestamp.toString());
  }

  @FXML
  public void buttonPressed(ActionEvent e)
  {
    if (e.getSource() == requestFrameButton)
    {
      System.out.println("Raw frame requested!");
      System.out.println();
      imgView.setStyle("-fx-background-color: BLACK");
      imgView.setImage(this.image);
      requestFrameButton.setDisable(true);
      requestFrameButton.setTooltip(new Tooltip("The raw image for this asteroid is displayed."));
    }
  }


  @Override
  public void initialize(URL url, ResourceBundle rb)
  {

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

