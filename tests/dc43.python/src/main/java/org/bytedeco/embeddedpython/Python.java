package org.bytedeco.embeddedpython;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.bytedeco.cpython.global.python.METH_VARARGS;
import static org.bytedeco.cpython.global.python.PyBool_FromLong;
import static org.bytedeco.cpython.global.python.PyByteArray_AsString;
import static org.bytedeco.cpython.global.python.PyByteArray_Size;
import static org.bytedeco.cpython.global.python.PyBytes_AsString;
import static org.bytedeco.cpython.global.python.PyBytes_FromStringAndSize;
import static org.bytedeco.cpython.global.python.PyBytes_Size;
import static org.bytedeco.cpython.global.python.PyCFunction_NewEx;
import static org.bytedeco.cpython.global.python.PyDict_GetItemString;
import static org.bytedeco.cpython.global.python.PyDict_New;
import static org.bytedeco.cpython.global.python.PyDict_Next;
import static org.bytedeco.cpython.global.python.PyDict_SetItem;
import static org.bytedeco.cpython.global.python.PyDict_SetItemString;
import static org.bytedeco.cpython.global.python.PyErr_Clear;
import static org.bytedeco.cpython.global.python.PyErr_Occurred;
import static org.bytedeco.cpython.global.python.PyErr_Print;
import static org.bytedeco.cpython.global.python.PyErr_SetString;
import static org.bytedeco.cpython.global.python.PyEval_EvalCode;
import static org.bytedeco.cpython.global.python.PyExc_RuntimeError;
import static org.bytedeco.cpython.global.python.PyFloat_AsDouble;
import static org.bytedeco.cpython.global.python.PyFloat_FromDouble;
import static org.bytedeco.cpython.global.python.PyImport_AddModule;
import static org.bytedeco.cpython.global.python.PyIter_Next;
import static org.bytedeco.cpython.global.python.PyList_Append;
import static org.bytedeco.cpython.global.python.PyList_New;
import static org.bytedeco.cpython.global.python.PyList_SetItem;
import static org.bytedeco.cpython.global.python.PyLong_AsLong;
import static org.bytedeco.cpython.global.python.PyLong_FromLong;
import static org.bytedeco.cpython.global.python.PyModule_GetDict;
import static org.bytedeco.cpython.global.python.PyObject_GetIter;
import static org.bytedeco.cpython.global.python.PyObject_Str;
import static org.bytedeco.cpython.global.python.PyRun_SimpleStringFlags;
import static org.bytedeco.cpython.global.python.PySys_SetArgvEx;
import static org.bytedeco.cpython.global.python.PyTuple_GetItem;
import static org.bytedeco.cpython.global.python.PyTuple_Size;
import static org.bytedeco.cpython.global.python.PyUnicode_AsUTF8;
import static org.bytedeco.cpython.global.python.PyUnicode_FromString;
import static org.bytedeco.cpython.global.python.Py_CompileString;
import static org.bytedeco.cpython.global.python.Py_DecRef;
import static org.bytedeco.cpython.global.python.Py_DecodeLocale;
import static org.bytedeco.cpython.global.python.Py_Initialize;
import static org.bytedeco.cpython.global.python.Py_UnbufferedStdioFlag;
import static org.bytedeco.cpython.global.python.Py_eval_input;
import static org.bytedeco.cpython.global.python._Py_NoneStruct;
import static org.bytedeco.cpython.global.python._Py_SetProgramFullPath;
import static org.bytedeco.cpython.helper.python.Py_AddPath;
import static org.bytedeco.cpython.presets.python.cachePackages;
import static org.bytedeco.embeddedpython.PyTypes.PyBool_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyByteArray_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyBytes_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyDict_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyFloat_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyLong_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyNone_Check;
import static org.bytedeco.embeddedpython.PyTypes.PyUnicode_Check;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Stream;

import org.bytedeco.cpython.PyCFunction;
import org.bytedeco.cpython.PyMethodDef;
import org.bytedeco.cpython.PyObject;
import org.bytedeco.cpython.PyTypeObject;
import org.bytedeco.cpython.global.python;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.SizeTPointer;

/**
 * Python interpreter.
 * <p>
 * If you embed two Python interpreters, many Python libraries do not work correctly.
 * Therefore this class is a singleton class. All the methods are static.
 * <p>
 * This class is thread-safe. All the methods are synchronized.
 */
public class Python {
	public static class Pip {
		private Pip() {
		}

