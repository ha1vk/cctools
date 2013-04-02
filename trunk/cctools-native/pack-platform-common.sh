#!/bin/sh

VERSION=r8d

TOPDIR=$PWD
NDK_DIR="$1"

if [ "x$NDK_DIR" = "x" ]; then
    echo "No ndk dir"
    exit 1
fi

TARGET_DIR="${TOPDIR}/tmp/platform-common-$USER"

error() {
    echo "Error: $@"
    exit 1
}

copysrc() {
    mkdir -p $2
    tar -C "$1" -c . | tar -C $2 -xv || error "copysrc $1 $2"
}

mkdir -p $TARGET_DIR/cctools

pushd .
echo $VERSION > $TARGET_DIR/cctools/platform-common-version

copysrc $NDK_DIR/sources/android/cpufeatures $TARGET_DIR/cctools/sources/cpufeatures
copysrc $NDK_DIR/sources/android/native_app_glue $TARGET_DIR/cctools/sources/native_app_glue

mkdir -p $TARGET_DIR/cctools/bin
echo "#!/system/bin/sh" 			 > $TARGET_DIR/cctools/bin/run_na
echo "echo \"\$PWD/\$1\" > \$TMPDIR/runme_na" >> $TARGET_DIR/cctools/bin/run_na
chmod 755 $TARGET_DIR/cctools/bin/run_na

echo "#!/system/bin/sh" 			 > $TARGET_DIR/cctools/bin/run_ca
echo "echo \"\$PWD/\$1\" > \$TMPDIR/runme_ca" >> $TARGET_DIR/cctools/bin/run_ca
chmod 755 $TARGET_DIR/cctools/bin/run_ca

cd $TARGET_DIR
zip -r9y platform-common.zip cctools

popd
