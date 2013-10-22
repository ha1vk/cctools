#!/bin/sh

SRC_PREFIX="$1"

TARGET_ARCH="$2"
HOST_ARCH="$2"

SYSROOT_DIR="$3"

if [ "x$SRC_PREFIX" = "x" ]; then
    echo "No source dir"
    exit 1
fi

if [ "x$TARGET_ARCH" = "x" ]; then
    echo "No target arch"
    exit 1
fi

if [ "x$SYSROOT_DIR" = "x" ]; then
    echo "No sysroot dir"
    exit 1
fi

if [ "x$MAKEARGS" = "x" ]; then
    MAKEARGS=-j9
fi

TOPDIR="$PWD"

case $TARGET_ARCH in
arm*|i*86*)
    binutils_version="2.19"
    ;;
*)
    binutils_version="2.21"
    ;;
esac


binutils_version="2.23"
gcc_version="4.8"
gmp_version="5.0.5"
mpc_version="1.0.1"
mpfr_version="3.1.1"
cloog_version="0.18.0"
isl_version="0.11.1"
ppl_version="1.0"
make_version="3.82"
expat_version="2.0.1"
ncurses_version="5.9"
#gdb_version="6.6"
gdb_version="7.3.x"
#gdb_version="7.4.1"

work_dir="${TOPDIR}/tmp/native-ndk-$TARGET_ARCH-$USER"
build_dir="$work_dir/build"
src_dir="$work_dir/src"
patch_dir="$TOPDIR/patches"

TARGET_DIR="$work_dir/cctools"
TMPINST_DIR="$build_dir/tmpinst"

MAKE=make
INSTALL=install

XBUILD_ARCH=`uname -m`
BUILD_SYSTEM=`uname`

case $BUILD_SYSTEM in
Linux)
    BUILD_ARCH=${XBUILD_ARCH}-unknown-linux
    ;;
Darwin)
    BUILD_ARCH=${XBUILD_ARCH}-unknown-darwin
    ;;
CYGWIN*)
    BUILD_ARCH=${XBUILD_ARCH}-unknown-cygwin
    ;;
*)
    BUILD_ARCH=
    echo "unknown host system!"
    exit 1
    ;;
esac

echo "Target arch: $TARGET_ARCH"
echo "Host   arch: $HOST_ARCH"
echo "Build  arch: $BUILD_ARCH"

error() {
    echo "Error: $@"
    exit 1
}

makedirs() {
    mkdir -p $src_dir
    mkdir -p $work_dir/tags
    mkdir -p $build_dir/binutils
    mkdir -p $build_dir/gmp
    mkdir -p $build_dir/mpc
    mkdir -p $build_dir/mpfr
    mkdir -p $build_dir/isl
    mkdir -p $build_dir/ppl
    mkdir -p $build_dir/cloog
    mkdir -p $build_dir/binutils
    mkdir -p $build_dir/gcc
}

s_tag() {
    touch $work_dir/tags/$1
}

c_tag() {
    test -e $work_dir/tags/$1
}

copysrc() {
    mkdir -p $2
    tar -C "$1" -c . | tar -C $2 -xv || error "copysrc $1 $2"
}

build_binutils() {
    PKG=binutils
    O_DIR=$SRC_PREFIX/binutils/binutils-$binutils_version
    S_DIR=$src_dir/binutils-$binutils_version
    B_DIR=$build_dir/binutils

    c_tag $PKG && return

    echo "build $PKG"

    pushd .

    copysrc $O_DIR $S_DIR

    cd $S_DIR
    patch -p1 < $patch_dir/binutils-$binutils_version.patch

    cd $B_DIR

    $S_DIR/configure \
	--target=$TARGET_ARCH \
	--host=$TARGET_ARCH \
	--prefix=$TARGET_DIR \
	--with-sysroot=$SYSROOT_DIR \
	--disable-werror || error "configure"


    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    ln -sf ld $TARGET_DIR/bin/ld.bfd
    cd $TARGET_DIR/$TARGET_ARCH/bin
    for f in * ; do
	ln -sf ../../bin/$f $f
    done

    rm -f ${TARGET_DIR}/lib/libbfd.*a
    rm -f ${TARGET_DIR}/lib/libiberty.a
    rm -f ${TARGET_DIR}/lib/libopcodes.*a

    popd
    s_tag $PKG
}

