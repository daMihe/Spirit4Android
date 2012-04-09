package michaels.spirit4android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class Spirit4AndroidActivity extends Activity {
	static SharedPreferences saveFile;
	static double VERSION = 0.1;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.start);
        saveFile = this.getSharedPreferences("s4apref",Context.MODE_WORLD_READABLE);
        if(saveFile.getBoolean("licenseAccepted", false)){
        	Intent i = new Intent(this,mainActivity.class);
        	this.startActivity(i);
        } else {
        	Intent i = new Intent(this,licenseActivity.class);
        	this.startActivity(i);
        }
        this.finish();
        
    }
      
}