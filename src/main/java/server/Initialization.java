package server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class Initialization implements ServletContextListener{
	
	private static String mysqlURI = "uhuracloud.csail.mit.edu";
	private static String mysqlUsername = "uhura";
	private static String mysqlPassword = "fr5slcg0";
	
	public static final String poiURI = "jdbc:mysql://"+mysqlURI+"/pois?"
			+ "user="+mysqlUsername+"&password="+mysqlPassword;
	
	public static String uhuraPlannerURI = "http://uhura.csail.mit.edu:8080/uhura-web-interface/planner/";
	
	public void contextInitialized(ServletContextEvent arg0) {		
		Manager.resetManagerState();
	}

	public void contextDestroyed(ServletContextEvent arg0) {
		Manager.resetManagerState();
	}

}