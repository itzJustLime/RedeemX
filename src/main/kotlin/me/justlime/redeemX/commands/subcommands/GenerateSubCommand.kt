package me.justlime.redeemX.commands.subcommands

import me.justlime.redeemX.RedeemX
import me.justlime.redeemX.config.ConfigManager
import me.justlime.redeemX.data.service.RedeemCodeService
import me.justlime.redeemX.state.RedeemCodeState
import me.justlime.redeemX.state.StateManager
import org.bukkit.command.CommandSender

class GenerateSubCommand(private val plugin: RedeemX) {
    private val config = ConfigManager(plugin)
    private val stateManager = plugin.stateManager
    private val service = RedeemCodeService(plugin)

    fun execute(sender: CommandSender, args: Array<out String>) {
        // Retrieve or create a state for the sender
        val state = stateManager.createState(sender)

        // Validate arguments
        if (args.size <= 1) {
            config.sendMessage("commands.gen.invalid-syntax", state)
            return
        }

        state.inputCode = args[1].uppercase()
        val maxAttempts = plugin.config.getInt("max-attempts")

        if (state.inputCode.toIntOrNull() == null) {
            createRedeemCode(state, stateManager)
            return
        }

        generateUniqueCode(state.inputCode.toInt(), maxAttempts) { uniqueCode ->
            if (uniqueCode == null) {
                config.sendMessage("commands.gen.length-error", state)
                return@generateUniqueCode
            }
            state.inputCode = uniqueCode
            createRedeemCode(state, stateManager)
        }
    }

    private fun createRedeemCode(state: RedeemCodeState, stateManager: StateManager) {
        val commands = config.getString("default.commands")?.removePrefix("[")?.removeSuffix("]")

        val duration = service.adjustDuration("0s", config.getString("default.code-expired-duration").toString(), true)
        val permissionRequired = config.getString("default.permission.required").equals("true", ignoreCase = true)

        // Check if code already exists
        if (stateManager.fetchState(state.sender, state.inputCode)) {
            config.sendMessage("commands.gen.code-already-exist", state)
            return
        }

        state.apply {
            this.commands = service.parseToMapId(commands)
            this.duration = "${duration}s"
            this.storedTime = if (duration > 1) service.currentTime else null
            this.isEnabled = config.getString("default.enabled") == "true"
            this.maxRedeems = config.getString("default.max_redeems")?.toIntOrNull() ?: 1
            this.maxPlayers = config.getString("default.max_players")?.toIntOrNull() ?: 1
            this.permission = if (permissionRequired) config.getString("default.permission.value")
                ?.replace("{code}", state.inputCode) else null
            this.pin = config.getString("default.pin")?.toIntOrNull() ?: -1
        }

        try {
            // Insert the redeem code into the database
            state.code = state.inputCode
//            state.sender.sendMessage(state.toString())

            val success = stateManager.updateDb(state.sender)
            if (!success) {
                config.sendMessage("commands.gen.failed", state)
                return
            }
            config.sendMessage("commands.gen.success", state)
        } catch (e: Exception) {
            config.sendMessage("commands.gen.error", state)
            e.printStackTrace()
        }
    }

    private fun generateUniqueCode(length: Int, maxAttempts: Int = 1024, callback: (String?) -> Unit) {
        val charset = ('A'..'Z') + ('0'..'9')
        var attempts = 0
        do {
            val code = (1..length).map { charset.random() }.joinToString("")
            attempts++

            // Stop if max attempts are exceeded
            if (attempts >= maxAttempts) {
                callback(null)
                return
            }

            // Ensure the generated code is unique
            if (plugin.redeemCodeDB.get(code) == null) {
                callback(code)
                return
            }
        } while (true)
    }
}
