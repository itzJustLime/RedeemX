package me.justlime.redeemX.commands

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.justlime.redeemX.RedeemX
import me.justlime.redeemX.data.models.RedeemCode
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.util.*

class RCXCommand(private val plugin: RedeemX) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage("Usage: /rxc <gen|delete|modify|info>")
            return true
        }
        if (sender.hasPermission("redeemx.admin")) {
            when (args[0].lowercase()) {
                "gen" -> handleGenerate(sender, args)
                "modify" -> handleModify(sender, args)
                "delete" -> handleDelete(sender, args)
                "delete_all" -> handleDeleteAll(sender, args)
                "info" -> handleInfo(sender)
                else -> sender.sendMessage("Unknown subcommand. Use 'gen', 'delete','delete_all', 'modify', or 'info'.")
            }
            return true
        }
        sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
        return true
    }


    private fun handleGenerate(sender: CommandSender, args: Array<out String>) {
        if (args.size > 1) {
            var code = args[1]
//            val commands = args.slice(3 until args.size)
            val maxAttempts = plugin.config.getInt("max_attempts")
            // Check if codeNameOrSize is a number
            if (code.toIntOrNull() != null) {

                generateUniqueCode(code.toInt(), maxAttempts) { uniqueCode ->
                    if (uniqueCode != null) {
                        createRedeemCode(sender, uniqueCode)
                    } else {
                        // Handle the failure to generate a unique code
                        sender.sendMessage("Unable to generate a unique code of length ${code.toInt()}. Please try a different length or name. (Total $maxAttempts attempts)")
                    }
                }

            } else {
                code = code.uppercase()
                createRedeemCode(sender, code)
            }
        } else {
            sender.sendMessage("Usage: /rxc gen <code> <commands/template> <commands/template_name>")
        }
    }

    private fun createRedeemCode(sender: CommandSender, codeName: String) {
        // Check if code already exists
        sender.sendMessage("Hello $codeName!")
        if (plugin.redeemCodeDao.getByCode(codeName) != null) {
            sender.sendMessage("The code '$codeName' already exists. Please choose a unique code.")
            return
        }

        // Create the redeem code with default or example values for the other fields
        val redeemCode = RedeemCode(
            code = codeName,
            max_player = 1,
            isEnabled = true,
            duration = null,
            permission = null,
            pin = -1,
            target = null,
        )

        try {
            // Attempt to insert the code into the database
            val success = plugin.redeemCodeDao.insert(redeemCode)
            if (success) {
                sender.sendMessage("Code generated successfully: $codeName")
            } else {
                sender.sendMessage("Failed to generate the code.")
            }
        } catch (e: Exception) {
            sender.sendMessage("An error occurred while generating the code.")
            e.printStackTrace() // Log the error to the console for debugging
        }
    }

    private fun generateUniqueCode(length: Int, maxAttempts: Int = 1024, callback: (String?) -> Unit) {
        val charset = ('A'..'Z') + ('0'..'9')

        // Launch a coroutine
        CoroutineScope(Dispatchers.IO).launch {
            var code: String
            var attempts = 0

            do {
                code = (1..length)
                    .map { charset.random() }
                    .joinToString("")
                attempts++
                // Check if max attempts is reached
                if (attempts >= maxAttempts) {
                    withContext(Dispatchers.Main) { callback(null) } // Notify failure on the main thread
                    return@launch // Exit the coroutine
                }
            } while (plugin.redeemCodeDao.getByCode(code) != null) // Ensure code is unique in DB

            // Return the unique code on the main thread
            withContext(Dispatchers.Main) { callback(code) }
        }
    }


    private fun handleDelete(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage("Usage: /rxc delete <code>")
            return
        }

        val codeToDelete = args[1]

        // Attempt to find the redeem code by the provided code
        val redeemCode = plugin.redeemCodeDao.getByCode(codeToDelete)

        if (redeemCode == null) {
            sender.sendMessage("The code '$codeToDelete' does not exist.")
            return
        }

        // Attempt to delete the redeem code from the database
        val success = plugin.redeemCodeDao.deleteByCode(codeToDelete)
        if (success) {
            sender.sendMessage("Successfully deleted the code: ${redeemCode.code}")
        } else {
            sender.sendMessage("Failed to delete the code: ${redeemCode.code}")
        }
    }

    private fun handleDeleteAll(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2 || args[1] != "CONFIRM") {
            sender.sendMessage("${ChatColor.YELLOW}Usage: /rxc delete_all CONFIRM")
            return
        }

        // Attempt to delete all redeem codes
        val success = plugin.redeemCodeDao.deleteAll()
        if (success) {
            sender.sendMessage("${ChatColor.GREEN}Successfully deleted all codes from the database.")
        } else {
            sender.sendMessage("${ChatColor.RED}Failed to delete all codes from the database.")
        }
    }


    private fun handleModify(sender: CommandSender, args: Array<out String>) {
        if (args.size <= 4) {
            sender.sendMessage("Usage: /rxc modify <code> <property> <value>")
            return
        }

        val codeIdToModify = args[1]
        val property = args[2].lowercase()
        val value = args[3]

        // Attempt to find the redeem code by the provided code
        val redeemCode = plugin.redeemCodeDao.getByCode(codeIdToModify)
        if (redeemCode == null) {
            sender.sendMessage("The code '$codeIdToModify' does not exist.")
            return
        }

        when (property) {
            "max_redeems" -> {
                redeemCode.max_redeems =
                    value.toIntOrNull() ?: return sender.sendMessage("Invalid value for max_redeems.")
                sender.sendMessage("Updated max_redeems for code '${redeemCode.code}' to ${redeemCode.max_redeems}.")
            }

            "max_per_player" -> {
                redeemCode.max_player =
                    value.toIntOrNull() ?: return sender.sendMessage("Invalid value for max_per_player.")
                sender.sendMessage("Updated max_per_player for code '$codeIdToModify' to ${redeemCode.max_player}.")
            }

            "enabled" -> {
                redeemCode.isEnabled = value.lowercase() == "true"
                sender.sendMessage("Updated enabled status for code '$codeIdToModify' to ${redeemCode.isEnabled}.")
            }

            "command" -> {
                if (args.size <= 5) {
                    sender.sendMessage("Usage for commands: /rxc modify <code> command <add|remove> <command_text>")
                    return
                }

                val method = args[3].lowercase()
                val commandValue = args.drop(4).joinToString(" ") // Joins all words from index 4 onwards
                sender.sendMessage("entered $commandValue")

                when (method) {
                    "add" -> {
                        if (plugin.redeemCodeDao.addCommand(codeIdToModify, commandValue)) {
                            sender.sendMessage("Command added to code '$codeIdToModify': $commandValue")
                        } else {
                            sender.sendMessage("Failed to add command to code '$codeIdToModify'.")
                        }
                    }
//                    "remove" -> {
//                        if (plugin.redeemCodeDao.removeCommand(codeIdToModify, commandValue)) {
//                            sender.sendMessage("Command removed from code '$codeIdToModify': $commandValue")
//                        } else {
//                            sender.sendMessage("Failed to remove command from code '$codeIdToModify'.")
//                        }
//                    }
                    else -> sender.sendMessage("Unknown method '$method' for commands. Use 'add' or 'remove'.")
                }
            }

            else -> {
                sender.sendMessage("Unknown property '$property'. Available properties: max_redeems, max_per_player, enabled, command.")
                return
            }
        }

        // Attempt to update the redeem code in the database
        val success = plugin.redeemCodeDao.update(redeemCode)
        if (success) {
            sender.sendMessage("Successfully modified the code: $codeIdToModify")
        } else {
            sender.sendMessage("Failed to modify the code: $codeIdToModify")
        }
    }


    private fun handleInfo(sender: CommandSender) {
        sender.sendMessage("RedeemX Plugin Version: ${plugin.description.version}")
    }

}
