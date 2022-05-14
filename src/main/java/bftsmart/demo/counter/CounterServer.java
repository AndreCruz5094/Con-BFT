/**
Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package bftsmart.demo.counter;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Arrays;
import java.util.Base64;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;
import sgxUtils.SgxFunctions;

/**
 * Example replica that implements a BFT replicated service (a counter).
 * If the increment > 0 the counter is incremented, otherwise, the counter
 * value is read.
 * 
 * @author alysson
 */

public final class CounterServer extends DefaultSingleRecoverable  {

	private int counter = 0;
	private int iterations = 0;

	public CounterServer(int id) {
		SgxFunctions sgx = new SgxFunctions(5);
		//Create .pem File:
		File pem = null;
		try {
			pem = createPem(5);
		} catch (Exception e) {
			e.printStackTrace();
		}

		sgx.createSignedEnclave(System.getProperty("user.dir"), pem.getAbsolutePath(), 5);
		System.out.println("Enclave Signed.");
//		sgx.jni_initialize_enclave(20, enc_path);
		sgx.jni_initialize_enclave(5);
		System.out.println("Enclave created.");
//		System.out.println("Enclave created.");
//		byte[] ec_params = sgx.jni_sgx_begin_ec_dh();
//		System.out.println(Arrays.toString(ec_params)); //Start my EC Curve.
//		byte[] sealed_key = sgx.jni_calculate_shared_dh(ec_params);
//		//System.out.println(Arrays.toString(sealed_key));
//		byte[] enc_whatever = sgx.jni_sgx_aes_dh_encrypt(sealed_key, "Consegui iniciar o Enclave.".getBytes(StandardCharsets.UTF_8));
//		//System.out.println(new String(enc_whatever,StandardCharsets.UTF_8));
//		System.out.println(new String(sgx.jni_sgx_aes_dh_decrypt(sealed_key, enc_whatever, -1),StandardCharsets.UTF_8));
		new ServiceReplica(id, this, this);
	}


	private File createPem(int enclaveId) throws Exception{
		String fileName = enclaveId + "private.pem";
		File f = new File(fileName);
		if(!f.exists()) {
			createPrivateKey(f);
		}
		return f;
	}

	private void createPrivateKey(File f) throws Exception {
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


@Override
public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {         
	iterations++;
	System.out.println("(" + iterations + ") Counter current value: " + counter);
	try {
		ByteArrayOutputStream out = new ByteArrayOutputStream(4);
		new DataOutputStream(out).writeInt(counter);
		return out.toByteArray();
	} catch (IOException ex) {
		System.err.println("Invalid request received!");
		return new byte[0];
	}
}

@Override
public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
	iterations++;
	try {
		int increment = new DataInputStream(new ByteArrayInputStream(command)).readInt();
		counter += increment;

		System.out.println("(" + iterations + ") Counter was incremented. Current value = " + counter);

		ByteArrayOutputStream out = new ByteArrayOutputStream(4);
		new DataOutputStream(out).writeInt(counter);
		return out.toByteArray();
	} catch (IOException ex) {
		System.err.println("Invalid request received!");
		return new byte[0];
	}
}

public static void main(String[] args){
	if(args.length < 1) {
		System.out.println("Use: java CounterServer <processId>");
		System.exit(-1);
	}      
	new CounterServer(Integer.parseInt(args[0]));
}


@SuppressWarnings("unchecked")
@Override
public void installSnapshot(byte[] state) {
	try {
		ByteArrayInputStream bis = new ByteArrayInputStream(state);
		ObjectInput in = new ObjectInputStream(bis);
		counter = in.readInt();
		in.close();
		bis.close();
	} catch (IOException e) {
		System.err.println("[ERROR] Error deserializing state: "
				+ e.getMessage());
	}
}

@Override
public byte[] getSnapshot() {
	try {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = new ObjectOutputStream(bos);
		out.writeInt(counter);
		out.flush();
		bos.flush();
		out.close();
		bos.close();
		return bos.toByteArray();
	} catch (IOException ioe) {
		System.err.println("[ERROR] Error serializing state: "
				+ ioe.getMessage());
		return "ERROR".getBytes();
	}
}
}
