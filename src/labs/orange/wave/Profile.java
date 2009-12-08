package labs.orange.wave;

import com.google.wave.api.ProfileServlet;

public class Profile extends ProfileServlet {
  @Override
  public String getRobotName() {
    return "Orange Status";
  }

	@Override
  public String getRobotAvatarUrl() {
	  return "http://phonestat.com/downloads/bill.png";
  }

}