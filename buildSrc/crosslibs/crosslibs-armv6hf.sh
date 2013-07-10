#!/bin/bash

confirm() {
    echo -n "Is this correct? [Y/n]: "
    read -n 1 -r
    echo
    if [[ $REPLY == "y" || $REPLY == "Y" || $REPLY == "" ]]; then
        CONFIRMED=1
    else
        CONFIRMED=0
    fi
}

checkReinstall() {
    if [[ -d $1 ]]; then
        echo
        echo $1 already exists.
        echo -n "Delete and re-install? [y/N]: "
        read -n 1 -r
        echo
        if [[ $REPLY == "y" || $REPLY == "Y" ]]; then
            /bin/rm -rf $1
        fi
    fi
}

# Check whether or not FS points to the root filesystem of a Raspberry Pi
testFS() {
    if [[ -d $1/opt/vc/include && -d $1/lib/arm-linux-gnueabihf ]]; then
        FS_OK=1
    else
        FS_OK=0
    fi
}

installPiLibs() {
    cat << EOF
Run the following command on your Raspberry Pi:

sudo apt-get install \\
  libatk1.0-dev \\
  libdirectfb-dev \\
  libgtk2.0-dev \\
  libgstreamer0.10-dev \\
  libgstreamer-plugins-base0.10-dev \\
  libudev-dev \\
  libxml2-dev \\
  libxslt1-dev \\
  libxtst-dev

Then shutdown your Pi and connect its SD card to this computer.
Make sure its filesystem is mounted, then press ENTER.
EOF

    read
    FS=
    for filesystem in `mount -t ext4 | cut -f3 -d" "`; do
        testFS $filesystem
        if [[ $FS_OK -eq 1 ]]; then
            FS=$filesystem
        fi
    done

    if [[ -z "$FS" ]]; then
        echo Raspberry Pi filesystem not found.
    else
        echo Reading Pi filesystem from $FS
        confirm()
        if [[ $CONFIRMED -eq 0 ]]; then
            FS=
        fi
    fi

    while [[ -z "$FS" ]]; do
        echo -n "Enter the location of the filesystem: "
        read -e FS
        testFS $FS
        if [[ $FS_OK -eq 0 ]]; then
            echo "'"$FS"'" does not contain a Raspberry Pi root filesystem.
            FS=
        fi
    done

    echo Using $FS as the Raspberry Pi filesystem

    echo Copying files to $PILIBS
    mkdir -p $PILIBS
    cd $FS
    cp --parents -rdt $PILIBS \
        usr/include/atk-1.0 \
        usr/include/cairo \
        usr/include/directfb \
        usr/include/fontconfig \
        usr/include/freetype2 \
        usr/include/ft2build.h \
        usr/include/gdk-pixbuf-2.0/ \
        usr/include/gio-unix-2.0/ \
        usr/include/glib-2.0 \
        usr/include/gstreamer-0.10 \
        usr/include/gtk-2.0 \
        usr/include/libudev.h \
        usr/include/libpng12 \
        usr/include/libxml2 \
        usr/include/libxslt \
        usr/include/linux \
        usr/include/pango-1.0 \
        usr/include/pixman-1 \
        usr/include/X11 \
        usr/include/xcb \
        usr/include/zlib.h \
        usr/lib/arm-linux-gnueabihf/glib-2.0/include \
        usr/lib/arm-linux-gnueabihf/gtk-2.0/include \
        opt/vc/include \
        \
        usr/lib/libudev* \
        usr/lib/arm-linux-gnueabihf/libasound* \
        usr/lib/arm-linux-gnueabihf/libatk-1.0* \
        usr/lib/arm-linux-gnueabihf/libcairo* \
        usr/lib/arm-linux-gnueabihf/libdirect* \
        usr/lib/arm-linux-gnueabihf/libdirectfb* \
        usr/lib/arm-linux-gnueabihf/libfontconfig* \
        usr/lib/arm-linux-gnueabihf/libfreetype* \
        usr/lib/arm-linux-gnueabihf/libfusion* \
        usr/lib/arm-linux-gnueabihf/libgdk-x11-2.0* \
        usr/lib/arm-linux-gnueabihf/libgdk_pixbuf-2.0* \
        usr/lib/arm-linux-gnueabihf/libgio-2.0* \
        usr/lib/arm-linux-gnueabihf/libGL* \
        usr/lib/arm-linux-gnueabihf/libglib-2.0* \
        usr/lib/arm-linux-gnueabihf/libgmodule-2.0* \
        usr/lib/arm-linux-gnueabihf/libgobject-2.0* \
        usr/lib/arm-linux-gnueabihf/libgst* \
        usr/lib/arm-linux-gnueabihf/libgthread-2.0* \
        usr/lib/arm-linux-gnueabihf/libgtk-x11-2.0* \
        usr/lib/arm-linux-gnueabihf/libm.* \
        usr/lib/arm-linux-gnueabihf/libpango-1.0* \
        usr/lib/arm-linux-gnueabihf/libpangocairo-1.0* \
        usr/lib/arm-linux-gnueabihf/libpangoft2-1.0* \
        usr/lib/arm-linux-gnueabihf/libpthread_nonshared.a \
        usr/lib/arm-linux-gnueabihf/librt* \
        usr/lib/arm-linux-gnueabihf/librt* \
        usr/lib/arm-linux-gnueabihf/libX11* \
        usr/lib/arm-linux-gnueabihf/libXau* \
        usr/lib/arm-linux-gnueabihf/libxcb* \
        usr/lib/arm-linux-gnueabihf/libXdmcp* \
        usr/lib/arm-linux-gnueabihf/libXext* \
        usr/lib/arm-linux-gnueabihf/libXtst.so* \
        usr/lib/arm-linux-gnueabihf/libXxf86vm* \
        usr/lib/arm-linux-gnueabihf/libxml2.so* \
        usr/lib/arm-linux-gnueabihf/libxslt.so* \
        usr/lib/arm-linux-gnueabihf/libz* \
        opt/vc/lib \
        \
        usr/lib/pkgconfig \
        usr/lib/arm-linux-gnueabihf/pkgconfig \
        usr/share/pkgconfig \
        lib/arm-linux-gnueabihf/libpthread* \
        lib/arm-linux-gnueabihf/libudev* \
        \
        |& grep -v warning

    testFS $PILIBS
    if [[ $FS_OK -eq 0 ]]; then
        echo Copy failed.
        exit 1
    fi

    cat > $PILIBS/usr/lib/arm-linux-gnueabihf/libpthread.so << EOF
OUTPUT_FORMAT(elf32-littlearm)
GROUP ( ../../../lib/arm-linux-gnueabihf/libpthread.so.0 libpthread_nonshared.a )
EOF
    if [[ ! $? -eq 0 ]]; then
        echo libpthread patch failed.
        exit 1
    fi

# Install an alternative pkg-config
    mkdir -p $PILIBS/bin || exit 1
    cp $SCRIPTDIR/pkg-config $PILIBS/bin || exit 1
    chmod +x $PILIBS/bin/pkg-config || exit 1

# Patch package configuration files
    find $PILIBS/usr -name "*.pc" | xargs sed -i -e "s:=/usr/:=\${prefix}/:"
    if [[ ! $? -eq 0 ]]; then
        echo pkg-config patch failed.
        exit 1
    fi
}

