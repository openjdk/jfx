list(APPEND PAL_SOURCES
    text/KillRing.cpp
    system/ClockGeneric.cpp
    system/java/SoundJava.cpp
    java/LoggingJava.cpp
)

list(APPEND PAL_INCLUDE_DIRECTORIES
    "${ICU_INCLUDE_DIRS}"
)

list(APPEND PAL_SOURCES
    crypto/java/CryptoDigestJava.cpp
)

add_definitions(-DSTATICALLY_LINKED_WITH_JavaScriptCore)
add_definitions(-DSTATICALLY_LINKED_WITH_WTF)
