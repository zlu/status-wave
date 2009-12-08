package labs.orange.wave;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;

@SuppressWarnings("serial")
public class StatusWaveServlet extends AbstractRobotServlet {

	private static final Logger log = Logger.getLogger(StatusWaveServlet.class
	    .getName());

	@Override
	public void processEvents(RobotMessageBundle bundle) {
		Wavelet wavelet = bundle.getWavelet();

		if (bundle.wasSelfAdded()) {
			String[][] stat = retrieveContactStatus();
			buildBlips(wavelet, stat);
		}

		for (Event blipSubmittedEvent : bundle.getBlipSubmittedEvents()) {
			log.info("processing blip submit event");
			handleBlip(wavelet, blipSubmittedEvent.getBlip());
		}
	}

	private void buildBlips(Wavelet wavelet, String[][] stat) {
		log.info("+++++++++++building blips");
		for (int i = 0; i < stat.length; i++) {
			Blip blip = wavelet.appendBlip();
			TextView textView = blip.getDocument();
			textView.append(stat[i][0]);
		}
	}

	private void handleBlip(Wavelet wavelet, Blip blip) {
		String text = blip.getDocument().getText();
		log.info("blip text is: " + text);
		text = text.trim();

		if (text.contains("#statut")) {
			// in this case, user is creating a status
			log.info("+++++++++creating status+++++++++++");
			create_status(text);
		} else {
			if (text.equalsIgnoreCase("fetch") 
					|| text.equalsIgnoreCase("f")
			    || text.equalsIgnoreCase("what is my contacts status?")) {
				log.info("off retriving contact status");
				buildBlips(wavelet, getContactStatus());
			} else if (text.equalsIgnoreCase("last_vm") 
								|| text.equalsIgnoreCase("lvm")) {
				log.info("off retrieving the last voicemail");
				buildVmBlips(wavelet, getLastVm());
			} else if (text.equalsIgnoreCase("latest")
			    || text.equalsIgnoreCase("latest_status")
			    || text.equalsIgnoreCase("what is my status?")) {
				log.info("off retriving the last status");
				buildBlips(wavelet, latestStatus());
			} else if (text.equalsIgnoreCase("what are my voicemails?")
								||text.equalsIgnoreCase("voicemails")) {
				log.info("+++++++++++++++ Off building replies ++++++++++++++++");
				buildVmBlips(wavelet, retrieveVoicemails());
			} else {

				// in this case, user is creating a status
				log.info("+++++++++creating status+++++++++++");

			}
		}
	}

