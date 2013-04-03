package repastcity3.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FluLogger {

	private BufferedWriter logFile;
	
	public FluLogger(){
		try {
			this.logFile = new BufferedWriter(new FileWriter(new File("logs.txt")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void logException(String msg){
		try {
			this.logFile.write(msg + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void closeLogger(){
		try {
			this.logFile.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
