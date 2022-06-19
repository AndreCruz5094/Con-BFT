package bftsmart.demo.map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;

public class MapServer<K> extends DefaultSingleRecoverable {

	private Map<K, byte[]> replicaMap;
	private Logger logger;

	//SGX enabled stuff:
	private ServiceReplica rep;

	public MapServer(int id, int enclaveId) {
		replicaMap = new TreeMap<>();
		logger = Logger.getLogger(MapServer.class.getName());
		this.rep = new ServiceReplica(id, this, this, enclaveId);
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: demo.map.MapServer <server id> <enclaveId>");
			System.exit(-1);
		}
		new MapServer<String>(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
	}
	
	private byte[] sealValue(String val) {
		return this.rep.getEnclave().jni_sgx_seal_info(val.getBytes(StandardCharsets.UTF_8));
	}
	
	private String unsealValue(byte[] sealed) {
		return new String(this.rep.getEnclave().jni_sgx_unseal_info(sealed),StandardCharsets.UTF_8);
	}

	@SuppressWarnings("unchecked")
	@Override
	public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
		byte[] reply = null;
		K key = null;
		String value = null;
		boolean hasReply = false;
		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
				ObjectInput objIn = new ObjectInputStream(byteIn);
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(byteOut);) {
			MapRequestType reqType = (MapRequestType)objIn.readObject();
			switch (reqType) {
			case PUT:
				key = (K)objIn.readObject();
				value = (String)objIn.readObject();
				byte[] sealedValue = sealValue(value);
		

				byte[] sealedOldValue = replicaMap.put(key, sealedValue);
				
				if (sealedOldValue != null) {
					String oldValue = unsealValue(sealedOldValue);
					objOut.writeObject(oldValue);
					hasReply = true;
				}
				break;
			case GET:
				key = (K)objIn.readObject();
				sealedValue = replicaMap.get(key);
				
				if (sealedValue != null) {
					value = unsealValue(sealedValue);
					objOut.writeObject(value);
					hasReply = true;
				}
				break;
			case REMOVE:
				key = (K)objIn.readObject();
				sealedValue = replicaMap.remove(key);
				
				if (sealedValue != null) {
					value = unsealValue(sealedValue);
					objOut.writeObject(value);
					hasReply = true;
				}
				break;
			case SIZE:
				int size = replicaMap.size();
				objOut.writeInt(size);
				hasReply = true;
				break;
			case KEYSET:
				keySet(objOut);
				hasReply = true;
				break;
			}
			if (hasReply) {
				objOut.flush();
				byteOut.flush();
				reply = byteOut.toByteArray();
			} else {
				reply = new byte[0];
			}

		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Ocurred during map operation execution", e);
		}
		return reply;
	}

	@SuppressWarnings("unchecked")
	@Override
	public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
		byte[] reply = null;
		K key = null;
		String value = null;
		boolean hasReply = false;

		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(command);
				ObjectInput objIn = new ObjectInputStream(byteIn);
				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(byteOut);) {
			MapRequestType reqType = (MapRequestType)objIn.readObject();
			switch (reqType) {
			case GET:
				key = (K)objIn.readObject();
				byte[] sealedValue = replicaMap.get(key);
				
				if (sealedValue != null) {
					value = unsealValue(sealedValue);
					objOut.writeObject(value);
					hasReply = true;
				}
				break;
			case SIZE:
				int size = replicaMap.size();
				objOut.writeInt(size);
				hasReply = true;
				break;
			case KEYSET:
				keySet(objOut);
				hasReply = true;
				break;
			default:
				logger.log(Level.WARNING, "in appExecuteUnordered only read operations are supported");
			}
			if (hasReply) {
				objOut.flush();
				byteOut.flush();
				reply = byteOut.toByteArray();
			} else {
				reply = new byte[0];
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Ocurred during map operation execution", e);
		}

		return reply;
	}

	private void keySet(ObjectOutput out) throws IOException, ClassNotFoundException {
		Set<K> keySet = replicaMap.keySet();
		int size = replicaMap.size();
		out.writeInt(size);
		for (K key : keySet)
			out.writeObject(key);
	}

	@Override
	public byte[] getSnapshot() {
		try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				ObjectOutput objOut = new ObjectOutputStream(byteOut)) {
			objOut.writeObject(replicaMap);
			return byteOut.toByteArray();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error while taking snapshot", e);
		}
		return new byte[0];
	}

	@SuppressWarnings("unchecked")
	@Override
	public void installSnapshot(byte[] state) {
		try (ByteArrayInputStream byteIn = new ByteArrayInputStream(state);
				ObjectInput objIn = new ObjectInputStream(byteIn)) {
			replicaMap = (Map<K, byte[]>)objIn.readObject();
		} catch (IOException | ClassNotFoundException e) {
			logger.log(Level.SEVERE, "Error while installing snapshot", e);
		}
	}
}