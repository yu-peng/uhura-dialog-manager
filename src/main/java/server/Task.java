package server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class Task {

	public enum TaskType{Full,Partial};
	
	private int index = 1;
	private TaskType type = null;
	private String name = null;
	private HashMap<String,String> description = null;
	
	private int order = 0;
	
	private long start_time = 0;
	private double duration = 0;
	private boolean relaxable = false;
	private ArrayList<Location> candidates = new ArrayList<Location>();

	public Task(String _name, TaskType _type){
		name = _name;
		type = _type;
	}
	
	public String getName(){
		return name;
	}
	
	public void setName(String _name){
		name = _name;
	}
	
	public TaskType getType(){
		return type;
	}
	
	public void setType(TaskType _type){
		type = _type;
	}
	
	public void setIndex(int _index){
		index = _index;
	}
	
	public int getIndex(){
		return index;
	}
	
	public void setDuration(double _duration){
		duration = _duration;
	}
	
	public double getDuration(){
		return duration;
	}
	
	public void setRelaxable(boolean _relaxable){
		relaxable = _relaxable;
	}
	
	public boolean isRelaxable(){
		return relaxable;
	}
	
	public void addCandidate(Location location){
		location.setIndex(candidates.size());
		candidates.add(location);
	}
	
	public void addCandidates(Collection<Location> locations){
		for (Location location : locations){
			addCandidate(location);
		}
	}
	
	public ArrayList<Location> getCandidates(){
		return candidates;
	}
	
	public void clearCandidates(){
		candidates.clear();
	}
	
	public void clearDescription(){
		description = null;
	}
	
	public void addDescription(String key, String value){
		if (description == null){
			description = new HashMap<String,String>();
		}
		
		description.put(key, value);
	}
	
	public JSONObject getJSONObject(){
		JSONObject obj = new JSONObject();
		
		try {
			
			obj.put("index", index);
			obj.put("type", type);
			obj.put("name", name);
			if (description != null){
				JSONObject descriptionObj = new JSONObject();

				for (String key : description.keySet()){
					descriptionObj.put(key, description.get(key));
				}
				
				obj.put("description", descriptionObj);
			}
			
			obj.put("order", order);
			obj.put("time", start_time);
			obj.put("duration", duration);
			obj.put("relaxable", relaxable);
			
			JSONArray candidatesObject = new JSONArray();
			
			for (Location candidate : candidates){
				candidatesObject.put(candidate.getJSONObject());
			}

			obj.put("candidates", candidatesObject);
		
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return obj;
	}
	
}
