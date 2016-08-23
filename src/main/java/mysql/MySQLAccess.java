package mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import server.Initialization;
import server.Location;

public class MySQLAccess {
	
	private Connection connect = null;
	private Statement statement = null;
	private ResultSet resultSet = null;
	
	private static String tableName = null;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MySQLAccess newAccess = new MySQLAccess("shopping");
		HashMap<String,String> requirements = new HashMap<String,String>();
		requirements.put("type", "sporting goods");
		requirements.put("subtype1", "bikes");
		
		ArrayList<String> names = newAccess.getUniqueValues("name",requirements);
		for (String name : names){
			System.out.println(name);
		}
		
		newAccess.close();
	}
	
	public MySQLAccess(String _tableName){		
		
		try {			
			
			Class.forName("com.mysql.jdbc.Driver");
			connect = DriverManager.getConnection(Initialization.poiURI);
			statement = connect.createStatement();				
			tableName = _tableName;	
			
		} catch (SQLException e) {
			e.printStackTrace();
			close();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public ArrayList<Location> getLocations(HashMap<String,String> requirements){
		ArrayList<Location> locations = new ArrayList<Location>();		
		ArrayList<HashMap<String,String>> results = getPOIs(requirements);
		for (HashMap<String,String> result : results){
//			System.out.print("Candidate -- ");
			for (String key: result.keySet()){
	            String value = result.get(key);  
//	            System.out.print(key + ": " + value + "; "); 				
			}
//			System.out.print("\n"); 
			locations.add(new Location(result));
		}
		
		return locations;
	}
	
	public ArrayList<String> getUniqueValues(String columnName, HashMap<String,String> requirements){
		
		ArrayList<String> values = new ArrayList<String>();
		ArrayList<String> queryConstraints = new ArrayList<String>();		

		try {
									
			StringBuilder sb = new StringBuilder();
			sb.append("select distinct "+columnName+" from "+tableName);
			if (!requirements.isEmpty()){
				for (String key : requirements.keySet()){
					queryConstraints.add(key + " = \'"+requirements.get(key)+"\'");
				}
				sb.append(" WHERE "+String.join(" AND ", queryConstraints));
			}
			
			resultSet = statement.executeQuery(sb.toString());

			while (resultSet.next()){
				String value = resultSet.getString(columnName);
				values.add(value);
			}			
			
		} catch (SQLException e) {
			
			e.printStackTrace();			
		} catch (NullPointerException e) { 
			
			e.printStackTrace();			
		}

		return values;
	}
	
	public ArrayList<HashMap<String,String>> getPOIs(HashMap<String,String> requirements) {
				
		ArrayList<HashMap<String,String>> pois = new ArrayList<HashMap<String,String>>();		
		HashSet<String> columnNames = new HashSet<String>();
		ArrayList<String> queryConstraints = new ArrayList<String>();		

		try {
			resultSet = statement.executeQuery("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE "+
					"TABLE_SCHEMA=\'pois\' AND TABLE_NAME=\'"+tableName+"\';");
			
			// GET ALL COLUMN NAMES FIRST
			while (resultSet.next()){
				String columnName = resultSet.getString("COLUMN_NAME");
				columnNames.add(columnName);
//				System.out.println("Column: " + columnName);
			}
									
			StringBuilder sb = new StringBuilder();
			sb.append("select * from "+tableName);
			if (!requirements.isEmpty()){
				for (String key : requirements.keySet()){
					queryConstraints.add(key + " = \'"+requirements.get(key)+"\'");
				}
				sb.append(" WHERE "+String.join(" AND ", queryConstraints));
			}
			
			
			resultSet = statement.executeQuery(sb.toString());

			while (resultSet.next()){
				HashMap<String,String> result = new HashMap<String,String>();
				for (String columnName : columnNames){
					String value = resultSet.getString(columnName);
					result.put(columnName, value);
					//System.out.println(columnName + ": " + value);
				}
				pois.add(result);
			}
			
			
		} catch (SQLException e) {
			
			e.printStackTrace();			
		} catch (NullPointerException e) { 
			
			e.printStackTrace();			
		}

		return pois;
	}
	
	public void close() {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
			
			if (statement != null) {
				statement.close();
			}
			
			if (connect != null) {
				connect.close();
			}
		} catch (Exception e) {
			// don't throw now as it might leave following closables in undefined state
		}
	}

}
