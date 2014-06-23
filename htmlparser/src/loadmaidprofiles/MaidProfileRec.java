package loadmaidprofiles;

public class MaidProfileRec {

	public MaidProfileRec(String ID, String update_at)
	{
		this.ID = Integer.valueOf(ID);
		this.update_at = update_at;		
	}
	public int ID;
	public String update_at;
}