		/**
		 * Install pip packages.
		 *
		 * @param packages The package names to install.
		 * @return 0 on success.
		 * @throws IOException If an I/O error occurs.
		 * @throws InterruptedException If the current thread is interrupted by another thread.
		 */
		public synchronized int install(String... packages) throws IOException, InterruptedException {
			return exec(concat(new String[] { pythonPath, "-m", "pip", "--disable-pip-version-check",
				"--no-python-version-warning", "install" }, packages));
		}

		/**
		 * Upgrade pip packages.
		 *
		 * @param packages The package names to upgrade.
		 * @return 0 on success.
		 * @throws IOException If an I/O error occurs.
		 * @throws InterruptedException If the current thread is interrupted by another thread.
		 */
		public synchronized int upgrade(String... packages) throws IOException, InterruptedException {
			return exec(concat(new String[] { pythonPath, "-m", "pip", "--disable-pip-version-check",
				"--no-python-version-warning", "install", "--upgrade" }, packages));
		}

		/**
		 * Uninstall pip packages.
		 *
		 * @param packages The package names to uninstall.
		 * @return 0 on success.
		 * @throws IOException If an I/O error occurs.
		 * @throws InterruptedException If the current thread is interrupted by another thread.
		 */
		public synchronized int uninstall(String... packages) throws IOException, InterruptedException {
			return exec(concat(new String[] { pythonPath, "-m", "pip", "--disable-pip-version-check",
				"--no-python-version-warning", "uninstall", "-y" }, packages));
		}

		private int exec(String[] commands) throws IOException, InterruptedException {
			return new ProcessBuilder(commands).inheritIO().start().waitFor();
		}

		private String[] concat(String[] a, String[] b) {
			return Stream.concat(Arrays.stream(a), Arrays.stream(b)).toArray(String[]::new);
		}
	}

	private static final PyObject mainModule;
	private static final PyObject globals;
	private static final String PRINTSTREAM = "iilspythonPrintStream";
	private static final String ERRORSTREAM = "iilspythonErrorStream";
	private static String pythonPath;
	public static Pip pip = new Pip();

	static {
		try {
			init();
		} catch (Exception e) {
			throw new PythonException("Failed at Python.init()", e);
		}
		mainModule = PyImport_AddModule("__main__");
		if (mainModule == null) { // don't kill the entire JVM
			throw new PythonException("Failed to load __main__");
		}
		globals = PyModule_GetDict(mainModule);
		// import and define the basic stdout,stderr stringIO
		PyRun_SimpleStringFlags("import sys\nfrom io import StringIO\n" + PRINTSTREAM + " = StringIO()\n" + ERRORSTREAM
			+ " = StringIO()\nsys.stderr = " + ERRORSTREAM, null);
	}

	public static void initialize() {
		// calling this just ensures the class is loaded and Python is fully set up
	}

	private static void init() throws IOException {
		// System.setProperty("org.bytedeco.openblas.load", "mkl");
		// String cacheLocation = getCacheLocation().toString();
		// System.setProperty("org.bytedeco.javacpp.cacheDir", cacheLocation);

		File[] packages = cachePackages();
		Py_AddPath(packages);
		// Py_AddPath(org.bytedeco.numpy.presets.numpy.cachePackages());

		String javaCppVersion = Loader.getVersion();
		if (javaCppVersion.equals("1.5.3") || javaCppVersion.equals("1.5.4")) {
			Py_AddPath(new File(packages[0], "site-packages"));
		}

		pythonPath = Loader.load(org.bytedeco.cpython.python.class);
		_Py_SetProgramFullPath(Py_DecodeLocale(pythonPath, null));

		Py_UnbufferedStdioFlag(1);
		Py_Initialize();
		PySys_SetArgvEx(1, new PointerPointer<>(1).put(Py_DecodeLocale("", null)), 0);
		// _import_array();

		Runtime.getRuntime().addShutdownHook(new Thread(python::Py_Finalize));
	}

	// private static Path getCacheLocation() throws IOException {
	// Path configLocation = DC43FileUtil.getPluginInstallationConfigurationLocation(Python.class, null);
	// return configLocation.resolve(".javacpp").resolve("cache");
	// }

