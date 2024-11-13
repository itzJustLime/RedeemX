package me.justlime.redeemX.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.justlime.redeemX.RedeemX
import me.justlime.redeemX.data.dao.RedeemCodeDaoImpl
import java.io.File
import java.sql.Connection
import java.sql.SQLException

class DatabaseManager(private val plugin: RedeemX) {

    private val databaseFile = File(plugin.dataFolder, "redeemx.db")
    private val hikariDataSource: HikariDataSource

    init {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite:${databaseFile.absolutePath}"
            maximumPoolSize = 5  // Adjust the pool size as necessary for your server load
            isAutoCommit = true
            connectionTestQuery = "SELECT 1"
        }

        hikariDataSource = HikariDataSource(config)

        // Initialize tables or any required setup here
        getRedeemCodeDao().createTable()
    }

    companion object {
        private var instance: DatabaseManager? = null

        fun getInstance(plugin: RedeemX): DatabaseManager {
            return instance ?: synchronized(this) {
                instance ?: DatabaseManager(plugin).also { instance = it }
            }
        }
    }

    fun getConnection(): Connection? {
        return try {
            hikariDataSource.connection
        } catch (e: SQLException) {
            plugin.logger.info("Failed to get a connection from the pool: ${e.message}")
            null
        }
    }

    fun closePool() {
        hikariDataSource.close()
    }

    fun getRedeemCodeDao(): RedeemCodeDaoImpl {
        return RedeemCodeDaoImpl(this)
    }
}
