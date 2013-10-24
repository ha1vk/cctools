build_netcat() {
    PKG=netcat
    PKG_VERSION=1.10
    PKG_DESC="Netcat is a featured networking utility which reads and writes data across network connections, using the TCP/IP protocol."
    c_tag ${PKG} && return

    banner "Build $PKG"

    mkdir -p ${TMPINST_DIR}/${PKG}/cctools/bin

    ${TARGET_ARCH}-gcc ${SRC_PREFIX}/nc/netcat.c -s -o ${TMPINST_DIR}/${PKG}/cctools/bin/netcat || error "compilation"
    ln -s netcat ${TMPINST_DIR}/${PKG}/cctools/bin/nc

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${TMPINST_DIR}/${PKG} $filename ${PKG} $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${TMPINST_DIR}/${PKG}
    zip -r9y ../$filename cctools pkgdesc

    s_tag ${PKG}
}
