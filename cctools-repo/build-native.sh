#!/bin/ash

if [ ! -f /system/bin/sh ]; then

    echo "Only for on-device native execution."

    exit 0
fi

WRKDIR=${HOME}/tmp

ash ./build-shell-utils.sh ${PWD}/src arm-linux-androideabi ${WRKDIR}/arm-repo  || exit 1

#./build-shell-utils.sh ${PWD}/src mipsel-linux-android  ${WRKDIR}/mips-repo || exit 1

#./build-shell-utils.sh ${PWD}/src i686-linux-android    ${WRKDIR}/i686-repo || exit 1

echo "DONE!"
