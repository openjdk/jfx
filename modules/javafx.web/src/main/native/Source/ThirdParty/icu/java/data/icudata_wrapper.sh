#!/bin/sh

# $1 : Cmake build directory 
# $2 : Release / Debug
# $3 : icu Data library location
# $4 : CFLAGS used by cmake
# $5 : CPPFLAGS used by cmake

JAVA_DATA_LIBRARY=$3

case "$(uname -s)" in
   Darwin)
     JAVA_LIBDIR=$1
     PLATFORM=MacOSX
     ;;

   Linux)
     JAVA_LIBDIR=$1
     PLATFORM=Linux/gcc
     export CFLAGS=$4
     export CPPFLAGS=$5
     ;;

   CYGWIN*)
     JAVA_LIBDIR=$1
     PLATFORM=Cygwin/MSVC
     export CFLAGS=$4
     export CPPFLAGS=$5
     ;;
   *)
     echo 'Building icudata failed due to unknown build environment'
     return 3
     ;;
esac

function check_library {
    if [[ ! -f $JAVA_DATA_LIBRARY ]]; then
        echo "data is not built, building now"
        build_data
    fi
}

ICU_CONFIGURE_PATH="../../../../src/main/native/Source/ThirdParty/icu/source/configure"
ICU_RUNCONFIGURE_PATH="../../../../src/main/native/Source/ThirdParty/icu/source/runConfigureICU"
ICU_STATIC_ARGS="--enable-static --enable-shared=no --enable-extras=no --enable-tests=no --enable-samples=no --disable-dyload --with-data-packaging=static"

function build_data {
    echo "Building icudata for $PLATFORM"
    mkdir -p $JAVA_LIBDIR/icu/data
    unzip -o ../../source/data/in/icudt51l.zip -d $JAVA_LIBDIR/icu/data/
    # create icu autoconf build folder
    cd $JAVA_LIBDIR
    mkdir -p icu/lib && cd icu
    # run configure
    bash ${ICU_RUNCONFIGURE_PATH} $PLATFORM --libdir=$PWD/../lib ${ICU_STATIC_ARGS}
    # build stubdata
    cd stubdata && make
    # build data (static library) 
    cd ../data && make && make install
}

function main() {
    check_library
}

main "$@"
