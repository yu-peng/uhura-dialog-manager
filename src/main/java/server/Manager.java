package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import mysql.MySQLAccess;
import server.Problem.TransitMode;
import server.Task.TaskType;

@Path("/Manager")
public class Manager {
	
	public static String currentSession = null;
	public static int currentSessionState = Manager.START;
	
	public static Problem currentPlanningProblem = null;
	public static ArrayList<Solution> allSolutions = new ArrayList<Solution>();
	public static Solution currentSolution = null;
	public static Location tripOrigin = null;
	public static Location tripDestination = null;
	
	public static ArrayList<String> prevDecisions = new ArrayList<String>();	
	public static ArrayList<String> prevTemporalRelaxations = new ArrayList<String>();	
	public static ArrayList<String> prevSemanticRelaxations = new ArrayList<String>();	
	public static HashSet<String> agreedTemporalRelaxations = new HashSet<String>();	

	public static int START = 0;
	public static int GOAL_COLLECTION = 1;
	public static int PLANNING = 2;
	public static int SOLUTION_FOUND = 3;
	public static int SOLUTION_NOT_FOUND = 4;
	
	public static boolean problemChanged = true;
	static int departureHour = 18; // 18:00
	static int departureMinute = 0; // 18:00

	public static String uhuraURI = "http://uhura.csail.mit.edu:8080/uhura-web-interface/planner/";
	
	public static void main(String[] args){
		int departureTime = 18*60; // 18:00
		
		System.out.println(String.format("%02d", departureTime/60) + ":" + String.format("%02d", departureTime%60));
	}
	
	public static void resetManagerState(){
		currentSession = null;
		currentSessionState = Manager.START;
		
		currentPlanningProblem = new Problem("Echo",TransitMode.walking);
		currentSolution = null;
		allSolutions.clear();
		problemChanged = true;
		prevDecisions = new ArrayList<String>();	
		prevTemporalRelaxations = new ArrayList<String>();
		prevSemanticRelaxations = new ArrayList<String>();
		agreedTemporalRelaxations = new HashSet<String>();
		
		tripOrigin = new Location("MIT",42.361602, -71.090568);
//		tripOrigin = new Location("Hilton Midtown",40.762283, -73.979691);
				
		tripOrigin.setDepartureTime(String.format("%02d", departureHour) + ":" + String.format("%02d", departureMinute));
		tripOrigin.setRelaxable(true);
		currentPlanningProblem.setOrigin(tripOrigin);
		
//		Task newTask1 = new Task("Amtrak Station",TaskType.Full);
//		newTask1.setDuration(30);
//		newTask1.setRelaxable(true);
//		Location newLocation1 = new Location("South Station",42.35187759999999,-71.05510420000002);
//		newTask1.addCandidate(newLocation1);
//		Location newLocation2 = new Location("Back Bay",42.347340, -71.075985);
//		newTask1.addCandidate(newLocation2);
//		currentPlanningProblem.addActivity(newTask1);
//		
//		Task newTask2 = new Task("Chinese Restaurant",TaskType.Full);
//		newTask2.setDuration(30);
//		newTask2.setRelaxable(true);
//		Location newLocation3 = new Location("Mary Chung",42.363660, -71.100967);
//		newTask2.addCandidate(newLocation3);
//		Location newLocation4 = new Location("Shanghai Fresh",42.366342, -71.105078);
//		newTask2.addCandidate(newLocation4);
//		currentPlanningProblem.addActivity(newTask2);
	}
	
