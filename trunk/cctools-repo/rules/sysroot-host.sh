build_sysroot_host() {
    PKG=sysroot

    c_tag ${PKG}-host && return

    banner "Build $PKG host"

    pushd .

    local FROM="${NDK_DIR}/platforms/android-9"

    case $TARGET_ARCH in
    arm*)
	FROM="${FROM}/arch-arm"
	;;
    mips*)
	FROM="${FROM}/arch-mips"
	;;
    *86*)
	FROM="${FROM}/arch-x86"
	;;
    *)
	error "host sysroot - unknown target"
	;;
    esac

    copysrc $FROM $SYSROOT

    rm -rf ${SYSROOT}/usr/lib/libstdc++.*

    popd
    s_tag ${PKG}-host
}
