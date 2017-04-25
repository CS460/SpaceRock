package sensor.Universe;

/**
 * Created by Arthur on 4/24/2017.
 *
 *  Moved the main method of UniverseThread to TestUniverseThread
 */
public class TestUniverseThread
{
  public static void main(String[] args)
  {
    UniverseThread runUni = new UniverseThread();
    try
    {
      runUni.run();
    } catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
