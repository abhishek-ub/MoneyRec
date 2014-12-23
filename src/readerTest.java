package MoneyRec.src;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.junit.Test;

public class readerTest {
	TreeMap<Double, String> tickdata=new TreeMap<Double, String>();
	@Test
	public void test() {
		File dicfile =new File("BasicIndustries.csvid-name");
		if(dicfile.canRead()){
			RandomAccessFile rf;
			try {
				rf = new RandomAccessFile(dicfile, "r");
			
				byte[] barray = new byte[(int) dicfile.length()];
		
				rf.read( barray, 0, (int) dicfile.length() );
			
				ObjectInputStream dicIndex = new ObjectInputStream(new ByteArrayInputStream(barray));	
		
				tickdata=(TreeMap<Double, String>) dicIndex.readObject();
				int i=tickdata.size();
			dicIndex.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

}
}
