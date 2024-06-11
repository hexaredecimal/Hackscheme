package jscheme;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import package_manager.DependecyResolver;
import javassist.*;

class Config {

	private ArrayList<String> files;
	private String version;
	private String author;
	private boolean repl;
	private String name;

	public Config(String[] files, String v, String auth, boolean r) {
		this.files = new ArrayList<>();
		for (String file : files) {
			this.files.add(file);
		}
		this.version = v;
		this.author = auth;
		this.repl = r;
		this.name = "";
	}

	Config() {
		this.files = new ArrayList<>();
	}

	public String getVersion() {
		return version;
	}

	public String getAuthor() {
		return author;
	}

	public void setProgram(String name) {
		this.name = name;
	}

	public String[] getFiles() {
		if (Config.isProject()) {
			this.files.add("main.scm");
		}

		Object[] objs = this.files.toArray();
		String[] files = new String[objs.length];
		for (int i = 0; i < objs.length; i++) {
			String file = (String) objs[i];
			files[i] = file;
		}
		return files;
	}

	public boolean isRepl() {
		return repl;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setRepl(boolean repl) {
		this.repl = repl;
	}

	public void addFile(String file) {
		this.files.add(file);
	}

	public void help() {
		String out_put = this.name.concat(" [options] <file(s)>");
		out_put += "\n\n";
		out_put
						+= "[options]\n"
										.concat("\tnew\t\t - Creates a new project\n")
										.concat("\trepl \t\t - Starts the repl\n")
										.concat("\thelp\t\t - Displays this help information\n")
										.concat("\tversion\t\t - Displays the version of this program\n");

		String logo
						= "\t _           _   _____     _ \n"
						+ "\t| |_ ___ ___| |_|   __|___| |_ ___ _____ ___\n"
						+ "\t|   | .'|  _| '_|__   |  _|   | -_|     | -_|\n"
						+ "\t|_|_|__,|___|_,_|_____|___|_|_|___|_|_|_|___|\n\n"
						+ "\t\t a proud fork of JScheme :) \n\n"
						+ "\t\t (c) 2024 - Gama Sibusiso\n\n";
		System.out.println(out_put);
		System.out.println(logo);
		System.exit(101);
	}

	public void showVersion() {
		System.out.println(this.name.concat(" version ").concat(this.version));
		System.exit(101);
	}

	public void reportError(String err) {
		System.out.println("error: ".concat(err));
		System.exit(101);
	}

	public void initNewProject(String project_name) {
		if (project_name != null) {
			createProjectFile(name);
			return;
		}

		Scanner sc = new Scanner(System.in);
		System.out.print("Enter project name: ");
		String name = sc.nextLine();

		if (name.isBlank()) {
			System.err.println("Error: Invalid project name");
			System.exit(1);
		}

		createProjectFile(name);
		System.out.println("Successfully created a new project");
		System.exit(0);
		return;
	}

	public static boolean isProject() {
		File project = new File("project.toml");
		File project_dir = new File(".pkg");
		return project.exists() && project_dir.exists() && project_dir.isDirectory();
	}

	private void createProjectFile(String project_name) {
		if (isProject()) {
			System.err.println("Error: project files already exists");
			System.exit(1);
		}

		String txt
						= "[package]\n"
						+ "name = \"" + project_name + "\"\n"
						+ "version = \"0.0.1\"\n"
						+ "authors = []\n"
						+ "\n"
						+ "\n"
						+ "[dependencies]\n";
		String project_file = "project.toml";
		File fp = new File(project_file);
		File fp_dir = new File(".pkg");
		try {
			File main_file = new File("main.scm");
			main_file.createNewFile();
			fp_dir.mkdirs();
			fp.createNewFile();
			FileWriter fw = new FileWriter(fp);
			fw.write(txt);
			fw.close();
			FileAttributes att = new FileAttributes(fp.getAbsolutePath());
			att.saveAttributes();
		} catch (IOException e) {
			System.err.println("Error: failed to initialize a new project");
			System.exit(1);
		}
	}

	public void parse(ArgParser parser) {
		String top = parser.get();
		boolean is_adding_files = false;
		boolean has_error = false;
		while (top != null) {
			if (top.equals("new")) {
				String name = parser.get();
				this.initNewProject(name);
				break;
			} else if (top.equals("repl")) {
				if (is_adding_files) {
					has_error = true;
					break;
				}
				this.setRepl(true);
			} else if (top.equals("help")) {
				if (is_adding_files) {
					has_error = true;
					break;
				}
				this.help();
			} else if (top.equals("version")) {
				if (is_adding_files) {
					has_error = true;
					break;
				}
				this.showVersion();
			} else {
				is_adding_files = true;
				this.addFile(top);
			}
			top = parser.get();
		}

		if (has_error) {
			reportError("Attempt to set `".concat("argument").concat("` while reading files"));
		}
	}
}

/**
 * This class represents a Scheme interpreter. See
 * http://www.norvig.com/jscheme.html for more documentation. This is version
 * 1.4.
 *
 * @author Peter Norvig, peter@norvig.com http://www.norvig.com Copyright 1998
 * Peter Norvig, see http://www.norvig.com/license.html *
 */
public class Scheme extends SchemeUtils {

