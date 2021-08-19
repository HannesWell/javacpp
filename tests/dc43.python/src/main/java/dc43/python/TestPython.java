package dc43.python;

import java.nio.file.Path;

import org.bytedeco.embeddedpython.Python;

public class TestPython {

	public static void main(String[] args) {
		Path cache = Path.of(System.getProperty("user.home")).getParent().resolve("cacheMaven2");
		System.setProperty("org.bytedeco.javacpp.cacheDir", cache.toString());

		long start = System.currentTimeMillis();
		Python.put("a", 5);
		Python.put("b", 3);
		Python.exec("v = a/b");
		Object x = Python.get("v");
		System.out.println(x);

		System.out.println("Took " + (System.currentTimeMillis() - start) + "ms");
	}
}
