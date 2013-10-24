build_make() {
    PKG=make
    PKG_URL="http://ftp.gnu.org/gnu/make/make-$make_version.tar.bz2"
    PKG_DESC="An utility for Directing compilation."
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

    $MAKE install prefix=${TMPINST_DIR}/${PKG}/cctools || error "package install"

    $TARGET_ARCH-strip ${TMPINST_DIR}/${PKG}/cctools/bin/*

    rm -rf ${TMPINST_DIR}/${PKG}/cctools/share

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename $PKG $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    zip -r9y ../$filename cctools pkgdesc

    popd
    s_tag $PKG
}
