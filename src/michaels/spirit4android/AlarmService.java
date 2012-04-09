package michaels.spirit4android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.provider.MediaStore.Audio.Media;
import android.util.Log;

public class AlarmService extends Service {

	private static MediaPlayer player;
	private static NotificationManager not_man;
	private static int id;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	public void onCreate(){
		not_man = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
		SharedPreferences sp = this.getSharedPreferences("s4apref",MODE_WORLD_READABLE);
		String sound = sp.getString("alarmMusic", "");
		player = new MediaPlayer();
		player.setLooping(true);
		if(sound.equals("")){
			// Search a random Sound. (No sound was specified in settings)
			Cursor c = getContentResolver().query(Media.EXTERNAL_CONTENT_URI, 
					new String[]{ Media.DATA }, 
					null, 
					null, 
					null);
			int count;
			if(c == null || (count = c.getCount()) == 0)
				return;
			c.moveToPosition((int)(Math.random()*count));
			sound = c.getString(c.getColumnIndex(Media.DATA));
			c.close();
		}
		try {
			if(sp.getBoolean("alarmPenetrantMode", true))
				player.setAudioStreamType(AudioManager.STREAM_ALARM);
			player.setDataSource(sound);
			player.prepare();
			player.start();
		} catch (Exception e) {
			Log.e("Spirit AlarmService", 
					e.getClass().getSimpleName()+" while preparing and starting player.");
		}
		
		// Tray-Icon
		id = (int)(Math.random()*Integer.MAX_VALUE);
		Notification n = new Notification(R.drawable.icon,
				getString(R.string.LANG_EVENTREMINDING),
				System.currentTimeMillis());
		n.setLatestEventInfo(this, 
				getString(R.string.app_name), 
				getString(R.string.LANG_EVENTREMINDING), 
				PendingIntent.getActivity(this, 
						0, 
						new Intent(this,AlarmActivity.class), 
						0));
		not_man.notify(id, n);
	}
	
	public void onDestroy(){
		not_man.cancelAll();
		player.stop();
		player.release();
	}

}