	InputPort input = new InputPort(System.in);
	PrintWriter output = new PrintWriter(System.out, true);
	Environment globalEnvironment = new Environment();

	/**
	 * Create a Scheme interpreter and load an array of files into it. Also load
	 * SchemePrimitives.CODE. *
	 */
	public Scheme(String[] files, boolean is_project) {
		Primitive.installPrimitives(globalEnvironment);
		try {
			load(new InputPort(new StringReader(SchemePrimitives.CODE)));
			for (int i = 0; i < (files == null ? 0 : files.length); i++) {
				load(files[i]);
			}

			if (files == null || files.length == 0) {
				return;
			}

			Object entry = globalEnvironment.lookup("main");
			if (entry != null) {
				if (entry instanceof Closure) {
					Closure c = (Closure) entry;
					c.apply(this, null);
				}
			}
		} catch (RuntimeException e) {
			;
		}
	}

	//////////////// Main Loop
	/**
	 * Create a new Scheme interpreter, passing in the command line args as files
	 * to load, and then enter a read eval write loop. *
	 */
	public static void main(String[] args) {
		ArgParser argp = new ArgParser(args);
		Config conf = new Config();
		conf.setProgram("HackScheme");
		conf.setVersion("0.0.5");
		conf.parse(argp);

		if (Config.isProject()) {
			FileAttributes current_config = new FileAttributes("project.toml");
			FileAttributes previous_config = current_config.loadAttributes();

			// System.out.println(current_config + " : " + previous_config);
			if (!current_config.equals(previous_config)) {
				DependecyResolver deps = new DependecyResolver(true, current_config);
				deps.resolve();
				System.out.println("Done resolving all dependencies");
			}
			current_config.saveAttributes();
		}

		String[] files = conf.getFiles();
		Scheme scheme = new Scheme(files, Config.isProject());

		if (conf.isRepl()) {
			scheme.readEvalWriteLoop();
		}
	}

	/**
	 * Prompt, read, eval, and write the result. Also sets up a catch for any
	 * RuntimeExceptions encountered. *
	 */
	public void readEvalWriteLoop() {
		Object x;
		System.out.println("Welcome to HackScheme");
		for (;;) {
			try {
				output.print("λ ");
				output.flush();
				if (input.isEOF(x = input.read())) {
					return;
				}
				write(eval(x), output, true);
				output.println();
				output.flush();
			} catch (RuntimeException e) {
				;
			}
		}
	}

	/**
	 * Eval all the expressions in a file. Calls load(InputPort). *
	 */
	public Object load(Object fileName) {
		String name = stringify(fileName, false);
		try {
			return load(new InputPort(new FileInputStream(name)));
		} catch (IOException e) {
			return error("can't load " + name + ", with message: " + e.getMessage());
		}
	}

	public Object _import(Object fileName) {
		String name = stringify(fileName, false).concat(".scm");

		File fp = new File(name);
		if (!fp.exists()) {
			return loadFromDependencyList(name);
		}
		return load(name);
	}