	private String[][] getReplies() {
		
//    try {
//      String addr = "/replies?format=json&status_id=" + status_id; 
//      URI uri = new URI(orange.status.Status.SERVER_URL + addr);
//      HttpGet method = new HttpGet(uri);
//      ResponseHandler<String> responseHandler = new BasicResponseHandler();
//      String responseBody = client.execute(method, responseHandler);
//      if( responseBody != null && responseBody != "") {
//        try {
//          JSONArray jsons = new JSONArray(responseBody);
//          stat = new String[jsons.length()][5];
//          for(int i=0; i<jsons.length(); i++) {
//            JSONObject json = jsons.getJSONObject(i);
//            JSONArray ar = json.toJSONArray(json.names());
//            JSONObject elem = ar.getJSONObject(0);
//            extractReply(stat, i, elem);
//          }
//        } catch(JSONException jsone) {
//          try {
//            JSONObject contact_status = new JSONObject(responseBody).getJSONObject("reply");
//            stat = new String[1][5];
//            extractReply(stat, 0, contact_status);
//          } catch(JSONException e) {
//            Log.e(getClass().getName(), e.getMessage());
//          }
//        }
//        StatusDBOpenHelper.getInstance(mContext).insertReplies(stat, new ContentValues());
//      }
//    } catch (Exception e) {
//      Log.e(getClass().getName(), e.getMessage());
//    } finally {
//      // TODO do something here
//    }
		
		
		String[][] replies = null;
		try {
			URL url = new URL(
			    "http://phonestat.com/statuses/get_contact_status?user_id=1&format=json");
			BufferedReader reader = new BufferedReader(new InputStreamReader(url
			    .openStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				JSONArray jsons = new JSONArray(line);
				replies = new String[jsons.length()][3];
				for (int i = 0; i < jsons.length(); i++) {
					JSONObject json = jsons.getJSONObject(i);
					JSONArray ar = json.toJSONArray(json.names());
					JSONObject elem = ar.getJSONObject(0);
					replies[i][0] = elem.getString("stat");
					replies[i][1] = elem.getString("updated_at");
					replies[i][2] = elem.getString("user_id");
				}
			}
			reader.close();
		} catch (Exception e) {
			// TODO error logging and maybe init stat so that it's not null
		}
		return replies;	  

  }

	private void create_status(String text) {
		try {
			String stat = URLEncoder.encode(text.replace("#statut", "").trim());
			String params = "user_id=1&format=json&status[type]=CustomStatus&status[stat]="
			    + stat + "&status[latitude]=&status[longitude]=";

			URL url = new URL("http://phonestat.com/statuses");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			OutputStreamWriter writer = new OutputStreamWriter(connection
			    .getOutputStream());
			writer.write(params);
			writer.close();
			if (true) {
				log.info("++++++++++++++++++successfully posted status++++++++++");
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(connection
			    .getInputStream()));

			String decodedString;

			while ((decodedString = in.readLine()) != null) {
				System.out.println(decodedString);
			}
			in.close();

		} catch (Exception e) {
			// TODO error logging and maybe init stat so that it's not null
			log.info("++++++++++++++++Error creating status+++++++++++++++++");
		}
	}

	private void buildVmBlips(Wavelet wavelet, String[][] vm) {
		log.info("+++++++++++building voicemail blips");
		for (int i = 0; i < vm.length; i++) {
			Blip blip = wavelet.appendBlip();
			TextView textView = blip.getDocument();
			String callerID = "Caller";
			if(vm[i][1] != null && !vm[i][1].equals("")) {
				callerID = vm[i][1];
			}
			textView.append(callerID + " left a message at " + vm[i][2]);
			String location = "https://phonestat.com/recordings/voicemails/"
			    + vm[i][0] + ".wav";
			String locationMarkup = "<a href=\"" + location + "\"> play</a>";
			log.info("appending markup: " + locationMarkup);
//			String locationMarkup2 = "<audio src=\"" + location
//			    + "\" controls>No support html5</audio>";
//			log.info("appending audio markup: " + locationMarkup2);
			textView.appendMarkup(locationMarkup);
			textView.append("\n");
//			textView.appendMarkup(locationMarkup2);
		}
	}

	private String[][] getLastVm() {
		String[][] lastVm = new String[1][3];
		String[][] vm = retrieveVoicemails();
		for (int i = 0; i < 3; i++) {
			lastVm[0][i] = vm[vm.length - 1][i];
		}
		return lastVm;
	}

	private String[][] getContactStatus() {
		String[][] lastStat = new String[1][3];
		String[][] stat = retrieveContactStatus();
		for (int i = 0; i < 3; i++) {
			lastStat[0][i] = stat[0][i];
		}
		return lastStat;
	}

	private String[][] retrieveVoicemails() {
		String[][] vm = null;
		try {
			URL url = new URL("http://phonestat.com/voicemails?user_id=1&format=json");
			BufferedReader reader = new BufferedReader(new InputStreamReader(url
			    .openStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				JSONArray jsons = new JSONArray(line);
				vm = new String[jsons.length()][3];
				for (int i = 0; i < jsons.length(); i++) {
					JSONObject json = jsons.getJSONObject(i);
					JSONArray ar = json.toJSONArray(json.names());
					JSONObject elem = ar.getJSONObject(0);
					vm[i][0] = elem.getString("file_name");
					vm[i][1] = elem.getString("caller_id");
					vm[i][2] = elem.getString("created_at");
				}
			}
			reader.close();
		} catch (Exception e) {
			// TODO error logging and maybe init stat so that it's not null
		}
		return vm;
	}

	private String[][] retrieveContactStatus() {
		String[][] stat = null;
		try {
			URL url = new URL(
			    "http://phonestat.com/statuses/get_contact_status?user_id=1&format=json");
			BufferedReader reader = new BufferedReader(new InputStreamReader(url
			    .openStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				JSONArray jsons = new JSONArray(line);
				stat = new String[jsons.length()][3];
				for (int i = 0; i < jsons.length(); i++) {
					JSONObject json = jsons.getJSONObject(i);
					JSONArray ar = json.toJSONArray(json.names());
					JSONObject elem = ar.getJSONObject(0);
					stat[i][0] = elem.getString("stat");
					stat[i][1] = elem.getString("updated_at");
					stat[i][2] = elem.getString("user_id");
				}
			}
			reader.close();
		} catch (Exception e) {
			// TODO error logging and maybe init stat so that it's not null
		}
		return stat;
	}

	private String[][] latestStatus() {
		String[][] stat = null;
		try {
			URL url = new URL(
			    "http://phonestat.com/users/get_latest_status?user_id=1&format=json");
			BufferedReader reader = new BufferedReader(new InputStreamReader(url
			    .openStream()));
			String line;   
			while ((line = reader.readLine()) != null) {
				JSONObject json = new JSONObject(line);
				JSONArray ar = json.toJSONArray(json.names());
				JSONObject elem = ar.getJSONObject(0);
				stat = new String[1][3];
				stat[0][0] = elem.getString("stat");
				stat[0][1] = elem.getString("updated_at");
				stat[0][2] = elem.getString("user_id");
			}
			reader.close();
		} catch (Exception e) {
			// TODO error logging and maybe init stat so that it's not null
		}
		return stat;
	}
	
//  private void extract_status(String[][] stat, int i, JSONObject elem)
//  throws JSONException {
//    stat[i][0] = elem.getString("stat");
//    stat[i][1] = new Long(elem.getLong("id")).toString();
//    //stat[i][2] = elem.getString("type");
//  }
}
