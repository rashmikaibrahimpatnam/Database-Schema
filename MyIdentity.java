package mysql_Data;

import java.util.Properties;

public class MyIdentity {

	

		//class that holds the identity properties
		
	    public void setIdentity(Properties prop) {
	      prop.setProperty("database", "rashmika"); //database to be accessed
	      prop.setProperty("user", "rashmika");  // username to access the database
	      prop.setProperty("password", "B00832190"); // password to access the database
	    }
	

}