	// public static Path getSitePackageLocation() throws IOException {
	// Path cacheLocation = getCacheLocation();
	// String cpythonVersion = Loader.getVersion("org.bytedeco", "cpython", org.bytedeco.cpython.python.class);
	// String platform = Loader.getPlatform();
	// Path pythonLocation = cacheLocation.resolve("cpython-" + cpythonVersion + "-" + platform + ".jar")
	// .resolve(Path.of("org", "bytedeco", "cpython")).resolve(platform);
	// return pythonLocation.resolve("lib").resolve("site-packages");
	// }

	private static void resetPrintStream() {
		PyRun_SimpleStringFlags("if(" + PRINTSTREAM + "):\n\t" + PRINTSTREAM + ".close()\n" + PRINTSTREAM
			+ " = StringIO()\nsys.stdout = " + PRINTSTREAM, null);
	}

	private static void resetErrorStream() {
		PyRun_SimpleStringFlags("if(" + ERRORSTREAM + "):\n\t" + ERRORSTREAM + ".close()\n" + ERRORSTREAM
			+ " = StringIO()\nsys.stderr = " + ERRORSTREAM, null);
	}

	private Python() {
	}

	/**
	 * Don't forget to call Py_DecRef().
	 */
	private static PyObject compile(String src) {
		PyObject co = Py_CompileString(src, "<string>", Py_eval_input);
		if (co == null) {
			if (PyErr_Occurred() != null) {
				PyErr_Print();
			}
			throw new PythonException("Py_CompileString() failed. src = " + src);
		}
		return co;
	}

	/**
	 * Python built-in eval().
	 *
	 * @param src Python code. This must be a single line code.
	 * @param <T> The Java class after conversion from Python.
	 * @return The Java object converted from the Python object.
	 */
	@SuppressWarnings("unchecked")
	public synchronized static <T> T eval(String src) {
		PyObject co = compile(src);
		try {
			PyObject obj = PyEval_EvalCode(co, globals, globals);
			try {
				if (obj == null) {
					if (PyErr_Occurred() != null) {
						PyErr_Print();
						throw new PythonException("Failed to evaluate: " + src);
					} else {
						throw new PythonException("PyEval_EvalCode() failed. src = " + src);
					}
				}
				TypeTreeBuilder builder = new TypeTreeBuilder(1);
				return (T) toJava(obj, builder);
			} finally {
				Py_DecRef(obj);
			}
		} finally {
			Py_DecRef(co);
		}
	}

	/**
	 * Python built-in exec().
	 *
	 * @param src Python code. This can be multiple lines code.
	 * @return the console printouts
	 */
	public synchronized static String exec(String src) {
		resetPrintStream();
		if (PyRun_SimpleStringFlags(src, null) != 0) {
			String pythonErrorConsole = eval(ERRORSTREAM + ".getvalue()");
			resetErrorStream();
			throw new PythonException("Failed to execute: " + src + "\n" + pythonErrorConsole);
		}
		return eval(PRINTSTREAM + ".getvalue()");
	}

	/**
	 * Get the global Python variable and convert it to a Java object.
	 * <table border="1">
	 * <caption>Type mappings. Python to Java.</caption>
	 * <thead>
	 * <tr>
	 * <th>Python</th>
	 * <th>Java</th>
	 * </tr>
	 * </thead>
	 * <tbody>
	 * <tr>
	 * <td>None</td>
	 * <td>null</td>
	 * </tr>
	 * <tr>
	 * <td>bool</td>
	 * <td>boolean</td>
	 * </tr>
	 * <tr>
	 * <td>int</td>
	 * <td>long</td>
	 * </tr>
	 * <tr>
	 * <td>float</td>
	 * <td>double</td>
	 * </tr>
	 * <tr>
	 * <td>str</td>
	 * <td>String</td>
	 * </tr>
	 * <tr>
	 * <td>bytes</td>
	 * <td>byte[]</td>
	 * </tr>
	 * <tr>
	 * <td>bytearray</td>
	 * <td>byte[]</td>
	 * </tr>
	 * <tr>
	 * <td>dict</td>
	 * <td>LinkedHashMap</td>
	 * </tr>
	 * <tr>
	 * <td>iterable</td>
	 * <td>ArrayList</td>
	 * </tr>
	 * </tbody>
	 * </table>
	 *
	 * @param name The variable name
	 * @param <T> The Java class after conversion from Python.
	 * @return The Java object converted from the Python object.
	 * @throws PythonException If the value cannot convert to a Java object.
	 * @throws NoSuchElementException If the variable does not exists.
	 */
	@SuppressWarnings("unchecked")
	public synchronized static <T> T get(String name) {
		TypeTreeBuilder builder = new TypeTreeBuilder(1);
		return (T) toJava(getPyObject(name), builder);
	}

