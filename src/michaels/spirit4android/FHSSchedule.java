package michaels.spirit4android;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;

public class FHSSchedule {
	// Stundenplan in Dreidimensionales Array, [WOCHE%2(=gerade/ungerade)][Tag][Slot(=Stundenbereich)]
	JSONObject[][][] scheduleTable;
	// Starts der Slots
	public final short[][] slots = {
			{8,15}, //08:15-09:45
			{10,0}, //10:00-11:30
			{11,45},//11:45-13:15
			{14,15},//14:45-15:45
			{16,0}, //16:00-17:30
			{17,45},//17:45-19:15
			{19,30} //19:30-21:00
		}; 
	
	final String[] dayNames = new String[]{
			"Sonntag",
			"Montag",
			"Dienstag",
			"Mittwoch",
			"Donnerstag",
			"Freitag",
			"Samstag"
		};
	
	// JSON der Rest-Schnittstelle parsen
	public FHSSchedule(JSONArray rawData, SharedPreferences saveFile){
		scheduleTable = new JSONObject[2][7][7];
		for(short i=0; i<rawData.length(); i++){
			try {
				JSONObject currentEvent = rawData.getJSONObject(i);
				JSONObject currentAppointment = currentEvent.getJSONObject("appointment");
				String preGroup = currentEvent.getString("group").replaceAll("[^0-9]+", "");
				if((preGroup.length() == 0 ? 0 : Integer.parseInt(preGroup)) == 
						saveFile.getInt("group"+MD5(currentEvent.getString("eventType")+
								"/"+currentEvent.getString("titleShort")), 0)){
					short hour = Short.parseShort(currentAppointment.getString("time").substring(0, 2).
							replaceAll("[^0-9]+",""));
					short min = Short.parseShort(currentAppointment.getString("time").substring(3, 5).
							replaceAll("[^0-9]+",""));
					short slot = -1;
					for(short si=0; si<slots.length;si++){
						if(hour == slots[si][0] && min == slots[si][1])
							slot = si;
					}
					short day = (short) (Arrays.asList(dayNames).indexOf(currentAppointment.
							getString("day")));
					if(currentAppointment.getString("week").equals("w")){
						scheduleTable[0][day][slot] = currentEvent;
						scheduleTable[1][day][slot] = currentEvent;
					} else {
						short week = (short) (currentAppointment.getString("week").equals("g")?0:1);
						scheduleTable[week][day][slot] = currentEvent;
					}
				}
			} catch (JSONException e) {}
		}
	}
	
	public int length(){
		int rtn = 0;
		for(int week=0; week <2; week++){
			for(int day=0; day<7; day++){
				for(int slot=0;slot<scheduleTable[week][day].length;slot++){
					if(scheduleTable[week][day][slot] != null)
						rtn++;
				}
			}
		}
		return rtn;
	}
	
	public JSONObject[] getEventsAtDay(Calendar day){
		short week = (short) (day.get(Calendar.WEEK_OF_YEAR)%2);
		return scheduleTable[week][day.get(Calendar.DAY_OF_WEEK)-1];
	}
	
	public JSONObject[] getDaysComingEvents(Calendar after){
		Calendar actualTime = after;
		ArrayList<JSONObject> comingEvents = new ArrayList<JSONObject>();
		
		JSONObject[] eventsToday = this.getEventsAtDay(actualTime);
		for(short i=0; i<eventsToday.length; i++){
			Calendar startTime = (Calendar) actualTime.clone();
			startTime.set(Calendar.HOUR_OF_DAY, slots[i][0]);
			startTime.set(Calendar.MINUTE, slots[i][1]);
			startTime.set(Calendar.SECOND,0);
			startTime.set(Calendar.MILLISECOND, 0);
			if(eventsToday[i] != null && actualTime.before(startTime)){
				comingEvents.add(eventsToday[i]);
			}
		}
		
		return comingEvents.toArray(new JSONObject[0]);
		
	}
	public JSONObject getNextEvent(Calendar actualTime){
		JSONObject rtn = null;
		if(getDaysComingEvents(actualTime).length != 0){
			rtn = getDaysComingEvents(actualTime)[0];
		} else {
			// setting Calendar to hour 0, because there sure will be no event. (needed for non-skipping events)
			actualTime.set(Calendar.HOUR_OF_DAY, 0);
			while(rtn == null){
				actualTime.add(Calendar.DAY_OF_MONTH, 1);
				JSONObject[] eventsAtDay = getEventsAtDay(actualTime);
				for(JSONObject jso:eventsAtDay){
					if(jso != null && rtn == null)
						rtn = jso;
				}
			}
		}
		return rtn;
	}
	
	public JSONObject getNextEvent(){
		return getNextEvent(Calendar.getInstance());
	}
	
	public JSONObject getNextEventIncludingCurrent(){
		Calendar actualTime = Calendar.getInstance();
		
		JSONObject rtn = null;
		
		JSONObject[] currentDay = this.getEventsAtDay(actualTime);
		for(int i=0; i<currentDay.length; i++){
			Calendar end = (Calendar) actualTime.clone();
			end.set(Calendar.HOUR_OF_DAY, slots[i][0]);
			end.set(Calendar.MINUTE, slots[i][1]);
			end.set(Calendar.SECOND, 0);
			end.add(Calendar.MINUTE, 90);
			if(currentDay[i] != null && actualTime.before(end))
				return currentDay[i];
		}
		
		while(rtn == null){
			actualTime.add(Calendar.DAY_OF_MONTH, 1);
			JSONObject[] eventsAtDay = getEventsAtDay(actualTime);
			for(JSONObject jso:eventsAtDay){
				if(jso != null && rtn == null)
					rtn = jso;
			}
		}
		
		return rtn;
	}
	
	public Calendar getNextCalendar(JSONObject jso){
		for(short week = 0; week<2; week++){
			for(short day = 0; day<7; day++){
				for(short slot = 0; slot<slots.length; slot++){
					if(scheduleTable[week][day][slot] != null && 
							scheduleTable[week][day][slot].equals(jso)){
						Calendar current = Calendar.getInstance();
						current.set(Calendar.HOUR_OF_DAY, slots[slot][0]);
						current.set(Calendar.MINUTE, slots[slot][1]);
						current.set(Calendar.SECOND, 0);
						current.set(Calendar.MILLISECOND, 0);
						
						current.add(Calendar.DAY_OF_MONTH, day+1-current.get(Calendar.DAY_OF_WEEK));
						
						if(week != current.get(Calendar.WEEK_OF_YEAR)%2 && // Woche != soll-Woche
								(scheduleTable[~week &1][day][slot] == null || // und Rhytmus == 2-wöchig
								!scheduleTable[~week &1][day][slot].
									equals(scheduleTable[week][day][slot])) ||
								current.before(Calendar.getInstance()) && 
								current.get(Calendar.DAY_OF_YEAR) != 
									Calendar.getInstance().get(Calendar.DAY_OF_YEAR)){ // oder liegt in der vergangenheit
							current.add(Calendar.DAY_OF_MONTH, 7);
						} else if(Calendar.getInstance().after(current) &&
								week == current.get(Calendar.WEEK_OF_YEAR)%2 &&
								current.get(Calendar.DAY_OF_YEAR) != 
									Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
							current.add(Calendar.DAY_OF_MONTH, 14);
						}
						
						return current;
					}
				}
			}
		}
		return null;
	}
	
	public static String MD5(String toHash){
		String rtn = "";
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.reset();
			byte[] bytes = md.digest(toHash.getBytes());
			for(byte b:bytes)
				rtn += String.format("%02x", b&255);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rtn;
	}
}
