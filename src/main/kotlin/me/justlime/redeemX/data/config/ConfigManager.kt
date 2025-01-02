package me.justlime.redeemX.data.config

import me.justlime.redeemX.RedeemX
import me.justlime.redeemX.enums.JFiles
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.logging.Level

class ConfigManager(val plugin: RedeemX) {

    init {
        plugin.saveDefaultConfig()
        getConfig(JFiles.MESSAGES)
        getConfig(JFiles.CONFIG)
        getConfig(JFiles.TEMPLATE)
    }

    private fun getFile(configFile: JFiles): File {
        return if (configFile == JFiles.MESSAGES) {
            val lang = getConfig(JFiles.CONFIG).getString("lang", "en") ?: "en"
            val filename = configFile.filename.replace("{lang}", lang)
            File(plugin.dataFolder, filename)
        } else {
            File(plugin.dataFolder, configFile.filename)
        }
    }

    fun getConfig(configFile: JFiles): FileConfiguration {
        if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdir()
        val file = getFile(configFile)

        if (!file.exists()) {
            plugin.logger.log(Level.WARNING, "File not found: ${file.name}. Falling back to default or generating new.")
            if (configFile == JFiles.MESSAGES) {
                // Fallback to default language (en)
                val defaultFile = File(plugin.dataFolder, configFile.filename.replace("{lang}", "en"))
                if (!defaultFile.exists()) {
                    plugin.saveResource("messages_en.yml", false)
                }
                return YamlConfiguration.loadConfiguration(defaultFile)
            } else {
                plugin.saveResource(configFile.filename, false)
            }
        }
        return YamlConfiguration.loadConfiguration(file)
    }

    fun saveConfig(configFile: JFiles) {
        try {
            val file = getFile(configFile)
            getConfig(configFile).save(file)
            plugin.logger.log(Level.INFO, "${file.name} saved successfully.")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Could not save ${configFile.filename}: ${e.message}")
        }
    }
}