build_gmp() {
    PKG=gmp
    O_FILE=$SRC_PREFIX/gmp/gmp-$gmp_version.tar.bz2
    S_DIR=$src_dir/gmp-$gmp_version
    B_DIR=$build_dir/gmp

    c_tag $PKG && return

    echo "build $PKG"

    pushd .

    tar jxf $O_FILE -C $src_dir || error "tar jxf $O_FILE"

    cd $S_DIR
    patch -p1 < $patch_dir/gmp-$gmp_version.patch || error "patch"

    cd $B_DIR

    $S_DIR/configure --target=$TARGET_ARCH --host=$TARGET_ARCH --prefix=$TMPINST_DIR --enable-cxx --disable-werror --enable-static --disable-shared || error "configure"

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    popd
    s_tag $PKG
}

build_mpfr() {
    PKG=mpfr
    O_FILE=$SRC_PREFIX/mpfr/mpfr-$mpfr_version.tar.bz2
    S_DIR=$src_dir/mpfr-$mpfr_version
    B_DIR=$build_dir/mpfr

    c_tag $PKG && return

    echo "build $PKG"

    pushd .

    tar jxf $O_FILE -C $src_dir || error "tar jxf $O_FILE"

    cd $S_DIR
    patch -p1 < $patch_dir/mpfr-$mpfr_version.patch || error "patch"

    cd $B_DIR

    $S_DIR/configure --target=$TARGET_ARCH --host=$TARGET_ARCH --prefix=$TMPINST_DIR --with-gmp=$TMPINST_DIR --disable-werror --enable-static --disable-shared || error "configure"

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    popd
    s_tag $PKG
}

build_mpc() {
    PKG=mpc
    O_FILE=$SRC_PREFIX/mpc/mpc-$mpc_version.tar.gz
    S_DIR=$src_dir/mpc-$mpc_version
    B_DIR=$build_dir/mpc

    c_tag $PKG && return

    echo "build $PKG"

    pushd .

    tar zxf $O_FILE -C $src_dir || error "tar jxf $O_FILE"

    cd $S_DIR
    patch -p1 < $patch_dir/mpc-$mpc_version.patch || error "patch"

    cd $B_DIR

    $S_DIR/configure --target=$TARGET_ARCH --host=$TARGET_ARCH --prefix=$TMPINST_DIR --with-gmp=$TMPINST_DIR --with-mpfr=$TMPINST_DIR --disable-werror --enable-static --disable-shared || error "configure"

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    popd
    s_tag $PKG
}

build_isl() {
    PKG=isl
    O_FILE=$SRC_PREFIX/isl/isl-$isl_version.tar.bz2
    S_DIR=$src_dir/isl-$isl_version
    B_DIR=$build_dir/isl

    c_tag $PKG && return

    echo "build $PKG"

    pushd .

    tar jxf $O_FILE -C $src_dir || error "tar jxf $O_FILE"

    cd $B_DIR

    $S_DIR/configure --target=$TARGET_ARCH --host=$TARGET_ARCH --prefix=$TMPINST_DIR --with-gmp-prefix=$TMPINST_DIR --disable-werror --enable-static --disable-shared || error "configure"

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    popd
    s_tag $PKG
}

build_ppl() {
    PKG=ppl
    O_FILE=$SRC_PREFIX/ppl/ppl-$ppl_version.tar.bz2
    S_DIR=$src_dir/ppl-$ppl_version
    B_DIR=$build_dir/ppl

    c_tag $PKG && return

    echo "build $PKG"

    pushd .

    tar jxf $O_FILE -C $src_dir || error "tar jxf $O_FILE"

#    cd $S_DIR
#    patch -p1 < $patch_dir/ppl-$ppl_version.patch || error "patch"

    cd $B_DIR

    $S_DIR/configure --target=$TARGET_ARCH --host=$TARGET_ARCH --prefix=$TMPINST_DIR --with-gmp=$TMPINST_DIR --disable-werror --enable-static --disable-shared || error "configure"

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    popd
    s_tag $PKG
}

build_cloog() {
    PKG=cloog
    O_FILE=$SRC_PREFIX/cloog/cloog-$cloog_version.tar.gz
    S_DIR=$src_dir/cloog-$cloog_version
    B_DIR=$build_dir/cloog

    c_tag $PKG && return

    echo "build $PKG"

    pushd .

    tar zxf $O_FILE -C $src_dir || error "tar zxf $O_FILE"

#    cd $S_DIR
#    patch -p1 < $patch_dir/cloog-$cloog_version.patch || error "patch"

    cd $B_DIR

    $S_DIR/configure --target=$TARGET_ARCH --host=$TARGET_ARCH --prefix=$TMPINST_DIR --with-gmp-prefix=$TMPINST_DIR --with-isl-prefix=$TMPINST_DIR --disable-werror --enable-static --disable-shared || error "configure"

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    popd
    s_tag $PKG
}

