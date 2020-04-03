FILE_PATH=../../eye/target/appassembler/bin/eye


for file in $(ls cam*.txt);
do
    $FILE_PATH localhost 8080 $file 1 1 < $file > $file.out &
    echo Sending file $file
done

echo All cameras launched. Waiting now for them to finish
wait
echo Done!
