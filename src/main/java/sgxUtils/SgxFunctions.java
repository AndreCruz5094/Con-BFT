package sgxUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SgxFunctions {

	//Load the SGX library created using the makefile.
	/**
	 * Instructions:
	 *  - use the createHeader.sh in order to generate the .h required by the C/C++ code.
	 *  - run the given makefile ("make SGX_MODE=SIM") in order to compile the shared library in
	 *  simulation mode (Strongly recommend this due to the Library not being tested for HW mode.)
	 *  - Run the JavaApp file or import this as an external project for your Java project.
	 */
	static {

		System.loadLibrary("Sgx");
	}

	private int enclaveId;
	private String log_file_path;
	private File log_file;
	
	public static File createPem(int enclaveId) throws Exception{
		String fileName = enclaveId + ".pem";
		File f = new File(fileName);
		if(!f.exists()) {
			createPrivateKey(f);
		}
		return f;
	}

	private static void createPrivateKey(File f) throws Exception {
		//First step, create a private RSA key.
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		RSAKeyGenParameterSpec specs = new RSAKeyGenParameterSpec(3072,BigInteger.valueOf(3));
		keyGen.initialize(specs);
		KeyPair k = keyGen.generateKeyPair();
		PrivateKey privKey = k.getPrivate();


		String encodedString = "-----BEGIN PRIVATE KEY-----\n";
		encodedString += Base64.getEncoder().encodeToString(privKey.getEncoded()) + "\n";
		encodedString = encodedString+"-----END PRIVATE KEY-----\n";
		f.createNewFile();
		BufferedWriter b = new BufferedWriter(new PrintWriter(f));
		b.write(encodedString);
		b.close();
	}


	/**
	 * SGX_Functions Constructor. This Object encapsulates the SGX Functions that are used as Ecalls.
	 * @param i - The Enclave ID.
	 */
	public SgxFunctions(int i) {
		log_file_path = i + "_enclave_log.txt";
		this.enclaveId = i;
		log_file = new File(log_file_path);
		if(!log_file.exists()){
			try {
				log_file.createNewFile();
			} catch (IOException e) {
				System.out.println("Log file creation Failed.");
			}
		}
	}

	/**
	 * Function used to create a .signed enclave file and move it to the current dir.
	 * @param dir - Current directory
	 * @param pemFilePath - File of the .pem file used to sign the enclave.
	 * @param enclaveID - Global Id for the enclave.
	 */
	public void createSignedEnclave(String dir,String pemFilePath, int enclaveID, int replicaId) {
		
		String shPath = dir + "/SignEnclaveAux.sh"; //Get the correct Script.
		String[] cmd = {"sh", shPath,Integer.toString(enclaveID),pemFilePath,Integer.toString(replicaId)};
		try {
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String l = null;
			while( (l = br.readLine()) != null )
				System.out.println(l);

			BufferedReader errBr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String err = null;
			while( (err = errBr.readLine()) != null)
				System.out.println(err);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * JNI call in order to initialize the Intel SGX Enclave.
	 * @param enclaveId - globalId for the enclave;
	 * @param enclaveFilePath -  Enclave File path in a byte array with the UTF-8 Format.
	 * @return an Integer to indicate success, with anything below 0 indicating error.
	 */
	public native int jni_initialize_enclave(int enclaveId, byte[] enclaveFilePath);


	//Destroy the enclave.
	public native void jni_sgx_destroy_enclave();

	/**
	 * This function is used to seal an array of bytes using the Enclave key derived from the CPU's master key.
	 * This information can be stored in untrusted memory with privacy guarantees.
	 * @param toSeal - A byte array of the information to seal with the Enclave's private key. (UTF-8).
	 * 
	 * An important note, due to the fact that sizeof(jchar) != sizeof(char), with the former being 2 bytes vs the 
	 * latter being equal to 1 byte, do not forget to get a byte array with the UTF-8 Format.
	 * @return a byte array with the sealed information.
	 */
	public native byte[] jni_sgx_seal_info(byte[] toSeal);


	/**
	 * This function is used to create a digest of a given byte array. It uses the SHA256 protocol,
	 * creating a byte array equivalent to the hash of the given byte array.
	 * @param toHash - The byte array to hash. 
	 * @return A byte array with the hashed information using the SHA 256 protocol.
	 */
	public native byte[] jni_sgx_createDigest(byte[] toHash);


	/**
	 * This function is used to unseal an array of bytes that has previously been sealed using the Enclave's derived
	 * key from the CPU master key. The byte array is returned in UTF-8 format.
	 * @requires jni_sgx_seal_info(x) == c  
	 * @ensures jni_sgx_unseal_info(c) == x
	 * @param c - The byte array to unseal. 
	 * @return
	 */
	public native byte[] jni_sgx_unseal_info(byte[] c);


	/**
	 * Function created to generate a RSA key pair and store it in memory. This function returns the generated public key in order 
	 * for it to be shared with other replicas.
	 * @return
	 */
	public native byte[] jni_sgx_create_RSA_pair();


	/**
	 * Function created to start the Diffie Hellman-Algorithm between Replicas. For every connection, This function has to be used
	 * @return EC Public Key.
	 */
	public native byte[] jni_sgx_begin_ec_dh();


	/**
	 * Function created to calculate a Shared key with the given Diffie Hellman parameters.
	 * @param dh_params - Diffie Hellman public returned by the previous function (jni_sgx_begin_ec_dh())
	 * @return - The sealed Shared key.
	 */
	public native byte[] jni_calculate_shared_dh(byte[] dh_params);


	/**
	 * Function created to encrypt information using a sealed DH key. The enclave will unseal the key and encrypt the information given.
	 * @param sealedKey - The Sealed DH-Shared Key.
	 * @param toEncrypt - Information to Encrypt.
	 * @return An AES-Encrypted Buffer with the given AES Key.
	 */
	public native byte[] jni_sgx_aes_dh_encrypt(byte[] sealedKey, byte[] toEncrypt);


	/**
	 * Function created to decrypt information encrypted using a sealed DH-Shared key. The enclave will unseal the key and decrypt the given
	 * information.
	 * @requires toDecrypt is a buffer of encrypted information using the jni_sgx_aes_dh_encrypt function above.
	 * @requires EncryptionKey == DecryptionKey. Considering this function uses 256-bit GCM AES, keys need to match.
	 * @param sealedKey - The sealed Decryption key. 
	 * @param toDecrypt - The information to Decrypt.
	 * @param toSeal - If the information requires sealing. -1 for plaintext , anything else for sealed information.
	 * @return - a byte[] of decrypted information to be sent to another replica.
	 */
	public native byte[] jni_sgx_aes_dh_decrypt(byte[] sealedKey, byte[] toDecrypt, int toSeal);


	/**
	 * Function used to create a SHA-256 HMAC digest of a given input byte array. This can be used to verify message integrity
	 * by creating a HMAC of the given message.
	 * @param sealed_hmac - The sealed HMAC key.
	 * @param input - The input to hash using SHA-256 protocol.
	 * @return A byte array containing the 256-bit SHA-256 HMAC digest.
	 */
	public native byte[] jni_sgx_create_hmac(byte[] sealed_hmac, byte[] input);


	/**
	 * @param sealed_hmac - Sealed HMAC key.
	 * @param hmac - The HMAC to verify
	 * @param input - The input to HMAC in order to make comparisons.
	 * @return an Integer Value indicating whether the given HMAC is valid (0 if valid, any other value if not)
	 */
	public native int jni_sgx_verify_hmac(byte[] sealed_hmac, byte[] hmac, byte[] input);


	/**
	 * 
	 * @param key - The Request Key;
	 * @param value - The Request Value;
	 * @param request_type - The Request Type, is usually converted from the Enum request Type.
	 * @param sealed_key - The DH shared key used to encrypt both key and value.
	 * @return 
	 */
	public native byte[] jni_sgx_execute_request(byte[] key, byte[] value, String request_type, byte[] sealed_key);


	/**
	 * Function created in order to generate an HMAC Key. To be used by the leader replica in order to create a HMAC key to be shared amongst the replicas.
	 * @return a Sealed HMAC key.
	 */
	public native byte[] jni_sgx_generate_hmac_key();

	/**
	 * Function created in order to swap SGX Sealing to AES-GCM Encryption.
	 * @param sealed_key - The Sealed AES Encryption key
	 * @param sealed_input - The Sealed Input to encrypt with the AES Encrypted Key
	 * @return - The same Input encrypted with the AES-GCM Protocol
	 */
	public native byte[] jni_sgx_swap_sealed_aes(byte[] sealed_key, byte[] sealed_input);


	/**
	 * Function created in order to Swap AES-GCM Encryption to SGX Sealing.
	 * @param sealed_key - The sealed AES Encryption Key
	 * @param sealed_input - The AES Encrypted Input
	 * @return - the same Input encrypted with SGX Sealing.
	 */
	public native byte[] jni_sgx_swap_aes_sealed(byte[] sealed_key,byte[] sealed_input);
	
	
	//Client counterpart functions that do the same as their SGX-based functions.
	
	public native byte[] jni_begin_client_ecdh();
	
	public native byte[] jni_calculate_client_sharedDH();

	
	/**
	 * Function to fully read a Replica log file. Returns a list with all the requests the Replica has executed.
	 * @return List<Request> with all the requests the Replica has executed.
	 */
	public List<Request> getLogRequests(){
		List<Request> reqs = new ArrayList<>();
		try {
			FileInputStream fin = new FileInputStream(this.log_file);
			boolean finished = false;
			while(!finished) {
				int size = fin.read();
				if(size == -1) {
					finished = true;
					continue;
				}
				byte[] req_bytes = fin.readNBytes(size);
				Request r = Request.fromBytes(req_bytes);
				reqs.add(r);
			}
			fin.close();
			return reqs;
		} catch (FileNotFoundException e) {
			System.out.println("Log file Not found.");
		} catch (IOException e) {
			System.out.println("Input read failed.");
		}
		return null;
	}



	/**
	 * Function created in order to write down the Request information a given Request in the Log file.
	 * @param request_data - The .toBytes function of a Request Object.
	 */
	public void write_request_log(byte[] request_data) {
		try {
			FileOutputStream fout = new FileOutputStream(log_file);
			fout.write(request_data.length);
			fout.write(request_data);
		} catch (FileNotFoundException e) {
			System.out.println("Cannot find The Replica Log File.");
		}catch (IOException e) {
			System.out.println("IOException - Error Writing in Log file.");
		}
	}

}
