build_sdktools() {
    PKG=sdktools
    PKG_VERSION=1.0
    PKG_DESC="android sdk tools"
    O_DIR=${TOPDIR}/misc/sdktools
    B_DIR=${build_dir}/${PKG}

    c_tag ${PKG} && return

    banner "Build $PKG"

    preparesrc $O_DIR $B_DIR

    cd $B_DIR

    local OPTS
    case $TARGET_ARCH in
    arm*)
	OPTS="CPU=arm"
	;;
    mips*)
	OPTS="CPU=mips"
	;;
    i*86*)
	OPTS="CPU=i686"
	;;
    esac

    make -C aapt CC=${TARGET_ARCH}-gcc CXX=${TARGET_ARCH}-g++ ${OPTS} || error "build aapt and zipalign"
    make -C aidl CC=${TARGET_ARCH}-gcc CXX=${TARGET_ARCH}-g++ ${OPTS} || error "build aapt and zipalign"

    install -D -m 755 aapt/aapt     ${TMPINST_DIR}/${PKG}/cctools/bin/aapt
    install -D -m 755 aapt/zipalign ${TMPINST_DIR}/${PKG}/cctools/bin/zipalign
    install -D -m 755 aidl/aidl     ${TMPINST_DIR}/${PKG}/cctools/bin/aidl

    $STRIP ${TMPINST_DIR}/${PKG}/cctools/bin/*

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    rm -f ${REPO_DIR}/$filename; zip -r9y ${REPO_DIR}/$filename *

    s_tag ${PKG}
}
