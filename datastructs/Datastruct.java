package datastructs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class Datastruct {
	public abstract int getTotalByteLength();

	public byte[] toBytes() {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		try {
			this.toBytes(buf);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return buf.toByteArray();
	}

	public abstract void toBytes(ByteArrayOutputStream buf) throws IOException;
}
