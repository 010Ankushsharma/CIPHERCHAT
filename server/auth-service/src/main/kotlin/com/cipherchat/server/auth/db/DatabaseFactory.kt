package com.cipherchat.server.auth.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("DatabaseFactory")

object DatabaseFactory {

    fun init() {
        val jdbcUrl = System.getenv("DATABASE_URL")
            ?: "jdbc:postgresql://localhost:5432/cipherchat_auth"
        val dbUser = System.getenv("DATABASE_USER") ?: "cipherchat"
        val dbPassword = System.getenv("DATABASE_PASSWORD")
            ?: error("DATABASE_PASSWORD env var must be set")

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = dbUser
            this.password = dbPassword
            driverClassName = "org.postgresql.Driver"
            // Pool sizing: formula is (number of CPU cores * 2) + effective_spindle_count.
            // For a typical 4-core auth service container, 10 is a safe starting point.
            // Tune based on observed connection wait times in Grafana — never just set
            // maximumPoolSize to a large number "to be safe"; too many connections
            // hurt PostgreSQL more than too few hurt the application.
            maximumPoolSize = System.getenv("DB_POOL_SIZE")?.toIntOrNull() ?: 10
            minimumIdle = 2
            idleTimeout = 600_000L   // 10 minutes
            connectionTimeout = 30_000L
            // Validate connections before handing them out — prevents "stale
            // connection" errors after PostgreSQL restarts or network hiccups.
            connectionTestQuery = "SELECT 1"
            poolName = "CipherChatAuthPool"
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        log.info("Database connected: $jdbcUrl")

        // Create tables if they don't exist. In production this should
        // be replaced with a proper migration tool (Flyway or Liquibase)
        // so schema changes are versioned, auditable, and reversible.
        // SchemaUtils.create is acceptable for development and initial
        // deploy; mark it for replacement before first production release.
        transaction {
            SchemaUtils.create(
                Users,
                OAuthAccounts,
                Devices,
                AuthSessions,
                IdentityKeys,
                OneTimePrekeys,
                OtpCodes,
            )
        }
        log.info("Schema verified/created")
    }
}

/** Suspending transaction wrapper — runs the block on the IO dispatcher. */
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
