build_fortran_host() {
    PKG=gcc
    PKG_VERSION=$gcc_version
    PKG_DESC="The GNU fortran compiler"
    O_DIR=$SRC_PREFIX/$PKG/${PKG}-${PKG_VERSION}
    S_DIR=$src_dir/${PKG}-${PKG_VERSION}
    B_DIR=$build_dir/host-${PKG}-fortran

    c_tag ${PKG}-host-fortran && return

    echo "build $PKG"

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
	--host=x86_64-linux-gnu \
	--prefix=${TARGET_DIR}-host \
	--build=x86_64-linux-gnu \
	--with-gnu-as \
	--with-gnu-ld \
	--enable-languages=fortran \
	--with-gmp=${TARGET_DIR}-host \
	--with-mpfr=${TARGET_DIR}-host \
	--with-mpc=${TARGET_DIR}-host \
	--with-cloog=${TARGET_DIR}-host \
	--with-isl=${TARGET_DIR}-host \
	--with-ppl=${TARGET_DIR}-host \
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

    $MAKE install || error "make install"

    ln -sf ${SYSROOT}/../${TARGET_ARCH}/bin ${TARGET_DIR}-host/${TARGET_ARCH}/bin

    rm -f ${TARGET_DIR}-host/bin/${TARGET_ARCH}-cpp*
    rm -f ${TARGET_DIR}-host/bin/${TARGET_ARCH}-gcc*
    rm -f ${TARGET_DIR}-host/bin/${TARGET_ARCH}-gcov*

    popd
    s_tag ${PKG}-host-fortran
}