	private static PyObject getPyObject(String name) {
		PyObject obj = PyDict_GetItemString(globals, name);
		if (obj == null) {
			throw new NoSuchElementException("name = " + name);
		}
		return obj;
	}

	/**
	 * Convert the Java object and set it to the global Python variable.
	 * <table border="1">
	 * <caption>Type mappings. Java to Python.</caption>
	 * <thead>
	 * <tr>
	 * <th>Java</th>
	 * <th>Python</th>
	 * </tr>
	 * </thead>
	 * <tbody>
	 * <tr>
	 * <td>null</td>
	 * <td>None</td>
	 * </tr>
	 * <tr>
	 * <td>boolean</td>
	 * <td>bool</td>
	 * </tr>
	 * <tr>
	 * <td>byte</td>
	 * <td>int</td>
	 * </tr>
	 * <tr>
	 * <td>short</td>
	 * <td>int</td>
	 * </tr>
	 * <tr>
	 * <td>char</td>
	 * <td>int</td>
	 * </tr>
	 * <tr>
	 * <td>int</td>
	 * <td>int</td>
	 * </tr>
	 * <tr>
	 * <td>long</td>
	 * <td>int</td>
	 * </tr>
	 * <tr>
	 * <td>float</td>
	 * <td>float</td>
	 * </tr>
	 * <tr>
	 * <td>double</td>
	 * <td>float</td>
	 * </tr>
	 * <tr>
	 * <td>String</td>
	 * <td>str</td>
	 * </tr>
	 * <tr>
	 * <td>byte[]</td>
	 * <td>bytes</td>
	 * </tr>
	 * <tr>
	 * <td>java.util.Map</td>
	 * <td>dict</td>
	 * </tr>
	 * <tr>
	 * <td>Object[]</td>
	 * <td>list</td>
	 * </tr>
	 * <tr>
	 * <td>Iterable</td>
	 * <td>list</td>
	 * </tr>
	 * </tbody>
	 * </table>
	 *
	 * @param name The variable name
	 * @param value The value to put.
	 * @throws PythonException If the value cannot convert to a Python object.
	 */
	public synchronized static void put(String name, Object value) {
		TypeTreeBuilder builder = new TypeTreeBuilder(1);
		putPyObject(name, toPyObject(value, builder));
	}

	private static void putPyObject(String name, PyObject obj) {
		try {
			if (PyDict_SetItemString(globals, name, obj) != 0) {
				throw new PythonException("PyDict_SetItemString() failed");
			}
		} finally {
			Py_DecRef(obj);
		}
	}

	private static Object toJava(PyObject obj, TypeTreeBuilder builder) {
		PyObject iterator;
		PyTypeObject t = obj.ob_type();
		if (PyNone_Check(obj)) {
			builder.addType("None");
			return null;
		} else if (PyBool_Check(obj)) {
			builder.addType("bool");
			return PyLong_AsLong(obj) != 0;
		} else if (PyLong_Check(obj)) {
			builder.addType("int");
			return PyLong_AsLong(obj);
		} else if (PyFloat_Check(obj)) {
			builder.addType("float");
			return PyFloat_AsDouble(obj);
		} else if (PyUnicode_Check(obj)) {
			builder.addType("str");
			return new BytePointer(PyUnicode_AsUTF8(obj)).getString(UTF_8);
		} else if (PyBytes_Check(obj)) {
			builder.addType("bytes");
			byte[] ary = new byte[lengthToInt(PyBytes_Size(obj))];
			new BytePointer(PyBytes_AsString(obj)).get(ary);
			return ary;
		} else if (PyByteArray_Check(obj)) {
			builder.addType("bytearray");
			byte[] ary = new byte[lengthToInt(PyByteArray_Size(obj))];
			new BytePointer(PyByteArray_AsString(obj)).get(ary);
			return ary;
		} else if (PyDict_Check(obj)) {
			builder.addType("dict");
			builder.tab++;

			SizeTPointer pos = new SizeTPointer(1).put(0);
			LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
			while (true) {
				PyObject key = new PyObject();
				PyObject value = new PyObject();
				int ok = PyDict_Next(obj, pos, key, value);
				if (ok == 0) {
					break;
				}

				builder.addType("item");
				builder.tab++;
				map.put(toJava(key, builder), toJava(value, builder));
				builder.tab--;
			}

			builder.tab--;
			return map;
		} else if ((iterator = getIter(obj)) != null) {
			try {
				builder.addType("iterable(" + new BytePointer(t.tp_name()).getString(UTF_8) + ")");
				builder.tab++;

				ArrayList<Object> list = new ArrayList<>();
				while (true) {
					PyObject item = PyIter_Next(iterator);
					try {
						if (item == null) {
							break;
						}
						list.add(toJava(item, builder));
					} finally {
						Py_DecRef(item);
					}
				}

				builder.tab--;
				return list;
			} finally {
				Py_DecRef(iterator);
			}
		}

		builder.addType(new BytePointer(t.tp_name()).getString(UTF_8) + "  <- Unsupported");
		PyObject valueStrObj = PyObject_Str(obj);
		try {
			String msgPrefix = "Cannot convert the Python object to a Java object.\n" + "\nValue type tree\n"
				+ builder.toString();
			if (valueStrObj == null) {
				throw new PythonException(msgPrefix);
			} else {
				String valueStr = new BytePointer(PyUnicode_AsUTF8(valueStrObj)).getString(UTF_8);
				throw new PythonException(msgPrefix + "\nvalue = " + valueStr);
			}
		} finally {
			Py_DecRef(valueStrObj);
		}
	}

