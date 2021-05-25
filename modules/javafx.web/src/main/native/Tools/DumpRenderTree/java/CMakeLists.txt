set(DumpRenderTree_DIR "${TOOLS_DIR}/DumpRenderTree")
set(DumpRenderTree_BINDINGS_DIR "${TOOLS_DIR}/DumpRenderTree/Bindings")
set(WebKitTestRunner_SHARED_DIR "${TOOLS_DIR}/TestRunnerShared/")
set(WebKitTestRunner_UISCRIPTCONTEXT_DIR "${TOOLS_DIR}/TestRunnerShared/UIScriptContext")

file(MAKE_DIRECTORY ${DumpRenderTree_DERIVED_SOURCES_DIR})

set(DumpRenderTree_SOURCES
    ${WebKitTestRunner_SHARED_DIR}/Bindings/JSWrapper.cpp

    ${WebKitTestRunner_UISCRIPTCONTEXT_DIR}/UIScriptContext.cpp
    ${WebKitTestRunner_UISCRIPTCONTEXT_DIR}/UIScriptControllerShared.cpp

    ../CyclicRedundancyCheck.cpp
    ../DumpRenderTreeCommon.cpp
    ../GCController.cpp
    ../TestRunner.cpp
    ../UIScriptController.cpp
    ../WorkQueue.cpp
    DumpRenderTree.cpp
    EventSender.cpp
    TestRunnerJava.cpp
    GCControllerJava.cpp
    JavaEnv.cpp
    WorkQueueItemJava.cpp
)

set(DumpRenderTree_LIBRARIES
    WebKitLegacy
    ${JAVA_JVM_LIBRARY}
    ${ICU_I18N_LIBRARIES}
    ${ICU_LIBRARIES}
)

set(DumpRenderTree_PRIVATE_INCLUDE_DIRECTORIES
    ${DumpRenderTree_DERIVED_SOURCES_DIR}
    ${DumpRenderTree_DIR}
    ${WebKitTestRunner_SHARED_DIR}/Bindings
    ${WebKitTestRunner_UISCRIPTCONTEXT_DIR}
    ${CMAKE_BINARY_DIR}
    ${CMAKE_SOURCE_DIR}/Source
    ${WEBCORE_DIR}/testing/js
    ${FORWARDING_HEADERS_DIR}
    ${FORWARDING_HEADERS_DIR}/WebCore
)

list(APPEND DumpRenderTree_PRIVATE_INCLUDE_DIRECTORIES
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
    ${PAL_FRAMEWORK_HEADERS_DIR}
    ${WebKitLegacy_FRAMEWORK_HEADERS_DIR}
)

set(DumpRenderTree_IDL_FILES
    "${WebKitTestRunner_UISCRIPTCONTEXT_DIR}/Bindings/UIScriptController.idl"
)

GENERATE_BINDINGS(DumpRenderTreeBindings
    OUTPUT_SOURCE DumpRenderTree_SOURCES
    INPUT_FILES ${DumpRenderTree_IDL_FILES}
    BASE_DIR ${DumpRenderTree_BINDINGS_DIR}
    IDL_INCLUDES ../Bindings
    FEATURES ${FEATURE_DEFINES_WITH_SPACE_SEPARATOR}
    DESTINATION ${DumpRenderTree_DERIVED_SOURCES_DIR}
    GENERATOR DumpRenderTree)

WEBKIT_INCLUDE_CONFIG_FILES_IF_EXISTS()

include_directories(${DumpRenderTree_PRIVATE_INCLUDE_DIRECTORIES})

add_library(DumpRenderTreeJava SHARED ${DumpRenderTree_SOURCES})
add_definitions(-DWEBCORE_EXPORT=WTF_IMPORT_DECLARATION -DWEBCORE_TESTSUPPORT_EXPORT=WTF_IMPORT_DECLARATION)
add_dependencies(DumpRenderTreeJava DumpRenderTreeBindings)

set_target_properties(DumpRenderTreeJava PROPERTIES OUTPUT_NAME "DumpRenderTreeJava")
target_link_libraries(DumpRenderTreeJava ${DumpRenderTree_LIBRARIES})