	@Path("/ResetSession")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject resetSession() {
		
		JSONObject result = new JSONObject();
		resetManagerState();
		
		try {
			result.put("text_output", "Session Reset");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}

	@Path("/AddGoal")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject addGoal(
			@DefaultValue("") @QueryParam("destination") String destination,
			@DefaultValue("") @QueryParam("prep") String prep,
			@DefaultValue("") @QueryParam("duration") String duration,
			@DefaultValue("") @QueryParam("time") String time,
			@DefaultValue("") @QueryParam("cuisine") String cuisine,
			@DefaultValue("") @QueryParam("genre") String genre,
			@DefaultValue("") @QueryParam("food") String food) {
		
		JSONObject result = new JSONObject();

		if (destination.isEmpty() && food.isEmpty()){
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid task from your input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return result;
			
		} else {
			
			// OK, we have got destination!
			
			// Encode it as part of the goal
			if (currentSessionState == GOAL_COLLECTION || currentSessionState == START){
				
				currentSessionState = GOAL_COLLECTION;
				
				StringBuilder sb = new StringBuilder();
				
//				if (destination.equalsIgnoreCase("Penn Station") 
//						|| destination.equalsIgnoreCase("Grand Central")
//						|| destination.equalsIgnoreCase("South Station")
//						|| destination.equalsIgnoreCase("Logan Airport")
//						|| destination.equalsIgnoreCase("Home")){
//					
//				     switch (destination.toUpperCase()) {
//				         case "PENN STATION":
//				        	 tripDestination = new Location("Penn Station", 40.750584, -73.993487);
//				             break;
//				         case "GRAND CENTRAL":
//				        	 tripDestination = new Location("Grand Central Terminal", 40.752675, -73.977396);
//				             break;
//				         case "SOUTH STATION":
//				        	 tripDestination = new Location("South Station", 42.351952, -71.055110);
//				             break;
//				         case "LOGAN AIRPORT":
//				        	 tripDestination = new Location("Logan Airport", 42.365781, -71.007049);
//				             break;
//				         case "HOME":
//				        	 tripDestination = new Location("Home", 42.357406, -71.107672);
//				             break;
//				         default:
//				        	 tripDestination = new Location("Home", 42.357406, -71.107672);
//				     }
//					
//					
//					
//					if (!time.isEmpty()){
//						tripDestination.setArrivalTime(time);
//					} else if (!duration.isEmpty()){
//						try {
//							Duration dur = DatatypeFactory.newInstance().newDuration(duration);
//							
//							int hours = dur.getHours() + departureHour;
//							int minutes = dur.getMinutes() + departureMinute;
//							
//							while (minutes >= 60){
//								minutes -= 60;
//								hours++;
//							}
//							
//							if (dur.getMinutes() < 10){
//								tripDestination.setArrivalTime((18+hours) + ":0" + minutes);
//							} else {
//								tripDestination.setArrivalTime((18+hours) + ":" + minutes);
//							}
//							
//							tripDestination.setArrivalTime(String.format("%02d", hours) + ":" + String.format("%02d", minutes));
//
//							
//						} catch (DatatypeConfigurationException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}						
//					} else {
//						tripDestination.setArrivalTime("19:00");
//					}
//					
//					tripDestination.setRelaxable(true);
//					currentPlanningProblem.setDestination(tripDestination);
//					
//					sb.append("OK, trip destination is set to " + destination + " with an arrival time at " + tripDestination.getArrivalTime() + ".");
//					
//				} else {
					
				String destinationName = null;
//					String destinationName = destination;
				
				if (!cuisine.isEmpty()){
					destinationName = cuisine + " " + destination;
				} else {
					destinationName = destination;
				}
				
				Task newTask = null;
				if (destination.equals("restaurant")){
					newTask = new Task(destinationName,TaskType.Partial);
					newTask.addDescription("type", "restaurant");
					newTask.addDescription("cuisine", cuisine);
				} else {
					newTask = new Task(destinationName,TaskType.Full);
				}
				
				
				if (!duration.isEmpty()){

					try {
						Duration dur = DatatypeFactory.newInstance().newDuration(duration);
						int minutes = dur.getMinutes() + dur.getHours() * 60;
						newTask.setDuration(minutes);
					} catch (DatatypeConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				getCandidatesForTask(newTask, cuisine, food, destination);
				currentPlanningProblem.addActivity(newTask);
				
				if (newTask.getDuration() < 0.01){
					newTask.setDuration(30.0);
				}
//					newTask.setRelaxable(true);

				sb.append("OK, stop at " + destinationName + " for " + Math.round(newTask.getDuration()) + " minutes.");
//				}				
				
				try {
					
					result.put("text_output", sb.toString() + " Anything else?");
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				problemChanged = true;
				
			} else {
				
				// This is not an expected response.
				
				try {
					result.put("text_output", "Sorry, I did not capture any valid input.");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	@Path("/SetDestination")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject setDestination(
			@DefaultValue("") @QueryParam("destination") String destination,
			@DefaultValue("") @QueryParam("duration") String duration,
			@DefaultValue("") @QueryParam("time") String time) {
		
		JSONObject result = new JSONObject();

		if (destination.isEmpty()){
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid destination from your input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return result;
			
		} else {
			
			// OK, we have got origin!
			
			// Encode it as part of the goal
			if (currentSessionState == GOAL_COLLECTION || currentSessionState == START){
				
				currentSessionState = GOAL_COLLECTION;
				
				StringBuilder sb = new StringBuilder();
				
				Task destinationTask = new Task(null,null);
				getCandidatesForTask(destinationTask, "", "", destination);
				
				if (destinationTask.getCandidates().isEmpty()){
					try {
						result.put("text_output", "Sorry, I did not capture any valid destination from your input.");
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					return result;
				}
				
				for (Location location : destinationTask.getCandidates()){
					tripDestination = location;
					break;
				}
				
				if (!time.isEmpty()){
					tripDestination.setArrivalTime(time);
				} else if (!duration.isEmpty()){
					try {
						Duration dur = DatatypeFactory.newInstance().newDuration(duration);
						
						int hours = dur.getHours() + departureHour;
						int minutes = dur.getMinutes() + departureMinute;
						
						while (minutes >= 60){
							minutes -= 60;
							hours++;
						}
						
						tripDestination.setArrivalTime(String.format("%02d", hours) + ":" + String.format("%02d", minutes));

						
					} catch (DatatypeConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}						
				} else {
					tripDestination.setArrivalTime("19:00");
				}
				
				tripDestination.setRelaxable(true);
				currentPlanningProblem.setDestination(tripDestination);
				
				sb.append("OK, trip destination is set to " + destination + " with an arrival time at " + tripDestination.getArrivalTime() + ".");		
				
				try {
					
					result.put("text_output", sb.toString() + " Anything else?");
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				problemChanged = true;
				
			} else {
				
				// This is not an expected response.
				
				try {
					result.put("text_output", "Sorry, I did not capture any valid destination input.");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	@Path("/SetOrigin")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject setOrigin(
			@DefaultValue("") @QueryParam("origin") String origin,
			@DefaultValue("") @QueryParam("time") String time) {
		
		JSONObject result = new JSONObject();

		if (origin.isEmpty()){
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid origin from your input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return result;
			
		} else {
			
			// OK, we have got origin!
			
			// Encode it as part of the goal
			if (currentSessionState == GOAL_COLLECTION || currentSessionState == START){
				
				currentSessionState = GOAL_COLLECTION;
				
				StringBuilder sb = new StringBuilder();
				
				Task originTask = new Task(null,null);
				getCandidatesForTask(originTask, "", "", origin);
				
				if (originTask.getCandidates().isEmpty()){
					try {
						result.put("text_output", "Sorry, I did not capture any valid origin from your input.");
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					return result;
				}
				
				for (Location location : originTask.getCandidates()){
					tripOrigin = location;
					break;
				}
			     
				if (!time.isEmpty()){
					tripOrigin.setDepartureTime(time);
					String[] timeElements = time.split(":");
					departureHour = Integer.parseInt(timeElements[0]);
					departureMinute = Integer.parseInt(timeElements[1]);
				} else {
					tripOrigin.setDepartureTime("18:00");
				}
				
//				tripOrigin = new Location("MIT",42.361602, -71.090568);
				tripOrigin.setRelaxable(true);
				currentPlanningProblem.setOrigin(tripOrigin);
				
				sb.append("OK, trip origin is set to " + origin + " with departure time at " + tripOrigin.getDepartureTime() + ".");			
				
				try {
					
					result.put("text_output", sb.toString() + " Anything else?");
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				problemChanged = true;
				
			} else {
				
				// This is not an expected response.
				
				try {
					result.put("text_output", "Sorry, I did not capture any valid input.");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	@Path("/CorrectConstraint")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject addConstraint(
			@DefaultValue("") @QueryParam("prep") String prep,
			@DefaultValue("") @QueryParam("duration") String duration,
			@DefaultValue("") @QueryParam("time") String time) throws JSONException {
		
		JSONObject result = new JSONObject();

		if (duration.isEmpty() && time.isEmpty()){
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid temporal requirement from your input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return result;
			
		} else {
			
			// OK, we have got constraint!
			
			// Encode it as part of the constraint
			
			if (currentSessionState == GOAL_COLLECTION){
				
				Task prevTask = currentPlanningProblem.getLastActivity();
				
				if (prevTask != null && !duration.isEmpty()){

					try {
						Duration dur = DatatypeFactory.newInstance().newDuration(duration);
						int minutes = dur.getMinutes() + dur.getHours() * 60;
						prevTask.setDuration(minutes);
					} catch (DatatypeConfigurationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					if (prevTask.getDuration() < 0.01){
						prevTask.setDuration(30.0);
					}
//						newTask.setRelaxable(true);

					result.put("text_output", "OK, stop at " + prevTask.getName() + " for " + Math.round(prevTask.getDuration()) + " minutes, anything else?");
					
					problemChanged = true;

				} else {
					result.put("text_output", "Sorry, I did not capture any valid input.");
				}

			} else {
				// This is not an expected response.
				
				try {
					result.put("text_output", "Sorry, I did not capture any valid input.");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			

			
		}
		
		return result;
	}
	
	@Path("/CorrectGoal")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject addConstraint(
			@DefaultValue("") @QueryParam("cuisine") String cuisine,
			@DefaultValue("") @QueryParam("location") String location) throws JSONException {
		
		JSONObject result = new JSONObject();

		if (location.isEmpty()){
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid location from your input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return result;
			
		} else {
			
			// Encode it as part of the constraint
			
			if (currentSessionState == GOAL_COLLECTION){
				
				Task prevTask = currentPlanningProblem.getLastActivity();

				if (prevTask != null && !location.isEmpty()){
					if (location.equals("restaurant") && !cuisine.isEmpty()){
						prevTask.setType(TaskType.Partial);
						prevTask.setName(cuisine + " " + location);
						prevTask.clearDescription();
						prevTask.addDescription("type", "restaurant");
						prevTask.addDescription("cuisine", cuisine);
					} else {
						prevTask.setType(TaskType.Full);
						prevTask.setName(location);
					}
					
					prevTask.clearCandidates();
					getCandidatesForTask(prevTask, cuisine, "", location);
									
					if (!prevTask.getCandidates().isEmpty()){

						result.put("text_output", "OK, stop at " + prevTask.getName() + " for " + Math.round(prevTask.getDuration()) + " minutes, anything else?");
						
						problemChanged = true;

					} else {
						result.put("text_output", "Sorry, I did not capture any valid input.");
					}
				} else {
					result.put("text_output", "Sorry, I did not capture any valid input.");
				}
				

			} else {
				// This is not an expected response.
				
				try {
					result.put("text_output", "Sorry, I did not capture any valid input.");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return result;
	}
	
	@Path("/SendConfirm")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject sendConfirm() {
		
		JSONObject result = new JSONObject();
		
		if (currentSessionState == SOLUTION_FOUND) {
			
			// This is a new constraint. Try to find a new plan that
			// meets it.
			
			try {
				result.put("status", "terminal");
				result.put("text_output", "Ok, Have a nice trip!");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
			// This is not an expected response.
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	@Path("/SendDecline")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject sendDecline() {
		
		JSONObject result = new JSONObject();
		
		if (currentSessionState == SOLUTION_FOUND) {

			plan();
			
			presentSolution(result);
			
		} else if (currentSessionState == GOAL_COLLECTION) {
			
			// No more goal to collect.
			
			if (currentPlanningProblem.getOrigin() == null){
				try {
					result.put("text_output", "What is the origin of your trip?");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return result;
			}
			
			if (currentPlanningProblem.getDestination() == null){
				try {
					result.put("text_output", "What is the destination of your trip?");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return result;
			}
			
			// Start planning.			
			plan();
			
			presentSolution(result);
			
		} else {
			// This is not an expected response.
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	@Path("/SendDeclineDepartureRelaxation")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject sendDeclineDepartureRelaxation(
			@DefaultValue("") @QueryParam("time") String time) throws JSONException {
		
		JSONObject result = new JSONObject();
		String departureConstraintId = currentSolution.getDepartureConstraintID();

		if (currentSessionState == SOLUTION_FOUND && departureConstraintId != null) {
						
			// The current solution is rejected by the users.
			// Ask for more input on why.
			
			JSONArray temporalRelaxations = currentSolution.getTemporalRelaxations();
			
			if (temporalRelaxations != null && temporalRelaxations.length() > 0){
				
				for (int i=0;i<temporalRelaxations.length();i++){
					
					JSONObject temporalRelaxation = temporalRelaxations.getJSONObject(i);
					String constraintID = temporalRelaxation.getString("id");
					if (departureConstraintId.equals(constraintID)){
						
						String[] timeElements = time.split(":");
						int hours = Integer.parseInt(timeElements[0]);
						int minutes = Integer.parseInt(timeElements[1]);
						int minutesInDay = hours*60+minutes;
						agreedTemporalRelaxations.add(constraintID);
						addTemporalConflictAndReplan(currentPlanningProblem, constraintID, 
								temporalRelaxation.getBoolean("relaxedLB"),
								temporalRelaxation.getBoolean("relaxedUB"),
								minutesInDay,
								minutesInDay);

						break;
					}
					
				}

			} else {
				plan();
			}
			
			presentSolution(result);
			
		} else {
			// This is not an expected response.
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	@Path("/SendDeclineArrivalRelaxation")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject sendDeclineArrivalRelaxation(
			@DefaultValue("") @QueryParam("time") String time,
			@DefaultValue("") @QueryParam("duration") String duration) throws JSONException {
		
		JSONObject result = new JSONObject();
		String arrivalConstraintId = currentSolution.getArrivalConstraintID();

		if (currentSessionState == SOLUTION_FOUND && arrivalConstraintId != null) {
						
			// The current solution is rejected by the users.
			// Ask for more input on why.
			
			JSONArray temporalRelaxations = currentSolution.getTemporalRelaxations();
			
			if (temporalRelaxations != null && temporalRelaxations.length() > 0){
				
				for (int i=0;i<temporalRelaxations.length();i++){
					
					JSONObject temporalRelaxation = temporalRelaxations.getJSONObject(i);
					String constraintID = temporalRelaxation.getString("id");
					if (arrivalConstraintId.equals(constraintID)){
						
						String[] timeElements = time.split(":");
						int hours = Integer.parseInt(timeElements[0]);
						int minutes = Integer.parseInt(timeElements[1]);
						int minutesInDay = hours*60+minutes;

						agreedTemporalRelaxations.add(constraintID);
						addTemporalConflictAndReplan(currentPlanningProblem, constraintID, 
								temporalRelaxation.getBoolean("relaxedLB"),
								temporalRelaxation.getBoolean("relaxedUB"),
								minutesInDay,
								minutesInDay);

						break;
					}
					
				}

			} else {
				plan();
			}
			
			presentSolution(result);
			
		} else {
			// This is not an expected response.
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	@Path("/SendDeclineTemporalRelaxation")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject sendDeclineTemporalRelaxation(
			@DefaultValue("") @QueryParam("duration") String duration) {
		
		JSONObject result = new JSONObject();
		
		if (currentSessionState == SOLUTION_FOUND) {
						
			// The current solution is rejected by the users.
			// Ask for more input on why.
			
			JSONArray temporalRelaxations = currentSolution.getTemporalRelaxations();
			if (temporalRelaxations != null && temporalRelaxations.length() > 0){
				try {
					JSONObject temporalRelaxation = temporalRelaxations.getJSONObject(0);
					addTemporalConflictAndReplan(currentPlanningProblem, temporalRelaxation);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					plan();
				}
			} else {
				plan();
			}
			
			presentSolution(result);
			
		} else {
			// This is not an expected response.
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	@Path("/SendDeclineChoice")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject sendDeclineChoice(
			@DefaultValue("") @QueryParam("cuisine") String cuisine,
			@DefaultValue("") @QueryParam("location") String location) throws JSONException {
		
		JSONObject result = new JSONObject();
		
		if (currentSessionState == SOLUTION_FOUND) {
						
			// Search for the decision the user wants to negate
			// through the guards of activities.
			boolean foundDecision = false;
			JSONArray activities = currentSolution.getActivities();
			if (activities != null && activities.length() > 0){
				
				for (int i=0;i<activities.length();i++){
					JSONObject activity = activities.getJSONObject(i);
					
					if (!activity.has("guards")){
						continue;
					}
					
					JSONArray guards = activity.getJSONArray("guards");	

					if (guards != null && guards.length() > 0){
						for (int j=0;j<guards.length();j++){
							JSONObject guard = guards.getJSONObject(j);
							String guardVariableName = guard.getString("name");

							if (guardVariableName.contains(location)){
								addDecisionConflictAndReplan(currentPlanningProblem, guard);
								foundDecision = true;
								break;
							}							
						}
					}	
					
					if (foundDecision){
						break;
					}
				}

			} else {
				plan();
			}
			
			presentSolution(result);
			
		} else {
			// This is not an expected response.
			
			try {
				result.put("text_output", "Sorry, I did not capture any valid input.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	@Path("/RemoveLastTask")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject removeLastTask() {
		
		JSONObject result = new JSONObject();
		StringBuilder sb = new StringBuilder();

		if (currentSessionState == GOAL_COLLECTION) {
			
			if (currentPlanningProblem.getActivities().size() <= 0){
				sb.append("Sorry, there is no activity to remove at the moment.");
			} else {
				
				Task removedActivity = currentPlanningProblem.removeLastActivity();				
				sb.append("OK, activity " + removedActivity.getName() + " has been removed from your trip.");				
				problemChanged = true;
			}
			
		} else {
			// This is not an expected response.			
			sb.append("Sorry, I cannot remove activities at the moment.");
		}
		
		try {			
			result.put("text_output", sb.toString() + " Anything else?");			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	@Path("/GetProblem")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject getProblem() {
		
		JSONObject result = new JSONObject();
		
		if (currentPlanningProblem != null) {
						
			return currentPlanningProblem.getJSONObject();
			
		}
		
		return result;
	}
	
	@Path("/GetSolution")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JSONObject getSolution() {
		
		JSONObject result = new JSONObject();
		
		if (currentSolution != null) {
						
			// The current solution is not null.
			// return its json representation.
			
			return currentSolution.getJSONObject();
			
		} else {
			try {
				result.put("error", "No solution exists at the moment.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	public void plan(){
		if (problemChanged){
			sendProblemAndPlan(currentPlanningProblem);
		} else {
			nextSolution(currentPlanningProblem);
		}
		
		problemChanged = false;
	}
	
	public void sendProblemAndPlan(Problem problem){
		
//		System.out.println("Sending problem: " + problem.getJSONObject().toString());
		
		String url = uhuraURI + "updateProblem?";		
		
		try {
			HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 
		    	
	        HttpPost request = new HttpPost(url);
	        StringEntity params =new StringEntity(problem.getJSONObject().toString());
	        request.addHeader("Accept", "application/json");
	        request.addHeader("content-type", "application/json");
	        request.addHeader("Access-Control-Allow-Headers", "*");
	        request.addHeader("Access-Control-Allow-Origin", "*");
	        request.setEntity(params);
	        HttpResponse response = httpClient.execute(request);
            InputStream content = response.getEntity().getContent(); //Get the data in the entity
            
            BufferedReader in = new BufferedReader (new InputStreamReader (content));

			StringBuffer sb = new StringBuffer();
			String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            
//            System.out.println(sb.toString());
			parseSolution(sb.toString());           

			
		} catch (JSONException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void nextSolution(Problem problem){
		String url = uhuraURI + "replan?email=" + problem.getUserID();		

		try {
			URLConnection conn = new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
	
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				sb.append(inputLine);
			}			
			br.close();
		
//			System.out.println(sb.toString());
			parseSolution(sb.toString());           

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
	
	public void addTemporalConflictAndReplan(Problem problem, JSONObject temporalRelaxation) throws JSONException{
		String url = uhuraURI + "addTemporalConflictAndReplan?email=" + problem.getUserID()+
				"&constraintID="+temporalRelaxation.getString("id")+
				"&isLB="+temporalRelaxation.getString("relaxedLB")+
				"&isUB="+temporalRelaxation.getString("relaxedUB")+
				"&newLB="+temporalRelaxation.getString("originalValue")+
				"&newUB="+temporalRelaxation.getString("originalValue");

		try {
//			System.out.println("Add conflict URL: " + url);
			URLConnection conn = new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
	
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				sb.append(inputLine);
			}			
			br.close();
		
//			System.out.println(sb.toString());
			parseSolution(sb.toString());           

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
	
	public void addTemporalConflictAndReplan(Problem problem, String id, boolean relaxedLB, boolean relaxedUB, 
			double newLBValue, double newUBValue) throws JSONException{
		String url = uhuraURI + "addTemporalConflictAndReplan?email=" + problem.getUserID()+
				"&constraintID="+id+
				"&isLB="+relaxedLB+
				"&isUB="+relaxedUB+
				"&newLB="+newLBValue+
				"&newUB="+newUBValue;

		try {
//			System.out.println("Add conflict URL: " + url);
			URLConnection conn = new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder sb = new StringBuilder();
	
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				sb.append(inputLine);
			}			
			br.close();
		
//			System.out.println(sb.toString());
			parseSolution(sb.toString());           

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
	
	public void addDecisionConflictAndReplan(Problem problem, JSONObject assignment) throws JSONException{
		
		JSONObject obj = new JSONObject();	
		obj.put("email",problem.getUserID());

		JSONArray assignments = new JSONArray();
		assignments.put(assignment);
		obj.put("assignments", assignments);

		String url = uhuraURI + "addDecisionConflictAndReplan?";

		try {
			HttpClient httpClient = HttpClientBuilder.create().build(); //Use this instead 
	    	
	        HttpPost request = new HttpPost(url);
	        StringEntity params =new StringEntity(obj.toString());
	        request.addHeader("Accept", "application/json");
	        request.addHeader("content-type", "application/json");
	        request.addHeader("Access-Control-Allow-Headers", "*");
	        request.addHeader("Access-Control-Allow-Origin", "*");
	        request.setEntity(params);
	        HttpResponse response = httpClient.execute(request);
            InputStream content = response.getEntity().getContent(); //Get the data in the entity
            
			BufferedReader br = new BufferedReader(new InputStreamReader(content));
			StringBuilder sb = new StringBuilder();
	
			String inputLine;
			while ((inputLine = br.readLine()) != null) {
				sb.append(inputLine);
			}			
			br.close();
		
//			System.out.println(sb.toString());
			parseSolution(sb.toString());           

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
	
	public void presentSolution(JSONObject result){
		
		if (currentSolution == null){
			
			currentSessionState = SOLUTION_NOT_FOUND;
			
			try {
				result.put("status", "terminal");
				result.put("text_output", "Sorry, I cannot find another plan for your trip. Please modify your requirements and try again.");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
			

			try {
				
				StringBuilder sb = new StringBuilder();
				
				if (currentSessionState != SOLUTION_FOUND){
					sb.append("Ok. I have found a plan for you. ");
				} else {
					sb.append("Ok. I have found another plan for you. ");
				}				
				sb.append(currentSolution.getDescription(prevDecisions, prevTemporalRelaxations, prevSemanticRelaxations));					
				sb.append("Is it ok?");
				
				result.put("text_output", sb.toString());
				
				currentSessionState = SOLUTION_FOUND;
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void getCandidatesForTask(Task task, String cuisine, String food, String destination){
		
		if (destination.equals("restaurant")){
			
			if (cuisine.equalsIgnoreCase("chinese")){

				MySQLAccess newAccess = new MySQLAccess("restaurant");
				HashMap<String,String> requirements = new HashMap<String,String>();
				requirements.put("cuisine", "Chinese");
				ArrayList<Location> locations = newAccess.getLocations(requirements);
				for (Location location : locations){
					task.addCandidate(location);
				}
				newAccess.close();
				
			} else if (cuisine.equalsIgnoreCase("korean")){
								
				MySQLAccess newAccess = new MySQLAccess("restaurant");
				HashMap<String,String> requirements = new HashMap<String,String>();
				requirements.put("cuisine", "Korean");
				ArrayList<Location> locations = newAccess.getLocations(requirements);
				for (Location location : locations){
					task.addCandidate(location);
				}
				newAccess.close();
				
			} else {
				MySQLAccess newAccess = new MySQLAccess("restaurant");
				HashMap<String,String> requirements = new HashMap<String,String>();
				ArrayList<Location> locations = newAccess.getLocations(requirements);
				for (Location location : locations){
					task.addCandidate(location);
				}
				newAccess.close();
				
			}
			
		} else if (destination.equals("gas station")){
			
			MySQLAccess newAccess = new MySQLAccess("automotive");
			HashMap<String,String> requirements = new HashMap<String,String>();
			requirements.put("type", "gas & service stations");
			ArrayList<Location> locations = newAccess.getLocations(requirements);
			for (Location location : locations){
				task.addCandidate(location);
			}
			newAccess.close();
			
		} else if (destination.equals("bike shop")){
			
			MySQLAccess newAccess = new MySQLAccess("shopping");
			HashMap<String,String> requirements = new HashMap<String,String>();
			requirements.put("type", "sporting goods");
			requirements.put("subtype1", "bikes");

			ArrayList<Location> locations = newAccess.getLocations(requirements);
			for (Location location : locations){
				task.addCandidate(location);
			}
			newAccess.close();
		
		} else if (destination.equals("grocery store")){
			
			MySQLAccess newAccess = new MySQLAccess("food");
			HashMap<String,String> requirements = new HashMap<String,String>();
			requirements.put("type", "grocery");
			ArrayList<Location> locations = newAccess.getLocations(requirements);
			for (Location location : locations){
				task.addCandidate(location);
			}
			newAccess.close();
		
		} else if (destination.equalsIgnoreCase("empire state building") 
				|| destination.equalsIgnoreCase("the empire state building")){
			
			task.addCandidate(new Location("Empire State Building",40.748562, -73.985640));
		
		} else if (destination.equalsIgnoreCase("south station") 
				|| destination.equalsIgnoreCase("the south station")){
			
			task.addCandidate(new Location("South Station",42.351870, -71.055126));
		
		} else if (destination.equalsIgnoreCase("logan airport") 
				|| destination.equalsIgnoreCase("the logan airport")){
			
			task.addCandidate(new Location("Logan Airport",42.364829, -71.021077));
		
		} else if (destination.equalsIgnoreCase("mit")){
			
			task.addCandidate(new Location("MIT",42.359052, -71.093559));
		
		} else if (destination.equalsIgnoreCase("home")){
			
			task.addCandidate(new Location("Home",42.357406, -71.107672));

		} else if (destination.equalsIgnoreCase("harvard")){
			
			task.addCandidate(new Location("Harvard",42.373370, -71.118959));
		
		} else if (destination.equalsIgnoreCase("back bay")){
			
			task.addCandidate(new Location("Back Bay",42.347340, -71.075985));
		
		}
		
				
	}
	
	public void parseSolution(String solutionString) throws JSONException{
		JSONObject obj = new JSONObject(solutionString);	
		if (obj.has("error") || !obj.has("solution")){
			currentSolution = null;
			return;
		} 
				
		Solution newSolution = new Solution(obj.getJSONObject("solution"));
		newSolution.setIndex(allSolutions.size());
		allSolutions.add(newSolution);
		currentSolution = newSolution;
	}
}
