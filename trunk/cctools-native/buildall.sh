#!/bin/sh

TOPDIR=$PWD
SRCDIR=/home/sash/Work/android/ndk-source
NDKDIR=/home/sash/Work/android/android-ndk-r8d


${NDKDIR}/build/tools/make-standalone-toolchain.sh --arch=arm  --install-dir=${TOPDIR}/tmp/arm-android --toolchain=arm-linux-androideabi-4.7
${NDKDIR}/build/tools/make-standalone-toolchain.sh --arch=mips --install-dir=${TOPDIR}/tmp/mips-android --toolchain=mipsel-linux-android-4.7
${NDKDIR}/build/tools/make-standalone-toolchain.sh --arch=x86  --install-dir=${TOPDIR}/tmp/x86-android --toolchain=x86-4.7

export PATH=${TOPDIR}/tmp/arm-android/bin:${TOPDIR}/tmp/mips-android/bin:${TOPDIR}/tmp/x86-android/bin:$PATH

MAKEARGS=-j4 ./build-native-toolchain.sh \
    $SRCDIR \
    mipsel-linux-android \
    ${NDKDIR}/platforms/android-14/arch-mips

MAKEARGS=-j4 ./build-native-toolchain.sh \
    $SRCDIR \
    arm-linux-androideabi \
    ${NDKDIR}/platforms/android-9/arch-arm

MAKEARGS=-j4 ./build-native-toolchain.sh \
    $SRCDIR \
    i686-linux-android \
    ${NDKDIR}/platforms/android-14/arch-x86

./pack-platform-files.sh ${NDKDIR}/platforms/

./pack-platform-common.sh $NDKDIR
