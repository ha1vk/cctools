build_gcc() {
    PKG=gcc
    PKG_VERSION=$gcc_version
    PKG_DESC="The GNU C compiler"
    O_DIR=$SRC_PREFIX/$PKG/${PKG}-${PKG_VERSION}
    S_DIR=$src_dir/${PKG}-${PKG_VERSION}
    B_DIR=$build_dir/${PKG}

    c_tag $PKG && return

    banner "Build $PKG"

    pushd .

    copysrc $O_DIR $S_DIR

    cd $S_DIR
    patch -p1 < $patch_dir/gcc-$gcc_version.patch || error "patch"

    mkdir -p $B_DIR
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
	--with-sysroot=$SYSROOT \
	$EXTRA_CONF \
	|| error "configure"

#	--with-gdb-version=6.6

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install prefix=${TMPINST_DIR}/${PKG}/cctools || error "package install"

    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-gcc-ar
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-gcc-nm
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-gcc-ranlib
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-c++
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-g++
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-gcc
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-gcc-$gcc_version

    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/bin/*
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/cc1
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/cc1plus
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/collect2
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/lto-wrapper
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/lto1
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/install-tools/fixincl
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/liblto_plugin.so.0.0.0
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/plugin/gengtype

    ln -sf g++ ${TMPINST_DIR}/${PKG}/cctools/bin/c++
    ln -sf gcc ${TMPINST_DIR}/${PKG}/cctools/bin/cc

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/info
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/man
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/share

    rm -f ${TMPINST_DIR}/${PKG}/cctools/lib/libiberty.a

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    zip -r9y ../$filename cctools pkgdesc

    local OLDPKGPATH=${TMPINST_DIR}/${PKG}/cctools/

    popd
    s_tag $PKG

    PKG=libgcc-standalone
    PKG_DESC="The libgcc and support files. Normally you don't need this for gcc package, but required for alternative compilers such as clang."

    copysrc ${OLDPKGPATH}/lib/gcc ${TMPINST_DIR}/${PKG}/cctools/lib/gcc

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${PKG_VERSION}/finclude
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${PKG_VERSION}/include
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${PKG_VERSION}/include-fixed
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${PKG_VERSION}/install-tools
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${PKG_VERSION}/plugin

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    zip -r9y ../$filename cctools pkgdesc
}
