set(JavaScriptCore_LIBRARY_TYPE STATIC)
add_definitions(-DSTATICALLY_LINKED_WITH_WTF)

list(APPEND JavaScriptCore_LIBRARIES
	${ICU_LIBRARIES}
)
list(APPEND JavaScriptCore_LUT_FILES
	runtime/ArrayPrototype.cpp
	runtime/MathObject.cpp
	runtime/NamePrototype.cpp
	runtime/RegExpObject.cpp
)

list(APPEND JavaScriptCore_INCLUDE_DIRECTORIES
    ${ICU_INCLUDE_DIRS}
    ${CMAKE_BINARY_DIR}/../../gensrc/headers/javafx.web
    ${WTF_DIR}
    ${CMAKE_SOURCE_DIR}/Source/WebCore/platform
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
)

if (APPLE)
    find_library(COREFOUNDATION_LIBRARY CoreFoundation)
    list(APPEND JavaScriptCore_LIBRARIES
        ${COREFOUNDATION_LIBRARY}
    )
endif()

if (WIN32)
    file(MAKE_DIRECTORY ${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore)

    set(JavaScriptCore_PRE_BUILD_COMMAND "${CMAKE_BINARY_DIR}/DerivedSources/JavaScriptCore/preBuild.cmd")
    file(REMOVE "${JavaScriptCore_PRE_BUILD_COMMAND}")
    foreach (_directory ${JavaScriptCore_FORWARDING_HEADERS_DIRECTORIES})
        file(APPEND "${JavaScriptCore_PRE_BUILD_COMMAND}" "@xcopy /y /d /f \"${JAVASCRIPTCORE_DIR}/${_directory}/*.h\" \"${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore\" >nul 2>nul\n")
    endforeach ()

    set(JavaScriptCore_POST_BUILD_COMMAND "${CMAKE_BINARY_DIR}/DerivedSources/JavaScriptCore/postBuild.cmd")
    file(WRITE "${JavaScriptCore_POST_BUILD_COMMAND}" "@xcopy /y /d /f \"${DERIVED_SOURCES_DIR}/JavaScriptCore/*.h\" \"${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore\" >nul 2>nul\n")
    file(APPEND "${JavaScriptCore_POST_BUILD_COMMAND}" "@xcopy /y /d /f \"${DERIVED_SOURCES_DIR}/JavaScriptCore/inspector/*.h\" \"${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore\" >nul 2>nul\n")
endif()

list(APPEND JavaScriptCore_LIBRARIES
    ${JAVA_JVM_LIBRARY}
)

list(APPEND JavaScriptCore_SYSTEM_INCLUDE_DIRECTORIES
	${JDK_INCLUDE_DIRS}
)

add_dependencies(WTF icudatagen)
