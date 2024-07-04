package datastructs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import util.ByteArrayUtil;

public class AttributeInfo extends Datastruct {
	//u2	attribute_name_index;
	private int attribute_name_index;

	//u4	attribute_length;
	//private int attribute_length;//We'll just use info.length instead

	//u1	info[attribute_length];
	private byte info[];

	public AttributeInfo(byte data[], int index) {
		this.attribute_name_index = ByteArrayUtil.bytes2(data, index);
		index += 2;

		int attribute_length = ByteArrayUtil.bytes4(data, index);
		index += 4;

		this.info = new byte[attribute_length];
		for (int i = 0; i < info.length; ++i) {
			this.info[i] = data[i + index];
		}
	}

	public int getTotalByteLength() {
		return 2 + 4 + info.length;
	}

	public String toString() {
		String str = "attribute_info{\n";
		str += "attribute_name_index\t" + attribute_name_index + "\n";
		str += "attribute_length\t" + info.length + "\n";
		str += "info\t" + Arrays.toString(info) + "\n";
		str += "}\n";
		return str;
	}

	public void toBytes(ByteArrayOutputStream buf) throws IOException {
		buf.write(ByteArrayUtil.intTo2Bytes(attribute_name_index));
		buf.write(ByteArrayUtil.intTo4Bytes(info.length));
		buf.write(info);
	}

	public int getAttribute_name_index() {
		return attribute_name_index;
	}

	public void setAttribute_name_index(int attribute_name_index) {
		this.attribute_name_index = attribute_name_index;
	}

	public byte[] getInfo() {
		return info;
	}

	public void setInfo(byte[] info) {
		this.info = info;
	}
}