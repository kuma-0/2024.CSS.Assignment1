package datastructs;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import util.ByteArrayUtil;

public class ClassFile extends Datastruct {
	/**
	 * Bytes of the original file.
	 * These are not modified.
	 */
	private final byte rawdata[];

	/*
	 * The data that is modified and written out.
	 */
	private ConstantEntry constant_pool[];
	private MethodInfo fields[];
	//TODO: interfaces[] is just skipped over.
	private MethodInfo methods[];
	private AttributeInfo attributes[];

	/*
	 * Indices and such that don't qualify as actual data but can be useful for internal stuff.
	 */
	private final int AFTER_CONSTANT_POOL_INDEX;
	private final int FIELDS_INDEX;

	public ClassFile(String filename) throws IOException {
		//System.out.println(System.getProperty("user.dir"));//pwd
		{
			Path file = Paths.get(filename, new String[0]);
			rawdata = Files.readAllBytes(file);
			System.out.println("data.length = " + rawdata.length);//TODO: deleteme
		}

		/*
		 * Check magic number = 0xCAFEBABE.
		 * This will always be the case if the file is a .class file.
		 */
		{
			int magic = ByteArrayUtil.bytes4(rawdata, 0);
			if (magic != 0xCAFEBABE) {
				//Explode
				throw new IOException("File <" + filename + "> isn't a properly formatted .class file.");
			}
		}

		constant_pool = parseConstantTable(rawdata);

		//Get the index of the first byte after the constant_pool
		int index;
		{
			ConstantEntry last = constant_pool[constant_pool.length - 1];
			index = last.getByteIndex() + last.getTotalByteLength();
		}
		AFTER_CONSTANT_POOL_INDEX = index;

		//u2	access_flags;
		//u2	this_class;
		//u2	super_class;
		index += 2 * 3;

		/*
		 * Skip the interfaces[] table.
		 */
		//u2	interfaces_count;
		//u2	interfaces[interfaces_count];
		{
			int interfaces_count = ByteArrayUtil.bytes2(rawdata, index);
			System.out.println("interfaces_count = " + interfaces_count);//TODO: deleteme
			index += 2;
			index += 2 * interfaces_count;
		}

		/*
		 * Parse the fields[] table.
		 */
		FIELDS_INDEX = index;
		IntegerByRef indexRef = new IntegerByRef(index);
		fields = parseMethodFieldTable(rawdata, indexRef);
		System.out.println("fields.length = " + fields.length);

		/*
		 * We're now (finally) at the methods[] table.
		 * method_info	methods[methods_count];
		 */
		//METHODS_INDEX = indexRef.val;//Because it could be useful for debug stuff [compiler should remove it if it's unused]
		System.out.println("methods[] index = " + indexRef.val);
		methods = parseMethodFieldTable(rawdata, indexRef);
		System.out.println("methods.length = " + methods.length);

		/*
		 * Parse the attributes[] table.
		 */
		attributes = parseAttributesTable(rawdata, indexRef);
	}

	/**
	 * Parse the bytes of a class file to extract a constant table.
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private static ConstantEntry[] parseConstantTable(byte data[]) throws IOException {
		//u4 magic
		//u2 minor_version
		//u2 major_version
		int index = 8;

		/*
		 * Extract and parse the constant table.
		 * The constant table includes method names and this is where we'd look for and modify them.
		 *  First two bytes are the number of constants.
		 *  Parse the array of constants.
		 *  	Constant type is first byte.
		 *  	Rest is variable length based on type.
		 */
		ConstantEntry constant_pool[];
		{
			int constant_pool_count = ByteArrayUtil.bytes2(data, index);//constant_pool_count
			--constant_pool_count;//Because it's +1 for some reason
			index += 2;

			System.out.println("constant_pool_count = " + constant_pool_count + " + 1");//TODO:deleteme
			constant_pool = new ConstantEntry[constant_pool_count];
		}

		//Parse the constant table.
		for (int i = 0; i < constant_pool.length; ++i) {
			System.out.println("i = " + (i + 1) + ", tag = " + data[index]);
			//System.out.println("i = " + (i+1) + ", tag = " + data[index] + ", tag-1 = " + data[index-1] + ", tag+1 = " + data[index+1]);
			constant_pool[i] = new ConstantEntry(data, index);
			if (constant_pool[i].getType() == 1) {
				System.out.println("\tUTF8 = " + ConstantEntry.parseCONSTANT_Utf8(data, constant_pool[i]));
			}
			if (data[index] == 0) {
				System.err.println(data[index - 1] + " : " + data[index] + " : " + data[index + 1]);
			}
			index = constant_pool[i].getByteIndex() + constant_pool[i].getTotalByteLength();

			//System.out.println("[" + i + "] <t#" + constant_pool[i].getType() + ", bi#"
			//		+ constant_pool[i].getByteIndex() + ", l#" + constant_pool[i].getLength() + ">");//TODO:deleteme
		}

