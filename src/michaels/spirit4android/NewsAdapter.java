package michaels.spirit4android;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

public class NewsAdapter implements ListAdapter {

	SQLiteDatabase db;
	Context context;
	ArrayList<Long> contents = new ArrayList<Long>();
	ArrayList<String> content_titles = new ArrayList<String>();
	ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();
	
	
	public NewsAdapter(Context c, SQLiteDatabase db){
		context = c;
		this.db = db;
		this.updateData();
	}
	
	public void updateData(){
		contents.clear();
		content_titles.clear();
		Cursor cursor = db.rawQuery("SELECT id, title, receivers FROM news ORDER BY date DESC", null);
		cursor.moveToFirst();
		String searching_for_semester = mainActivity.saveFile.getString("semester","");
		searching_for_semester = searching_for_semester.toLowerCase();
		searching_for_semester = (searching_for_semester.startsWith("ba") ? searching_for_semester.substring(2) : searching_for_semester);
		while(!cursor.isAfterLast()){
			String[] current_receivers = cursor.getString(cursor.getColumnIndex("receivers")).split(" ");
			if(!searching_for_semester.equals("")){
				for(String s:current_receivers){
					s = s.toLowerCase();
					if(s.equals(searching_for_semester) || s.equals("") || s.equals("semester")){
						contents.add(cursor.getLong(cursor.getColumnIndex("id")));
						content_titles.add(cursor.getString(cursor.getColumnIndex("title")));
						break;
					}
				}
			} else {
				contents.add(cursor.getLong(cursor.getColumnIndex("id")));
				content_titles.add(cursor.getString(cursor.getColumnIndex("title")));
			}
			cursor.moveToNext();
		}
		cursor.close();
		
		for(DataSetObserver o:observers){
			o.onChanged();
		}
	}
	
	@Override
	public int getCount() {
		return contents.size();
	}

	@Override
	public Object getItem(int arg0) {
		return content_titles.get(arg0);
	}

	@Override
	public long getItemId(int arg0) {
		return contents.get(arg0);
	}

	@Override
	public int getItemViewType(int arg0) {
		return 0;
	}

	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		TextView tv = (TextView)arg1;
		if(tv == null){
			LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			tv = (TextView) li.inflate(R.layout.listelement, null);
		}
		tv.setText(content_titles.get(arg0));
		return tv;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return contents.isEmpty();
	}

	@Override
	public void registerDataSetObserver(DataSetObserver arg0) {
		observers.add(arg0);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver arg0) {
		observers.remove(arg0);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int arg0) {
		return true;
	}

}
