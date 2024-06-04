package jscheme;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

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

	public String[] getFiles() {
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
										.concat("\trepl \t\t - Starts the repl\n")
										.concat("\thelp\t\t - Displays this help information\n")
										.concat("\tversion\t\t - Displays the version of this program\n");

		String logo = 
"\t _           _   _____     _ \n" +                  
"\t| |_ ___ ___| |_|   __|___| |_ ___ _____ ___\n" + 
"\t|   | .'|  _| '_|__   |  _|   | -_|     | -_|\n" + 
"\t|_|_|__,|___|_,_|_____|___|_|_|___|_|_|_|___|\n\n" + 
"\t\t a proud fork of JScheme :) \n\n" + 
"\t\t (c) 2024 - Gama Sibusiso\n\n";
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

	public void parse(ArgParser parser) {
		String top = parser.get();
		this.name = parser.getProgram();
		boolean is_adding_files = false;
		boolean has_error = false;
		while (top != null) {
			if (top.equals("repl")) {
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
	public Scheme(String[] files) {
		Primitive.installPrimitives(globalEnvironment);
		try {
			load(new InputPort(new StringReader(SchemePrimitives.CODE)));
			for (int i = 0; i < (files == null ? 0 : files.length); i++) {
				load(files[i]);
			}

			if (files == null) {
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
		ArgParser argp = new ArgParser("HackScheme", args);
		Config conf = new Config();
		conf.parse(argp);

		String[] files = conf.getFiles();
		Scheme scheme = new Scheme(files);
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
			return error("can't load " + name);
		}
	}

	public Object _import(Object fileName) {
		String name = stringify(fileName, false).concat(".scm");
		return load(name);
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
