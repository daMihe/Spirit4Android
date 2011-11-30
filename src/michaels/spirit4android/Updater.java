package michaels.spirit4android;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.SharedPreferences.Editor;
import android.util.Log;

public class Updater implements Runnable {

	public void run(){
		HttpClient client = new DefaultHttpClient();
		long lastUpdate = mainActivity.saveFile.getLong("lastUpdate", 0);
		if(lastUpdate < System.currentTimeMillis()-24*60*60*1000){
			try {
				String responseText = "";
				HttpGet hg = new HttpGet("http://spirit.fh-schmalkalden.de/rest/1.0/news");
				hg.setHeader("User-Agent", mainActivity.USERAGENT);
				HttpResponse response = client.execute(hg); //News
				responseText = EntityUtils.toString(response.getEntity()); //Antwort in einen String wandeln.
				try {
					new JSONArray(responseText);
					Editor e = mainActivity.saveFile.edit();
					e.putString("newsJSON", responseText);
					e.putLong("lastUpdate", System.currentTimeMillis());
					e.commit();
				} catch(JSONException e){
					Log.e("a","E:"+e.getClass().getName());
				}
				
				
			} catch (Exception e) {
				Log.e("Spirit4Android - Client", e.getClass().getName()+": "+e.getMessage());
			}
			
		}
	}

}
