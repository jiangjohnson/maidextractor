package loadmaidprofiles;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.*;

public class DB_CONNECT {

	private  Connection connection;  
	public   Statement command;
	//private static ResultSet data;
	
	public DB_CONNECT() throws SQLException {		
	  this.connect();		
	}
	
	public void  connect() throws SQLException
	   {
	      connection = DriverManager.getConnection(CONFIG_SETTINGS.DB_ConnnectionString,CONFIG_SETTINGS.DB_USER,CONFIG_SETTINGS.DB_PASSWORD);
		  command    = connection.createStatement();
	  }
	
	  public void close() throws SQLException
	   {
		  connection.close();
		  command.close();
	   }

}
