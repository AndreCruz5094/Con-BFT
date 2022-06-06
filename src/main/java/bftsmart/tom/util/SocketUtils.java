package bftsmart.tom.util;

import java.io.DataInputStream;
import java.io.IOException;

public class SocketUtils {

	public static void readInfo(byte[] array, DataInputStream in) {
		
		int pos = 0;
		do {
			int read;
			try {
				read = in.read(array, 0, array.length - pos);
				if(read == -1)
					break;
				System.out.println("Reading.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}while(pos < array.length);

	}
}
