build_gcc_host() {
    PKG=gcc
    PKG_VERSION=$gcc_version
    PKG_DESC="The GNU fortran compiler"
    O_DIR=$SRC_PREFIX/$PKG/${PKG}-${PKG_VERSION}
    S_DIR=$src_dir/${PKG}-${PKG_VERSION}
    B_DIR=$build_dir/host-${PKG}

    c_tag ${PKG}-host && return

    banner "Build $PKG host"

    pushd .

    preparesrc $O_DIR $S_DIR

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
	--bindir=${TARGET_DIR}-host/xbin \
	--build=x86_64-linux-gnu \
	--with-gnu-as \
	--with-gnu-ld \
	--enable-languages=c,c++,fortran,objc,obj-c++ \
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
	--with-gxx-include-dir=${SYSROOT}/../include/c++/${gcc_version} \
	--enable-objc-gc \
	$EXTRA_CONF \
	|| error "configure"

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    $MAKE install || error "make install"

    ln -sf ${SYSROOT}/../${TARGET_ARCH}/bin ${TARGET_DIR}-host/${TARGET_ARCH}/bin
    ln -sf ${SYSROOT} ${TARGET_DIR}-host/sysroot

    cd ${SYSROOT}/../${TARGET_ARCH}/lib
    find . -type f -name "libstdc++.a" -exec install -D -m 644 {} ${TARGET_DIR}-host/${TARGET_ARCH}/lib/{} \;
    find . -type f -name "libsupc++.a" -exec install -D -m 644 {} ${TARGET_DIR}-host/${TARGET_ARCH}/lib/{} \;
    find . -type f -name "libgnustl_*" -exec install -D -m 644 {} ${TARGET_DIR}-host/${TARGET_ARCH}/lib/{} \;

    popd
    s_tag ${PKG}-host
}
