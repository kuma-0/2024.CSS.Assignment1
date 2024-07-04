package util;

public class ByteArrayUtil {
	/**
	 * Read 4 consecutive bytes from the array and treat it as an integer.
	 * @param array The byte array.
	 * @param index The index (in number of bytes) to start reading.
	 * @return
	 */
	public static int bytes4(byte array[], int index) {
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
	public static int bytes2(byte array[], int index) {
		return (array[index] << (1 * 8) & 0x0000FF00)
				| (array[index + 1] & 0x000000FF);
	}

	/**
	 * Converts an integer to an array of 2 bytes.
	 */
	public static byte[] intTo2Bytes(int val) {
		return intToBytes(val, 2);
	}

	/**
	 * Converts an integer to an array of 4 bytes.
	 */
	public static byte[] intTo4Bytes(int val) {
		return intToBytes(val, 4);
	}

	private static byte[] intToBytes(int val, int num_bytes) {
		byte array[] = new byte[num_bytes];
		int i = 0;
		int j = array.length - 1;//Using i instead of j in the following code would change the endianness
		for (; i < array.length; ++i, --j) {
			array[i] = (byte) ((val >> j * 8) & 0xFF);
		}
		return array;
	}

	public static String bytes4toString(int bytes) {
		return Integer.toHexString(bytes);
	}

	public static String bytes2toString(int bytes) {
		return Integer.toHexString(bytes & 0xFFFF);
	}

	public static String bytesArraytoString(byte data[]) {
		String str = "";
		for (int i = 0; i < data.length; ++i) {
			str += Integer.toHexString(data[i]);
		}
		return str;
	}
}