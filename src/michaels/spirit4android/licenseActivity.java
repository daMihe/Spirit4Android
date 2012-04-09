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
    	willkommen.setText("Willkommen in der Spirit-App für Android. Mit der App wirst Du automatisch über aktuelle Nachrichten von Spirit informiert und kannst dir Deinen Stundenplan inklusive Erinnerung an deine nächste Veranstaltung holen.\n\n" +
    			"Vor der Benutzung:\n" +
    			"Die App ist Entwicklung. Vor jedem Release wird die Software zwar sorgfältig geprüft, eine Garantie, dass sie funktioniert gibt es nicht. In dem Fall können Programmierer keine Verantwortung übernehmen. Rechne damit.\n" +
    			"Es werden Daten aus dem Internet geladen. Dabei können für Dich Kosten des Betreibers entstehen.");
    	
    	Button akzeptieren = new Button(this);
    	akzeptieren.setText("Habe den Text gelesen und aktzeptiere die Bedingungen");
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
    	ablehnen.setText("Habe den Text gelesen und lehne die Bedingungen ab (App wird beendet)");
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
