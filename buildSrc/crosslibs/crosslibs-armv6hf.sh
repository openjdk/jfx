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

getPackages() {
    TOOLCHAIN=$1
    REPO=$2
    DISTRO=$3
    CATEGORY=$4
    ARCH=$5
    PACKAGES=${@:6}

    PACKAGEDIR=`echo $REPO | tr /: -`-$DISTRO-$CATEGORY-$ARCH
    PACKAGELIST=$PACKAGEDIR/Packages

    cd $RT/..
    OUT="crosslibs/$TOOLCHAIN"
    mkdir -p $OUT
    cd $OUT
    echo Working in $PWD

    WGET="`which wget` -N --no-verbose"
    if [ ! -f ${PACKAGELIST}/ ]
    then
        mkdir -p $PACKAGEDIR
        cd $PACKAGEDIR
        echo Getting package list
        $WGET $REPO/dists/$DISTRO/$CATEGORY/binary-$ARCH/Packages.gz
        if [ ! -f Packages.gz ]
        then
            echo "Failed to download Packages for this distro"
            exit -1
        fi
        gunzip -c Packages.gz > Packages
        cd ..
    else 
        echo "Already have ${PACKAGELIST}, will reuse"
    fi

    DPKG_DEB=`which dpkg-deb`
    if [ ! "$DPKG_DEB" ]
    then
        echo "did not find dpkg-deb"
    fi
    SED=`which sed`
    if [ ! "$SED" ]
    then
        echo "did not find sed"
    fi

    echo
    echo "Processing our packages"

    for PACKAGE in ${PACKAGES}; do
        echo Working on package $PACKAGE
        PACKPATH=`$SED -ne "/^Package: $PACKAGE\$/,/Filename:/ s/^Filename: // p" ${PACKAGELIST}`
        if [[ -z "$PACKPATH" ]]; then
            echo "Could not find package $PACKAGE at $PACKPATH"
        else
            FILE=`/usr/bin/basename $PACKPATH`
            if [ ! -f "${FILE}" ]
            then
                echo "Fetching $PACKAGE ($FILE)"
                $WGET $REPO/$PACKPATH
            else
                echo Reusing cached $PACKAGE 
            fi
            echo Unpacking $PACKAGE
            $DPKG_DEB -x $FILE .
        fi
    done

    echo
    echo "Have all of our packages"
    echo
    echo "Clean up some unneeded junk"
    # clean up good stuff we don't need that was pulled in.
    rm -rf \
        bin etc sbin var selinux \
        usr/lib/gnu-smalltalk \
        usr/lib/mime/ \
        usr/lib/locale/ \
        usr/lib/girepository-1.0 \
        usr/share/man \
        usr/share/locale \
        usr/share/doc \
        usr/sbin \
        usr/lib/compat-ld \
        usr/lib/gold-ld \
        \
        usr/lib/arm-linux-gnueabihf/gconv \
        usr/share/X11/locale \
        \
        usr/bin

    echo
    echo "Checking for symlinks that need to be patched"

    #!/bin/bash

    find usr lib -type l ! -exec test -r {} \; -print | while read link
    do 
        fileo=`file $link`
        src=${fileo##*broken symbolic link to }
        src=${src##\`}
        src=${src%\'}
        tgt=${fileo%:*}

        if [[ "$src" =~ '/' ]]
        then
            # Fix absolute path by adding ../ for each level in the src
            lsrc=${src#/}
            ltgt=${tgt}
            j=`expr index "$ltgt" /`
            while [[ $j > 0 ]]
            do
                ltgt=${ltgt:$j}
                j=`expr index "$ltgt" /`
                lsrc="../${lsrc}"
            done
            echo fixing broken link  ln -sf $lsrc $tgt
            ln -sf $lsrc $tgt
        fi
    done

    remaining=`find usr lib -type l ! -exec test -r {} \; -print `
    if [ "$remaining" ]
    then
        echo 
        echo 
        echo "Warning broken links remaining:"
        find usr lib -type l ! -exec test -r {} \; -print | while read link
        do 
            fileo=`file $link`
            src=${fileo##* \`}
            src=${src%\'}
            tgt=${fileo%:*}

            echo "broken link $tgt -> $src"
        done
    fi

    # misc fixups, patch absolute ld scripts

        cat > usr/lib/arm-linux-gnueabihf/libpthread.so << EOF
    /* GNU ld script
       Use the shared library, but some functions are only in
          the static library, so try that secondarily.  */
    OUTPUT_FORMAT(elf32-littlearm)
    GROUP ( ../../../lib/arm-linux-gnueabihf/libpthread.so.0 ../../../usr/lib/arm-linux-gnueabihf/libpthread_nonshared.a )
EOF
        if [[ ! $? -eq 0 ]]; then
            echo libpthread patch failed.
            exit 1
        fi

        cat > usr/lib/arm-linux-gnueabihf/libc.so << EOF
    /* GNU ld script
       Use the shared library, but some functions are only in
          the static library, so try that secondarily.  */
    OUTPUT_FORMAT(elf32-littlearm)
    GROUP ( ../../../lib/arm-linux-gnueabihf/libc.so.6 ../../../usr/lib/arm-linux-gnueabihf/libc_nonshared.a  AS_NEEDED ( ../../../lib/arm-linux-gnueabihf/ld-linux-armhf.so.3 ) )
EOF
        if [[ ! $? -eq 0 ]]; then
            echo libc patch failed.
            exit 1
        fi


    # Install an alternative pkg-config
        mkdir -p bin || exit 1
        cp $SCRIPTDIR/pkg-config bin || exit 1
        chmod +x bin/pkg-config || exit 1

    # Patch package configuration files
        find usr -name "*.pc" | xargs sed -i -e "s:=/usr/:=\${prefix}/:"
        if [[ ! $? -eq 0 ]]; then
            echo pkg-config patch failed.
            exit 1
        fi

}

installLibs() {
    DESTINATION=armhf-01

    $SCRIPTDIR/get-deb.sh $DESTINATION \
        http://ftp.us.debian.org/debian/ stable main armhf \
            libatk1.0-dev \
            libatk1.0-0 \
            libc6 \
            libc-bin \
            libgcc1 \
            gcc-4.7-base \
            libglib2.0-0 \
            libffi5 \
            libpcre3 \
            libselinux1 \
            zlib1g \
            libatk1.0-data \
            gir1.2-atk-1.0 \
            gir1.2-glib-2.0 \
            libgirepository-1.0-1 \
            pkg-config \
            libpopt0 \
            libglib2.0-dev \
            libglib2.0-bin \
            libelf1 \
            libglib2.0-data \
            libpcre3-dev \
            libc6-dev \
            libc-dev-bin \
            linux-libc-dev \
            libpcrecpp0 \
            libstdc++6 \
            zlib1g-dev \
            libcairo2-dev \
            libcairo2 \
            libfontconfig1 \
            libexpat1 \
            libfreetype6 \
            fontconfig-config \
            libpixman-1-0 \
            libpng12-0 \
            libx11-6 \
            libxcb1 \
            libxau6 \
            libxdmcp6 \
            libx11-data \
            libxcb-render0 \
            libxcb-shm0 \
            libxrender1 \
            libcairo-gobject2 \
            libcairo-script-interpreter2 \
            libfontconfig1-dev \
            libexpat1-dev \
            libfreetype6-dev \
            libx11-dev \
            libxau-dev \
            x11proto-core-dev \
            xorg-sgml-doctools \
            libxdmcp-dev \
            x11proto-input-dev \
            x11proto-kb-dev \
            xtrans-dev \
            libxcb1-dev \
            libpthread-stubs0-dev \
            libpthread-stubs0 \
            libxrender-dev \
            x11proto-render-dev \
            libpng12-dev \
            libsm-dev \
            libsm6 \
            libice6 \
            libuuid1 \
            libpam0g \
            libsemanage1 \
            libsemanage-common \
            libbz2-1.0 \
            libsepol1 \
            libustr-1.0-1 \
            libice-dev \
            libpixman-1-dev \
            libxcb-render0-dev \
            libxcb-shm0-dev \
            libgdk-pixbuf2.0-dev \
            libgdk-pixbuf2.0-0 \
            libjasper1 \
            libjpeg8 \
            libtiff4 \
            libjbig0 \
            libgdk-pixbuf2.0-common \
            gir1.2-gdkpixbuf-2.0 \
            libglib2.0-0-refdbg \
            libgstreamer0.10-dev \
            libgstreamer0.10-0 \
            libxml2 \
            liblzma5 \
            libxml2-dev \
            gir1.2-gstreamer-0.10 \
            gir1.2-freedesktop \
            libgstreamer-plugins-base0.10-dev \
            libgstreamer-plugins-base0.10-0 \
            liborc-0.4-0 \
            gir1.2-gst-plugins-base-0.10 \
            libgtk2.0-dev \
            libgtk2.0-0 \
            libgtk2.0-common \
            libcomerr2 \
            libcups2 \
            libavahi-client3 \
            libavahi-common3 \
            libavahi-common-data \
            libdbus-1-3 \
            libgnutls26 \
            libgcrypt11 \
            libgpg-error0 \
            libp11-kit0 \
            libtasn1-3 \
            libgssapi-krb5-2 \
            libk5crypto3 \
            libkeyutils1 \
            libkrb5support0 \
            libkrb5-3 \
            libpango1.0-0 \
            libthai0 \
            libthai-data \
            libdatrie1 \
            libxft2 \
            fontconfig \
            libxcomposite1 \
            libxcursor1 \
            libxfixes3 \
            libxdamage1 \
            libxext6 \
            libxi6 \
            libxinerama1 \
            libxrandr2 \
            shared-mime-info \
            libpango1.0-dev \
            gir1.2-pango-1.0 \
            libxft-dev \
            libxext-dev \
            x11proto-xext-dev \
            libxinerama-dev \
            x11proto-xinerama-dev \
            libxi-dev \
            libxrandr-dev \
            x11proto-randr-dev \
            libxcursor-dev \
            libxfixes-dev \
            x11proto-fixes-dev \
            libxcomposite-dev \
            x11proto-composite-dev \
            libxdamage-dev \
            x11proto-damage-dev \
            libxml2-utils \
            libreadline6 \
            readline-common \
            libtinfo5 \
            libgtk2-gst \
            libgst7 \
            libgmp10 \
            libltdl7 \
            libsigsegv2 \
            libstdc++6-4.4-dev \
            gcc-4.4-base \
            g++-4.4 \
            gcc-4.4 \
            cpp-4.4 \
            libmpfr4 \
            libgomp1 \
            libstdc++6-4.6-dev \
            gcc-4.6-base \
            g++-4.6 \
            gcc-4.6 \
            cpp-4.6 \
            libmpc2 \
            libstdc++6-4.7-dev \
            g++-4.7 \
            gcc-4.7 \
            cpp-4.7 \
            libxtst-dev \
            libxtst6 \
            x11proto-record-dev \
            libdirectfb-dev \
            libdirectfb-1.2-9 \
            libudev-dev \
            libudev0

    # get some rapberry Pi specials
    $SCRIPTDIR/get-deb.sh $DESTINATION \
        http://archive.raspbian.org/raspbian wheezy firmware armhf \
        libraspberrypi-dev
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

PILIBS=$CROSSLIBS/armhf-01

checkReinstall $PILIBS
#if [[ ! -d $PILIBS ]]; then
    installLibs
#fi

CROSSCOMPILER=$CROSSLIBS/gcc-linaro-arm-linux-gnueabihf-raspbian-2012.09-20120921_linux
checkReinstall $CROSSCOMPILER
if [[ ! -d $CROSSCOMPILER ]]; then
    installCrossCompiler
fi

echo Done.

