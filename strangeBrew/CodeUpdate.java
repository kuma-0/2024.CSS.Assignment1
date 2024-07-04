package strangeBrew;

import util.ByteArrayUtil;

public class CodeUpdate {
	/*
	 * Borked.
	 *	CodeUpdate.updateTheCodes(bytes, method_start, index, code_length, change_amount);
	 */

	/**
	 * Seems to be the number of bytes from the start of method_info
	 *  to the end of the code in the attribute.
	 */
	public final static int MAGIC_NUMBER = 22; //24;

	/**
	 * 
	 * @param bytes The array of bytes containing the code.
	 * @param method_start_index The fpointer of the first byte in the code.
	 * @param code_start_index
	 * @param code_length The length of the code.
	 * @param change_amount The change amount of change_amount.
	 */
	public static void updateTheCodes(byte bytes[], final int method_start_index,
			final int code_start_index, int code_length, int change_amount) {
		//fpointer must be at the start of the executable code
		int fpointer = code_start_index;

		int index = fpointer;//index = fpointer;  

		while (fpointer < (method_start_index + code_length + MAGIC_NUMBER)) {
			int data1;
			int total_nulls;
			int tag = bytes[index];
			index += 1;
			fpointer++;
			switch (tag) {
			//the default is a one byte instruction 
			default:
				break;
			//these are all of the two byte instructions 
			case 16:
			case 21:
			case 22:
			case 23:
			case 24:
			case 25:
			case 54:
			case 55:
			case 56:
			case 57:
			case 58:
			case 169:
			case 188:
				fpointer++;
				break;
			//these are all of the three byte instructions 
			case 17:
			case 132:
			case 153:
			case 154:
			case 155:
			case 156:
			case 157:
			case 158:
			case 159:
			case 160:
			case 161:
			case 162:
			case 163:
			case 164:
			case 165:
			case 166:
			case 167:
			case 168:
			case 198:
			case 199:
				fpointer += 2;
				break;
			//these are all of the five byte instructions 
			case 200:
			case 201:
				fpointer += 4;
				break;

			//these are all of the referencing instructions 
			case 18:
				data1 = bytes[index];
				index += 1;
				index = fpointer;
				System.out.println((byte) data1 + " to " + (byte) (data1 + change_amount));//TODO:deleteme
				bytes[index] = (byte) (data1 + change_amount);
				++index;
				//System.out.println(data1 + " to " + (byte)(data1 + change_amount));//TODO:CommentMe
				fpointer++;
				break;
			case 19:
			case 20:
			case 178:
			case 179:
				//System.out.print(Integer.toHexString(179) + "\n\t");
			case 180:
			case 181:
			case 182:
			case 183:
				/* invokespecial */
			case 184:
			case 187:
			case 189:
			case 192:
			case 193:
				data1 = ByteArrayUtil.bytes2(bytes, index);
				index += 2;
				index = fpointer;
				{
					System.out.println((short) data1 + " to " + (short) (data1 + change_amount));//TODO:deleteme
					byte tmp[] = ByteArrayUtil.intTo2Bytes(data1 + change_amount);
					bytes[index] = tmp[0];
					bytes[index + 1] = tmp[1];
					index += 2;
				}
				//System.out.println(data1 + " to " + (short)(data1 + change_amount));//TODO:CommentMe
				fpointer += 2;
				break;
			case 197:
				data1 = ByteArrayUtil.bytes2(bytes, index);
				index += 2;
				index = fpointer;
				{
					System.out.println((short) data1 + " to " + (short) (data1 + change_amount));//TODO:deleteme
					byte tmp[] = ByteArrayUtil.intTo2Bytes(data1 + change_amount);
					bytes[index] = tmp[0];
					bytes[index + 1] = tmp[1];
					index += 2;
				}
				//System.out.println(data1 + " to " + (short)(data1 + change_amount));//TODO:CommentMe
				fpointer += 3;
				break;
			case 185:
				data1 = ByteArrayUtil.bytes2(bytes, index);
				index += 2;
				index = method_start_index;
				{
					System.out.println((short) data1 + " to " + (short) (data1 + change_amount));//TODO:deleteme
					byte tmp[] = ByteArrayUtil.intTo2Bytes(data1 + change_amount);
					bytes[index] = tmp[0];
					bytes[index + 1] = tmp[1];
					index += 2;
				}
				//System.out.println(data1 + " to " + (short)(data1 + change_amount));//TODO:CommentMe
				fpointer += 4;
				break;
			//these are all of the variable length instructions 
			case 170:
				total_nulls =
						3 - (fpointer - 1 - (method_start_index + 24)) % 4;
				for (int i = 0; i < total_nulls; i++) {
					tag = bytes[index];
					index += 1;
					fpointer++;
				}
				fpointer += 4;
				index = fpointer;
				int low = ByteArrayUtil.bytes4(bytes, index);
				index += 4;
				int high = ByteArrayUtil.bytes4(bytes, index);
				index += 4;
				fpointer += 8 + 4 * (high - low + 1);
				break;
			case 171:
				total_nulls =
						3 - (fpointer - 1 - (method_start_index + 24)) % 4;
				for (int i = 0; i < total_nulls; i++) {
					tag = bytes[index];
					index += 1;
					fpointer++;
				}
				fpointer += 4;
				index = fpointer;
				int npairs = ByteArrayUtil.bytes4(bytes, index);
				index += 4;
				fpointer += 4 + 8 * npairs;
				break;
			case 196:
				tag = bytes[index];
				index += 1;
				if (tag == 132)
					fpointer += 4;
				else
					fpointer += 2;
				break;
			} // end of switch 
			index = fpointer;
		} //end of for loop  
	}
}