installCrossCompiler() {
    echo
    echo Fetching and unpacking compiler in $CROSSLIBS
    echo
    echo NOTE: if you use a proxy server then this download will probably fail. In that
    echo case you need to set a value for the environment variable https_proxy and run
    echo this script again.
    echo
    COMPILER_URL=https://launchpad.net/linaro-toolchain-unsupported/trunk/2012.09/+download/gcc-linaro-arm-linux-gnueabihf-raspbian-2012.09-20120921_linux.tar.bz2
    CMD="wget $COMPILER_URL -O - | tar jx -C $CROSSLIBS"
    echo $CMD
    echo
    /bin/sh -c "$CMD"
}

SCRIPTDIR=`dirname $0`
SCRIPTDIR=`cd $SCRIPTDIR ; pwd`
RT=`cd $SCRIPTDIR/../.. ; pwd`

echo Using OpenJFX working directory at $RT
confirm()
if [[ $CONFIRMED -eq 0 ]]; then
    echo -n "Enter the location of the OpenJFX working directory: "
    read RT
fi

CROSSLIBS=`dirname $RT`/crosslibs
echo Using crosslibs directory $CROSSLIBS

mkdir -p $CROSSLIBS || exit 1

PILIBS=$CROSSLIBS/armhf-raspberry-pi-09

checkReinstall $PILIBS
if [[ ! -d $PILIBS ]]; then
    installPiLibs
fi

CROSSCOMPILER=$CROSSLIBS/gcc-linaro-arm-linux-gnueabihf-raspbian-2012.09-20120921_linux
checkReinstall $CROSSCOMPILER
if [[ ! -d $CROSSCOMPILER ]]; then
    installCrossCompiler
fi

echo Done.

