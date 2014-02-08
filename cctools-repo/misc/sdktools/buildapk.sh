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

aapt p -f -v -M AndroidManifest.xml -F ./build/resources.res -I ~/android.jar -S res/ -J gen || error "aapt"

find . -name "*.aidl" -exec aidl -Isrc -p../android-17/framework.aidl -ogen {} \;

javac -verbose -cp ~/android.jar -d build/classes `find src -name "*.java"` `find gen -name "*.java"` || error "aapt"

dx --dex --verbose --no-strict --output=build/${APKNAME}.dex `find build/classes -maxdepth 1 -not -path "build/classes"`

apkbuilder build/${APKNAME}.apk -v -u -z ./build/resources.res -f ./build/${APKNAME}.dex

apksigner build/${APKNAME}.apk build/${APKNAME}-signed.apk
