#!/bin/bash
#
# given a pointer to the directory containing the modules...
# create a working run.args file that has the full paths
# in place
#

# TODO KCR: update this for standalone vs bundled sdk

TOP="$1"

do_cygpath() 
{
    if [ "$platform" = "windows" ]
    then
        cygpath -m "$1"
    else
        echo "$1"
    fi
}

MODULES="javafx.base javafx.graphics javafx.swing javafx.controls javafx.fxml javafx.media javafx.web"
XPATCHFILE=run.args

if [ ! -d "$TOP" ]
then
    if [ ! -z "$JAVAFX_HOME" ]
    then
        echo "Assuming you meant ${JAVAFX_HOME}"
        TOP="${JAVAFX_HOME}"
    else
        echo "Error: please provide the path to the directory"
        exit 1
    fi
fi

# add some easy affordances :-)
if [ "X$TOP" = 'X.' ]
then
    TOP="$PWD"
fi

if [ -d "$TOP/build/modular-sdk/modules" ]
then
    echo "adding build/modular-sdk/modules"
    TOP=`do_cygpath "$TOP/build/modular-sdk"`
elif [ -d "$TOP/modular-sdk/modules" ]
then
    echo "adding modular-sdk/modules"
    TOP=`do_cygpath "$TOP/modular-sdk"`
fi
MODTOP="$TOP/modules"
LIBTOP="$TOP/modules_libs"

if [ ! -d "$MODTOP" -o ! -d "$LIBTOP" ]
then
    echo "Error, did not find one of:"
    echo "    module top  $MODTOP"
    echo "    library top $LIBTOP"
    exit -1
fi

echo "#generated from $0" > "${XPATCHFILE}"

JAVA_LIBRARY_PATH=''

PATHSEP=':'
case "`uname`" in
    Darwin*) platform="macosx";;
    CYGWIN*) platform="windows" ; PATHSEP=";";;
    Windows_NT*) platform="mks" ; PATHSEP=";";;
    Linux*)  platform="linux";;
    SunOS*)  platform="solaris";;
esac

NL=$'\n'

for mod in $MODULES
do
    if [ ! -d "$MODTOP/${mod}" ]
    then
        echo "Warning: ${mod} package is missing from $MODTOP/${mod}"
    fi
    mp=`do_cygpath "$MODTOP/${mod}"`
    echo "--patch-module=\"${mod}=$mp\""
    echo "--patch-module=\"${mod}=$mp\"" >> "${XPATCHFILE}"

    # note: javafx.base exists, but currently does not have any shared libs in it.
    # add it anyway
    lp=`do_cygpath "$LIBTOP/${mod}"`
    if [ -d "${lp}" ]
    then
        if [ -z "${JAVA_LIBRARY_PATH}" ]
        then
            JAVA_LIBRARY_PATH="-Djava.library.path=\"\\${NL}  ${lp}"
        else
            JAVA_LIBRARY_PATH="${JAVA_LIBRARY_PATH}\\${NL}  ${PATHSEP}${lp}"
        fi
    fi
done

if [ ! -z "${JAVA_LIBRARY_PATH}" ]
then
    echo "${JAVA_LIBRARY_PATH}\"" 
    echo "${JAVA_LIBRARY_PATH}\"" >> "${XPATCHFILE}"
fi

echo "#"
echo "#Your run.args file is ${XPATCHFILE}"

