#!/bin/bash

# build prerequisites for the Xcode project

# This script assumes that the gradle based build system is set up and working properly
# otherwise this will fail miserably
JFX_RT_DIR=$(cd ${JFX_RT_DIR:-../../../../../..}; pwd)

# Use the base rt dir for all builds
cd "${JFX_RT_DIR}"

GRADLE_BIN=$(command -v gradle)

test -z "${GRADLE_BIN}" && {
    # Make a half-hearted attempt at finding gradle
    # We don't want the full users environment loaded in this script, so use a sub-shell
    # to load .profile and see if we have better luck
    GRADLE_BIN=$(source ~/.profile; command -v gradle)
}

test -z "${GRADLE_BIN}" && {
    # if still not then skip the rest with a nasty warning to the user
    echo "WARNING: gradle not found, can't build prerequisites"
    exit 0
}

# determine which build type to use
if test "${CONFIGURATION}" = "Debug"; then
    CONF_TYPE=DebugNative
else
    # Debug CONF uses "Release" for natives
    CONF_TYPE=Debug
fi

# use gradle to build our prerequisites, up to fxplugins
"${GRADLE_BIN}" :media:generateHeaders :media:buildMacPlugins :media:generateMediaErrorHeader -PCONF=${CONF_TYPE}
