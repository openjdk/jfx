set(Sqlite_SOURCES
    sqlite3.c
)

add_library(SqliteJava STATIC ${Sqlite_SOURCES})

