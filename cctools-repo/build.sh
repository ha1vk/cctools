#!/bin/bash

WRKDIR=$PWD/tmp
NDKDIR=/opt/android-ndk
NDKSRC=/home/sash/Work/android/ndk-source

for d in binutils gcc gmp mpc mpfr cloog isl ppl llvm-3.3; do
    ln -sf ${NDKSRC}/${d} src/
done

for f in ../cctools-native/patches/*; do
    ln -sf ../${f} patches/
done

export PATH=${WRKDIR}/arm-android/bin:${WRKDIR}/mips-android/bin:${WRKDIR}/x86-android/bin:/home/opt/CodeSourcery/Sourcery_G++_Lite/bin:/opt/CodeSourcery/Sourcery_G++_Lite/bin:$PATH

${NDKDIR}/build/tools/make-standalone-toolchain.sh --arch=arm --install-dir=${WRKDIR}/arm-android --toolchain=arm-linux-androideabi-4.8
./build-shell-utils.sh ${PWD}/src arm-linux-androideabi ${WRKDIR}/arm-repo ${WRKDIR}/arm-android/sysroot || exit 1

${NDKDIR}/build/tools/make-standalone-toolchain.sh --arch=mips --install-dir=${WRKDIR}/mips-android --toolchain=mipsel-linux-android-4.8
./build-shell-utils.sh ${PWD}/src mipsel-linux-android  ${WRKDIR}/mips-repo ${WRKDIR}/mips-android/sysroot || exit 1

${NDKDIR}/build/tools/make-standalone-toolchain.sh --arch=x86 --install-dir=${WRKDIR}/x86-android --toolchain=x86-4.8
./build-shell-utils.sh ${PWD}/src i686-linux-android    ${WRKDIR}/i686-repo ${WRKDIR}/x86-android/sysroot || exit 1

echo "DONE!"