build_gcc() {
    PKG=gcc
    O_DIR=$SRC_PREFIX/gcc/gcc-$gcc_version
    S_DIR=$src_dir/gcc-$gcc_version
    B_DIR=$build_dir/gcc

    c_tag $PKG && return

    echo "build $PKG"

    pushd .

    copysrc $O_DIR $S_DIR

    cd $S_DIR
    patch -p1 < $patch_dir/gcc-$gcc_version.patch || error "patch"

    cd $B_DIR

    local EXTRA_CONF=
    case $TARGET_ARCH in
    mips*)
	EXTRA_CONF="--with-arch=mips32 --disable-threads --disable-fixed-point"
	;;
    arm*)
	EXTRA_CONF="--with-arch=armv5te --with-float=soft --with-fpu=vfp"
	;;
    *)
	;;
    esac

#    ac_cv_func_getc_unlocked=no \
#    ac_cv_func_getchar_unlocked=no \
#    ac_cv_func_putc_unlocked=no \
#    ac_cv_func_putchar_unlocked=no \
#    ac_cv_func_getc_unlocked=no \
#    ac_cv_func_getchar_unlocked=no \
#    ac_cv_func_putc_unlocked=no \
#    ac_cv_func_putchar_unlocked=no

    $S_DIR/configure \
	--target=$TARGET_ARCH \
	--host=$TARGET_ARCH \
	--prefix=$TARGET_DIR \
	--build=x86_64-linux-gnu \
	--with-gnu-as \
	--with-gnu-ld \
	--enable-languages=c,c++ \
	--with-gmp=$TMPINST_DIR \
	--with-mpfr=$TMPINST_DIR \
	--with-mpc=$TMPINST_DIR \
	--with-cloog=$TMPINST_DIR \
	--with-isl=$TMPINST_DIR \
	--with-ppl=$TMPINST_DIR \
	--disable-ppl-version-check \
	--disable-cloog-version-check \
	--disable-isl-version-check \
	--enable-cloog-backend=isl \
	--with-host-libstdcxx='-static-libgcc -Wl,-Bstatic,-lstdc++,-Bdynamic -lm' \
	--disable-libssp \
	--enable-threads \
	--disable-nls \
	--disable-libmudflap \
	--disable-libgomp \
	--disable-libstdc__-v3 \
	--disable-sjlj-exceptions \
	--disable-shared \
	--disable-tls \
	--disable-libitm \
	--enable-initfini-array \
	--disable-nls \
	--prefix=$TARGET_DIR \
	--with-binutils-version=$binutils_version \
	--with-mpfr-version=$mpfr_version \
	--with-mpc-version=$mpc_version \
	--with-gmp-version=$gmp_version \
	--with-gcc-version=$gcc_version \
	--disable-bootstrap \
	--disable-libquadmath \
	--enable-plugins \
	--enable-libgomp \
	--disable-libsanitizer \
	--enable-graphite=yes \
	--with-cloog-version=$cloog_version \
	--with-isl-version=$isl_version \
	--with-sysroot=$SYSROOT_DIR \
	$EXTRA_CONF \
	|| error "configure"

#	--with-gdb-version=6.6

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    rm -f $TARGET_DIR/bin/$TARGET_ARCH-gcc-ar
    rm -f $TARGET_DIR/bin/$TARGET_ARCH-gcc-nm
    rm -f $TARGET_DIR/bin/$TARGET_ARCH-gcc-ranlib
    rm -f $TARGET_DIR/bin/$TARGET_ARCH-c++
    rm -f $TARGET_DIR/bin/$TARGET_ARCH-g++
    rm -f $TARGET_DIR/bin/$TARGET_ARCH-gcc
    rm -f $TARGET_DIR/bin/$TARGET_ARCH-gcc-$gcc_version
    ln -sf g++ $TARGET_DIR/bin/c++
    ln -sf gcc $TARGET_DIR/bin/cc

    rm -rf $TARGET_DIR/info
    rm -rf $TARGET_DIR/man
    rm -rf $TARGET_DIR/share

    rm -f ${TARGET_DIR}/lib/libiberty.a

    popd
    s_tag $PKG
}

