package loadmaidprofiles;

import java.awt.Image;
import java.awt.List;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.ResultSet;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mysql.jdbc.StringUtils;

public class TOOLS {

	public static void LoadMaidProfileIntoDatabase(String urlPrefix,
			String domain, int startId, int endId) throws IOException,
			SQLException {
		Log log = new Log();
		for (int i = startId; i < endId; i++) {
			String url = urlPrefix + String.valueOf(i);
			System.out.println("next extract " + url);

			Map<String, String> ProfileData = new HashMap<String, String>();
			String update_at = ExtractHtml(ProfileData, domain, url);
			boolean success = CheckMaidProfile(i, update_at, url, ProfileData);
			success = success && CheckUserProfile(ProfileData);
			if (success) {
				log.write("Success :  " + url);
				System.out.println("Success :  " + url);
			} else {
				log.write("failure :  " + url);
				System.out.println("Failed :  " + url);
			}
		}

		log.close();

	}

	public static boolean CheckUserProfile(Map<String, String> ProfileData)
			throws SQLException {
		boolean success = false;
		boolean found = false;
		for (int j = 0; j < GlobalData.UserProfileRecs.size(); j++) {
			UserProfileRec rec = GlobalData.UserProfileRecs.get(j);
			if (rec.username.equalsIgnoreCase(ProfileData.get("username"))) {
				found = true;
				// String userid;
				// String username;
				String email = ProfileData.get("email");
				String address = ProfileData.get("address");
				String officehours = ProfileData.get("officehours");
				String telecontact = ProfileData.get("telecontact");
				String mobilecontacts = ProfileData.get("mobilecontacts");
				String logo = ProfileData.get("logo");

				if (!rec.email.equalsIgnoreCase(email)
						|| !rec.address.equalsIgnoreCase(address)
						|| !rec.officehours.equalsIgnoreCase(officehours)
						|| !rec.telecontact.equalsIgnoreCase(telecontact)
						|| !rec.mobilecontacts.equalsIgnoreCase(mobilecontacts)
					    || !rec.logo.equalsIgnoreCase(logo)){
					success = UpdateUserProfilesTable(ProfileData);
					rec.UpdateProfileRec(email, address, officehours,
							telecontact, mobilecontacts,logo);

				}

				break;
			}
		}

		if (!found) {
			success = InsertIntoUserProfilesTable(ProfileData);
			GlobalData.UserProfileRecs
					.add(new UserProfileRec(null, ProfileData));
		}

		return success;
	}

	public static boolean CheckMaidProfile(int UrlId, String update_at,
			String url, Map<String, String> ProfileData) throws SQLException {
		boolean success = false;
		if (!update_at.isEmpty()) 
		{
			success = true;
			int found = -1;
			for (int j = 0; j < GlobalData.MaidProfileRecs.size(); j++) {
				MaidProfileRec rec = GlobalData.MaidProfileRecs.get(j);
				if (rec.ID == UrlId) {
					// if update_at changes, remove old rec from database
					if (!(rec.update_at.contains(update_at) || update_at
							.contains(rec.update_at))) {
						found = j;
						success = RemoveFromMaidProfilesTable(url);
					} else {
						success = false;
					}

					break;
				}
			}

			if (success) {
				if (found != -1) {
					GlobalData.MaidProfileRecs.remove(found);
				}
				GlobalData.MaidProfileRecs.add(new MaidProfileRec(String
						.valueOf(UrlId), update_at));
				success = InsertIntoMaidProfilesTable(ProfileData);
			}
		}
		return success;
	}

