package datastructs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import util.ByteArrayUtil;

public class Code extends Datastruct {
	//u2 attribute_name_index;
	private int attribute_name_index;

	//u4 attribute_length;
	private int attribute_length;

	//u2 max_stack;
	private int max_stack;

	//u2 max_locals;
	private int max_locals;

	//u4 code_length;
	private int code_length;

	//u1 code[code_length];
	//private byte code[];
	private int code_index;
	private byte rawdata[];

	//u2 exception_table_length;
	private int exception_table_length;

	//u8 exception_table
	private Exception_entry exception_table[];

	//u2 attributes_count;
	private int attributes_count;

	//attribute_info attributes[attributes_count];
	private AttributeInfo attributes[];

	/**
	 * TODO: Full parsing rather than half [don't skip attribute_name_index]
	 * @param data
	 * @param index
	 * @param attribute_length
	 */
	public Code(byte data[], int index, int attribute_length) {
		/*this.attribute_name_index = ByteArrayUtil.bytes2(data, index);
		index += 2;
		System.out.println("1 = " + attribute_name_index);*/

		/*this.attribute_length = ByteArrayUtil.bytes4(data, index);
		index += 4;
		System.out.println("2 = " + attribute_length);*/
		System.out.println("2 = " + attribute_length);

		this.max_stack = ByteArrayUtil.bytes2(data, index);
		index += 2;
		System.out.println("3 = " + max_stack);

		this.max_locals = ByteArrayUtil.bytes2(data, index);
		index += 2;
		System.out.println("4 = " + max_locals);

		this.code_length = ByteArrayUtil.bytes4(data, index);
		index += 4;
		System.out.println("5 = " + code_length);

		this.code_index = index;
		if (index % 4 != 0) {
			throw new Error("OH GOD WHAT DO? Something _might_ be wrong.");
		}

		this.rawdata = data;
		index += this.code_length;

		this.exception_table_length = ByteArrayUtil.bytes2(data, index);
		index += 2;

		this.exception_table = new Exception_entry[exception_table_length];
		for (int i = 0; i < exception_table.length; ++i) {
			exception_table[i] = new Exception_entry(data, index);
			index += 8;
		}

		this.attributes_count = ByteArrayUtil.bytes2(data, index);
		index += 2;

		System.out.println("7 = " + attributes_count);
		this.attributes = new AttributeInfo[attributes_count];
		for (int i = 0; i < attributes.length; ++i) {
			this.attributes[i] = new AttributeInfo(data, index);
			index += attributes[i].getTotalByteLength();
		}
	}

	@Override
	public int getTotalByteLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void toBytes(ByteArrayOutputStream buf) throws IOException {
		// TODO Auto-generated method stub
	}

	public String toString() {
		String str = "Code {";

		str += "attribute_name_index = " + attribute_name_index + "\n";
		str += "attribute_length = " + attribute_length + "\n";
		str += "max_stack = " + max_stack + "\n";
		str += "max_locals = " + max_locals + "\n";
		str += "code_length = " + code_length + "\n";

		str += Arrays.toString(Arrays.copyOfRange(rawdata, code_index, code_index + code_length)) + "\n";

		str += "exception_table_length = " + exception_table_length + "\n";
		str += "exception_table = " + exception_table + "\n";
		str += "attributes_count = " + attributes_count + "\n";
		//str += "attributes = " + attributes + "\n";

		for (int i = 0; i < attributes.length; ++i) {
			str += attributes[i].toString();
		}

		str += "}";
		return str;
	}

	public int getAttribute_name_index() {
		return attribute_name_index;
	}

	public int getAttribute_length() {
		return attribute_length;
	}

	public int getMax_stack() {
		return max_stack;
	}

	public int getMax_locals() {
		return max_locals;
	}

	public int getCode_length() {
		return code_length;
	}

	public int getCode_index() {
		return code_index;
	}

	public byte[] getRawdata() {
		return rawdata;
	}

	public int getException_table_length() {
		return exception_table_length;
	}

	public Exception_entry[] getException_table() {
		return exception_table;
	}

	public int getAttributes_count() {
		return attributes_count;
	}

	public AttributeInfo[] getAttributes() {
		return attributes;
	}

	public byte[] getCode() {
		return Arrays.copyOfRange(rawdata, code_index, code_index + code_length);
	}
}

class Exception_entry {
	//u2 start_pc;
	int start_pc;

	//u2 end_pc;
	int end_pc;

	//u2 handler_pc;
	int handler_pc;

	//u2 catch_type;
	int catch_type;

	public Exception_entry(byte data[], int index) {
		this.start_pc = ByteArrayUtil.bytes2(data, index);
		index += 2;
		this.end_pc = ByteArrayUtil.bytes2(data, index);
		index += 2;
		this.handler_pc = ByteArrayUtil.bytes2(data, index);
		index += 2;
		this.catch_type = ByteArrayUtil.bytes2(data, index);
	}
}
