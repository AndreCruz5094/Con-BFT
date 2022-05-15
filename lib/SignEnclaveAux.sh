Signer="/opt/intel/sgxsdk/bin/x64/sgx_sign"
xmlPath="/Enclave/Enclave.config.xml"
signedEnclave="$1enclave_signed.so"
configPath="/home/andre/Tese/Java-SGX$xmlPath"

cd /home/andre/Tese/Java-SGX #Move to the Java-SGX Folder;
$Signer sign -key $2 -enclave /home/andre/Tese/Con-BFT/build/install/library/enclave.so -out $signedEnclave -config $configPath
mv $signedEnclave /home/andre/Tese/Con-BFT/build/install/library


