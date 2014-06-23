package loadmaidprofiles;

import java.awt.Image;
import java.awt.List;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class main {	
	public static void main(String[] args) throws IOException, SQLException {
		int NTHREDS = 10;		
		int startId = 78000; 
	    int endId   = 79000; 
	    String urlPrefix = "http://bestmaid.com.sg/listmaid.asp?id=";	    
	    String domain = "http://bestmaid.com.sg/";	    	   
	    TOOLS.ReadMaidProfieRecs();
	    TOOLS.ReadUserProfileRecs();
	    TOOLS.LoadMaidProfileIntoDatabase(urlPrefix,domain,startId,endId);
	    TOOLS.WriteMaidProfieRecs();
	}
}
