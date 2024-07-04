
public class Util {
	public static int align4bytes(long index, long method_index) {
		return (int) ((4 - ((index - method_index) % 4)) % 4);
	}
}
