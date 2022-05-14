Signer="/opt/intel/sgxsdk/bin/x64/sgx_sign"
xmlPath="/Enclave/Enclave.config.xml"
signedEnclave="$1enclave_signed.so"
configPath="$2$xmlPath"

cd $2 #Move to the Java-SGX Folder;
$Signer sign -key $3 -enclave enclave.so -out $signedEnclave -config $configPath
mv $signedEnclave $4