	private static int lengthToInt(long length) {
		if (length > Integer.MAX_VALUE) {
			throw new PythonException("Cannot convert because the length is larger than 2G");
		}
		return (int) length;
	}

	/**
	 * Don't forget to call Py_DecRef().
	 */
	private static PyObject toPyObject(Object value, TypeTreeBuilder builder) {
		if (value == null) {
			builder.addType("null");
			return _Py_NoneStruct();
		} else if (value instanceof Boolean) {
			builder.addType("Boolean");
			return PyBool_FromLong((Boolean) value ? 1 : 0);
		} else if (value instanceof Byte) {
			builder.addType("Byte");
			return PyLong_FromLong((Byte) value);
		} else if (value instanceof Character) {
			builder.addType("Character");
			return PyLong_FromLong((Character) value);
		} else if (value instanceof Short) {
			builder.addType("Short");
			return PyLong_FromLong((Short) value);
		} else if (value instanceof Integer) {
			builder.addType("Integer");
			return PyLong_FromLong((Integer) value);
		} else if (value instanceof Long) {
			builder.addType("Long");
			return PyLong_FromLong((Long) value);
		} else if (value instanceof Float) {
			builder.addType("Float");
			return PyFloat_FromDouble((Float) value);
		} else if (value instanceof Double) {
			builder.addType("Double");
			return PyFloat_FromDouble((Double) value);
		} else if (value instanceof String) {
			builder.addType("String");
			return PyUnicode_FromString((String) value);
		} else if (value instanceof byte[]) {
			builder.addType("byte[]");
			byte[] ary = (byte[]) value;
			return PyBytes_FromStringAndSize(new BytePointer(ary), ary.length);
		} else if (value instanceof Map) {
			builder.addType("Map");
			builder.tab++;

			@SuppressWarnings("unchecked")
			Map<Object, Object> map = (Map<Object, Object>) value;
			PyObject obj = PyDict_New();
			map.forEach((key, v) -> {
				builder.addType("Map.Entry");
				builder.tab++;
				PyDict_SetItem(obj, toPyObject(key, builder), toPyObject(v, builder));
				builder.tab--;
			});

			builder.tab--;
			return obj;
		} else if (value instanceof Object[]) {
			builder.addType("Object[]");
			builder.tab++;

			Object[] ary = (Object[]) value;
			PyObject obj = PyList_New(ary.length);
			for (int i = 0; i < ary.length; i++) {
				PyList_SetItem(obj, i, toPyObject(ary[i], builder));
			}

			builder.tab--;
			return obj;
		} else if (value instanceof Iterable) {
			builder.addType("Iterable(" + value.getClass().getName() + ")");
			builder.tab++;

			@SuppressWarnings("unchecked")
			Iterable<Object> iter = (Iterable<Object>) value;
			PyObject obj = PyList_New(0);
			iter.forEach(v -> PyList_Append(obj, toPyObject(v, builder)));

			builder.tab--;
			return obj;
			// } else if (value instanceof Functions.Function0) {
			// builder.addType("Functions.Function0");
			// @SuppressWarnings("unchecked")
			// Functions.Function0<Object> fn = (Functions.Function0<Object>) value;
			// return toPyCFunction(args -> fn.apply());
			// } else if (value instanceof Functions.Function1) {
			// builder.addType("Functions.Function1");
			// @SuppressWarnings("unchecked")
			// Functions.Function1<Object, Object> fn = (Functions.Function1<Object, Object>) value;
			// return toPyCFunction(args -> fn.apply(args[0]));
			// } else if (value instanceof Functions.Function2) {
			// builder.addType("Functions.Function2");
			// @SuppressWarnings("unchecked")
			// Functions.Function2<Object, Object, Object> fn = (Functions.Function2<Object, Object, Object>) value;
			// return toPyCFunction(args -> fn.apply(args[0], args[1]));
			// } else if (value instanceof Functions.Function3) {
			// builder.addType("Functions.Function3");
			// @SuppressWarnings("unchecked")
			// Functions.Function3<Object, Object, Object, Object> fn = (Functions.Function3<Object, Object, Object,
			// Object>) value;
			// return toPyCFunction(args -> fn.apply(args[0], args[1], args[2]));
			// } else if (value instanceof Functions.Function4) {
			// builder.addType("Functions.Function4");
			// @SuppressWarnings("unchecked")
			// Functions.Function4<Object, Object, Object, Object, Object> fn = (Functions.Function4<Object, Object,
			// Object, Object, Object>) value;
			// return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3]));
			// } else if (value instanceof Functions.Function5) {
			// builder.addType("Functions.Function5");
			// @SuppressWarnings("unchecked")
			// Functions.Function5<Object, Object, Object, Object, Object, Object> fn = (Functions.Function5<Object,
			// Object, Object, Object, Object, Object>) value;
			// return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4]));
			// } else if (value instanceof Functions.Function6) {
			// builder.addType("Functions.Function6");
			// @SuppressWarnings("unchecked")
			// Functions.Function6<Object, Object, Object, Object, Object, Object, Object> fn =
			// (Functions.Function6<Object, Object, Object, Object, Object, Object, Object>) value;
			// return toPyCFunction(args -> fn.apply(args[0], args[1], args[2], args[3], args[4], args[5]));
		}
		builder.addType(value.getClass().getName() + "  <- Unsupported");
		throw new PythonException("Cannot convert the Java object to a Python object.\n" + "\nValue type tree\n"
			+ builder.toString() + "\nvalue = " + value);
	}

