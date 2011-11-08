package michaels.spirit4android;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
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
			if(current.before(Calendar.getInstance()) || current.before(day)){
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
		String s = (day.get(Calendar.DAY_OF_YEAR) == 
				Calendar.getInstance().get(Calendar.DAY_OF_YEAR) ? "heute" : 
					"am "+DateFormat.getDateInstance(DateFormat.LONG).format(day.getTime()));
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
			p.setStyle(Paint.Style.STROKE);
			g.drawRect(0, 0, this.getWidth()/5, this.getHeight(), p);
			g.drawRect(this.getWidth()*(4f/5f), 0, this.getWidth(), this.getHeight(), p);
			p.setStyle(Paint.Style.FILL);
			p.setTextSize(40);
			String text = (day.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().
						get(Calendar.DAY_OF_YEAR) ?
					"heute":DateFormat.getDateInstance(DateFormat.SHORT).format(day.getTime()));
			Rect rC = new Rect();
			p.getTextBounds(text, 0, text.length(), rC);
			g.drawText(text, this.getWidth()/2-rC.width()/2, this.getHeight()/2+rC.height()/2, p);
			
			float size = (this.getWidth()/5 > this.getHeight() ? this.getHeight() : this.getWidth()/5)
					*0.8f;
			float w = this.getWidth();
			float h = this.getHeight();
			g.drawLines(new float[]{
					w/10+size/4, h/2-size/2, w/10+size/4, h/2+size/2, //Linkes Oben>Unten
					w/10+size/4, h/2+size/2, w/10-size/4, h/2, //Linkes Unten>Mitte
					w/10-size/4, h/2, w/10+size/4, h/2-size/2, //Linkes Mitte>Oben
					w*9/10-size/4, h/2-size/2, w*9/10-size/4, h/2+size/2, //Rectes Oben>Unten
					w*9/10-size/4, h/2+size/2, w*9/10+size/4, h/2, //Rechtes Unten>Mitte
					w*9/10+size/4, h/2, w*9/10-size/4, h/2-size/2 //Rechtes Mitte>Oben
			}, p);
		}		
	}
	
	public void onMeasure(int specWidth,int specHeight){
		int width = MeasureSpec.getSize(specWidth);
		Paint p = new Paint();
		p.setTextSize(20);
		
		day = (day == null ? schedule.getNextCalendar(schedule.getNextEventIncludingCurrent()) : day);
		JSONObject[] objects = schedule.getEventsAtDay(day);
		
		String longestString = "";
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
		if(event.getAction() == MotionEvent.ACTION_DOWN && event.getX() < this.getWidth()*(3/5f) &&
				event.getX() > this.getWidth()*(2/5f)){
			this.showChangeDay = true;
			this.invalidate();
			return true;
		} else if(event.getAction() == MotionEvent.ACTION_UP) {
			if(event.getX() < this.getWidth()/5){
				this.switchDayAutomatically = false;
				day.add(Calendar.DAY_OF_YEAR, -1);
				this.requestLayout();
				this.invalidate();
			} else if(event.getX() > this.getWidth()*4/5) {
				this.switchDayAutomatically = false;
				day.add(Calendar.DAY_OF_YEAR, 1);
				this.requestLayout();
				this.invalidate();
			}
			this.showChangeDay = false;
			this.invalidate();
			return true;
		}
		return false;
	}
	
}
