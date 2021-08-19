package dc43.python;

import org.bytedeco.embeddedpython.Python;

public class TestPython {

	public static void main(String[] args) {
		System.setProperty("org.bytedeco.javacpp.cacheDir", "C:\\Users\\Hannes\\.javacpp\\cacheMaven2");

		long start = System.currentTimeMillis();
		Python.put("a", 5);
		Python.put("b", 3);
		Python.exec("v = a/b");
		Object x = Python.get("v");
		System.out.println(x);
		System.out.println("Took " + (System.currentTimeMillis() - start) + "ms");
	}
}
