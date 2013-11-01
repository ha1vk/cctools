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

    preparesrc $O_DIR $S_DIR

    #copysrc $O_DIR $S_DIR

    #cd $S_DIR
    #patch -p1 < $patch_dir/gcc-$gcc_version.patch || error "patch"

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

    local OLDPATH=$PATH

    export PATH=${TARGET_DIR}-host/xbin:$PATH

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
	--enable-languages=c,c++,fortran,objc,obj-c++ \
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
	--program-suffix=-${gcc_version} \
	--enable-objc-gc \
	$EXTRA_CONF \
	|| error "configure"

#	--with-gdb-version=6.6

    $MAKE $MAKEARGS || error "make $MAKEARGS"

    rm -rf ${TMPINST_DIR}/gcc

    $MAKE install prefix=${TMPINST_DIR}/${PKG}/cctools || error "package install"

    export PATH=$OLDPATH

    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-gcc-ar-$gcc_version
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-gcc-nm-$gcc_version
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-gcc-ranlib-$gcc_version
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-c++-$gcc_version
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-g++-$gcc_version
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-gcc-$gcc_version
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/$TARGET_ARCH-gfortran-$gcc_version
    rm -f ${TMPINST_DIR}/${PKG}/cctools/bin/c++-$gcc_version
    ln -sf g++-${gcc_version} ${TMPINST_DIR}/${PKG}/cctools/bin/c++-$gcc_version

    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/bin/*
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/cc1
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/cc1obj
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/cc1objplus
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/cc1plus
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/collect2
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/f951
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/lto-wrapper
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/lto1
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/install-tools/fixincl
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/liblto_plugin.so.0.0.0
    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/$TARGET_ARCH/$gcc_version/plugin/gengtype

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/info
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/man
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/share

    rm -f ${TMPINST_DIR}/${PKG}/cctools/lib/libiberty.a
    find ${TMPINST_DIR}/${PKG}/cctools/ -name "*.la"  -exec rm -f {} \;

    rm -rf ${TMPINST_DIR}/gfortran
    rm -rf ${TMPINST_DIR}/gobjc
    rm -rf ${TMPINST_DIR}/libgcc-dev
    rm -rf ${TMPINST_DIR}/libgfortran-dev
    rm -rf ${TMPINST_DIR}/libobjc-dev

    cd ${TMPINST_DIR}/${PKG}/cctools/lib
    find . -name "libatomic.*" -type f -exec install -D -m644 {} ${TMPINST_DIR}/libgcc-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/{} \; -exec rm -f {} \;
    find . -name "libgomp.*"   -type f -exec install -D -m644 {} ${TMPINST_DIR}/libgcc-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/{} \; -exec rm -f {} \;

    find . -name "libgfortran.*" -type f -exec install -D -m644 {} ${TMPINST_DIR}/libgfortran-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/{} \; -exec rm -f {} \;

    find . -name "libobjc.*" -type f -exec install -D -m644 {} ${TMPINST_DIR}/libobjc-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/{} \; -exec rm -f {} \;
    find . -name "libobjc_gc.*" -type f -exec install -D -m644 {} ${TMPINST_DIR}/libobjc-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/{} \; -exec rm -f {} \;

    cd ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}
    find . -name "crt*.o"      -type f -exec install -D -m644 {} ${TMPINST_DIR}/libgcc-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/{} \; -exec rm -f {} \;
    find . -name "libgcc.*"    -type f -exec install -D -m644 {} ${TMPINST_DIR}/libgcc-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/{} \; -exec rm -f {} \;
    find . -name "libgcov.*"   -type f -exec install -D -m644 {} ${TMPINST_DIR}/libgcc-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/{} \; -exec rm -f {} \;

    find . -name "libgfortranbegin.*" -type f -exec install -D -m644 {} ${TMPINST_DIR}/libgfortran-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/{} \; -exec rm -f {} \;
    find . -name "libcaf_single.*"    -type f -exec install -D -m644 {} ${TMPINST_DIR}/libgfortran-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/{} \; -exec rm -f {} \;

    copysrc ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/include/objc \
		${TMPINST_DIR}/libobjc-dev/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/include/objc

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/gcc/${TARGET_ARCH}/${gcc_version}/include/objc

    install -D -m 755 ${TMPINST_DIR}/${PKG}/cctools/bin/gfortran-${gcc_version} ${TMPINST_DIR}/gfortran/cctools/bin/gfortran-${gcc_version}
    install -D -m 755 ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/${TARGET_ARCH}/${gcc_version}/f951 \
			${TMPINST_DIR}/gfortran/cctools/libexec/gcc/${TARGET_ARCH}/${gcc_version}/f951

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/bin/gfortran-${gcc_version}
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/${TARGET_ARCH}/${gcc_version}/f951

    install -D -m 755 ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/${TARGET_ARCH}/${gcc_version}/cc1obj \
			${TMPINST_DIR}/gobjc/cctools/libexec/gcc/${TARGET_ARCH}/${gcc_version}/cc1obj
    install -D -m 755 ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/${TARGET_ARCH}/${gcc_version}/cc1objplus \
			${TMPINST_DIR}/gobjc/cctools/libexec/gcc/${TARGET_ARCH}/${gcc_version}/cc1objplus

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/${TARGET_ARCH}/${gcc_version}/cc1obj
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/libexec/gcc/${TARGET_ARCH}/${gcc_version}/cc1objplus

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/thumb
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/armv7-a
    rm -rf ${TMPINST_DIR}/${PKG}/cctools/lib/mips-r2

    cat >> ${TMPINST_DIR}/${PKG}/cctools/bin/set-default-compiler-gcc << EOF
#!/system/bin/sh

ln -sf gcc-${gcc_version}  \${CCTOOLSDIR}/bin/gcc
ln -sf gcc-${gcc_version}  \${CCTOOLSDIR}/bin/cc
ln -sf g++-${gcc_version}  \${CCTOOLSDIR}/bin/g++
ln -sf g++-${gcc_version}  \${CCTOOLSDIR}/bin/c++
ln -sf cpp-${gcc_version}  \${CCTOOLSDIR}/bin/cpp
ln -sf gcov-${gcc_version} \${CCTOOLSDIR}/bin/gcov
EOF
    chmod 755 ${TMPINST_DIR}/${PKG}/cctools/bin/set-default-compiler-gcc

    cat >> ${TMPINST_DIR}/${PKG}/postinst << EOF
#!/system/bin/sh

set-default-compiler-gcc
EOF
    chmod 755 ${TMPINST_DIR}/${PKG}/postinst

    cat >> ${TMPINST_DIR}/${PKG}/prerm << EOF
#!/system/bin/sh

test \`readlink \${CCTOOLSDIR}/bin/gcc\`  = "gcc-${gcc_version}"  && rm -f \${CCTOOLSDIR}/bin/gcc
test \`readlink \${CCTOOLSDIR}/bin/cc\`   = "gcc-${gcc_version}"  && rm -f \${CCTOOLSDIR}/bin/cc
test \`readlink \${CCTOOLSDIR}/bin/g++\`  = "g++-${gcc_version}"  && rm -f \${CCTOOLSDIR}/bin/g++
test \`readlink \${CCTOOLSDIR}/bin/c++\`  = "g++-${gcc_version}"  && rm -f \${CCTOOLSDIR}/bin/c++
test \`readlink \${CCTOOLSDIR}/bin/cpp\`  = "cpp-${gcc_version}"  && rm -f \${CCTOOLSDIR}/bin/cpp
test \`readlink \${CCTOOLSDIR}/bin/gcov\` = "gcov-${gcc_version}" && rm -f \${CCTOOLSDIR}/bin/gcov

which set-default-compiler-clang && set-default-compiler-clang
EOF

    chmod 755 ${TMPINST_DIR}/${PKG}/prerm

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC" "libgcc-dev cxxstl-dev"
    cd ${TMPINST_DIR}/${PKG}
    rm -f ${REPO_DIR}/$filename; zip -r9y ${REPO_DIR}/$filename *

    popd
    s_tag $PKG

    PKG="gobjc"
    PKG_DESC="GNU Objective-C and Objective-C++ compilers"

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC" "gcc libobjc-dev"
    cd ${TMPINST_DIR}/${PKG}
    rm -f ${REPO_DIR}/$filename; zip -r9y ${REPO_DIR}/$filename cctools pkgdesc

    PKG="gfortran"
    PKG_DESC="GNU Fortran compiler"

    cat >> ${TMPINST_DIR}/${PKG}/postinst << EOF
#!/system/bin/sh

ln -sf gfortran-${gcc_version}  \${CCTOOLSDIR}/bin/gfortran
EOF
    chmod 755 ${TMPINST_DIR}/${PKG}/postinst

    cat >> ${TMPINST_DIR}/${PKG}/prerm << EOF
#!/system/bin/sh

test \`readlink \${CCTOOLSDIR}/bin/gfortran\` = "gfortran-${gcc_version}" && rm -f \${CCTOOLSDIR}/bin/gfortran
EOF

    chmod 755 ${TMPINST_DIR}/${PKG}/prerm

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC" "gcc libgfortran-dev"
    cd ${TMPINST_DIR}/${PKG}
    rm -f ${REPO_DIR}/$filename; zip -r9y ${REPO_DIR}/$filename *

    PKG="libgcc-dev"
    PKG_DESC="GCC support library (development files)"

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    rm -f ${REPO_DIR}/$filename; zip -r9y ${REPO_DIR}/$filename cctools pkgdesc

    # Cross package
    local filename="${PKG}-${PKG_ARCH}_${PKG_VERSION}_all.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG}-${PKG_ARCH} $PKG_VERSION all "$PKG_DESC"
    rm -f ${REPO_DIR}/$filename; zip -r9y ${REPO_DIR}/$filename cctools pkgdesc

    if [ "$PKG_ARCH" = "armel" ]; then
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../mips/
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../x86/
    fi

    if [ "$PKG_ARCH" = "mips" ]; then
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../armeabi/
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../x86/
    fi

    if [ "$PKG_ARCH" = "i686" ]; then
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../armeabi/
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../mips/
    fi

    rm -f ${REPO_DIR}/$filename

    PKG="libgfortran-dev"
    PKG_DESC="Runtime library for GNU Fortran applications (development files)"

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    rm -f ${REPO_DIR}/$filename; zip -r9y ${REPO_DIR}/$filename cctools pkgdesc

    # Cross package
    local filename="${PKG}-${PKG_ARCH}_${PKG_VERSION}_all.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG}-${PKG_ARCH} $PKG_VERSION all "$PKG_DESC"
    rm -f ${REPO_DIR}/$filename; zip -r9y ${REPO_DIR}/$filename cctools pkgdesc

    if [ "$PKG_ARCH" = "armel" ]; then
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../mips/
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../x86/
    fi

    if [ "$PKG_ARCH" = "mips" ]; then
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../armeabi/
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../x86/
    fi

    if [ "$PKG_ARCH" = "i686" ]; then
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../armeabi/
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../mips/
    fi

    rm -f ${REPO_DIR}/$filename

    PKG="libobjc-dev"
    PKG_DESC="Runtime library for GNU Objective-C applications (development files)"

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    rm -f ${REPO_DIR}/$filename; zip -r9y ${REPO_DIR}/$filename cctools pkgdesc

    # Cross package
    local filename="${PKG}-${PKG_ARCH}_${PKG_VERSION}_all.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG}-${PKG_ARCH} $PKG_VERSION all "$PKG_DESC"
    rm -f ${REPO_DIR}/$filename; zip -r9y ${REPO_DIR}/$filename cctools pkgdesc

    if [ "$PKG_ARCH" = "armel" ]; then
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../mips/
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../x86/
    fi

    if [ "$PKG_ARCH" = "mips" ]; then
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../armeabi/
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../x86/
    fi

    if [ "$PKG_ARCH" = "i686" ]; then
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../armeabi/
	cp -f ${REPO_DIR}/$filename ${REPO_DIR}/../mips/
    fi

    rm -f ${REPO_DIR}/$filename
}
