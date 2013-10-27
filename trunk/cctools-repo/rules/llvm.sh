build_llvm() {
    PKG=llvm
    PKG_VERSION=3.3
    PKG_URL="http://ya.ru/${PKG}-${PKG_VERSION}.tgz"
    PKG_DESC="Low-Level Virtual Machine (LLVM), runtime library"
    O_DIR=$SRC_PREFIX/${PKG}-${PKG_VERSION}
    S_DIR=$src_dir/${PKG}-${PKG_VERSION}
    B_DIR=$build_dir/${PKG}-${PKG_VERSION}/llvm

    c_tag $PKG && return

    banner "Build $PKG"

    pushd .

    copysrc $O_DIR $S_DIR

    cd $S_DIR
    patch -p1 < $patch_dir/${PKG}-${PKG_VERSION}.patch || error "patch"

    mkdir -p $B_DIR
    cd $B_DIR

    # Configure here

    local CLANG_TARGET=
    case $TARGET_ARCH in
    arm*)
	CLANG_TARGET="arm"
	;;
    mips*)
	CLANG_TARGET="mips"
	;;
    x86*|i*86*|amd64*)
	CLAGS_TARGET="x86"
	;;
    *)
	CLANG_TARGET="arm,mips,x86"
	;;
    esac

    #
    # enable all android architectures
    #
    CLANG_TARGET="arm,mips,x86"

    EXTRA_CONFIG_FLAGS=

    ac_cv_func_mmap_fixed_mapped=yes \
    ac_cv_func_mmap_file=yes\
    $S_DIR/llvm/configure \
    --target=$TARGET_ARCH \
    --host=$TARGET_ARCH \
    --prefix=$TMPINST_DIR \
    --enable-targets=$CLANG_TARGET \
    --enable-optimized \
    --with-binutils-include=$SRC_PREFIX/binutils/binutils-$binutils_version/include \
    --enable-keep-symbols \
    $EXTRA_CONFIG_FLAGS

#    --enable-shared
#    --disable-static

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install prefix=$TMPINST_DIR || error "make install"

    $MAKE install prefix=${TMPINST_DIR}/${PKG}/cctools || error "package install"

    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/bin/*
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/lib/*.so

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/docs
    #rm -f  ${TMPINST_DIR}/${PKG}/cctools/lib/*.a
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/share

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename $PKG $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    zip -r9y ../$filename cctools pkgdesc

    local LLVMROOTDIR=${TMPINST_DIR}/${PKG}/cctools

    popd
    s_tag $PKG

    pushd .

    PKG=clang
    PKG_DESC="C, C++ and Objective-C compiler (LLVM based)"

    mkdir -p ${TMPINST_DIR}/${PKG}/cctools/bin
    mkdir -p ${TMPINST_DIR}/${PKG}/cctools/lib
    cp -f ${LLVMROOTDIR}/bin/${TARGET_ARCH}-clang ${TMPINST_DIR}/${PKG}/cctools/bin/clang
    ln -sf clang ${TMPINST_DIR}/${PKG}/cctools/bin/clang++
    cp -Rf ${LLVMROOTDIR}/lib/clang ${TMPINST_DIR}/${PKG}/cctools/lib/

    cp -f ${LLVMROOTDIR}/lib/libprofile_rt.a  ${TMPINST_DIR}/${PKG}/cctools/lib/
    cp -f ${LLVMROOTDIR}/lib/libprofile_rt.so ${TMPINST_DIR}/${PKG}/cctools/lib/

    cat >> ${TMPINST_DIR}/${PKG}/postinst << EOF
#!/system/bin/sh

ln -s ${TARGET_ARCH} \${CCTOOLSDIR}/sysroot

if [ ! -f \${CCTOOLSDIR}/bin/cc ]; then
    echo "#!/system/bin/sh" > \${CCTOOLSDIR}/bin/cc
    echo "exec \${CCTOOLSDIR}/bin/clang -integrated-as \\\$@" >> \${CCTOOLSDIR}/bin/cc

    chmod 755 \${CCTOOLSDIR}/bin/cc
fi

if [ ! -f \${CCTOOLSDIR}/bin/c++ ]; then
    echo "#!/system/bin/sh" > \${CCTOOLSDIR}/bin/c++
    echo "exec \${CCTOOLSDIR}/bin/clang++ -integrated-as \\\$@" >> \${CCTOOLSDIR}/bin/c++

    chmod 755 \${CCTOOLSDIR}/bin/c++
fi

if [ ! -f \${CCTOOLSDIR}/bin/gcc ]; then
    ln -s cc \${CCTOOLSDIR}/bin/gcc
fi

if [ ! -f \${CCTOOLSDIR}/bin/g++ ]; then
    ln -s c++ \${CCTOOLSDIR}/bin/g++
fi

EOF

    chmod 755 ${TMPINST_DIR}/${PKG}/postinst

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename $PKG $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    zip -r9y ../$filename *

    PKG=libclang
    PKG_DESC="clang library"

    mkdir -p ${TMPINST_DIR}/${PKG}/cctools/lib
    cp -f ${LLVMROOTDIR}/lib/libclang.so ${TMPINST_DIR}/${PKG}/cctools/lib/

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename $PKG $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    zip -r9y ../$filename cctools pkgdesc

    PKG=clang-utils
    PKG_DESC="clang utilities"

    mkdir -p ${TMPINST_DIR}/${PKG}/cctools/bin
    cp -f ${LLVMROOTDIR}/bin/${TARGET_ARCH}-clang-check  ${TMPINST_DIR}/${PKG}/cctools/bin/clang-check
    cp -f ${LLVMROOTDIR}/bin/${TARGET_ARCH}-clang-format ${TMPINST_DIR}/${PKG}/cctools/bin/clang-format
    cp -f ${LLVMROOTDIR}/bin/${TARGET_ARCH}-clang-tblgen ${TMPINST_DIR}/${PKG}/cctools/bin/clang-tblgen

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename $PKG $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    zip -r9y ../$filename cctools pkgdesc

    popd
}