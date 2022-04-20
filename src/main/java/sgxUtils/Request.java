package sgxUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Class used to add a layer of abstraction to a Request Information.
 * This object will then be encrypted in the form of a byte[] before being sent
 * to a replica for execution.
 */
public class Request implements Serializable {
	
	private static final long serialVersionUID = -7248412708021972515L;
	/**
	 * Requests for a Key-value Store only require Key and Value for execution.
	 * (Some dont require both). If more support needs to be added, alter the RequestType enum,
	 * and the necessary values for execution before programming the Enclave function
	 * to execute the given RequestType.
	 */
	private int client_id; // Does this need to be encrypted?
	private RequestType operation;
	private byte[] key;
	private byte[] value;
	
	
	public Request(int id, RequestType op, byte[] k, byte[] v) {
		this.client_id = id;
		this.operation = op;
		this.key = k;
		this.value = v;
	}
	
	public byte[] getKey() {
		return this.key;
	}
	
	public byte[] getValue() {
		return this.value;
	}
	
	public int getClientId() {
		return this.client_id;
	}
	
	public byte[] toBytes() {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		try {
			ObjectOutputStream ob_out = new ObjectOutputStream(bs);
			ob_out.writeObject(this);
			ob_out.flush();
			return bs.toByteArray();
		} catch (IOException e) {
			System.out.println("Error converting Request to Byte[]");
		}
		return null;
	}
	
	public static Request fromBytes(byte[] obj) {
		ByteArrayInputStream in = new ByteArrayInputStream(obj);
		try {
			ObjectInputStream is = new ObjectInputStream(in);
			return (Request) is.readObject();
		} catch (ClassNotFoundException e) {
			System.out.println("Cant Find Request Class.");
		} catch (IOException e) {
			System.out.println("IOException - Error converting from byte[] to Request Object.");
		}
		return null;
	}

}
