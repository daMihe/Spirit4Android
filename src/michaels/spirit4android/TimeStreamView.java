package michaels.spirit4android;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class TimeStreamView extends View implements OnTouchListener{
	Context context;
	FHSSchedule schedule;
	float textSize = 0f;
	Calendar day;
	boolean showChangeDay = false;
	boolean switchDayAutomatically = true;
	float touchDown = -1;
	float touchMove = -1;
	public TimeStreamView(Context context,FHSSchedule schedule) {
		super(context);
		this.context = context;
		this.schedule = schedule;
		this.setOnTouchListener(this);
	}
	
	public void onDraw(Canvas g){
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(0xff000000);
		p.setStyle(Paint.Style.FILL);
		g.drawPaint(p);
		
		day.set(Calendar.HOUR_OF_DAY, 0);
		day.set(Calendar.MINUTE, 0);
		//Calendar nextEvent = schedule.getNextCalendar(schedule.getNextEvent());
		if(day.get(Calendar.DAY_OF_YEAR) != schedule.getNextCalendar(
				schedule.getNextEventIncludingCurrent()).get(Calendar.DAY_OF_YEAR) &&
				this.switchDayAutomatically){
			day = null;
			this.requestLayout();
			this.invalidate();
			return;
		}
		JSONObject[] eventsAtDay = schedule.getEventsAtDay(day);
		ArrayList<JSONObject> events = new ArrayList<JSONObject>();
		for(short i = 0; i<eventsAtDay.length; i++)
			if(eventsAtDay[i] != null)
				events.add(eventsAtDay[i]);
		int heightOfStripe = ((this.getHeight()-events.size())/(events.size()+1));
		p.setTextSize(textSize);
		
		for(int i=0; i<events.size(); i++){
			JSONObject jso = events.get(i);
			Calendar current = schedule.getNextCalendar(jso);
			float start = i*(1+heightOfStripe);
			float end = start+heightOfStripe;
			p.setColor(0xff00386a);
			g.drawRect(0, start, this.getWidth(), end, p);

			p.setColor(0xffffffff);
			JSONObject jsoPlace = jso.optJSONObject("appointment").optJSONObject("location").
					optJSONObject("place");
			String toWrite = String.format("%02d:%02d", current.get(Calendar.HOUR_OF_DAY),
						current.get(Calendar.MINUTE))+
					"/"+jsoPlace.optString("building")+jsoPlace.optString("room")+": "+
					jso.optString("titleShort")+" ("+jso.optString("eventType")+")";
			g.drawText(toWrite, 0, end-p.descent(), p);
			current.set(Calendar.DAY_OF_YEAR, day.get(Calendar.DAY_OF_YEAR));
			if(current.before(Calendar.getInstance())){
				Calendar endOfEvent = (Calendar) current.clone();
				endOfEvent.add(Calendar.MINUTE, 90);
				endOfEvent = (Calendar.getInstance().before(endOfEvent) ? 
						Calendar.getInstance():endOfEvent);
				float timeUp = this.getWidth() * 
						((endOfEvent.get(Calendar.HOUR_OF_DAY)-current.get(Calendar.HOUR_OF_DAY))*3600f+
						(endOfEvent.get(Calendar.MINUTE)-current.get(Calendar.MINUTE))*60f+
						(endOfEvent.get(Calendar.SECOND)-current.get(Calendar.SECOND)))/(90f*60f);
				p.setColor(0xaa000000);
				g.drawRect(0, start, timeUp, end, p);
			}
		}
		p.setColor(0xffaaaaaa);
		String s = "";
		int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
		int doy = day.get(Calendar.DAY_OF_YEAR);
		if(doy == today)
			s = context.getString(R.string.LANG_TODAY);
		else if(doy == today-1 || (doy == day.getActualMaximum(Calendar.DAY_OF_YEAR) && today == 0))
			s = context.getString(R.string.LANG_YESTERDAY);
		else if(doy == today+1 || (doy == 0 && Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_YEAR) == today))
			s = context.getString(R.string.LANG_TOMORROW);
		else
			s = context.getString(R.string.LANG_ON_DAY)+" "+DateFormat.getDateInstance(DateFormat.LONG).format(day.getTime());
		Rect r = new Rect();
		p.getTextBounds(s, 0, s.length(), r);
		g.drawText(s, this.getWidth()-r.width(), this.getHeight()-p.descent(), p);
		if(events.size() == 0){
			p.setColor(0xffffffff);
			g.drawText("keine Veranstaltung", 0, r.height()+p.descent(), p);
		}
		
		// ggf. touch-buttons zeichnen
		if(this.showChangeDay){
			p.setColor(0xccffffff);
			g.drawRect(0, 0, this.getWidth(), this.getHeight(), p);
			p.setColor(0xff000000);
			p.setTextSize(35);
			Calendar dayBeforeCalendar = (Calendar) day.clone();
			dayBeforeCalendar.add(Calendar.DAY_OF_YEAR, -1);
			Calendar dayAfterCalendar = (Calendar) day.clone();
			dayAfterCalendar.add(Calendar.DAY_OF_YEAR, 1);
			String dayBefore = (dayBeforeCalendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().
					get(Calendar.DAY_OF_YEAR) ? "heute" : DateFormat.getDateInstance(DateFormat.SHORT).
							format(dayBeforeCalendar.getTime()));
			String dayCurrent = (day.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().
					get(Calendar.DAY_OF_YEAR) ? "heute" : DateFormat.getDateInstance(DateFormat.SHORT).
							format(day.getTime()));
			String dayAfter = (dayAfterCalendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().
					get(Calendar.DAY_OF_YEAR) ? "heute" : DateFormat.getDateInstance(DateFormat.SHORT).
							format(dayAfterCalendar.getTime()));
			Rect mr = new Rect();
			p.setTextSize(this.touchMove-this.touchDown > this.getWidth()/3 ? 45 : 35);
			p.setColor(this.touchMove-this.touchDown > this.getWidth()/3 ? 0xff000000 : 0xaa000000);
			p.getTextBounds(dayBefore, 0, dayBefore.length(), mr);
			g.drawText(dayBefore, (this.touchMove-this.touchDown)-mr.width()/2, this.getHeight()/2+
					mr.height()/2, p);
			p.setTextSize(this.touchMove-this.touchDown < this.getWidth()/3 &&
					this.touchDown-this.touchMove < this.getWidth()/3? 45 : 35);
			p.setColor(this.touchMove-this.touchDown < this.getWidth()/3 &&
					this.touchDown-this.touchMove < this.getWidth()/3 ? 0xff000000 : 0xaa000000);
			p.getTextBounds(dayCurrent, 0, dayCurrent.length(), mr);
			g.drawText(dayCurrent, (this.touchMove-this.touchDown)+(this.getWidth()/2)-mr.width()/2, 
					this.getHeight()/2+mr.height()/2, p);
			p.setTextSize(this.touchDown-this.touchMove > this.getWidth()/3 ? 45 : 35);
			p.setColor(this.touchDown-this.touchMove > this.getWidth()/3 ? 0xff000000 : 0xaa000000);
			p.getTextBounds(dayAfter, 0, dayAfter.length(), mr);
			g.drawText(dayAfter, (this.touchMove-this.touchDown)+(this.getWidth())-mr.width()/2, 
					this.getHeight()/2+mr.height()/2, p);
		}		
	}
	
	public void onMeasure(int specWidth,int specHeight){
		int width = MeasureSpec.getSize(specWidth);
		Paint p = new Paint();
		p.setTextSize(20);
		
		day = (day == null ? schedule.getNextCalendar(schedule.getNextEventIncludingCurrent()) : day);
		JSONObject[] objects = schedule.getEventsAtDay(day);
		
		String longestString = DateFormat.getDateInstance(DateFormat.LONG).format(day.getTime());
		longestString = (longestString.length() < "keine Veranstaltung".length() ? 
				"keine Veranstaltung" : longestString);
		int widthOfLongest = 0;
		short count = 0;
		for(JSONObject currentEvent:objects){
			if(currentEvent != null){
				count++;
				JSONObject currentPlace = currentEvent.optJSONObject("appointment").
							optJSONObject("location").optJSONObject("place");
				String toMeasure = DateFormat.getTimeInstance(DateFormat.SHORT).format(schedule.
						getNextCalendar(currentEvent).getTime())+"/"+currentPlace.optString("building")+
						currentPlace.optString("room")+": "+currentEvent.optString("titleShort")+" ("+
						currentEvent.optString("eventType")+")";
				Rect r = new Rect();
				p.getTextBounds(toMeasure, 0, toMeasure.length(), r);
				if(widthOfLongest < r.width()){
					widthOfLongest = r.width();
					longestString = toMeasure;
				}
			}
		}
		Rect r = new Rect();			
		p.setTextSize(1f);
		p.getTextBounds(longestString, 0, longestString.length(), r);
		while(r.width() < width && p.getTextSize() < 31){
			p.setTextSize(p.getTextSize()+1f);
			p.getTextBounds(longestString, 0, longestString.length(), r);
		}
		p.setTextSize(p.getTextSize()-1f); // Da sonst Überschuss
		p.getTextBounds(longestString, 0, longestString.length(), r);
		textSize = p.getTextSize();
		this.setMeasuredDimension(width, (0-((int)p.ascent())+(int)p.descent())*(count += (count == 0 ? 2 : 1))+(count-1));
	}

	public boolean onTouch(View v, MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_DOWN){
			this.showChangeDay = true;
			this.touchDown = this.touchMove = event.getX();
			this.invalidate();
			return true;
		} else if(event.getAction() == MotionEvent.ACTION_UP) {
			if(event.getX() > this.touchDown+this.getWidth()/3){
				this.switchDayAutomatically = false;
				day.add(Calendar.DAY_OF_YEAR, -1);
				this.requestLayout();
				this.invalidate();
			} else if(event.getX() < this.touchDown-this.getWidth()/3) {
				this.switchDayAutomatically = false;
				day.add(Calendar.DAY_OF_YEAR, 1);
				this.requestLayout();
				this.invalidate();
			}
			this.showChangeDay = false;
			this.invalidate();
			return true;
		} else if(event.getAction() == MotionEvent.ACTION_MOVE){
			this.touchMove = event.getX();
			this.invalidate();
			return true;
		}
		return false;
	}
	
}