	private static PyObject toPyCFunction(Function<Object[], Object> fn) {
		PyCFunction pyFunc = new PyCFunction() {
			@Override
			public PyObject call(PyObject self, PyObject args) {
				try {
					TypeTreeBuilder builderToJava = new TypeTreeBuilder(1);
					builderToJava.addType("(arguments)");
					builderToJava.tab++;
					Object[] objs = new Object[(int) PyTuple_Size(args)];
					for (int i = 0; i < objs.length; i++) {
						objs[i] = toJava(PyTuple_GetItem(args, i), builderToJava);
					}
					builderToJava.tab--;

					TypeTreeBuilder builderToPython = new TypeTreeBuilder(1);
					builderToPython.addType("(return value)");
					builderToPython.tab++;
					PyObject pyObject = toPyObject(fn.apply(objs), builderToPython);
					builderToPython.tab--;
					return pyObject;
				} catch (Throwable e) {
					e.printStackTrace();

					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw, true);
					e.printStackTrace(pw);
					String msg = sw.getBuffer().toString();
					PyErr_SetString(PyExc_RuntimeError(), msg);

					return null;
				}
			}
		};
		PyMethodDef methodDef = new PyMethodDef().ml_name(new BytePointer("org.bytedeco.embeddedpython"))
			.ml_meth(pyFunc).ml_flags(METH_VARARGS);
		return PyCFunction_NewEx(methodDef, null, mainModule);
	}

	private static long[] toLongArray(int[] intAry) {
		return Arrays.stream(intAry).mapToLong(x -> x).toArray();
	}

	private static int[] toIntArray(long[] longAry) {
		return Arrays.stream(longAry).mapToInt(x -> (int) x).toArray();
	}

	private static int[] toIntArrayDiv(long[] longAry, int v) {
		return Arrays.stream(longAry).mapToInt(x -> (int) (x / v)).toArray();
	}

	/**
	 * Don't forget to call Py_DecRef().
	 */
	private static PyObject getIter(PyObject obj) {
		PyObject iterator = PyObject_GetIter(obj);
		if (iterator == null) {
			PyErr_Clear();
		}
		return iterator;
	}
}
