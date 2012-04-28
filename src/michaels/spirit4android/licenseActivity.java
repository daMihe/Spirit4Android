package michaels.spirit4android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class licenseActivity extends Activity {
	public void onCreate(Bundle b){
		super.onCreate(b);
		ScrollView sw = new ScrollView(this);
    	LinearLayout basis = new LinearLayout(this);
    	sw.addView(basis);
    	basis.setOrientation(LinearLayout.VERTICAL);
    	TextView willkommen = new TextView(this);
    	willkommen.setText(R.string.LANG_LICENSE);
    	
    	Button akzeptieren = new Button(this);
    	akzeptieren.setText(R.string.LANG_ACCEPTLICENSE);
    	akzeptieren.setOnClickListener(new View.OnClickListener(){

			public void onClick(View v) {
				Editor e = Spirit4AndroidActivity.saveFile.edit();
				e.putBoolean("licenseAccepted", true);
				e.commit();
				Intent intent = new Intent(licenseActivity.this,mainActivity.class);
				licenseActivity.this.startActivity(intent);
				licenseActivity.this.finish();
				
			}
    		
    	});
    	
    	Button ablehnen = new Button(this);
    	ablehnen.setText(R.string.LANG_DENYINGLICENSE);
    	ablehnen.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				licenseActivity.this.finish();	
			}
		});
    	
    	basis.addView(willkommen);
    	basis.addView(ablehnen);
    	basis.addView(akzeptieren);
    	this.setContentView(sw);
	}
}
