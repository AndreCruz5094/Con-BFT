### This File calls the Sign rule from the Makefile.
### It creates a signed enclave.so Object and moves it to the original directory.
#make filePath="$1$3" pemPath=$2 
Signer="/opt/intel/sgxsdk/bin/x64/sgx_sign"
xmlPath="Enclave/Enclave.config.xml"
pemToDelete="$1/$6private.pem"
#hexOutput="enclave$4_hash.hex"
signedEnclave="$6enclave_signed.so"
configPath="$1$xmlPath"


#echo $1
#echo $2
#echo $4
#echo $5
#echo $signedEnclave
#Copy the .pem file
cp $4 $1
cd $1
#Inside the SGX Folder:
$Signer sign -key $5 -enclave enclave.so -out $signedEnclave -config $configPath
mv $signedEnclave $3
rm $pemToDelete
#$Signer sign -enclave 
##Copy the .pem file.
#cp $4 $1
#echo "copied."
#rm "$1$5"
#echo "deleted."
#make filePath="$1" pemPath=$2 signedFile