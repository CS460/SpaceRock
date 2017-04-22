package operator.commands;

/**
 * Created by magik on 2/5/2017.
 * Modified by David R. 2/13/2017
 * <p>
 * OutgoingCameraSpec wraps the parameters of the pictures the camera takes.
 */

import java.io.Serializable;

public class OutgoingCameraSpec implements Serializable
{
  /* Might be good to have some constants to say "Don't change your height/width/zoom" */
  public static final int DEFAULT_SECT_HEIGHT = -1;
  public static final int DEFAULT_SECT_WIDTH = -1;
  public final int zoom;
  public final int sectorHeight;
  public final int sectorWidth;
  public final boolean onOff;
  public final boolean manualAuto;


  /**
   * Create a new camera spec, specifying view parameters and whether camera should be on or off
   *
   * @param zoom         zoom level on the camera
   * @param sectorHeight height of a chunk in the camera's images
   * @param sectorWidth  width of a chunk in the camera's images
   * @param onOff        true if camera should be on, false otherwise
   * @param manualAuto   true if camera is in manual mode, false otherwise
   */
  public OutgoingCameraSpec(int zoom, int sectorHeight, int sectorWidth, boolean onOff, boolean manualAuto)
  {
    this.zoom = zoom;
    this.sectorHeight = sectorHeight;
    this.sectorWidth = sectorWidth;
    this.onOff = onOff;
    this.manualAuto = manualAuto;
  }


  /**
   * Construct new camera parameters, assuming the camera should be on.
   *
   * @param zoom         zoom level on the camera
   * @param sectorHeight height of a chunk in the camera's images
   * @param sectorWidth  width of a chunk in the camera's images
   */
  public OutgoingCameraSpec(int zoom, int sectorHeight, int sectorWidth)
  {
    this(zoom, sectorHeight, sectorWidth, true, true);
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("\n\tZoom level: ").append(this.zoom);
    sb.append("\n\tSector Dimensions: ").append(this.sectorHeight).append(" (h), ").append(this.sectorWidth).append(" (w)");
    sb.append("\n\tPower status: ").append(this.onOff ? "ON" : "OFF");
    sb.append("\n\tCamera mode: ").append(this.manualAuto ? "MANUAL\n" : "AUTOMATIC\n");
    return sb.toString();
  }
}
