Signer="/opt/intel/sgxsdk/bin/x64/sgx_sign"
xmlPath="/Enclave/Enclave.config.xml"
signedEnclave="$1enclave_signed.so"
configPath="/home/andrecruz/Tese/Java-SGX$xmlPath"

cd /home/andrecruz/Tese/Java-SGX #Move to the Java-SGX Folder;
$Signer sign -key $2 -enclave /home/andrecruz/Tese/Con-BFT/build/local/rep$3/enclave.so -out $signedEnclave -config $configPath
mv $signedEnclave /home/andrecruz/Tese/Con-BFT/build/local/rep$3/


