set(Sqlite_SOURCES
    sqlite3.c
)

set(Sqlite_DEFINES
    "SQLITE_THREADSAFE=1"

    # The flags below are recommended in the SQLite documentation, and disable
    # features we don't use.
    "SQLITE_LIKE_DOESNT_MATCH_BLOBS"
    "SQLITE_OMIT_DEPRECATED"
    "SQLITE_OMIT_PROGRESS_CALLBACK"
    "SQLITE_OMIT_SHARED_CACHE"
    "SQLITE_USE_ALLOCA"

    # We don't use sqlite3_{enable_}load_extension().
    "SQLITE_OMIT_LOAD_EXTENSION"
    # We don't use TCL variables.
    "SQLITE_OMIT_TCL_VARIABLE"

    # We don't use sqlite3_{profile,trace}().
    "SQLITE_OMIT_TRACE"
)

if (WIN32)
    set(Sqlite_COMPILE_FLAGS
        # 'zTrace' unreferenced variable in src/vdbe.c
        "/wd4101"
    )
elseif (APPLE)
    set(Sqlite_COMPILE_FLAGS
        "-Wno-incompatible-pointer-types-discards-qualifiers"

        # lots of u8* to structure casts
        "-Wno-cast-align"
    )
else ()
    set(Sqlite_COMPILE_FLAGS
        "-Wno-implicit-fallthrough"
        "-Wno-discarded-qualifiers"

        # some if cases has empty body
        "-Wno-empty-body"
    )
    list(APPEND Sqlite_DEFINES
      # Linux provides fdatasync(), a faster equivalent of fsync().
      "fdatasync=fdatasync"
    )
endif ()

if (UNIX)
    list(APPEND Sqlite_DEFINES
        "HAVE_USLEEP=1"
        "USE_PREAD=1"
    )

    list(APPEND Sqlite_COMPILE_FLAGS
        # some defs are used directly without checking whether it is defined
        # or not.
        "-Wno-undef"

        # some parameter variables are unused
        "-Wno-unused-parameter"
    )
endif ()

add_library(SqliteJava STATIC ${Sqlite_SOURCES})
target_compile_definitions(SqliteJava PRIVATE ${Sqlite_DEFINES})
target_compile_options(SqliteJava PRIVATE ${Sqlite_COMPILE_FLAGS})

