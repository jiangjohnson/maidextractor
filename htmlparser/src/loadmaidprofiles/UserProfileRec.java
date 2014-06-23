package loadmaidprofiles;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class UserProfileRec {
	//public String userid;
	public String username;
	//public String password;
	public String email;
	public String address;
	public String officehours;
	public String telecontact;
	public String mobilecontacts;	
	//public String isagent;
	public String logo;
	
	public UserProfileRec(ResultSet set, Map<String, String> ProfileData) throws SQLException {
		if(set!=null)
		{
              
			//userid = String.valueOf(set.getInt("userid"));
			username = set.getString("username");
			email = set.getString("email");
			address = set.getString("address");
			officehours = set.getString("officehours");
			telecontact = set.getString("telecontact");
			mobilecontacts = set.getString("mobilecontacts");	
			logo           = set.getString("logo");
		}
		else if(ProfileData!=null)
		{
			//userid = String.valueOf(set.getInt("userid"));
			username = ProfileData.get("username");
			email = ProfileData.get("email");
			address = ProfileData.get("address");
			officehours = ProfileData.get("officehours");
			telecontact = ProfileData.get("telecontact");
			mobilecontacts = ProfileData.get("mobilecontacts");
			logo           = ProfileData.get("logo");
		}
			
	}
	
	public void UpdateProfileRec(String email,String address,String officehours,String telecontact,String mobilecontacts,String logo)
	{
		this.email = email;
		this.address = address;
		this.officehours = officehours;
		this.telecontact = telecontact;
		this.mobilecontacts = mobilecontacts;	
		this.logo = logo;	
	}

}
