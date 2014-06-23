package loadmaidprofiles;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class Log {
    private BufferedWriter writer;
    private String file = System.getProperty("user.dir")+"\\log.ini";
	public Log() throws UnsupportedEncodingException, FileNotFoundException {				
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
	}
	
	public void write(String line) throws IOException
	{
		writer.append(line);
		writer.newLine();
	}
	public void close() throws IOException
	{
		writer.close();
	}

}


