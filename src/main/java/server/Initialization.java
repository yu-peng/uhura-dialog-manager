package server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class Initialization implements ServletContextListener{
	
	private static String mysqlURI = "uhura-database.csail.mit.edu";
	private static String mysqlUsername = "uhura";
	private static String mysqlPassword = "fr5slcg0";
	
	public static final String poiURI = "jdbc:mysql://"+mysqlURI+"/pois?"
			+ "user="+mysqlUsername+"&password="+mysqlPassword;
	
	public void contextInitialized(ServletContextEvent arg0) {
		
		Manager.resetManagerState();
	}

	public void contextDestroyed(ServletContextEvent arg0) {


	}

}