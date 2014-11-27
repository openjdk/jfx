# Xcode has no way of running a tool to set up build settings, nor any way to
# externally supply build settings, so we'll create a symlink to JDK_HOME
# and the build settings will use that symlink to find JNI headers

GRADLE_PROPS="${JFX_MEDIA_DIR}"/../../gradle.properties
JAVA_HOME_SYMLINK="${SYMROOT}/jdk_home"

NEW_JDK_HOME=
test -f "${GRADLE_PROPS}" && \
    NEW_JDK_HOME=$(grep '^JDK_HOME' "${GRADLE_PROPS}" | sed -E 's/^JDK_HOME[[:space:]]*=[[:space:]]*(.*)$/\1/')

# Fall back on calling /usr/libexec/java_home to find a suitable JDK
test -z "${NEW_JDK_HOME}" && \
    NEW_JDK_HOME=$(/usr/libexec/java_home -v 1.8)

LINK_TARGET=$(readlink "${JAVA_HOME_SYMLINK}")

# keep our symlink in sync
test -L "${JAVA_HOME_SYMLINK}" -a "${LINK_TARGET}" != "${NEW_JDK_HOME}" && \
    rm -f "${JAVA_HOME_SYMLINK}"

test -L "${JAVA_HOME_SYMLINK}" || {
    test -z "${NEW_JDK_HOME}" && {
        # we can't find a suitable JDK, bail
        echo "Unable to find Java 8 JDK, please install one or set JDK_HOME in gradle.properties"
        exit 1
    }
    echo "Setting new JDK_HOME=${NEW_JDK_HOME}"
    ln -sf "${NEW_JDK_HOME}" "${JAVA_HOME_SYMLINK}"
}
