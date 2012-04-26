package michaels.spirit4android;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class FHSSchedule {
	SQLiteDatabase db;
	
	public static final byte EVENT_LECTURE = 0;
	public static final byte EVENT_EXERCISE = 1;
	
	public static final byte EVENT_RYTHM_EVEN = 1;
	public static final byte EVENT_RYTHM_ODD = 2;
	public static final byte EVENT_RYTHM_WEEKLY = 3;
	
	final static String[] dayNames = new String[]{
			"Sonntag",
			"Montag",
			"Dienstag",
			"Mittwoch",
			"Donnerstag",
			"Freitag",
			"Samstag"
		};
	
	public class Event {
		long time;
		long length;
		byte week;
		byte group;
		byte type;
		String title;
		String docent;
		String room;
	}
	
	public FHSSchedule(Context c){
		db = mainActivity.database;
		if(db == null || !db.isOpen()){
			mainActivity.openDB(c);
			db = mainActivity.database;
		}
	}
	
	public int length(){
		Cursor c = db.rawQuery("SELECT * FROM schedule",null);
		int rtn = c.getCount();
		c.close();
		return rtn;
	}
	
	public Event[] getEventsAtDay(Calendar day){
		long start = day.get(Calendar.DAY_OF_WEEK)*24*60*60;
		
		ArrayList<Event> rtn = new ArrayList<Event>();
		Cursor c = db.rawQuery("SELECT * FROM schedule WHERE (time BETWEEN "+start+" AND "+(start+24*60*60)+") AND ( week & "+(day.get(Calendar.WEEK_OF_YEAR) %2 +1)+") != 0 ORDER BY time", null);
		c.moveToFirst();
		while(!c.isAfterLast()){
			boolean add = true;
			if(c.getInt(c.getColumnIndex("egroup")) > 0){
				Cursor d = db.rawQuery("SELECT egroup FROM groups WHERE type = "+c.getInt(c.getColumnIndex("type"))+" AND title = '"+c.getString(c.getColumnIndex("title"))+"'",null);
				d.moveToFirst();
				if(d.isAfterLast() || d.getInt(d.getColumnIndex("egroup")) != c.getInt(c.getColumnIndex("egroup")))
					add = false;
				d.close();
			}
			
			if(add){
				Event ev = new Event();
				ev.docent = c.getString(c.getColumnIndex("docent"));
				ev.group = (byte) c.getInt(c.getColumnIndex("egroup"));
				ev.length = c.getLong(c.getColumnIndex("length"));
				ev.room = c.getString(c.getColumnIndex("room"));
				ev.time = c.getLong(c.getColumnIndex("time"));
				ev.title = c.getString(c.getColumnIndex("title"));
				ev.type = (byte) c.getInt(c.getColumnIndex("type"));
				ev.week = (byte) c.getInt(c.getColumnIndex("week"));
				rtn.add(ev);
			}
			c.moveToNext();
		}
		c.close();
		
		return rtn.toArray(new Event[0]);
	}
	
	public Event[] getDaysComingEvents(Calendar after){
		Calendar actualTime = after;
		ArrayList<Event> rtn = new ArrayList<Event>();
		
		Event[] eventsToday = this.getEventsAtDay(actualTime);
		for(short i=0; i<eventsToday.length; i++){
			Calendar startTime = (Calendar) actualTime.clone();
			startTime.set(Calendar.HOUR_OF_DAY, 0);
			startTime.set(Calendar.MINUTE, 0);
			startTime.set(Calendar.SECOND,0);
			startTime.add(Calendar.DAY_OF_YEAR, -startTime.get(Calendar.DAY_OF_WEEK));
			startTime.add(Calendar.SECOND, (int) eventsToday[i].time);
			if(actualTime.before(startTime)){
				rtn.add(eventsToday[i]);
			}
		}
		
		return rtn.toArray(new Event[0]);
		
	}
	
	public Event getNextEvent(Calendar actualTime, Calendar ttl){
		Event rtn = null;
		if(getDaysComingEvents(actualTime).length != 0){
			rtn = getDaysComingEvents(actualTime)[0];
		} else {
			// setting Calendar to hour 0, because there sure will be no event. (needed for non-skipping events)
			actualTime.set(Calendar.HOUR_OF_DAY, 0);
			while(rtn == null && actualTime.before(ttl)){
				actualTime.add(Calendar.DAY_OF_MONTH, 1);
				Event[] eventsAtDay = getEventsAtDay(actualTime);
				if(eventsAtDay.length > 0)
					rtn = eventsAtDay[0];
			}
		}
		return rtn;
	}
	
	public Event getNextEvent(Calendar actualTime){
		Calendar ttl = (Calendar) actualTime.clone();
		ttl.add(Calendar.DAY_OF_YEAR, 14);
		return getNextEvent(actualTime,ttl);
	}
	
	public Event getNextEvent(){
		return getNextEvent(Calendar.getInstance());
	}
	
	public Event getNextEventIncludingCurrent(){
		Calendar actualTime = Calendar.getInstance();
		Calendar week_start = (Calendar) actualTime.clone();
		week_start.set(Calendar.HOUR_OF_DAY, 0);
		week_start.set(Calendar.MINUTE, 0);
		week_start.set(Calendar.SECOND, 0);
		week_start.add(Calendar.DAY_OF_YEAR,-week_start.get(Calendar.DAY_OF_WEEK));
		long w_s = week_start.getTimeInMillis();
		
		Event[] currentDay = this.getEventsAtDay(actualTime);
		for(int i=0; i<currentDay.length; i++){
			long start = w_s+(currentDay[i].time *1000);
			long end = start+(currentDay[i].length *1000);
			if(System.currentTimeMillis()<end && System.currentTimeMillis()>start)
				return currentDay[i];
		}		
		// no event today. return next.
		return getNextEvent();
	}
	
	public Calendar getCalendar(Event jso, boolean current){
		if(jso == null){
			throw new NullPointerException();
		}
		Calendar rtn = Calendar.getInstance();
		rtn.add(Calendar.DAY_OF_YEAR, -rtn.get(Calendar.DAY_OF_WEEK));
		rtn.set(Calendar.HOUR_OF_DAY, 0);
		rtn.set(Calendar.MINUTE, 0);
		rtn.set(Calendar.SECOND, 0);
		rtn.set(Calendar.MILLISECOND, 0);
		rtn.add(Calendar.SECOND, (int) jso.time);
		long systime = System.currentTimeMillis();
		if(rtn.before(Calendar.getInstance()) && (!current || !(rtn.getTimeInMillis() < systime && rtn.getTimeInMillis()+jso.length*1000 > systime))) 
			rtn.add(Calendar.DAY_OF_YEAR,7);
		if(((rtn.get(Calendar.WEEK_OF_YEAR)%2+1)&jso.week) == 0)
			rtn.add(Calendar.DAY_OF_YEAR, 7);
		return rtn;
	}
	
	public Calendar getNextCalendar(Event e){
		return getCalendar(e,false);
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
	
	public static void parsePlan(JSONArray input){
		if(input == null || mainActivity.database == null)
			throw new NullPointerException();
		
		try {
			mainActivity.database.execSQL("DELETE FROM schedule");
			for(int i=0; i<input.length(); i++ ){
				JSONObject current = input.getJSONObject(i);
				JSONObject appointment = current.getJSONObject("appointment");
				
				String pre_time = appointment.getString("time");
				long time = (Arrays.asList(dayNames).indexOf(appointment.get("day"))+1)*24*60*60; // Days
				long length = time;
				time += Integer.parseInt(pre_time.substring(0,2))*60*60 + //Hours -> start
						Integer.parseInt(pre_time.substring(3,5))*60; // Minutes -> start
				length += Integer.parseInt(pre_time.substring(6,8))*60*60 + //Hours -> length
						Integer.parseInt(pre_time.substring(9))*60; // Minutes -> length
				length = length - time;
				
				String pre_week = appointment.getString("week").trim();
				byte week = (pre_week.equals("w") ? EVENT_RYTHM_WEEKLY : (pre_week.equals("g") ? EVENT_RYTHM_EVEN : EVENT_RYTHM_ODD));
				
				JSONObject pre_room = appointment.getJSONObject("location").getJSONObject("place");
				String room = pre_room.getString("building")+pre_room.getString("room");
				
				byte type = (current.getString("eventType").equals("Vorlesung") ? EVENT_LECTURE : EVENT_EXERCISE);
				
				String pre_group = current.getString("group").replaceAll("[^0-9]", "");
				byte group = (pre_group.equals("") ? 0 : Byte.parseByte(pre_group));
				
				mainActivity.database.execSQL("INSERT INTO schedule (time, length, egroup, week, room, title, docent, type) VALUES ("+
				time+","+length+","+group+", "+week+", '"+room+"', '"+current.getString("titleShort")+"', '"+current.getJSONArray("member").getJSONObject(0).getString("name")+"', "+type+")");
			}
		} catch(Exception e){
			Log.e("FHSSchedule-Parser", "Invalid JSON!");
		}
	}
}