	private Object loadFromDependencyList(String name) {
		File fp = new File(".pkg");
		if (!fp.isDirectory()) {
			System.out.println("Error: Cannot locate project directory");
			System.exit(101);
		}

		Object value = null;
		for (File inner : fp.listFiles()) {
			if (inner.isDirectory()) {
				value = getNested(inner, name);
			}
		}
		return value;
	}

	private Object getNested(File fp, String name) {
		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.getName().endsWith(".scm");
			}
		};
		for (File src : fp.listFiles(filter)) {
			String file_name = src.getName();

			if (file_name.equals(name)) {
				return load(src.getAbsolutePath());
			}
		}
		return null;
	}

	/**
	 * Eval all the expressions coming from an InputPort. *
	 */
	public Object load(InputPort in) {
		Object x = null;
		for (;;) {
			if (in.isEOF(x = in.read())) {
				return TRUE;
			}
			eval(x);
		}
	}

	private static CtClass resolveCtClass(String type, ClassPool pool) throws NotFoundException {
		switch (type) {
			case "int":
				return CtClass.intType;
			case "boolean":
				return CtClass.booleanType;
			case "float":
				return CtClass.floatType;
			case "double":
				return CtClass.doubleType;
			// Add other primitive types as needed
			case "string":
				return pool.get("java.lang.String");
			default:
				return pool.get(type);
		}
	}

	private static String restructureType(String t) {
		if (t.equals("string")) {
			return "String";
		}

		return t;
	}

	public String convert(String type_cast, String value) {
		if (type_cast.equals("int")) {
			return "Integer.valueOf(" + value + ".toString()).intValue()";
		}		

		if (type_cast.equals("double")) {
			return "Double.valueOf(" + value + ".toString()).doubleValue()";
		}		

		if (type_cast.equals("float")) {
			return "Float.valueOf(" + value + ".toString()).floatValue()"; 
		}

		if (type_cast.equals("boolean")) {
			return "Boolean.valueOf(" + value + ".toString()).booleanValue()"; 
		}

		if (type_cast.equals("string")) {
			return value + ".toString()";
		}


		return null;
	}

	
	public Object declareStruct(Object obj, Environment env) {
		String name = (String) first(obj);
		Object fields = rest(obj);

		ClassPool pool = ClassPool.getDefault();
		CtClass cls = pool.makeClass(name);

		String constructor_args = "";
		String constructor_inits = "";
		String args = "";
		ArrayList<String> getters = new ArrayList<>();
		while (fields != null) {
			Object field = first(fields);
			String f_name = (String) first(field);
			String f_type = (String) first(rest(field));
			try {
				CtField fd = new CtField(resolveCtClass(f_type, pool), f_name, cls);
				fd.setModifiers(Modifier.PRIVATE);
				cls.addField(fd);
			} catch (NotFoundException ex) {
				Logger.getLogger(Scheme.class.getName()).log(Level.SEVERE, null, ex);
			} catch (CannotCompileException ex) {
				Logger.getLogger(Scheme.class.getName()).log(Level.SEVERE, null, ex);
			}

			fields = rest(fields);
			String conv = convert(f_type, f_name + "[0]");

			if (conv != null)
				constructor_inits = constructor_inits.concat("\tthis.").concat(f_name).concat(" = ").concat(conv).concat(";\n");
			else 
				constructor_inits = constructor_inits.concat("\tthis.").concat(f_name).concat(" = ").concat(f_name).concat(";\n");
			
			String setter= "public void set_" + f_name + "( Object[] " +f_name + ") {\n";
			setter += "\tthis." + f_name + " = " + conv + ";\n}";
			
			constructor_args = constructor_args.concat("Object").concat(" ").concat(f_name);
			args = args.concat(f_name);
			String getter = restructureType(f_type).concat(" get_" + f_name).concat("() { return this.").concat(f_name).concat("; }");
			getters.add(getter);
			getters.add(setter);
			if (fields != null) {
				constructor_args += ", ";
				args += ", ";
			}
		}

		String constructor = "public ".concat(name).concat("(").concat(") {\n");
		constructor = constructor.concat("}\n\n");


		

		try {
			CtConstructor cns = CtNewConstructor.make(constructor, cls);
			cls.addConstructor(cns);
			for (String getter : getters) {
				CtMethod gt = CtNewMethod.make(getter, cls);
				cls.addMethod(gt);
			}
			Class cc = cls.toClass(Thread.currentThread().getContextClassLoader(), Scheme.class.getProtectionDomain());

			for (Method m : cc.getDeclaredMethods()) {
				JavaMethod md = new JavaMethod(m);
				env.define(m.getName(), md);
			}
			
			env.define(name, cc);
			return cc;
		} catch (Exception ex) {
			Logger.getLogger(Scheme.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	//////////////// Evaluation
	/**
	 * Evaluate an object, x, in an environment. *
	 */
	public Object eval(Object x, Environment env) {
		// The purpose of the while loop is to allow tail recursion.
		// The idea is that in a tail recursive position, we do "x = ..."
		// and loop, rather than doing "return eval(...)".
		while (true) {
			if (x instanceof String) {         // VARIABLE
				return env.lookup((String) x);
			} else if (!(x instanceof Pair)) { // CONSTANT
				return x;
			} else {
				Object fn = first(x);
				Object args = rest(x);

				if (fn == "struct") {
					return declareStruct(args, env);
				}
				if (fn == "quote") {             // QUOTE
					return first(args);
				} else if (fn == "begin") {      // BEGIN
					for (; rest(args) != null; args = rest(args)) {
						eval(first(args), env);
					}
					x = first(args);
				} else if (fn == "define") {     // DEFINE
					if (first(args) instanceof Pair) {
						return env.define(first(first(args)),
										eval(cons("lambda", cons(rest(first(args)), rest(args))), env));
					} else {
						return env.define(first(args), eval(second(args), env));
					}
				} else if (fn == "set!") {       // SET!
					return env.set(first(args), eval(second(args), env));
				} else if (fn == "if") {         // IF
					x = (truth(eval(first(args), env))) ? second(args) : third(args);
				} else if (fn == "cond") {       // COND
					x = reduceCond(args, env);
				} else if (fn == "lambda" || fn == "λ") {     // LAMBDA
					return new Closure(first(args), rest(args), env);
				} else if (fn == "macro") {      // MACRO
					return new Macro(first(args), rest(args), env);
				} else {                         // PROCEDURE CALL:
					fn = eval(fn, env);
					if (fn instanceof Macro) {          // (MACRO CALL)
						x = ((Macro) fn).expand(this, (Pair) x, args);
					} else if (fn instanceof Closure) { // (CLOSURE CALL)
						Closure f = (Closure) fn;
						x = f.body;
						env = new Environment(f.parms, evalList(args, env), f.env);
					} else {                            // (OTHER PROCEDURE CALL)
						return Procedure.proc(fn).apply(this, evalList(args, env));
					}
				}
			}
		}
	}

	/**
	 * Eval in the global environment. *
	 */
	public Object eval(Object x) {
		return eval(x, this.globalEnvironment);
	}

	/**
	 * Evaluate each of a list of expressions. *
	 */
	Pair evalList(Object list, Environment env) {
		if (list == null) {
			return null;
		} else if (!(list instanceof Pair)) {
			error("Illegal arg list: " + list);
			return null;
		} else {
			return cons(eval(first(list), env), evalList(rest(list), env));
		}
	}

	/**
	 * Reduce a cond expression to some code which, when evaluated, gives the
	 * value of the cond expression. We do it that way to maintain tail recursion.
	 * *
	 */
	Object reduceCond(Object clauses, Environment env) {
		Object result = null;
		for (;;) {
			if (clauses == null) {
				return FALSE;
			}
			Object clause = first(clauses);
			clauses = rest(clauses);
			if (first(clause) == "else"
							|| truth(result = eval(first(clause), env))) {
				if (rest(clause) == null) {
					return list("quote", result);
				} else if (second(clause) == "=>") {
					return list(third(clause), list("quote", result));
				} else {
					return cons("begin", rest(clause));
				}
			}
		}
	}

}
