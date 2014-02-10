#!/system/bin/sh

error() {
    echo "ERROR: $@"
    exit 1
}

if [ ! -f AndroidManifest.xml ]; then
    echo "Not android project directory!"
    exit 1
fi

if [ "$1" = "clean" ]; then
    rm -rf build gen
    exit 0
fi

TOPDIR=$1

if [ "$TOPDIR" = "" ]; then
    TOPDIR="."
fi

TOPDIR=`realpath $TOPDIR`

APKNAME=`basename $TOPDIR`

mkdir -p build/classes gen

ANDROID_SDK_VER=17

ANDROID_SDK=${CCTOOLSDIR}/sdk/android-${ANDROID_SDK_VER}

find_jar_for_aapt() {
    local d=
    for d in $@; do
	test -d $d && find $d -name "*.jar" -type f -exec printf "-I {} " \;
    done
}

find_aidl_for_aidl() {
    local d=
    for d in $@; do
	test -d $d && find $d -name "*.aidl" -type f -exec printf "-p{} " \;
    done
}

find_jar_for_javac() {
    printf "-cp "
    local d=
    local x=
    for d in $@; do
	test -d $d && find $d -name "*.jar" -type f -exec printf "{}:" \;
    done
}

aapt p -f -v -M AndroidManifest.xml -F ./build/resources.res `find_jar_for_aapt $ANDROID_SDK libs $ANDROID_LIBS` -S res/ -J gen || error "aapt"

find . -name "*.aidl" -exec aidl -Isrc `find_aidl_for_aidl $ANDROID_SDK $ANDROID_AIDL` -ogen {} \;

javac -verbose `find_jar_for_javac $ANDROID_SDK libs $ANDROID_LIBS` -d build/classes `find src -name "*.java"` `find gen -name "*.java"` || error "javac"

dx --dex --verbose --no-strict --output=build/${APKNAME}.dex `find build/classes -maxdepth 1 -not -path "build/classes"` || error "dx"

apkbuilder build/${APKNAME}.apk -v -u -z ./build/resources.res -f ./build/${APKNAME}.dex || error "apkbuilder"

apksigner build/${APKNAME}.apk build/${APKNAME}-signed.apk || error "apksigner"
