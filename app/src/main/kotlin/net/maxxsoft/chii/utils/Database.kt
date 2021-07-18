package net.maxxsoft.chii.utils

import java.io.File
import java.sql.*

/** Local database. */
class Database {
  // DB file name.
  private val DB_FILENAME = "local.db"

  // DB connection.
  private val conn: Connection

  init {
    // check if DB exists
    val initRequired = !File(DB_FILENAME).let { it.exists() && !it.isDirectory }
    // create DB connection
    Class.forName("org.sqlite.JDBC")
    conn = DriverManager.getConnection("jdbc:sqlite:$DB_FILENAME")
    conn.autoCommit = false
    // initialize database
    if (initRequired) initDatabase()
  }

  /** Free all DB related resources and close database. */
  fun close() {
    conn.commit()
    conn.close()
  }

  /** Use the current database object for only once. */
  inline fun <T> use(block: (Database) -> T): T {
    val ret = block(this)
    close()
    return ret
  }

  /**
   * Execute SQL statements (update).
   *
   * @param sql SQL statements.
   */
  fun executeUpdate(sql: String) {
    val stmt = conn.createStatement()
    stmt.executeUpdate(sql)
    stmt.close()
  }

  /**
   * Execute SQL statements (query).
   *
   * @param sql SQL statements.
   */
  fun <T> executeQuery(sql: String, block: (ResultSet) -> T): T {
    val stmt = conn.createStatement()
    val ret = block(stmt.executeQuery(sql))
    stmt.close()
    return ret
  }

  // initialize database
  private fun initDatabase() {
    executeUpdate(
        """
    CREATE TABLE IF NOT EXISTS noporn_records (
      id                  INTEGER PRIMARY KEY NOT NULL,
      group_id            INTEGER NOT NULL,
      last_check_in_time  INTEGER NOT NULL,
      lasting_days        INTEGER NOT NULL
    );"""
    )
  }
}
