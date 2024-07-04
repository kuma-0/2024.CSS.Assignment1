package test.virus;

import junit.framework.TestCase;

import org.junit.Test;

import util.ByteArrayUtil;

public class ByteArrayUtilTest extends TestCase {
	/**
	 * A simple test of bytes4
	 */
	@Test
	public void test_bytes4() {
		int val = 0xCAFEBABE;
		byte array[] = {
				(byte) ((val & 0xFF000000) >> 3 * 8),
				(byte) ((val & 0x00FF0000) >> 2 * 8),
				(byte) ((val & 0x0000FF00) >> 1 * 8),
				(byte) ((val & 0x000000FF) >> 0 * 8)
		};
		int val_again = ByteArrayUtil.bytes4(array, 0);

		if (val != val_again) {
			fail("Original didn't match reconstructed value.");
		}
	}

	@Test
	public void test_super_bytes2() {
		for (int val = 0; val <= 0xFFFF; ++val) {
			test_bytes2(val);
		}
	}

	public void test_bytes2(int val) {
		byte array[] = {
				(byte) ((val & 0x0000FF00) >> 1 * 8),
				(byte) ((val & 0x000000FF) >> 0 * 8)
		};
		int val_again = ByteArrayUtil.bytes2(array, 0);

		if (val != val_again) {
			fail("Original didn't match reconstructed value. " + val + ":" + val_again);
		}
	}
	
	/**
	 * Also tests bytes2.
	 */
	@Test
	public void test_intTo2Bytes() {
		for (int val = 0; val <= 0xFFFF; ++val) {
			test_bytes2(val);
		}
	}
	
	public void test_intTo2Bytes(int val) {
		byte array[] = ByteArrayUtil.intTo2Bytes(val);
		int val_again = ByteArrayUtil.bytes2(array, 0);

		if (val != val_again) {
			fail("Original didn't match reconstructed value. " + val + ":" + val_again);
		}
	}
}
