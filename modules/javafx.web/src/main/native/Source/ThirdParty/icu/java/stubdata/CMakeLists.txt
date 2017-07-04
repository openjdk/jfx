set(ICU_STUBDATA_SOURCES
	../../source/stubdata/stubdata.c
	)

set(ICU_STUBDATA_INCLUDE_DIRECTORIES
	  ../../source/common
      ../../source/common/include
)

include_directories(${ICU_STUBDATA_INCLUDE_DIRECTORIES})
set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY  ${CMAKE_BINARY_DIR}/lib/stubdata)
add_library(${ICU_JAVA_ICU_STUBDATA_LIB} STATIC ${ICU_STUBDATA_SOURCES})
set_target_properties(${ICU_JAVA_ICU_STUBDATA_LIB} PROPERTIES OUTPUT_NAME ${ICU_JAVA_ICU_STUBDATA_LIB_NAME})
