#!/bin/sh

TOPDIR=$PWD
SYSROOT_DIR="$1"

if [ "x$SYSROOT_DIR" = "x" ]; then
    echo "No sysroot dir"
    exit 1
fi

TARGET_DIR="${TOPDIR}/tmp/native-ndk-platforms-$USER"

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

cd $SYSROOT_DIR

for d in *; do
    pushd .
    vers=${d/*-}
    cd $d
    for a in *; do
	arch=${a/*-}
	x=
	case $arch in
	mips*)
	    x=mipsel-linux-android
	    ;;
	arm*)
	    x=arm-linux-androideabi
	    ;;
	x86*)
	    x=i686-linux-android
	    ;;
	*)
	    continue
	    ;;
	esac
	if [ -d $a/usr/lib ]; then
	    echo "$arch $vers" > $TARGET_DIR/cctools/platform-version
	    mkdir $TARGET_DIR/cctools/$x
	    copysrc $PWD/$a/usr/include $TARGET_DIR/cctools/$x/include
	    copysrc $PWD/$a/usr/lib     $TARGET_DIR/cctools/$x/lib
	    rm -f $TARGET_DIR/cctools/$x/lib/libstdc++*
	    pushd .
	    cd $TARGET_DIR
	    zip -r9y platform-$arch-$vers.zip cctools
	    popd
	    rm -rf $TARGET_DIR/cctools/$x
	    rm -rf $TARGET_DIR/cctools/platform-version
	    echo "platform $arch $vers"
	    sleep 1
	fi
    done
    popd
done

popd
