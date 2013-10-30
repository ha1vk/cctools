build_fortran() {
    PKG=gfortran
    PKG_VERSION=4.8
    PKG_DESC="The GNU fortran compiler"
    O_DIR=$SRC_PREFIX/gcc/gcc-${PKG_VERSION}
    S_DIR=$src_dir/gcc-${PKG_VERSION}
    B_DIR=$build_dir/${PKG}

    c_tag ${PKG} && return

    banner "Build $PKG"

    pushd .

#    copysrc $O_DIR $S_DIR

#    cd $S_DIR
#    patch -p1 < $patch_dir/${PKG}-${PKG_VERSION}.patch

    mkdir -p $B_DIR
    cd $B_DIR

    export PATH=${TARGET_DIR}-host/bin:$PATH

    local EXTRA_CONF=
    case $TARGET_ARCH in
    mips*)
	EXTRA_CONF="--with-arch=mips32 --disable-threads --disable-fixed-point"
	;;
    arm*)
	EXTRA_CONF="--with-arch=armv5te --with-float=soft --with-fpu=vfp"
	;;
    *86*)
	EXTRA_CONF="--disable-libquadmath-support"
	;;
    *)
	;;
    esac

    $S_DIR/configure \
	--target=$TARGET_ARCH \
	--host=$TARGET_ARCH \
	--prefix=${TARGET_DIR} \
	--build=x86_64-linux-gnu \
	--with-gnu-as \
	--with-gnu-ld \
	--enable-languages=fortran \
	--with-gmp=${TMPINST_DIR} \
	--with-mpfr=${TMPINST_DIR} \
	--with-mpc=${TMPINST_DIR} \
	--with-cloog=${TMPINST_DIR} \
	--with-isl=${TMPINST_DIR} \
	--with-ppl=${TMPINST_DIR} \
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
	--prefix=${TARGET_DIR} \
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

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install prefix=${TMPINST_DIR}/${PKG}/cctools || error "package install"

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/share
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/include

    for f in bin/gcc-nm bin/*-gcc-ar bin/*-gfortran bin/*-gcc bin/gcc-ar bin/*-gcc-nm \
	bin/*-ranlib bin/gcc bin/cpp bin/*-gcc-${PKG_VERSION} bin/gcc-ranlib bin/gcov; do
	rm -f ${TMPINST_DIR}/${PKG}/cctools/${f}
    done

    find ${TMPINST_DIR}/${PKG}/cctools/lib -name "*.la" | xargs rm -f
    rm -f ${TMPINST_DIR}/${PKG}/cctools/lib/libiberty.a

    find ${TMPINST_DIR}/${PKG}/cctools/lib -name "crtbegin*" -or -name "crtend*" | xargs rm -f
    find ${TMPINST_DIR}/${PKG}/cctools -name "libatomic.*" -or -name "libgomp.*" | xargs rm -f
    find ${TMPINST_DIR}/${PKG}/cctools -name "libgcc.*" -or -name "libgcov.*" | xargs rm -f

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/${TARGET_ARCH}/${PKG_VERSION}/install-tools
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/${TARGET_ARCH}/${PKG_VERSION}/plugin

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${PKG_VERSION}/include
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${PKG_VERSION}/include-fixed
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${PKG_VERSION}/install-tools
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${PKG_VERSION}/plugin

    for f in cc1  collect2  liblto_plugin.la  liblto_plugin.so  liblto_plugin.so.0 \
             liblto_plugin.so.0.0.0  lto1  lto-wrapper plugin/gengtype; do
	rm -f ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/${TARGET_ARCH}/${PKG_VERSION}/${f}
    done

    ${TARGET_ARCH}-strip ${TMPINST_DIR}/${PKG}/cctools/bin/*
    ${TARGET_ARCH}-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/${TARGET_ARCH}/${PKG_VERSION}/*

    ln -sf gfortran ${TMPINST_DIR}/${PKG}/cctools/bin/f77

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC" "gcc"
    cd ${TMPINST_DIR}/${PKG}
    zip -r9y ../$filename cctools pkgdesc

    popd
    s_tag ${PKG}
}
