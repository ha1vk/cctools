#!/system/bin/sh

TOPDIR=$PWD

if [ "$TOPDIR" = "" ]; then
    TOPDIR="."
fi

TOPDIR=`realpath $TOPDIR`

error() {
    echo "ERROR: $@"
    exit 1
}

get_ext_libdirs() {
    test -e ${TOPDIR}/project.properties && cat ${TOPDIR}/project.properties | grep android.library.reference | cut -f2 -d=
}

if [ ! -f AndroidManifest.xml ]; then
    echo "Not android project directory!"
    exit 1
fi

PROJECTNAME=`aproject-helper AppName`
if [ "$PROJECTNAME" = "" ]; then
    PROJECTNAME=`basename $TOPDIR`
fi

APROJECT_BIN=`realpath $0`

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
    echo "Cleaning $PROJECTNAME"
    rm -rf bin gen
    for d in `get_ext_libdirs`; do
	cd $d && $APROJECT_BIN $VERBOSE_AAPT clean
	cd $TOPDIR
    done
    exit 0
fi

for d in `get_ext_libdirs`; do
    cd $d
    $APROJECT_BIN $VERBOSE_AAPT $@ || error "external library"
    cd $TOPDIR
done

echo "Build $PROJECTNAME"

PROJECT_RELEASE="no"
if [ "$1" = "release" ]; then
    PROJECT_RELEASE="yes"
fi

mkdir -p bin/classes gen
mkdir -p bin/res

cp -f AndroidManifest.xml bin/

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

find_libs_for_aapt() {
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

find_libs_for_javac() {
    local d=
    local x=
    for d in $@; do
	test -d $d && find $d -name "*.jar" -type f -exec printf "{}:" \;
    done
}

find_libs_for_dex() {
    local d=
    local x=
    for d in $@; do
	test -d $d && find $d -name "*.jar" -type f -exec printf "{} " \;
    done
}

get_ext_resources() {
    local d
    for d in $@; do
	printf "-S ${d}/bin/res -S ${d}/res"
    done
}

find_ext_libs_for_javac() {
    local d
    for d in $@; do
	find_libs_for_javac ${d}/bin ${d}/libs
    done
}

find_ext_libs_for_dex() {
    local d
    for d in $@; do
	find_libs_for_dex ${d}/bin ${d}/libs
    done
}

ACTIVITIES=`aproject-helper Activities`
EXT_LIBDIRS=`get_ext_libdirs`

if [ "$PROJECT_RELEASE" = "yes" ]; then
    aproject-helper BuildConfig release
else
    aproject-helper BuildConfig debug
fi

if [ ! "$EXT_LIBDIRS" = "" ]; then
    AAPT_OVERLAY="--auto-add-overlay"
fi

echo "Starting aapt..."

if [ "$ACTIVITIES" = "" ]; then
    aapt p \
	--non-constant-id \
	-f $VERBOSE_AAPT \
	-m \
	--output-text-symbols ${TOPDIR}/bin \
	${AAPT_OVERLAY} \
	-M ${TOPDIR}/bin/AndroidManifest.xml \
	-F ${TOPDIR}/bin/resources.res \
	-S ${TOPDIR}/bin/res \
	-S ${TOPDIR}/res \
	`get_ext_resources $EXT_LIBDIRS`\
	`find_libs_for_aapt $ANDROID_SDK libs $ANDROID_LIBS` \
	-J ${TOPDIR}/gen \
	--generate-dependencies \
	 || error "aapt"
else
    aapt p \
	-f $VERBOSE_AAPT \
	-m \
	--output-text-symbols ${TOPDIR}/bin \
	${AAPT_OVERLAY} \
	-M ${TOPDIR}/bin/AndroidManifest.xml \
	-F ${TOPDIR}/bin/resources.res \
	-S ${TOPDIR}/bin/res \
	-S ${TOPDIR}/res \
	`get_ext_resources $EXT_LIBDIRS`\
	`find_libs_for_aapt $ANDROID_SDK libs $ANDROID_LIBS` \
	-J ${TOPDIR}/gen \
	--generate-dependencies \
	 || error "aapt"
fi

echo "Starting aidl..."

find . -name "*.aidl" -exec aidl -Isrc `find_aidl_for_aidl $ANDROID_SDK $ANDROID_AIDL` -ogen {} \;

echo "Starting javac..."

javac $VERBOSE_JAVAC \
    -d ${TOPDIR}/bin/classes \
    -classpath ${TOPDIR}/bin/classes:`find_ext_libs_for_javac $EXT_LIBDIRS`:`find_libs_for_javac $ANDROID_SDK libs $ANDROID_LIBS` \
    -sourcepath ${TOPDIR}/src:${TOPDIR}/gen \
    -target 1.5 \
    -bootclasspath ${ANDROID_SDK}/android.jar \
    -encoding UTF-8 \
    -source 1.5 \
    `find src -name "*.java"` \
    `find gen -name "*.java"` \
    || error "javac"

if [ "$ACTIVITIES" = "" ]; then

    echo "Build library..."

    jar c${VERBOSE_JAR}f bin/${PROJECTNAME}.jar -C bin/classes .
else

    mkdir -p ${TOPDIR}/bin/dexedLibs
    DEX_LIBS=
    DEXED_LIBS=
    for f in `find_ext_libs_for_dex $EXT_LIBDIRS` `find_libs_for_dex libs`; do
	for v in $DEX_LIBS; do
	    if cmp -s $f $v; then
		f=""
		break
	    fi
	done
	if [ ! "$f" = "" ]; then
	    DEX_LIBS="$DEX_LIBS $f"
	    fname=`basename $f`
	    
	    echo "Pre-dexing library ${fname}..."
	    
	    dx --dex \
		$VERBOSE_DEX \
		--output ${TOPDIR}/bin/dexedLibs/${fname} \
		$f \
		|| error "dx"
	    DEXED_LIBS="$DEXED_LIBS ${TOPDIR}/bin/dexedLibs/${fname}"
	fi
    done

    echo "Starting dx..."

    dx --dex \
	$VERBOSE_DEX \
	--no-strict \
	--output=bin/${PROJECTNAME}.dex \
	`find bin/classes -maxdepth 1 -not -path "bin/classes"` \
	$DEXED_LIBS \
	|| error "dx"

    NATIVE_LIBS=
    if [ -d libs ]; then
	NATIVE_LIBS="-nf libs"
    fi

    echo "Starting apkbuilder..."

    apkbuilder bin/${PROJECTNAME}.apk \
	$VERBOSE_APKBUILDER \
	-u \
	-z ./bin/resources.res \
	-f ./bin/${PROJECTNAME}.dex \
	$NATIVE_LIBS \
	|| error "apkbuilder"

    echo "Starting apksigner..."

    apksigner bin/${PROJECTNAME}.apk bin/${PROJECTNAME}-signed.apk || error "apksigner"
fi
