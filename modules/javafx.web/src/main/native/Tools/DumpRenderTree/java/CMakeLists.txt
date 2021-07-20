set(DumpRenderTree_DIR "${TOOLS_DIR}/DumpRenderTree")
set(DumpRenderTree_BINDINGS_DIR "${TOOLS_DIR}/DumpRenderTree/Bindings")
set(WebKitTestRunner_SHARED_DIR "${TOOLS_DIR}/TestRunnerShared/")
set(WebKitTestRunner_UISCRIPTCONTEXT_DIR "${TOOLS_DIR}/TestRunnerShared/UIScriptContext")

file(MAKE_DIRECTORY ${DumpRenderTree_DERIVED_SOURCES_DIR})

set(DumpRenderTree_SOURCES
    ../CyclicRedundancyCheck.cpp
    ../GCController.cpp
    ../TestRunner.cpp
    ../WorkQueue.cpp
    DumpRenderTree.cpp
    EventSender.cpp
    GCControllerJava.cpp
    JavaEnv.cpp
    TestRunnerJava.cpp
    UIScriptControllerJava.cpp
    WorkQueueItemJava.cpp
)

set(DumpRenderTree_LIBRARIES
    WebKitLegacy
    TestRunnerShared
    ${JAVA_JVM_LIBRARY}
    ${ICU_I18N_LIBRARIES}
    ${ICU_LIBRARIES}
)

set(DumpRenderTree_PRIVATE_INCLUDE_DIRECTORIES
    ${CMAKE_BINARY_DIR}
    ${DumpRenderTree_DERIVED_SOURCES_DIR}
    ${DumpRenderTree_DIR}
    ${WebKitTestRunner_SHARED_DIR}
    ${WebKitTestRunner_SHARED_DIR}/Bindings
    ${WebKitTestRunner_UISCRIPTCONTEXT_DIR}
)

list(APPEND DumpRenderTree_PRIVATE_INCLUDE_DIRECTORIES
    ${PAL_FRAMEWORK_HEADERS_DIR}
    ${WebCore_PRIVATE_FRAMEWORK_HEADERS_DIR}
    ${WebKitLegacy_FRAMEWORK_HEADERS_DIR}
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
    ${CMAKE_SOURCE_DIR}/Source
    ${WEBCORE_DIR}/testing/js
)

set(DumpRenderTree_WEB_PREFERENCES_TEMPLATES
    ${DumpRenderTree_DIR}/Scripts/PreferencesTemplates/TestOptionsGeneratedWebKitLegacyKeyMapping.cpp.erb
    ${DumpRenderTree_DIR}/Scripts/PreferencesTemplates/TestOptionsGeneratedKeys.h.erb
)

set(DumpRenderTree_WEB_PREFERENCES
    ${WTF_SCRIPTS_DIR}/Preferences/WebPreferences.yaml
    ${WTF_SCRIPTS_DIR}/Preferences/WebPreferencesDebug.yaml
    ${WTF_SCRIPTS_DIR}/Preferences/WebPreferencesExperimental.yaml
    ${WTF_SCRIPTS_DIR}/Preferences/WebPreferencesInternal.yaml
)

set_source_files_properties(${DumpRenderTree_WEB_PREFERENCES} PROPERTIES GENERATED TRUE)

add_custom_command(
    OUTPUT ${DumpRenderTree_DERIVED_SOURCES_DIR}/TestOptionsGeneratedWebKitLegacyKeyMapping.cpp ${DumpRenderTree_DERIVED_SOURCES_DIR}/TestOptionsGeneratedKeys.h
    DEPENDS ${DumpRenderTree_WEB_PREFERENCES_TEMPLATES} ${DumpRenderTree_WEB_PREFERENCES} WTF_CopyPreferences
    COMMAND ${RUBY_EXECUTABLE} ${WTF_SCRIPTS_DIR}/GeneratePreferences.rb --frontend WebKitLegacy --base ${WTF_SCRIPTS_DIR}/Preferences/WebPreferences.yaml --debug ${WTF_SCRIPTS_DIR}/Preferences/WebPreferencesDebug.yaml --experimental ${WTF_SCRIPTS_DIR}/Preferences/WebPreferencesExperimental.yaml --internal ${WTF_SCRIPTS_DIR}/Preferences/WebPreferencesInternal.yaml --outputDir "${DumpRenderTree_DERIVED_SOURCES_DIR}" --template ${DumpRenderTree_DIR}/Scripts/PreferencesTemplates/TestOptionsGeneratedWebKitLegacyKeyMapping.cpp.erb --template ${DumpRenderTree_DIR}/Scripts/PreferencesTemplates/TestOptionsGeneratedKeys.h.erb
    VERBATIM)

list(APPEND DumpRenderTree_SOURCES
    ${DumpRenderTree_DERIVED_SOURCES_DIR}/TestOptionsGeneratedWebKitLegacyKeyMapping.cpp
    ${DumpRenderTree_DERIVED_SOURCES_DIR}/TestOptionsGeneratedKeys.h
)

WEBKIT_INCLUDE_CONFIG_FILES_IF_EXISTS()

include_directories(${DumpRenderTree_PRIVATE_INCLUDE_DIRECTORIES})

add_library(DumpRenderTreeJava SHARED ${DumpRenderTree_SOURCES})
add_definitions(-DWEBCORE_EXPORT=WTF_IMPORT_DECLARATION -DWEBCORE_TESTSUPPORT_EXPORT=WTF_IMPORT_DECLARATION)
add_dependencies(DumpRenderTreeJava TestRunnerSharedBindings)

set_target_properties(DumpRenderTreeJava PROPERTIES OUTPUT_NAME "DumpRenderTreeJava")
target_link_libraries(DumpRenderTreeJava ${DumpRenderTree_LIBRARIES})
