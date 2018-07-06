import java.io.File;

import org.junit.Test;

public class Test_TemporaryTest {

	@Test
	public void test_io_operation() {
		File file = new File("tmp.tmp");
		file.delete();
	}
}
