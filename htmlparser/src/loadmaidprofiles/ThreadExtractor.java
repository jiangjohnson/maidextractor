package loadmaidprofiles;

import java.io.IOException;
import java.sql.SQLException;

public class ThreadExtractor implements Runnable {
	  private final String urlPrefix;
	  private final String domain; 
	  private final int startId;
	  private final int endId;
	  
	  ThreadExtractor(String urlPrefix,String domain,int startId,int endId) {
	    this.urlPrefix = urlPrefix;
	    this.domain = domain;
	    this.startId = startId;
	    this.endId = endId;	    
	  }

	  public void run() {	    
		  try {
			TOOLS.LoadMaidProfileIntoDatabase(urlPrefix,domain,startId,endId);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  
	  }
	} 
