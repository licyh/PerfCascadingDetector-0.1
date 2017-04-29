cd ../../../..
gradle jar_pre_dm
if [ $? -ne 0 ]; then
  echo "compile error"
  exit
fi
cd -

if [ -d ./tmp ]; then
  rm -rf ./tmp
fi
mkdir ./tmp
cp ../../../../build/lib/PreDM.jar ./tmp
cd ./tmp
jar -xvf PreDM.jar > /dev/null 2>&1

#$1: config file
#$2: allJar str
#$3: src path

wala_path=/mnt/storage/packages/wala/WALA-R_1.3.5
wala_core=${wala_path}/com.ibm.wala.core/bin
wala_util=${wala_path}/com.ibm.wala.util/bin
wala_shrike=${wala_path}/com.ibm.wala.shrike/bin
wala_testdata=${wala_path}/com.ibm.wala.core.testdata/bin
wala_tests=${wala_path}/com.ibm.wala.core.tests/bin
wala_cp=${wala_core}:${wala_util}:${wala_shrike}:${wala_tests}:${wala_testdata}:${walaUtil}
spoon_lib=/mnt/storage/jiaxinli/workspace/predm/lib/spoon-core-5.5.0-jar-with-dependencies.jar

#java -cp ./:${spoon_lib}:${wala_cp} com.prepare.PreDM $1 -i $src_path --source-classpath $allJar --output-type compilationunits
java -cp ./:${spoon_lib}:${wala_cp} com.prepare.PreDM $1 -i $3 --source-classpath $2 --output-type compilationunits

