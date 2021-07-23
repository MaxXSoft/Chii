package net.maxxsoft.chii.utils

import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

/** Local database. */
class Database {
  // DB file name.
  private val dbFileName = "local.db"

  // DB connection.
  private val conn: Connection

  init {
    // check if DB exists
    val initRequired = !File(dbFileName).let { it.exists() && !it.isDirectory }
    // create DB connection
    Class.forName("org.sqlite.JDBC")
    conn = DriverManager.getConnection("jdbc:sqlite:$dbFileName")
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
          id                  INTEGER NOT NULL,
          group_id            INTEGER NOT NULL,
          last_checkin_time   INTEGER NOT NULL,
          lasting_days        INTEGER NOT NULL,
          PRIMARY KEY (id, group_id)
        );"""
    )
  }
}
