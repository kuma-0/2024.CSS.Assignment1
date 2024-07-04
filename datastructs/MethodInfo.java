package datastructs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import util.ByteArrayUtil;

public class MethodInfo extends Datastruct {
	//u2	access_flags;
	private int access_flags;

	//u2	name_index;
	private int name_index;

	//u2	descriptor_index;
	private int descriptor_index;

	//u2	attributes_count;
	private int attributes_count;

	//attribute_info	attributes[attributes_count];
	private AttributeInfo attributes[];

	public MethodInfo(byte data[], int index) {
		System.out.println("{");
		this.access_flags = ByteArrayUtil.bytes2(data, index);
		System.out.println("\taccess_flags = 0x" + Integer.toHexString(access_flags));//TODO: deleteme
		index += 2;

		this.name_index = ByteArrayUtil.bytes2(data, index);
		System.out.println("\tname_index = " + name_index);//TODO: deleteme
		index += 2;

		this.descriptor_index = ByteArrayUtil.bytes2(data, index);
		System.out.println("\tdescriptor_index = " + descriptor_index);//TODO: deleteme
		index += 2;

		this.attributes_count = ByteArrayUtil.bytes2(data, index);
		System.out.println("\tattributes_count = " + attributes_count);//TODO: deleteme
		index += 2;

		this.attributes = new AttributeInfo[attributes_count];
		System.out.println("\tattributes_count = " + attributes_count);//TODO: deleteme
		for (int i = 0; i < attributes_count; ++i) {
			attributes[i] = new AttributeInfo(data, index);
			index += attributes[i].getTotalByteLength();
		}
		System.out.println("}");
	}

	/**
	 * Calculate length in bytes.
	 * Calculated instead of stored in case attributes[] changes.
	 */
	public int getTotalByteLength() {
		int length = 8;
		for (int i = 0; i < attributes.length; ++i) {
			length += attributes[i].getTotalByteLength();
		}
		return length;
	}

	public void toBytes(ByteArrayOutputStream buf) throws IOException {
		buf.write(ByteArrayUtil.intTo2Bytes(access_flags));
		buf.write(ByteArrayUtil.intTo2Bytes(name_index));
		buf.write(ByteArrayUtil.intTo2Bytes(descriptor_index));
		buf.write(ByteArrayUtil.intTo2Bytes(attributes_count));
		
		for(int i = 0; i < attributes.length; ++i){
			attributes[i].toBytes(buf);
		}
	}

	public String toString() {
		String str = "method_info{\n";
		str += "access_flags\t" + access_flags + "\n";
		str += "name_index\t" + name_index + "\n";
		str += "descriptor_index\t" + descriptor_index + "\n";
		str += "attributes_count\t" + attributes_count + "\n";
		str += "attributes\n";
		for (int i = 0; i < attributes.length; ++i) {
			str += attributes[i].toString();
		}
		str += "}";

		return str;
	}

	public int getAccess_flags() {
		return access_flags;
	}

	public int getName_index() {
		return name_index;
	}

	public int getDescriptor_index() {
		return descriptor_index;
	}

	public int getAttributes_count() {
		return attributes_count;
	}

	public AttributeInfo[] getAttributes() {
		return attributes;
	}
}