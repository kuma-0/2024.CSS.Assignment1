/**
 * The purpose of this class is just to be a dummy that we can infect.
 */
public class HelloWorld {
	private final static byte v_byte_array[] = { 1, 2, -1, 4, 9 };
	private final static String str = "Hello, World!";

	//private static String str2 = "Hello, World!";

	public HelloWorld(HelloWorld h) {

	}

	public static void main(String args[]) {
		final byte a_local_byte_array[] = { -1, 2, -1, 4, -9 };
		System.out.println("Hello, World!");
		System.out.println(str);
		//str2 = "butts";

		int z = 123;
		some_static_func(z);
	}

	private static int some_static_func(int a) {
		System.out.println("Hello, World!");
		return a;
	}
}
