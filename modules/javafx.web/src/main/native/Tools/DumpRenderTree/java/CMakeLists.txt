set(WEBKIT_TESTRUNNER_SHARED_DIR "${TOOLS_DIR}/TestRunnerShared/")
set(WEBKIT_TESTRUNNER_UISCRIPTCONTEXT_DIR "${TOOLS_DIR}/TestRunnerShared/UIScriptContext")
set(DUMP_RENDER_TREE_BINDINGS_DIR "${TOOLS_DIR}/DumpRenderTree/Bindings")

file(MAKE_DIRECTORY ${DERIVED_SOURCES_DIR}/DumpRenderTree)

set(DumpRenderTree_SOURCES
    ../CyclicRedundancyCheck.cpp
    ../DumpRenderTreeCommon.cpp
    ../GCController.cpp
    ../TestRunner.cpp
    ../WorkQueue.cpp
    DumpRenderTree.cpp
    EventSender.cpp
    TestRunnerJava.cpp
    GCControllerJava.cpp
    JavaEnv.cpp
    WorkQueueItemJava.cpp
    ${WEBKIT_TESTRUNNER_UISCRIPTCONTEXT_DIR}/UIScriptContext.cpp
    ${WEBKIT_TESTRUNNER_UISCRIPTCONTEXT_DIR}/UIScriptController.cpp
    ${WEBKIT_TESTRUNNER_SHARED_DIR}/Bindings/JSWrapper.cpp
)

set(DumpRenderTree_LIBRARIES
    WebKitLegacy
    ${JAVA_JVM_LIBRARY}
    ${ICU_I18N_LIBRARIES}
    ${ICU_LIBRARIES}
)

set(DumpRenderTree_INCLUDE_DIRECTORIES
    ${DERIVED_SOURCES_DIR}/DumpRenderTree
    ${WEBKIT_TESTRUNNER_UISCRIPTCONTEXT_DIR}
    ${WEBKIT_TESTRUNNER_SHARED_DIR}/Bindings
    ${TOOLS_DIR}/DumpRenderTree
    ${CMAKE_BINARY_DIR}
    ${CMAKE_SOURCE_DIR}/Source
    ${WEBCORE_DIR}/testing/js
    ${FORWARDING_HEADERS_DIR}
    ${FORWARDING_HEADERS_DIR}/WebCore
    ${PAL_DIR}
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
    ${BMALLOC_DIR}
    ${WTF_DIR}/icu
)

if (APPLE)
  list(APPEND DumpRenderTree_INCLUDE_DIRECTORIES
        ${WTF_DIR}/icu
    )
endif ()

set(DumpRenderTree_IDL_FILES
    "${WEBKIT_TESTRUNNER_UISCRIPTCONTEXT_DIR}/Bindings/UIScriptController.idl"
)

GENERATE_BINDINGS(DumpRenderTreeBindings
    OUTPUT_SOURCE DumpRenderTree_SOURCES
    INPUT_FILES ${DumpRenderTree_IDL_FILES}
    BASE_DIR ${DUMP_RENDER_TREE_BINDINGS_DIR}
    IDL_INCLUDES ../Bindings
    FEATURES ${FEATURE_DEFINES_WITH_SPACE_SEPARATOR}
    DESTINATION ${DERIVED_SOURCES_DIR}/DumpRenderTree
    GENERATOR DumpRenderTree)

WEBKIT_INCLUDE_CONFIG_FILES_IF_EXISTS()

include_directories(${DumpRenderTree_INCLUDE_DIRECTORIES})

add_library(DumpRenderTreeJava SHARED ${DumpRenderTree_SOURCES})
add_definitions(-DWEBCORE_EXPORT=WTF_IMPORT -DWEBCORE_TESTSUPPORT_EXPORT=WTF_IMPORT)
add_dependencies(DumpRenderTreeJava DumpRenderTreeBindings)

if (UNIX AND NOT APPLE)
  set_target_properties(DumpRenderTreeJava PROPERTIES LINK_FLAGS "-Wl,--no-undefined")
endif ()

set_target_properties(DumpRenderTreeJava PROPERTIES OUTPUT_NAME "DumpRenderTreeJava")
target_link_libraries(DumpRenderTreeJava ${DumpRenderTree_LIBRARIES})
