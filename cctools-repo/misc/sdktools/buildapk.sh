#!/system/bin/sh

error() {
    echo "ERROR: $@"
    exit 1
}

if [ ! -f AndroidManifest.xml ]; then
    echo "Not android project directory!"
    exit 1
fi

VERBOSE_JAVAC=
VERBOSE_JAR=
VERBOSE_DEX=
VERBOSE_APKBUILDER=
VERBOSE_AAPT=

if [ "$1" = "-v" ]; then
    VERBOSE_JAVAC="-verbose"
    VERBOSE_JAR="v"
    VERBOSE_DEX="--verbose"
    VERBOSE_APKBUILDER="-v"
    VERBOSE_AAPT="-v"
    shift
fi

if [ "$1" = "clean" ]; then
    rm -rf build gen
    exit 0
fi

PROJECT_RELEASE="no"
if [ "$1" = "release" ]; then
    PROJECT_RELEASE="yes"
fi

TOPDIR=$1

if [ "$TOPDIR" = "" ]; then
    TOPDIR="."
fi

TOPDIR=`realpath $TOPDIR`

PROJECTNAME=`aproject-helper AppName`
if [ "$PROJECTNAME" = "" ]; then
    PROJECTNAME=`basename $TOPDIR`
fi

mkdir -p build/classes gen

ANDROID_SDK_VER=`aproject-helper TargetSDK`
if [ "$ANDROID_SDK_VER" = "" ]; then
    ANDROID_SDK_VER=`aproject-helper MinSDK`
fi

if [ "$ANDROID_SDK_VER" = "" ]; then
    echo "WARNING: No SDK version found in AndroidManifest.xml!"
    ANDROID_SDK_VER=`getprop ro.build.version.sdk`
    echo "WARNING: Use OS defined version $ANDROID_SDK_VER"
fi

ANDROID_SDK=${CCTOOLSDIR}/sdk/android-${ANDROID_SDK_VER}

if [ ! -d $ANDROID_SDK ]; then
    echo "ERROR: No SDK Android-${ANDROID_SDK_VER} found!"
    exit 1
fi

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

if [ "$PROJECT_RELEASE" = "yes" ]; then
    aproject-helper BuildConfig release
else
    aproject-helper BuildConfig debug
fi

aapt p -f $VERBOSE_AAPT -M AndroidManifest.xml -F ./build/resources.res `find_jar_for_aapt $ANDROID_SDK libs $ANDROID_LIBS` -S res/ -J gen || error "aapt"

find . -name "*.aidl" -exec aidl -Isrc `find_aidl_for_aidl $ANDROID_SDK $ANDROID_AIDL` -ogen {} \;

javac $VERBOSE_JAVAC `find_jar_for_javac $ANDROID_SDK libs $ANDROID_LIBS` -d build/classes `find src -name "*.java"` `find gen -name "*.java"` || error "javac"

ACTIVITIES=`aproject-helper Activities`

if [ "$ACTIVITIES" = "" ]; then
    jar c${VERBOSE_JAR}f build/${PROJECTNAME}.jar -C build/classes .
else
    dx --dex $VERBOSE_DEX --no-strict --output=build/${PROJECTNAME}.dex `find build/classes -maxdepth 1 -not -path "build/classes"` || error "dx"

    apkbuilder build/${PROJECTNAME}.apk $VERBOSE_APKBUILDER -u -z ./build/resources.res -f ./build/${PROJECTNAME}.dex || error "apkbuilder"

    apksigner build/${PROJECTNAME}.apk build/${PROJECTNAME}-signed.apk || error "apksigner"
fi