build_cxxstl() {
    PKG=cxxstl
    c_tag $PKG && return

    local src_dir="$SYSROOT_DIR/../../../sources/cxx-stl/gnu-libstdc++/$gcc_version"
    local inc_dir="$TARGET_DIR/include/c++/$gcc_version"

    copysrc $src_dir/include $inc_dir
    case $TARGET_ARCH in
    mips*)
	copysrc $src_dir/libs/mips/include/bits $inc_dir/$TARGET_ARCH/bits
	$INSTALL -D -m 644 $src_dir/libs/mips/libgnustl_shared.so $TARGET_DIR/$TARGET_ARCH/lib/libgnustl_shared.so
	$INSTALL -D -m 644 $src_dir/libs/mips/libgnustl_static.a  $TARGET_DIR/$TARGET_ARCH/lib/libstdc++.a
	$INSTALL -D -m 644 $src_dir/libs/mips/libsupc++.a         $TARGET_DIR/$TARGET_ARCH/lib/libsupc++.a
	;;
    arm*)
	copysrc $src_dir/libs/armeabi/include/bits $inc_dir/$TARGET_ARCH/bits
	$INSTALL -D -m 644 $src_dir/libs/armeabi/libgnustl_shared.so $TARGET_DIR/$TARGET_ARCH/lib/libgnustl_shared.so
	$INSTALL -D -m 644 $src_dir/libs/armeabi/libgnustl_static.a  $TARGET_DIR/$TARGET_ARCH/lib/libstdc++.a
	$INSTALL -D -m 644 $src_dir/libs/armeabi/libsupc++.a         $TARGET_DIR/$TARGET_ARCH/lib/libsupc++.a

	copysrc $src_dir/libs/armeabi/include/bits $inc_dir/$TARGET_ARCH/thumb/bits
	$INSTALL -D -m 644 $src_dir/libs/armeabi/libgnustl_shared.so $TARGET_DIR/$TARGET_ARCH/lib/thumb/libgnustl_shared.so
	$INSTALL -D -m 644 $src_dir/libs/armeabi/libgnustl_static.a  $TARGET_DIR/$TARGET_ARCH/lib/thumb/libstdc++.a
	$INSTALL -D -m 644 $src_dir/libs/armeabi/libsupc++.a         $TARGET_DIR/$TARGET_ARCH/lib/thumb/libsupc++.a

	copysrc $src_dir/libs/armeabi-v7a/include/bits $inc_dir/$TARGET_ARCH/armv7-a/bits
	$INSTALL -D -m 644 $src_dir/libs/armeabi-v7a/libgnustl_shared.so $TARGET_DIR/$TARGET_ARCH/lib/armv7-a/libgnustl_shared.so
	$INSTALL -D -m 644 $src_dir/libs/armeabi-v7a/libgnustl_static.a  $TARGET_DIR/$TARGET_ARCH/lib/armv7-a/libstdc++.a
	$INSTALL -D -m 644 $src_dir/libs/armeabi-v7a/libsupc++.a         $TARGET_DIR/$TARGET_ARCH/lib/armv7-a/libsupc++.a
	;;
    i*86*)
	copysrc $src_dir/libs/x86/include/bits $inc_dir/$TARGET_ARCH/bits
	$INSTALL -D -m 644 $src_dir/libs/x86/libgnustl_shared.so $TARGET_DIR/$TARGET_ARCH/lib/libgnustl_shared.so
	$INSTALL -D -m 644 $src_dir/libs/x86/libgnustl_static.a  $TARGET_DIR/$TARGET_ARCH/lib/libstdc++.a
	$INSTALL -D -m 644 $src_dir/libs/x86/libsupc++.a         $TARGET_DIR/$TARGET_ARCH/lib/libsupc++.a
	;;
    *)
	error "unknown arch!"
	;;
    esac

    s_tag $PKG
}

