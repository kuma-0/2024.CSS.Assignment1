package tmp;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Test {

	public static void main(String[] args) throws IOException {
		String filename = "../tmp.deleteme";
		File file = new File(filename);
		RandomAccessFile out = new RandomAccessFile(file, "rw");

		byte bytes[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
		out.write(bytes, 0, bytes.length);
		out.seek(0);
		out.write(bytes, 0, bytes.length);
		
		long fp = out.getFilePointer() + 2;
		
		out.seek(2);
		out.write(bytes, 2, bytes.length - 2);
		
	}
}
