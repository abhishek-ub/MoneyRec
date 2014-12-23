package MoneyRec.src;

import static org.junit.Assert.*;

import org.junit.Test;

public class exampleTest {

	@Test
	public void test() {
		String[] args={"BasicIndustries.csv","BasicIndustriesid.csv"};
		try {
			MoneyRecApi.main(args);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
