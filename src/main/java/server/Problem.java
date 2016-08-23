package server;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Problem {

	public enum TransitMode{driving,biking,walking};
	
	private String userID = null;
	private TransitMode mode = null;	
	private Location origin = null;
	private Location destination = null;

	private ArrayList<Task> activities = new ArrayList<Task>();

	public Problem(String _userID, TransitMode _mode){
		userID = _userID;
		mode = _mode;
	}
	
	public void setOrigin(Location _origin){
		origin = _origin;
	}
	
	public Location getOrigin(){
		return origin;
	}
	
	public void setDestination(Location _destination){
		destination = _destination;
	}
	
	public Location getDestination(){
		return destination;
	}
	
	public void addActivity(Task _activity){
		_activity.setIndex(activities.size());
		activities.add(_activity);
	}
	
	public ArrayList<Task> getActivities(){
		return activities;
	}
	
	public Task removeLastActivity(){
		if (activities.isEmpty()){
			return null;
		}
		return activities.remove(activities.size()-1);
	}
	
	public Task getLastActivity(){
		
		if (activities.isEmpty()){
			return null;
		}
		
		return activities.get(activities.size()-1);
	}
	
	public String getUserID(){
		return userID;
	}
	
	public JSONObject getJSONObject(){
		JSONObject obj = new JSONObject();
		
		try {
			
			obj.put("email", userID);
			obj.put("mode", mode.toString());
			if (origin != null){
				obj.put("origin", origin.getJSONObject());
			}
			if (destination != null){
				obj.put("destination", destination.getJSONObject());
			}
			
			JSONArray activityArray = new JSONArray();
			for (Task activity : activities){
				activityArray.put(activity.getJSONObject());
			}
			
			// Add a dummy activity for stop at South station
//			JSONObject dummyActivity = new JSONObject();
//			dummyActivity.put("index", 0);
//			dummyActivity.put("type", 2);
//			dummyActivity.put("name", "South Station, Boston, MA, United States");
//			dummyActivity.put("description", "South Station, Boston, MA, United States");
//			dummyActivity.put("order", 0);
//			dummyActivity.put("time", 0);
//			dummyActivity.put("duration", 30);
//			dummyActivity.put("relaxable", true);
//			
//			JSONArray candidates = new JSONArray();
//			JSONObject dummyCandidate = new JSONObject();
//			dummyCandidate.put("index", 0);
//			dummyCandidate.put("name", "South Station, Boston, MA, United States");
//			dummyCandidate.put("description", "");
//			dummyCandidate.put("lat", 42.35187759999999);
//			dummyCandidate.put("lon", -71.05510420000002);
//			dummyCandidate.put("preferred", false);
//			candidates.put(dummyCandidate);
//			dummyActivity.put("candidates", candidates);
//
//			activityArray.put(dummyActivity);
			obj.put("activities", activityArray);
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return obj;
	}
	
}
