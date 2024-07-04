package datastructs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import util.ByteArrayUtil;

/**
 * 
 */
public class ConstantEntry extends Datastruct {

	public final static int CONSTANT_Class = 7;
	public final static int CONSTANT_Fieldref = 9;
	public final static int CONSTANT_Methodref = 10;
	public final static int CONSTANT_InterfaceMethodref = 11;
	public final static int CONSTANT_String = 8;
	public final static int CONSTANT_Integer = 3;
	public final static int CONSTANT_Float = 4;
	public final static int CONSTANT_Long = 5;
	public final static int CONSTANT_Double = 6;
	public final static int CONSTANT_NameAndType = 12;
	public final static int CONSTANT_Utf8 = 1;
	public final static int CONSTANT_MethodHandle = 15;
	public final static int CONSTANT_MethodType = 16;
	public final static int CONSTANT_InvokeDynamic = 18;

	/**
	 * Constant pool entry's tag.
	 */
	private byte type;

	/**
	 * First byte's index.
	 * If we end up changing the table we'll probably want to calculate this instead of save it. 
	 */
	private int byte_index;

	/**
	 * A copy of this constant table entry's data.
	 */
	private byte data[];

	/**
	 * Create a new Utf8 constant_pool entry.
	 * @param str
	 */
	public ConstantEntry(String str) {
		byte str_bytes[] = str.getBytes();
		this.data = new byte[str_bytes.length + 3];
		this.data[0] = CONSTANT_Utf8;

		byte len_bytes[] = ByteArrayUtil.intTo2Bytes(str_bytes.length);
		this.data[1] = len_bytes[0];
		this.data[2] = len_bytes[1];

		for (int i = 0; i < str_bytes.length; ++i) {
			this.data[3 + i] = str_bytes[i];
		}
	}

	/**
	 * Parse the constant table entry at the index index.
	 * @param array
	 * @param data_index
	 */
	public ConstantEntry(byte all_data[], int data_index) {
		this.type = all_data[data_index];
		this.byte_index = data_index;

		int length = 1;
		switch (this.type) {
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
			length += 2 + ByteArrayUtil.bytes2(all_data, this.byte_index + 1);
			break;
		}

		this.data = Arrays.copyOfRange(all_data, this.byte_index, this.byte_index + length);
	}

	/**
	 * Same as regular toString() except adds the index at the start.
	 * Warning: this will modify constant_pool by replacing elements with null.
	 * Remember that indices are 1 indexed.
	 * @return
	 */
	public static String toString(int index, byte data[], ConstantEntry constant_pool[]) {
		ConstantEntry entry = constant_pool[index - 1];
		if (entry == null) {
			return "";
		}
		constant_pool[index - 1] = null;

		String str = "[" + index + "]";
		return str + entry.toString(true, data, constant_pool);
	}

	public String toString(boolean recursive, byte data[], ConstantEntry constant_pool[]) {
		String str = "";

		switch (this.getType()) {
		case CONSTANT_Methodref: {
			ConstantEntry entry = this;
			str += "CONSTANT_Methodref_info{\n";
			int byte_index = entry.getByteIndex() + 1;
			str += "    tag = " + entry.getType() + "\n";

			int class_index = ByteArrayUtil.bytes2(data, byte_index);
			str += "    class_index = " + class_index + "\n";
			byte_index += 2;

			int name_and_type_index = ByteArrayUtil.bytes2(data, byte_index);
			str += "    name_and_type_index = " + name_and_type_index + "\n";

			str += "}\n";

			if (recursive) {
				str += ConstantEntry.toString(class_index, data, constant_pool);
				str += ConstantEntry.toString(name_and_type_index, data, constant_pool);
			}
			break;
		}
		case CONSTANT_NameAndType: {
			ConstantEntry entry = this;
			str += "CONSTANT_NameAndType_info{\n";
			int byte_index = entry.getByteIndex() + 1;
			str += "    tag = " + entry.getType() + "\n";

			int name_index = ByteArrayUtil.bytes2(data, byte_index);
			str += "    name_index = " + name_index + "\n";
			byte_index += 2;

			int descriptor_index = ByteArrayUtil.bytes2(data, byte_index);
			str += "    descriptor_index = " + descriptor_index + "\n";

			str += "}\n";

			if (recursive) {
				str += ConstantEntry.toString(name_index, data, constant_pool);
				str += ConstantEntry.toString(descriptor_index, data, constant_pool);
			}

			break;
		}
		case CONSTANT_Utf8: {
			ConstantEntry entry = this;
			str += "CONSTANT_Utf8_info{\n";
			int byte_index = entry.getByteIndex() + 1;
			str += "    tag = " + entry.getType() + "\n";

			int length = ByteArrayUtil.bytes2(data, byte_index);
			str += "    length = " + length + "\n";
			byte_index += 2;

			String name = parseCONSTANT_Utf8(data, entry);
			str += "    bytes[] = " + name + "\n";

			str += "}\n";
			break;
		}
		case CONSTANT_Class: {
			ConstantEntry entry = this;
			str += "CONSTANT_Class_info{\n";
			int byte_index = entry.getByteIndex() + 1;
			str += "    tag = " + entry.getType() + "\n";

			int name_index = ByteArrayUtil.bytes2(data, byte_index);
			str += "    name_index = " + name_index + "\n";

			str += "}\n";

			if (recursive) {
				str += ConstantEntry.toString(name_index, data, constant_pool);
			}
			break;
		}
		default: {
			str += "I don't know how to print that type.\n";
			break;
		}
		}

		return str;
	}

	public static String parseCONSTANT_Utf8(byte data[], ConstantEntry entry) {
		//Get string length
		int length = entry.getTotalByteLength() - (1 + 2);

		//<convert to str hopefully>
		int strIndex = entry.getByteIndex() + (1 + 2) + length;
		char str[] = new char[length];
		for (--length, --strIndex; length >= 0; --length, --strIndex) {
			str[length] = (char) data[strIndex];
		}
		//</convert to str hopefully>
		return new String(str);
	}

	@Override
	public int getTotalByteLength() {
		return data.length;
	}

	@Override
	public void toBytes(ByteArrayOutputStream buf) throws IOException {
		buf.write(data);
	}

	public byte getType() {
		return type;
	}

	public int getByteIndex() {
		return byte_index;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public void setByteIndex(int index) {
		this.byte_index = index;
	}
}