build_make() {
    PKG=make
    PKG_URL="http://ftp.gnu.org/gnu/make/make-$make_version.tar.bz2"
    O_FILE=$SRC_PREFIX/make/make-$make_version.tar.bz2
    S_DIR=$src_dir/make-$make_version
    B_DIR=$build_dir/make
    c_tag $PKG && return
    echo "build $PKG"
    pushd .
    mkdir -p $SRC_PREFIX/make
    test -e $O_FILE || wget $PKG_URL -O $O_FILE || error "download $PKG_URL"

    tar jxf $O_FILE -C $src_dir || error "tar jxf $O_FILE"

    mkdir -p $B_DIR
    cd $B_DIR

    CFLAGS="-g -O2 -DNO_ARCHIVES" $S_DIR/configure --target=$TARGET_ARCH --host=$TARGET_ARCH --prefix=$TARGET_DIR --disable-werror || error "configure"

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    rm -rf $TARGET_DIR/share

    popd
    s_tag $PKG
}

build_expat() {
    PKG=expat
    O_DIR=$SRC_PREFIX/expat/expat-$expat_version
    S_DIR=$src_dir/expat-$expat_version
    B_DIR=$build_dir/expat

    c_tag $PKG && return

    echo "build $PKG"

    pushd .

    copysrc $O_DIR $S_DIR

    cd $S_DIR
    patch -p1 < $patch_dir/expat-$expat_version.patch || error "patch"

    mkdir -p $B_DIR
    cd $B_DIR

    $S_DIR/configure --target=$TARGET_ARCH --host=$TARGET_ARCH --prefix=$TMPINST_DIR --disable-werror --enable-static --disable-shared || error "configure"

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    popd
    s_tag $PKG
}

build_ncurses() {
    PKG=ncurses
    PKG_URL="http://ftp.gnu.org/gnu/ncurses/ncurses-$ncurses_version.tar.gz"
    O_FILE=$SRC_PREFIX/ncurses/ncurses-$ncurses_version.tar.bz2
    S_DIR=$src_dir/ncurses-$ncurses_version
    B_DIR=$build_dir/ncurses
    c_tag $PKG && return
    echo "build $PKG"
    pushd .
    mkdir -p $SRC_PREFIX/ncurses
    test -e $O_FILE || wget $PKG_URL -O $O_FILE || error "download $PKG_URL"

    tar zxf $O_FILE -C $src_dir || error "tar jxf $O_FILE"

#    cd $S_DIR
#    patch -p1 < $patch_dir/ncurses-$ncurses_version.patch || error "patch"

    mkdir -p $B_DIR
    cd $B_DIR

    $S_DIR/configure --target=$TARGET_ARCH --host=$TARGET_ARCH --prefix=$TMPINST_DIR --disable-werror --enable-static --disable-shared || error "configure"

    sed -i -e "s|#define HAVE_LOCALE_H 1|#undef HAVE_LOCALE_H|" include/ncurses_cfg.h

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    rm -rf $TARGET_DIR/share

    popd
    s_tag $PKG
}

build_gdbserver() {
    case $TARGET_ARCH in
    arm*)
	$INSTALL -D -m 755 $SYSROOT_DIR/../../../prebuilt/android-arm/gdbserver/gdbserver $TARGET_DIR/bin/gdbserver || error "install gdbserver"
	;;
    mips*)
	$INSTALL -D -m 755 $SYSROOT_DIR/../../../prebuilt/android-mips/gdbserver/gdbserver $TARGET_DIR/bin/gdbserver || error "install gdbserver"
	;;
    x86*|i*86*)
	$INSTALL -D -m 755 $SYSROOT_DIR/../../../prebuilt/android-x86/gdbserver/gdbserver $TARGET_DIR/bin/gdbserver || error "install gdbserver"
	;;
    *)
	error "Unknown target for gdb server!"
	;;
    esac
}

build_gdbserver_unused() {
    PKG=gdbserver
    O_DIR=$SRC_PREFIX/gdb/gdb-$gdb_version
    S_DIR=$src_dir/gdb-$gdb_version
    B_DIR=$build_dir/gdb
    c_tag $PKG && return
    echo "build $PKG"
    pushd .
    copysrc $O_DIR $S_DIR

    cd $S_DIR
    patch -p1 < $patch_dir/gdb-$gdb_version.patch || error "patch"

    mkdir -p $B_DIR/../gdbserver
    cd $B_DIR/../gdbserver

    CPPFLAGS="-I$TMPINST_DIR/include" \
    LDFLAGS="-static -L$TMPINST_DIR/lib" \
    $S_DIR/gdb/gdbserver/configure \
	--target=$TARGET_ARCH \
	--host=$TARGET_ARCH \
	--prefix=$TARGET_DIR \
	--with-gmp=$TMPINST_DIR \
	--with-mpfr=$TMPINST_DIR \
	--with-build-sysroot=$SYSROOT_DIR \
	--disable-werror || error "configure"


    $MAKE $MAKEARGS || error "gdbserver"

    $INSTALL -D -m 755 gdbserver $TARGET_DIR/bin/gdbserver || error "install gdbserver"

    popd
    s_tag $PKG
}

