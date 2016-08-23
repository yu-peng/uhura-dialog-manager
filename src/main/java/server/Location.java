package server;

import java.util.HashMap;
import java.util.UUID;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Location {
	
	private String name = null;
	private String id = UUID.randomUUID().toString();
	
	private String address = null;
	private double latitude = 0;
	private double longitude = 0;	
	
	private String departure_time = null;
	private String arrival_time = null;
	
	private boolean relaxable = false;
	private boolean preferred = false;
	
	private int index = 0;

	public Location(String _name, double _latitude, double _longitude){
		name = _name;
		latitude = _latitude;
		longitude = _longitude;
	}
	
	public Location(HashMap<String, String> sqlData){
		name = sqlData.get("name");
		latitude = Double.parseDouble(sqlData.get("latitude"));
		longitude = Double.parseDouble(sqlData.get("longitude"));
		address = sqlData.get("address"); 
	}
	
	public String getName(){
		return name;
	}
	
	public void setIndex(int _index){
		index = _index;
	}
	
	public int getIndex(){
		return index;
	}
	
	public double getLatitude(){
		return latitude;
	}
	
	public double getLongitude(){
		return longitude;
	}
	
	public String getAddress(){
		return address;
	}
	
	public void setDepartureTime(String _time){
		departure_time = _time;
	}
	
	public String getDepartureTime(){
		return departure_time;
	}
	
	public void setArrivalTime(String _time){
		arrival_time = _time;
	}
	
	public String getArrivalTime(){
		return arrival_time;
	}
	
	public void setRelaxable(boolean _relaxable){
		relaxable = _relaxable;
	}
	
	public boolean isRelaxable(){
		return relaxable;
	}
	
	public JSONObject getJSONObject(){
		JSONObject obj = new JSONObject();
		
		try {
			obj.put("name", name);
			obj.put("id", id);
			obj.put("index", index);
			obj.put("latitude", latitude);
			obj.put("longitude", longitude);
			obj.put("departure-time", departure_time);
			obj.put("departure-date", "");
			obj.put("arrival-time", arrival_time);
			obj.put("relaxable", relaxable);
			obj.put("preferred", preferred);
		
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return obj;
	}
	
}
