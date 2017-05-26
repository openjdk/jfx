set(Sqlite_SOURCES
    sqlite3.c
)

add_library(SqliteJava STATIC ${Sqlite_SOURCES})

WEBKIT_SET_EXTRA_COMPILER_FLAGS(SqliteJava ${ADDITIONAL_COMPILER_FLAGS})
