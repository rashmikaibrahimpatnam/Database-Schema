package mysql_Data;

import java.sql.*;
import java.util.Properties;

public class ConnectDatabase {

	static Connection connect = null; // the link to the database
	static Statement statement = null; // used to form an SQL query
	static ResultSet resultSet = null; // receives results from a customer SQL query


	public static void connectDB() {

		Properties identity = new Properties(); // Hides info from other users.
		MyIdentity credentials = new MyIdentity(); // Class to fetch credentials. 

		String db_Username;
		String db_Password;
		String db_Name;

		credentials.setIdentity(identity); 

		db_Username = identity.getProperty("user"); //fetch user name from the identity file
		db_Password = identity.getProperty("password"); //fetch password from the identity file
		db_Name = identity.getProperty("database");    //fetch database name from the identity file


		try {
			// loads sql driver
			Class.forName("com.mysql.cj.jdbc.Driver");

			// connection to the Database
			connect = DriverManager.getConnection("jdbc:mysql://db.cs.dal.ca:3306?serverTimezone=UTC&useSSL=false",
					db_Username, db_Password);

			// instance that sends queries to the database.
			statement = connect.createStatement();

			// database that needs to be accessed
			statement.executeQuery("use " + db_Name + ";");
			System.out.println("connection established");


		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}



}
