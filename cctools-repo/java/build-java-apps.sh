#!/bin/sh

TOPDIR=$PWD
work_dir=$1

INSTALL=install

if [ "x$work_dir" = "x" ]; then
    work_dir=${TOPDIR}/tmp
    mkdir -p $work_dir
fi

mkdir -p ${work_dir}/tags

#
# build_package_desc <path> <filename> <name> <version> <arch> <description>
#

build_package_desc() {
    local filename=$2
    local name=$3
    local vers=$4
    local arch=$5
    local desc=$6
cat >$1/pkgdesc << EOF
    <package>
	<name>$name</name>
	<version>$vers</version>
	<arch>$arch</arch>
	<description>$desc</description>
	<size>@SIZE@</size>
	<file>$filename</file>
    </package>
EOF

}

error() {
    echo "Error: $@"
    exit 1
}

s_tag() {
    touch $work_dir/tags/$1
}

c_tag() {
    test -e $work_dir/tags/$1
}

build_java_compiler() {
    PKG="java-mini"
    PKG_DESC="Minimal java compiler pack based on JDK6 java compiler and dx utility"
    PKG_VERSION="1.0"
    PKG_ARCH="all"

    c_tag $PKG && return
    pushd .

    echo "Build javac"
    cd ${TOPDIR}/javac
    ant || error "javac build"

    echo "Build dx"
    cd ${TOPDIR}/dx
    ant || error "dx build"

    echo "Build package"
    rm -rf ${work_dir}/${PKG}/cctools

    $INSTALL -D -m 644 ${TOPDIR}/javac/dist/javac.jar	${work_dir}/${PKG}/cctools/classes/javac.jar
    $INSTALL -D -m 644 ${TOPDIR}/dx/dist/dx.jar		${work_dir}/${PKG}/cctools/classes/dx.jar
    $INSTALL -D -m 644 ${TOPDIR}/classes/android.jar	${work_dir}/${PKG}/cctools/classes/android.jar

    $INSTALL -D -m 755 ${TOPDIR}/bin/dx			${work_dir}/${PKG}/cctools/bin/dx
    $INSTALL -D -m 755 ${TOPDIR}/bin/java		${work_dir}/${PKG}/cctools/bin/java
    $INSTALL -D -m 755 ${TOPDIR}/bin/java-single	${work_dir}/${PKG}/cctools/bin/java-single
    $INSTALL -D -m 755 ${TOPDIR}/bin/javac		${work_dir}/${PKG}/cctools/bin/javac
    $INSTALL -D -m 755 ${TOPDIR}/bin/javac-single	${work_dir}/${PKG}/cctools/bin/javac-single

    local filename="${PKG}_${PKG_VERSION}_${PKG_ARCH}.zip"
    build_package_desc ${work_dir}/${PKG} $filename $PKG $PKG_VERSION $PKG_ARCH "$PKG_DESC"
    cd ${work_dir}/${PKG}
    zip -r9y ../$filename cctools pkgdesc

    popd
    s_tag $PKG
}

build_java_compiler
