package server;

import java.util.ArrayList;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import server.Task.TaskType;

public class Solution {

	private int index = 1;
	private JSONArray activities = new JSONArray();
	private JSONArray ccRelaxations = new JSONArray();
	private JSONArray temporalRelaxations = new JSONArray();
	private JSONArray semanticRelaxations = new JSONArray();
	private JSONArray pois = new JSONArray();
	private JSONArray decisions = new JSONArray();

	private String departure_constraint_id = null;
	private String departure_time = null;
	private String absolute_departure_time = null;	
	
	private String arrival_constraint_id = null;
	private String arrival_time = null;
	private String absolute_arrival_time = null;	
	
	public Solution(JSONObject obj){
		
		try {			

			if (obj.has("departureTime")){
				departure_constraint_id = obj.getString("departureConstraint");
				departure_time = obj.getString("departureTime");
				absolute_departure_time = obj.getString("absoluteDepartureTime");
			}
			
			if (obj.has("arrivalTime")){
				arrival_constraint_id = obj.getString("arrivalConstraint");
				arrival_time = obj.getString("arrivalTime");
				absolute_arrival_time = obj.getString("absoluteArrivalTime");
			}
			
			activities = obj.getJSONArray("activities");
			temporalRelaxations = obj.getJSONArray("temporalRelaxations");
			semanticRelaxations = obj.getJSONArray("semanticRelaxations");
			ccRelaxations = obj.getJSONArray("ccRelaxations");
			pois = obj.getJSONArray("pois");
			decisions = obj.getJSONArray("decisions");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setIndex(int _index){
		index = _index;
	}
	
	public int getIndex(){
		return index;
	}
	
	public String getDepartureConstraintID(){
		return departure_constraint_id;
	}
	
	public String getArrivalConstraintID(){
		return arrival_constraint_id;
	}
	
	public JSONArray getTemporalRelaxations(){
		return temporalRelaxations;
	}
	
	public JSONArray getCCRelaxations(){
		return ccRelaxations;
	}
	
	public JSONArray getDecisions(){
		return decisions;
	}
	
	public JSONArray getActivities(){
		return activities;
	}
	
	public JSONObject getJSONObject(){
		JSONObject obj = new JSONObject();
		
		try {
			
			if (departure_time != null){
				obj.put("departureConstraint", departure_constraint_id);
				obj.put("departureTime", departure_time);
				obj.put("absoluteDepartureTime", absolute_departure_time);
			}
			
			if (arrival_time != null){
				obj.put("arrivalConstraint", arrival_constraint_id);
				obj.put("arrivalTime", arrival_time);
				obj.put("absoluteArrivalTime", absolute_arrival_time);
			}
			
			obj.put("decisions", decisions);
			obj.put("ccRelaxations", ccRelaxations);
			obj.put("temporalRelaxations", temporalRelaxations);
			obj.put("semanticRelaxations", semanticRelaxations);
			obj.put("activities", activities);
			obj.put("pois", pois);
		
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return obj;
	}
	
	public String getDescription(ArrayList<String> prevDecisions, 
			ArrayList<String> prevCCRelaxations,
			ArrayList<String> prevTemporalRelaxations,
			ArrayList<String> prevSemanticRelaxations){

		StringBuilder sb = new StringBuilder();
		
		if (decisions.length() > 0){
			
			ArrayList<String> currDecisions = new ArrayList<String>();
			
			for (int i = 0; i < decisions.length(); i++){
				try {
					JSONObject decision = decisions.getJSONObject(i);
					if (!prevDecisions.contains(decision.getString("description"))){
						currDecisions.add(decision.getString("description"));
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
			
			if (currDecisions.size() > 0){
				sb.append("You can go to ");
				for (int i = 0; i < currDecisions.size(); i++){
					String currDecision = currDecisions.get(i);
					if (i == currDecisions.size() - 1 && i > 0) {
						sb.append("and " + currDecision + ". ");
					} else {
						sb.append(currDecision + "; ");
					}
					
				}
			}
			
			prevDecisions.clear();
			prevDecisions.addAll(currDecisions);
		} else {
			prevDecisions.clear();
		}
		
		if (ccRelaxations.length() == 0 && temporalRelaxations.length() == 0 && prevTemporalRelaxations.size() > 0){
			sb.append("You will be on time for everything .");
		}
		
		boolean foundRelaxation = false;
			
		if (semanticRelaxations.length() > 0){
			
			ArrayList<String> currSemanticRelaxations = new ArrayList<String>();

			for (int i = 0; i < semanticRelaxations.length(); i++){
				try {
					JSONObject semanticRelaxation = semanticRelaxations.getJSONObject(i);
					if (!prevSemanticRelaxations.contains(semanticRelaxation.getString("description"))){
						currSemanticRelaxations.add(semanticRelaxation.getString("description"));
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
			
			for (int i = 0; i < currSemanticRelaxations.size(); i++){
				if (!foundRelaxation){
					sb.append("However, you need to ");
					foundRelaxation = true;
				}
				
				if (i == semanticRelaxations.length() - 1 && i > 0) {
					sb.append("and " + currSemanticRelaxations.get(i) + ". ");
				} else {
					sb.append(currSemanticRelaxations.get(i) + "; ");
				}			
			}
			
			prevSemanticRelaxations.clear();
			prevSemanticRelaxations.addAll(currSemanticRelaxations);
		} else {
			prevSemanticRelaxations.clear();
		}
		
		if (temporalRelaxations.length() > 0){
			
			boolean connectorPresented = false;
			
			for (int i = 0; i < temporalRelaxations.length(); i++){
				
				try {
					JSONObject temporalRelaxation = temporalRelaxations.getJSONObject(i);
					String constraintID = temporalRelaxation.getString("id");
					
					if (Manager.agreedTemporalRelaxations.contains(constraintID)){
						continue;
					}
					
					if (!connectorPresented){
						if (foundRelaxation){
							sb.append("In addition, ");
						} else {
							sb.append("However, you need to ");
							foundRelaxation = true;
						}
						connectorPresented = true;
					}					
					
					if (i == temporalRelaxations.length() - 1 && i > 0) {
						sb.append("and " + temporalRelaxation.getString("description") + ". ");
					} else {
						sb.append(temporalRelaxation.getString("description") + "; ");
					}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			prevTemporalRelaxations.clear();
			prevTemporalRelaxations.add(temporalRelaxations.toString());
		} else {
			prevTemporalRelaxations.clear();
		}
		
		prevCCRelaxations.clear();
		if (ccRelaxations.length() > 0){
			for (int i = 0; i < ccRelaxations.length(); i++){
				try {
					JSONObject ccRelaxation = ccRelaxations.getJSONObject(i);	
					sb.append(ccRelaxation.getString("description") + ". ");
					prevCCRelaxations.add(ccRelaxation.getString("description"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		

		return sb.toString();
	}
}