	public static void RequestLoadingImagesIntoServer(String maidimgfilenames)
			throws IOException {
		String ScriptUrl = "http://sghouse.net/androidphpscripts/uploads/request_loading_maidimages.php";
		try {
			// open a connection to the site
			URL url = new URL(ScriptUrl);
			URLConnection con = url.openConnection();
			// activate the output
			con.setDoOutput(true);
			PrintStream ps = new PrintStream(con.getOutputStream());
			// send your parameters to your site
			ps.print("maidimgfilenames=" + maidimgfilenames);

			// we have to get the input stream in order to actually send the
			// request
			con.getInputStream();

			// close the print stream
			ps.close();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void createFiledsandValues(Map<String, String> FieldsValues,
			String fieldname, String fieldvalue, boolean IsString) {
		String fields = "";
		String values = "";
		if (!(fieldvalue.trim()).isEmpty()) {
			fields = FieldsValues.get("fields") + ", " + fieldname;
			if (IsString) {
				values = FieldsValues.get("values") + ", '" + fieldvalue + "'";
			} else {
				values = FieldsValues.get("values") + ", " + fieldvalue;
			}

			FieldsValues.put("fields", fields);
			FieldsValues.put("values", values);
		}

	}

	public static boolean UpdateUserProfilesTable(
			Map<String, String> ProfileData) throws SQLException {
		String username = ProfileData.get("username");
		String email = ProfileData.get("email");
		String address = ProfileData.get("address");
		String officehours = ProfileData.get("officehours");
		String telecontact = ProfileData.get("telecontact");
		String mobilecontacts = ProfileData.get("mobilecontacts");

		String Query = " update userprofiles set " + " email = '" + email
				+ "', " + " address = '" + address + "', " + " officehours = '"
				+ officehours + "', " + " telecontact = '" + telecontact
				+ "', " + " mobilecontacts = '" + mobilecontacts + "' "
				+ " where username = '" + username + "'";

		DB_CONNECT db = null;
		try {
			db = new DB_CONNECT();
			db.command.execute(Query);
			db.close();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			db.close();
			return false;
		}
	}

	public static boolean InsertIntoUserProfilesTable(
			Map<String, String> ProfileData) throws SQLException {
		String username = ProfileData.get("username");
		String email = ProfileData.get("email");
		String address = ProfileData.get("address");
		String officehours = ProfileData.get("officehours");
		String telecontact = ProfileData.get("telecontact");
		String mobilecontacts = ProfileData.get("mobilecontacts");
		String logo = ProfileData.get("logo");

		String Query = " insert into userprofiles(username,email,address,officehours,telecontact,mobilecontacts,logo) values( "
		+ " '"+ username+ "','"+ email+ "','"+ address+ "','"+ officehours+ "','"+ telecontact+ "','"+ mobilecontacts+ "','"+ logo+ "' )";

		DB_CONNECT db = null;
		try {
			db = new DB_CONNECT();
			db.command.execute(Query);
			db.close();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			db.close();
			return false;
		}
	}

	public static boolean RemoveFromMaidProfilesTable(String Url)
			throws SQLException {

		String Query = " delete from maidprofiles where sourceprofileurl = '"
				+ Url + "'";

		DB_CONNECT db = null;
		try {
			db = new DB_CONNECT();
			db.command.execute(Query);
			db.close();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			db.close();
			return false;
		}

	}

	public static boolean InsertIntoMaidProfilesTable(
			Map<String, String> ProfileData) throws SQLException {

		String fields;
		String values;

		String maidname;
		String maidimgfilenames;
		String maidrefcode;
		String maidtype;				
		String maidnationality;
		String maidage;
		String maidbirthplace;
		String maidheight;
		String maidweight;
		String maidreligion;
		String maidmarriage;
		String maidnumberofchildren;
		String maidchildrenages;
		String maidsiblings;
		String maideducation;
		String maidlanguage;
		String maidoffdays;
		String maidsalary;
		String maidskills;
		String maidselfintroduction;
		String maidotherinfo;
		String maidemployrecords;
	
		String logo;
		String email;
		String address;
		String officehours;
		String telecontact;
		String mobilecontacts;
		String username;
		String updated_at;
		String views;
		String sourcefromagent = "";
		String sourceprofileurl = "";

		Map<String, String> FieldsValues = new HashMap<String, String>();

		maidname = ProfileData.get("maidname");
		if (!maidname.isEmpty()) {
			fields = "maidname";
			values = "'" + maidname + "'";
			FieldsValues.put("fields", fields);
			FieldsValues.put("values", values);
		} else {
			return false;
		}

		maidimgfilenames = ProfileData.get("maidimgfilenames");
		createFiledsandValues(FieldsValues, "maidimgfilenames",
				maidimgfilenames, true);

		maidrefcode = ProfileData.get("maidrefcode");
		createFiledsandValues(FieldsValues, "maidrefcode", maidrefcode, true);

		maidtype = ProfileData.get("maidtype");
		createFiledsandValues(FieldsValues, "maidtype", maidtype, true);

		maidnationality = ProfileData.get("maidnationality");
		maidnationality = maidnationality.replace("maid", "");
		createFiledsandValues(FieldsValues, "maidnationality", maidnationality,
				true);

		maidage = ProfileData.get("maidage");
		createFiledsandValues(FieldsValues, "maidage", maidage, false);

		maidbirthplace = ProfileData.get("maidbirthplace");
		createFiledsandValues(FieldsValues, "maidbirthplace", maidbirthplace,
				true);

		maidheight = ProfileData.get("maidheight");
		createFiledsandValues(FieldsValues, "maidheight", maidheight, false);

		maidweight = ProfileData.get("maidweight");
		createFiledsandValues(FieldsValues, "maidweight", maidweight, false);

		maidreligion = ProfileData.get("maidreligion");
		createFiledsandValues(FieldsValues, "maidreligion", maidreligion, true);

		maidmarriage = ProfileData.get("maidmarriage");
		createFiledsandValues(FieldsValues, "maidmarriage", maidmarriage, true);

		maidnumberofchildren = ProfileData.get("maidnumberofchildren");
		createFiledsandValues(FieldsValues, "maidnumberofchildren",
				maidnumberofchildren, false);

		maidchildrenages = ProfileData.get("maidchildrenages");
		createFiledsandValues(FieldsValues, "maidchildrenages",
				maidchildrenages, true);

		maidsiblings = ProfileData.get("maidsiblings");
		createFiledsandValues(FieldsValues, "maidsiblings",
				maidsiblings, true);

		maideducation = ProfileData.get("maideducation");
		createFiledsandValues(FieldsValues, "maideducation", maideducation,
				true);

		maidlanguage = ProfileData.get("maidlanguage");
		createFiledsandValues(FieldsValues, "maidlanguage", maidlanguage, true);

		maidoffdays = ProfileData.get("maidoffdays");
		createFiledsandValues(FieldsValues, "maidoffdays", maidoffdays, true);

		maidsalary = ProfileData.get("maidsalary");
		createFiledsandValues(FieldsValues, "maidsalary", maidsalary, false);

		maidskills = ProfileData.get("maidskills");
		createFiledsandValues(FieldsValues, "maidskills",
				maidskills, true);

		maidselfintroduction = ProfileData.get("maidselfintroduction");
		createFiledsandValues(FieldsValues, "maidselfintroduction",
				maidselfintroduction, true);
		
		maidotherinfo = ProfileData.get("maidotherinfo");
		createFiledsandValues(FieldsValues, "maidotherinfo",
				maidotherinfo, true);
		
		maidemployrecords = ProfileData.get("maidemployrecords");
		createFiledsandValues(FieldsValues, "maidemployrecords",
				maidemployrecords, true);
		
		
		logo = ProfileData.get("logo");
		createFiledsandValues(FieldsValues, "logo", logo, true);
		
		email = ProfileData.get("email");
		createFiledsandValues(FieldsValues, "email", email, true);

		address = ProfileData.get("address");
		createFiledsandValues(FieldsValues, "address", address, true);

		officehours = ProfileData.get("officehours");
		createFiledsandValues(FieldsValues, "officehours", officehours, true);

		telecontact = ProfileData.get("telecontact");
		createFiledsandValues(FieldsValues, "telecontact", telecontact, true);

		mobilecontacts = ProfileData.get("mobilecontacts");
		createFiledsandValues(FieldsValues, "mobilecontacts", mobilecontacts,
				true);

		username = ProfileData.get("username");
		createFiledsandValues(FieldsValues, "username", username, true);

		updated_at = ProfileData.get("updated_at");
		createFiledsandValues(FieldsValues, "updated_at", updated_at, true);

		views = ProfileData.get("views");
		createFiledsandValues(FieldsValues, "views",views, false);

		sourcefromagent = ProfileData.get("sourcefromagent");
		createFiledsandValues(FieldsValues, "sourcefromagent", sourcefromagent,
				true);

		sourceprofileurl = ProfileData.get("sourceprofileurl");
		createFiledsandValues(FieldsValues, "sourceprofileurl",
				sourceprofileurl, true);

		String Query = " insert into maidprofiles( "
				+ FieldsValues.get("fields") + " ) values( "
				+ FieldsValues.get("values") + " ) ";

		DB_CONNECT db = null;
		try {
			db = new DB_CONNECT();
			boolean result = db.command.execute(Query);
			db.close();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			db.close();
			return false;
		}

	}

	private static String ExtractHtml(Map<String, String> ProfileData,
			String domain, String profileUrl) throws IOException {		
		Document doc = null;
		try {
			doc = Jsoup.connect(profileUrl).data("query", "Java")
					.userAgent("Mozilla").cookie("auth", "token")
					.timeout(30000).post();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String maidname = "";
		String maidrefcode = "";
		String maidtype = "";
		String maidimgfilenames = "";
		String maidskills = "";
		String maidnationality = "";
		String maidage = "";
		String maidbirthplace = "";
		String maidheight = "";
		String maidweight = "";
		String maidreligion = "";
		String maidmarriage = "";
		String maidnumberofchildren = "";
		String maidchildrenages = "";
		String maidsiblings = "";
		String maideducation = "";		
		String maidlanguage = "";
		String maidoffdays = "";
		String maidsalary = "";
		String maidotherinfo="";
		String maidselfintroduction = "";
		String maidemployrecords="";
		
		String logo="";
		String email = "";
		String address = "";
		String officehours = "";
		String telecontact = "";
		String mobilecontacts = "";
		String username = "";
		String updated_at = "";
		String views = "";
		String sourcefromagent = "";
		String sourceprofileurl = "";

		Elements ths = doc.select("div.profile th");
		Element td = null;
		Element th = null;
	
		if ((ths == null) || (ths.isEmpty())) {
			return "";
		}

		for (int i = 0; i < ths.size(); i++) {
			th = ths.get(i);
			String text = th.text().trim();
			td = th.nextElementSibling();			

			if (!text.isEmpty()) {
				if (text.equalsIgnoreCase("Maid Name:")) {
					maidname = td.text();
				} else if (text.equalsIgnoreCase("Ref Code:")) {
					maidrefcode = td.text();
				} else if (text.equalsIgnoreCase("Type:")) {
					maidtype = td.text();
				} else if (text.equalsIgnoreCase("Nationality:")) {
					maidnationality = td.text();
				} else if (text.equalsIgnoreCase("Date Of Birth:")) {
					maidage = td.text();
					maidage = maidage.substring(maidage.indexOf("(Age:") + "(Age:".length(),maidage.indexOf("yrs)") -1);
					maidage = maidage.trim();
				} else if (text.equalsIgnoreCase("Place Of Birth:")||text.equalsIgnoreCase("Home Address")) {
					maidbirthplace = td.text();
				} else if (text.equalsIgnoreCase("Height:")) {
					maidheight = td.text();
					maidheight = maidheight.replace("(cm)", "").trim();
				} else if (text.equalsIgnoreCase("Weight:")) {
					maidweight = td.text();
					maidweight = maidweight.replace("(kg)", "").trim();
				} else if (text.equalsIgnoreCase("Religion:")) {
					maidreligion = td.text();
				} else if (text.equalsIgnoreCase("Marital Status:")) {
					maidmarriage = td.text();
				} else if (text.equalsIgnoreCase("Number of Children:")) {
					maidnumberofchildren = td.text();
					maidnumberofchildren = maidnumberofchildren.replace("Children", "");
					maidnumberofchildren = maidnumberofchildren.replace("Child", "");
					maidnumberofchildren = maidnumberofchildren.replace("+", "");
				} else if (text.equalsIgnoreCase("Children's Ages")) {
					maidchildrenages = td.text();
				} else if (text.equalsIgnoreCase("Siblings")) {
					maidsiblings = td.text();
				} else if (text.equalsIgnoreCase("Education:")) {
					maideducation = td.text();
				}  else if (text.equalsIgnoreCase("Off-days:")) {
					maidoffdays = td.text();
					maidoffdays = maidoffdays.replace("(day/month)", "");
					if (maidoffdays.contains("Sundays"))
					{
						maidoffdays = "4";
					}
					
				} else if (text.equalsIgnoreCase("Language:")) {
					try {						
						maidlanguage = td.text();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else if (text.equalsIgnoreCase("Expected Salary")) {
					maidsalary = td.text();
					int start = -1;
					int end = -1;
					for (int j = 0; j < maidsalary.length(); j++) {
						char s = maidsalary.charAt(j);
						if (start == -1) {
							if (s >= '0' && s <= '9') {
								start = j;
								end = j;
							}
						} else if (!(s >= '0' && s <= '9')) {
							end = j;
							break;
						}
					}
					maidsalary = maidsalary.substring(start, end);
				} else if ("Address:,Tel:,Email:,Office Hour:,Handphone:".contains(text)) 
				{
					if (text.contains("Address:")) {
						address = td.text();
					}else if (text.contains("Tel:")) {
						telecontact = td.text();
					} else if (text.contains("Email:")) {
						email = td.text();
					}else if (text.contains("Office Hour:")) {
						officehours = td.text();
					} else if (text.contains("Handphone:")) {
						mobilecontacts = td.text();
					} 
				}

			} // end if not empty td
		} // end for loop
		
		Elements h2s = doc.select("div.panel h2");
		Element  h2  = null;
		Element  div = null;
		
		for (int i = 0; i < h2s.size(); i++) 
		{
			h2 = h2s.get(i);
			String text = h2.text();
			div = h2.nextElementSibling();
			if (!text.isEmpty()) 
			{
				if (text.equalsIgnoreCase("Other Information")) 
				{ 
					Elements imgs = div.select("div img[src=images/decline.gif]");
					for(Element img : imgs)
					{
					  if(img.parent().previousElementSibling().text().contains("handle pork"))
					  {
						  maidotherinfo = maidotherinfo +" , "+"unable to handle pork"; 
					  } 
					   else if(img.parent().previousElementSibling().text().contains("eat pork"))
					   {
						   maidotherinfo = maidotherinfo +" , "+"unable to eat pork";   
					   }
					   else if(img.parent().previousElementSibling().text().contains("care dog/cat"))
					   {
						   maidotherinfo = maidotherinfo +" , "+"unable to care dog/cat";   
					   }
					   else if(img.parent().previousElementSibling().text().contains("simple sewing"))
					   {
						   maidotherinfo = maidotherinfo +" , "+"unable to do simple sewing";   
					   }
					   else if(img.parent().previousElementSibling().text().contains("gardening work"))
					   {
						   maidotherinfo = maidotherinfo +" , "+"unable to do gardening work";  
					   }
					   else if(img.parent().previousElementSibling().text().contains("wash car"))
					   {
						   maidotherinfo = maidotherinfo +" , "+"unable to wash car";  
					   }
					   else if(img.parent().previousElementSibling().text().contains("off-days with compensation"))
					   {
						   maidotherinfo = maidotherinfo +" , "+"UnWilling to work on off-days with compensation";  
					   }
					}
										
				} else if(text.equalsIgnoreCase("Maid Skills")) 
				{ 			
					Elements Images = div.select("img[src=images/accept.gif]");
					Element parent = null;
					String Skill = "";
					for (int j = 0; j < Images.size(); j++) {
					  parent = Images.get(j).parent();  
					  Skill = parent.previousElementSibling().text();
					  maidskills = maidskills + "," + Skill;
					}					
					maidskills = maidskills.substring(1);
					
				} else if(text.equalsIgnoreCase("Maid Introduction")) { 
					maidselfintroduction = div.text();	
					maidselfintroduction = maidselfintroduction.replace("'","");
				} else if(text.contains("Employment Record")) { 
					maidemployrecords = div.html().replace("'", "");				
				} else if(text.contains("Maid Agency:")) { 
					username = text.replace("Maid Agency:", "");
					Elements imgs = h2.parent().select("div img[class=agenciesLogo]");
					if(imgs!=null && !imgs.isEmpty() && imgs.size()>0)
					{
						String UrlValue = imgs.get(0).attr("src");
						RequestLoadingImagesIntoServer(domain + UrlValue);
						logo = UrlValue.substring(UrlValue.indexOf("logo") + 5,
								UrlValue.length());
					}
				}
			}
		}
		
		sourcefromagent = "YES";
		sourceprofileurl = profileUrl;

		// extract images
		Elements ImageUrLs = doc.select("div.big_photo img[src*=MaidImg]");
		if ((ImageUrLs != null) && (!ImageUrLs.isEmpty())) {
			HashSet<String> images = new HashSet<String>();

			String Basenames = "";
			for (Element ImageUrL : ImageUrLs) {
				String UrlValue = ImageUrL.attr("src"); // imageUrl->getAttribute
														// ( 'src' );
				String fullUrl = domain + UrlValue;
				images.add(fullUrl);
				maidimgfilenames = maidimgfilenames + "," + fullUrl;
				Basenames = Basenames
						+ ","
						+ UrlValue.substring(UrlValue.indexOf("MaidImg") + 8,
								UrlValue.length());
			}

			RequestLoadingImagesIntoServer(maidimgfilenames);
			maidimgfilenames = Basenames;
		}
		
	    Elements Divs = doc.select("div[class=font_669933]");
	    for(int j=0; j<Divs.size();j++)
	    {
	    	div = Divs.get(j);
	    	if(div.text().contains("Last updated on"))
	    	{
	    		String text = div.text().replace(".", "");
	    		int lastupdateon = text.indexOf("Last updated on");
	    		int Totalviews   = text.indexOf("Total hits:");
	    		updated_at = text.substring(lastupdateon+"Last updated on".length(),Totalviews-1);
	    		views      = text.substring(Totalviews+"Total hits:".length()+1,text.length());
	    		
	    		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-M-yyyy");
	    		try
	    		{
	    			Date date = simpleDateFormat.parse(updated_at);
	    			simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");  //2014-04-18 05:48:43
	    			updated_at= simpleDateFormat.format(date);
	    		}
	    		catch (ParseException ex)
	    		{
	    			System.out.println("Exception "+ex);
	    		}

	    	}
	    }	
	    
		ProfileData.put("maidname", maidname.trim().replace(" ", "").replace("&nbsp;", ""));
		ProfileData.put("maidrefcode", maidrefcode.trim().replace(" ", ""));
		ProfileData.put("maidtype", maidtype.trim().replace(" ", "")); 
		ProfileData.put("maidimgfilenames", maidimgfilenames.trim().replace(" ", ""));
		ProfileData.put("maidskills", maidskills.trim().replace(" ", ""));
		ProfileData.put("maidnationality", maidnationality.trim().replace(" ", ""));
		ProfileData.put("maidage", maidage.trim().replace(" ", ""));
		ProfileData.put("maidbirthplace", maidbirthplace.trim().replace(" ", ""));
		ProfileData.put("maidheight", maidheight.trim().replace(" ", ""));
		ProfileData.put("maidweight", maidweight.trim().replace(" ", ""));
		ProfileData.put("maidreligion", maidreligion.trim().replace(" ", ""));
		ProfileData.put("maidmarriage", maidmarriage.trim().replace(" ", ""));
		ProfileData.put("maidnumberofchildren", maidnumberofchildren.trim().replace(" ", ""));
		ProfileData.put("maidchildrenages", maidchildrenages.trim().replace(" ", ""));
		ProfileData.put("maidsiblings", maidsiblings.trim().replace(" ", ""));
		ProfileData.put("maideducation", maideducation.trim().replace(" ", ""));
		ProfileData.put("maidlanguage", maidlanguage.trim().replace(" ", ""));
		ProfileData.put("maidoffdays", maidoffdays.trim().replace(" ", ""));
		ProfileData.put("maidsalary", maidsalary.trim().replace(" ", ""));
		ProfileData.put("maidselfintroduction", maidselfintroduction.trim().replace(" ", ""));
		ProfileData.put("maidotherinfo", maidotherinfo.trim().replace(" ", ""));
		ProfileData.put("maidemployrecords", maidemployrecords.trim().replace(" ", ""));
		
		ProfileData.put("logo", logo.trim().replace(" ", ""));
		ProfileData.put("email", email.trim().replaceAll("\u00A0", ""));
		ProfileData.put("address", address.trim().replace(" ", ""));
		ProfileData.put("officehours", officehours.trim().replace(" ", ""));
		ProfileData.put("telecontact", telecontact.trim().replace(" ", ""));
		ProfileData.put("mobilecontacts", mobilecontacts.trim().replace(" ", ""));
		ProfileData.put("username", username.trim().replace(" ", ""));
		ProfileData.put("updated_at", updated_at.trim().replace(" ", ""));
		ProfileData.put("views", views.trim().replace(" ", ""));
		ProfileData.put("sourcefromagent", sourcefromagent.trim().replace(" ", ""));
		ProfileData.put("sourceprofileurl", sourceprofileurl.trim().replace(" ", ""));
		
		return updated_at;

	}

	public static void WriteMaidProfieRecs() throws IOException {
		String file = System.getProperty("user.dir") + "\\MaidProfileRecs.ini";
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));		
		for (int i = 0; i < GlobalData.MaidProfileRecs.size(); i++) {
			MaidProfileRec rec = (MaidProfileRec) GlobalData.MaidProfileRecs
					.get(i);
			writer.write(String.valueOf(rec.ID) + "=" + rec.update_at);
			writer.newLine();
		}
		writer.close();
	}

	public static void ReadMaidProfieRecs() throws IOException {		
		String file = System.getProperty("user.dir") + "\\MaidProfileRecs.ini";
		//new FileOutputStream(file, false).close();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = "";
		while ((line = reader.readLine()) != null) {
			int pos = line.indexOf("=");
			String ID = line.substring(0, pos);
			String update_at = line.substring(pos + 1, line.length());
			GlobalData.MaidProfileRecs.add(new MaidProfileRec(ID, update_at));
		}
		reader.close();
	}

	public static void ReadUserProfileRecs() throws SQLException {
		String Query = " select * from userprofiles ";

		DB_CONNECT db = null;
		try {
			db = new DB_CONNECT();
			ResultSet set = db.command.executeQuery(Query);
			while (set.next()) 
			{	
				UserProfileRec rec = new UserProfileRec(set, null);
				GlobalData.UserProfileRecs.add(rec);
			}
			db.close();
		} catch (SQLException e) {
			e.printStackTrace();
			db.close();
		}
	}

}