build_gdb() {
    gdb_version=$gdb_version
    PKG=gdb
    O_DIR=$SRC_PREFIX/gdb/gdb-$gdb_version
    S_DIR=$src_dir/gdb-$gdb_version
    B_DIR=$build_dir/gdb
    c_tag $PKG && return
    echo "build $PKG"
    pushd .
    copysrc $O_DIR $S_DIR

    cd $S_DIR
    patch -p1 < $patch_dir/gdb-$gdb_version.patch || error "patch"

    mkdir -p $B_DIR
    cd $B_DIR

    tarch=
    case $TARGET_ARCH in
    arm*)
	tarch=arm-linux-android
	;;
    mips*)
	tarch=mipsel-linux-android
	;;
    i*86*)
	tarch=i686-linux-android
	;;
    *)
	error "unknown arch"
	;;
    esac

    CPPFLAGS="-I$TMPINST_DIR/include" \
    LDFLAGS="-L$TMPINST_DIR/lib" \
    $S_DIR/configure \
	--target=$tarch \
	--host=$TARGET_ARCH \
	--prefix=$TARGET_DIR \
	--with-gmp=$TMPINST_DIR \
	--with-mpfr=$TMPINST_DIR \
	--disable-werror || error "configure"

#	--with-build-sysroot=$SYSROOT_DIR \
#	--disable-gdbmi

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $INSTALL -D -m 755 gdb/gdb $TARGET_DIR/bin/gdb

    popd
    s_tag $PKG
}

strip_bins() {
    $TARGET_ARCH-strip $TARGET_DIR/bin/*
    $TARGET_ARCH-strip $TARGET_DIR/libexec/gcc/$TARGET_ARCH/$gcc_version/cc1
    $TARGET_ARCH-strip $TARGET_DIR/libexec/gcc/$TARGET_ARCH/$gcc_version/cc1plus
    $TARGET_ARCH-strip $TARGET_DIR/libexec/gcc/$TARGET_ARCH/$gcc_version/collect2
    $TARGET_ARCH-strip $TARGET_DIR/libexec/gcc/$TARGET_ARCH/$gcc_version/lto-wrapper
    $TARGET_ARCH-strip $TARGET_DIR/libexec/gcc/$TARGET_ARCH/$gcc_version/lto1
    $TARGET_ARCH-strip $TARGET_DIR/libexec/gcc/$TARGET_ARCH/$gcc_version/install-tools/fixincl
    $TARGET_ARCH-strip $TARGET_DIR/libexec/gcc/$TARGET_ARCH/$gcc_version/liblto_plugin.so.0.0.0
    $TARGET_ARCH-strip $TARGET_DIR/libexec/gcc/$TARGET_ARCH/$gcc_version/plugin/gengtype
}

install_sysroot() {
    PKG=sysroot
    c_tag $PKG && return

    copysrc $SYSROOT_DIR/usr/include $TARGET_DIR/$TARGET_ARCH/include
    copysrc $SYSROOT_DIR/usr/lib     $TARGET_DIR/$TARGET_ARCH/lib

    s_tag $PKG
}

pack_toolchain() {
    PKG=toolchain
    c_tag $PKG && return
    pushd .
    echo "Create toolchain archive"
    local tarch=
    case $TARGET_ARCH in
    mips*)
	tarch=mips
	;;
    arm*)
	tarch=arm
	;;
    i*86*)
	tarch=x86
	;;
    *)
	error "unknown architecture"
	;;
    esac
    rm -rf $TARGET_DIR/$TARGET_ARCH/bin
    cd $TARGET_DIR/..
    zip -r9y $work_dir/toolchain-$tarch.zip `basename $TARGET_DIR`
    popd
    s_tag $PKG
}

makedirs

build_binutils
build_gmp
build_mpfr
build_mpc
build_isl
build_ppl
build_cloog
build_gcc
build_cxxstl
build_make
build_expat
build_ncurses
build_gdbserver
#case $TARGET_ARCH in
#i*86*)
#    echo "skip gdb build, no support for sigsetjmp in bionic"
#    ;;
#*)
#    build_gdb
#    ;;
#esac
strip_bins
pack_toolchain
#install_sysroot
