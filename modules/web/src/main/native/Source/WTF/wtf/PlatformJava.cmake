message(STATUS "==== #### WTF_LIBRARY_TYPE ${WTF_LIBRARY_TYPE}")
set(WTF_LIBRARY_TYPE STATIC)

# set(WTF_OUTPUT_NAME WTFJava)

if (APPLE)
    list(APPEND WTF_INCLUDE_DIRECTORIES
        "${WTF_DIR}/icu"
    )

    list(APPEND WTF_SOURCES
        text/cf/StringImplCF.cpp
        text/cf/StringCF.cpp
    )
endif()

list(APPEND WTF_INCLUDE_DIRECTORIES
    ${ICU_INCLUDE_DIRS}
    ${WTF_DIR}/wtf/java
    ${CMAKE_SOURCE_DIR}/Source #//XXX move WebCore/platform/java/JavaEnv.h in Source/WTF/wtf/CurrentTime.cpp
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
)

list(APPEND WTF_SOURCES
    java/RunLoopJava.cpp
    java/StringJava.cpp
    java/MainThreadJava.cpp
    # FIXME(arunprasad): Move JavaEnv.{cpp, h} to wtf
    ../../WebCore/platform/java/JavaEnv.cpp
)

list(APPEND WTF_LIBRARIES
    ${ICU_LIBRARIES}
    ${ICU_I18N_LIBRARIES}
    # ${ZLIB_LIBRARIES}
    ${JAVA_JVM_LIBRARY}
)

if (WIN32)
  list(APPEND WTF_LIBRARIES
      winmm
  )
endif ()

if (DEFINED CMAKE_USE_PTHREADS_INIT)
    list(APPEND WTF_LIBRARIES pthread)
endif()

message(STATUS "==== #### WTF_SYSTEM_INCLUDE_DIRECTORIES ${WTF_SYSTEM_INCLUDE_DIRECTORIES}")
message(STATUS "==== #### WTF_SOURCES  ${WTF_SOURCES}")
message(STATUS "==== #### WTF_INCLUDE_DIRECTORIES  ${WTF_INCLUDE_DIRECTORIES}")
message(STATUS "==== #### DERIVED_SOURCES_DIR  ${DERIVED_SOURCES_DIR}")
message(STATUS "#### ####= WTF_LIBRARIES ${WTF_LIBRARIES}")
# target_link_libraries(WTF WebCore)
list(APPEND WTF_SYSTEM_INCLUDE_DIRECTORIES
	${JDK_INCLUDE_DIRS} #//XXX
#     ${GLIB_INCLUDE_DIRS}
)