		return constant_pool;
	}

	/**
	 * Extract an array of MethodInfo out of the bytes of a class file.
	 * Useful for both fields[] and methods[] since they have the same format.
	 */
	private static MethodInfo[] parseMethodFieldTable(byte data[], IntegerByRef index) {
		//u2	fields_count;
		final int count = ByteArrayUtil.bytes2(data, index.val);
		System.out.println("fields_count = " + count);//TODO: deleteme
		index.val += 2;

		/*
		 * Parse field_info
		 * 	field_info	fields[fields_count]
		 */
		MethodInfo methods[] = new MethodInfo[count];
		for (int i = 0; i < count; ++i) {
			methods[i] = new MethodInfo(data, index.val);
			index.val += methods[i].getTotalByteLength();
		}
		return methods;
	}

	/**
	 * Extract the AttributeInfo attributes[] table out of the bytes of a class file.
	 */
	private static AttributeInfo[] parseAttributesTable(byte data[], IntegerByRef index) {
		//u2	attributes_count;
		final int attributes_count = ByteArrayUtil.bytes2(data, index.val);
		//System.out.println("attributes_count = " + attributes_count);//TODO: deleteme
		index.val += 2;

		AttributeInfo attributes[] = new AttributeInfo[attributes_count];
		/*
		 * Parse attribute_info structures
		 * 	attribute_info	attributes[attributes_count]
		 */
		//System.out.println("\tattributes_count = " + attributes_count);
		for (int i = 0; i < attributes_count; ++i) {
			attributes[i] = new AttributeInfo(data, index.val);
			index.val += attributes[i].getTotalByteLength();
		}
		return attributes;
	}

	@Override
	public int getTotalByteLength() {
		throw new Error();
	}

	@Override
	public void toBytes(ByteArrayOutputStream buf) throws IOException {
		//Write out starting stuff.
		buf.write(rawdata, 0, 4 + 2 + 2);

		//Write out constant_pool_count.
		buf.write(ByteArrayUtil.intTo2Bytes(constant_pool.length + 1));

		//Write out the constant pool.
		for (int i = 0; i < constant_pool.length; ++i) {
			constant_pool[i].toBytes(buf);
		}

		//Write out stuff up to the fields table.
		buf.write(rawdata, AFTER_CONSTANT_POOL_INDEX, FIELDS_INDEX - AFTER_CONSTANT_POOL_INDEX);

		//Write out fields[] table
		buf.write(ByteArrayUtil.intTo2Bytes(fields.length));
		for (int i = 0; i < fields.length; ++i) {
			fields[i].toBytes(buf);
		}

		//Write out methods[] table
		buf.write(ByteArrayUtil.intTo2Bytes(methods.length));
		for (int i = 0; i < methods.length; ++i) {
			methods[i].toBytes(buf);
		}

		//Write out attributes[] table
		buf.write(ByteArrayUtil.intTo2Bytes(attributes.length));
		for (int i = 0; i < attributes.length; ++i) {
			attributes[i].toBytes(buf);
		}

		//System.out.println(Arrays.toString(buf.toByteArray()));
		//System.out.println(Arrays.toString(rawdata));
	}

	/**
	 * Write the class file out to a file.
	 * @param filename
	 * @param data
	 * @throws IOException
	 */
	public void writeFile(String filename) throws IOException {
		byte data[] = this.toBytes();

		//TODO: deleteme
		/*for(int i = 0; i < data.length; ++i){
			if(data[i] != rawdata[i]){
				System.out.println("!= at " + i);
			}
		}*/

		ClassFile.writeFile(filename, data);
	}

	public static void writeFile(String filename, byte data[]) throws IOException {
		File file = new File(filename);
		FileOutputStream out = new FileOutputStream(file);
		out.write(data);
		out.close();
	}

	public byte[] getRawdata() {
		return rawdata;
	}

	public ConstantEntry[] getConstant_pool() {
		return constant_pool;
	}

	public MethodInfo[] getFields() {
		return fields;
	}

	public MethodInfo[] getMethods() {
		return methods;
	}

	public AttributeInfo[] getAttributes() {
		return attributes;
	}

	public void setConstant_pool(ConstantEntry[] constant_pool) {
		this.constant_pool = constant_pool;
	}

	public void setFields(MethodInfo[] fields) {
		this.fields = fields;
	}

	public void setMethods(MethodInfo[] methods) {
		this.methods = methods;
	}

	public void setAttributes(AttributeInfo[] attributes) {
		this.attributes = attributes;
	}
}

/**
 * This class exists so we can pass an integer by reference.
 * Used for indices that are changed by parsing functions.
 */
class IntegerByRef {
	public int val;

	public IntegerByRef(int val) {
		this.val = val;
	}
}