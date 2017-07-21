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

# FIXME: Make including these files consistent in the source so these forwarding headers are not needed.
if (NOT EXISTS ${DERIVED_SOURCES_JAVASCRIPTCORE_DIR}/InspectorBackendDispatchers.h)
    file(WRITE ${DERIVED_SOURCES_JAVASCRIPTCORE_DIR}/InspectorBackendDispatchers.h "#include \"inspector/InspectorBackendDispatchers.h\"")
endif ()
if (NOT EXISTS ${DERIVED_SOURCES_JAVASCRIPTCORE_DIR}/InspectorFrontendDispatchers.h)
    file(WRITE ${DERIVED_SOURCES_JAVASCRIPTCORE_DIR}/InspectorFrontendDispatchers.h "#include \"inspector/InspectorFrontendDispatchers.h\"")
endif ()
if (NOT EXISTS ${DERIVED_SOURCES_JAVASCRIPTCORE_DIR}/InspectorProtocolObjects.h)
    file(WRITE ${DERIVED_SOURCES_JAVASCRIPTCORE_DIR}/InspectorProtocolObjects.h "#include \"inspector/InspectorProtocolObjects.h\"")
endif ()

list(APPEND JavaScriptCore_LIBRARIES
    ${JAVA_JVM_LIBRARY}
)

list(APPEND JavaScriptCore_SYSTEM_INCLUDE_DIRECTORIES
	${JDK_INCLUDE_DIRS}
)

add_dependencies(WTF icudatagen)
