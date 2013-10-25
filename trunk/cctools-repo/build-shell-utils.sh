#!/bin/bash

ndk_version="r9"

binutils_version="2.23"
gcc_version="4.8"
gmp_version="5.0.5"
mpc_version="1.0.1"
mpfr_version="3.1.1"
cloog_version="0.18.0"
isl_version="0.11.1"
ppl_version="1.0"

make_version="3.82"
ncurses_version="5.9"
nano_version="2.2.6"
busybox_version="1.21.1"
emacs_version="24.2"

binutils_avr_version="2.22"
gcc_avr_version="4.8"

TARGET_INST_DIR="/data/data/com.pdaxrom.cctools/root/cctools"
#TARGET_INST_DIR="/data/data/com.pdaxrom.cctools/cache/cctools"

SRC_PREFIX="$1"

TARGET_ARCH="$2"
HOST_ARCH="$2"

WORK_DIR="$3"

SYSROOT="$4"

NDK_DIR="$5"

if [ "x$SRC_PREFIX" = "x" ]; then
    echo "No source dir"
    exit 1
fi

if [ "x$TARGET_ARCH" = "x" ]; then
    echo "No target arch"
    exit 1
fi

if [ "x$WORK_DIR" = "x" ]; then
    work_dir="/tmp/native-ndk-$TARGET_ARCH-$USER"
else
    work_dir="$WORK_DIR"
fi

if [ "x$NDK_DIR" = "x" ]; then
    NDK_DIR=/opt/android-ndk
fi

if [ "x$MAKEARGS" = "x" ]; then
    MAKEARGS=-j9
fi

TOPDIR="$PWD"

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

case $TARGET_ARCH in
arm*)
    TARGET_ARCH_GLIBC=arm-none-linux-gnueabi
    ;;
mips*)
    TARGET_ARCH_GLIBC=mips-linux-gnu
    ;;
i*86*|x86*)
    TARGET_ARCH_GLIBC=i686-pc-linux-gnu
    ;;
*)
    echo "unknown arch $TARGET_ARCH"
    exit 1
    ;;
esac

echo "Target arch: $TARGET_ARCH"
echo "Host   arch: $HOST_ARCH"
echo "Build  arch: $BUILD_ARCH"

banner() {
    echo
    echo "*********************************************************************************"
    echo "$1"
    echo
    if [ "$TERM" = "xterm-color" -o "$TERM" = "xterm" ]; then
	echo -ne "\033]0;${1}\007"
    fi
}

trap "banner ''" 2

error() {
    echo
    echo "*********************************************************************************"
    echo "Error: $@"
    echo
    exit 1
}

makedirs() {
    mkdir -p $src_dir
    mkdir -p $work_dir/tags
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

preparesrc() {
    if [ ! -d $2 ]; then
	pushd .
	copysrc $1 $2
	cd $2
	patch -p1 < $patch_dir/`basename $2`.patch
	popd
    fi
}

#
# build_package_desc <path> <filename> <name> <version> <arch> <description>
#

build_package_desc() {
    local filename=$2
    local name=$3
    local vers=$4
    local arch=$5
    local desc=$6
    local unpacked_size=`du -sb ${1}/cctools | cut -f1`
cat >$1/pkgdesc << EOF
    <package>
	<name>$name</name>
	<version>$vers</version>
	<arch>$arch</arch>
	<description>$desc</description>
	<unpackedsize>$unpacked_size</unpackedsize>
	<size>@SIZE@</size>
	<file>$filename</file>
    </package>
EOF

}

case $TARGET_ARCH in
arm*)
    PKG_ARCH="armel"
    ;;
mips*)
    PKG_ARCH="mipsel"
    ;;
i*86*)
    PKG_ARCH="i686"
    ;;
*)
    error "Can't set PKG_ARCH from $TARGET_ARCH"
    ;;
esac

for f in rules/*.sh; do
    echo "Include $f"
    . $f
done

makedirs

build_gmp_host
build_gmp
build_mpfr_host
build_mpfr
build_mpc_host
build_mpc
build_isl_host
build_isl
build_ppl_host
build_ppl
build_cloog_host
build_cloog

# CCTools native tools moved from bundle
build_binutils
build_gcc
build_cxxstl
build_make
build_ndk_misc
build_ndk_sysroot

# Clang
build_llvm

# Addons
build_ncurses
build_busybox
build_htop
build_luajit
build_openssl
build_expat
build_sqlite
build_apr
build_aprutil
build_neon
build_subversion
build_curl
build_wget
build_git
build_dropbear
#build_fpc
#build_nano
#build_emacs
build_binutils_avr_host
build_binutils_avr
build_gcc_avr_host
build_gcc_avr
build_avr_libc
build_fortran_host
build_fortran
build_fortran_examples
build_netcat
