package me.justlime.redeemX.bot

import me.justlime.redeemX.RedeemX
import me.justlime.redeemX.commands.subcommands.GenerateSubCommand
import me.justlime.redeemX.config.ConfigManager
import me.justlime.redeemX.config.Files
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bukkit.Bukkit

class DiscordCommandListener(val plugin: RedeemX) : ListenerAdapter() {
    private val generateSubCommand = GenerateSubCommand(plugin)
    private val sender = Bukkit.getConsoleSender()
    private val db = plugin.redeemCodeDB
    val config: ConfigManager = ConfigManager(plugin)
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == "generate") return handleGenerateCommand(event)
        if (event.name == "delete") {
            val code = event.getOption("code").toString()
            val cachedCodes = db.getFetchCodes
            if (code.isEmpty()|| code.isBlank()||cachedCodes.contains(code)) return


            Bukkit.getScheduler().runTask(plugin, Runnable {
                try {
                    val delMessage = config.getString("commands.delete.success", Files.MESSAGES)?.replace("{code}", code) ?: ""
                    event.reply("Deleting code...").setEphemeral(false).queue()
                    val success = db.deleteByCode(code)

                    if (success) config.getString("commands.delete.success", Files.MESSAGES)?.let {
                        event.reply(it).setEphemeral(false).queue(){ _ ->
                            db.deleteByCode(code)
                            event.hook.editOriginal(delMessage).queue()
                        }
                    }
                    else config.getString("commands.delete.failed", Files.MESSAGES)?.let { event.reply(it) }

                }catch (_: Exception) {

                }


            })

        }

    }

    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        val cachedCodes = plugin.redeemCodeDB
        val templateCodes: List<String> = config.getTemplateNames()


        if (event.name == "generate" && event.focusedOption.name == "template") {
            event.replyChoiceStrings(templateCodes.filter {
                it.startsWith(event.focusedOption.value)
            }).queue()
        }
    }



    private fun handleGenerateCommand(event: SlashCommandInteractionEvent) {
        val template = event.getOption("template")
        val length = event.getOption("length")?.asInt ?: config.getString("code-minimum-digit")?.toIntOrNull() ?: 5
        val amount = event.getOption("amount")?.asInt ?: 1

        // Arguments to pass to GenerateSubCommand
        var args = arrayOf("generate", length.toString(), amount.toString())
        if (template != null) args = arrayOf("generate", "template", template.asString)

        // Schedule task to execute on Bukkit's main thread
        Bukkit.getScheduler().runTask(plugin, Runnable {
            try {

                event.reply("Generating codes...").setEphemeral(false).queue { _ ->
                    generateSubCommand.execute(sender, args)

                    val generatedCodes = generateSubCommand.generatedSubCommand
                    if (generatedCodes.isEmpty()) {
                        event.hook.editOriginal("No codes were generated. Please check your input.").queue()
                        return@queue
                    }

                    val response = buildString {
                        append("Successfully generated ${generatedCodes.size} code(s):\n")
                        generatedCodes.forEach { append("`$it`\n") }
                    }

                    event.hook.editOriginal(response).queue()
                    generateSubCommand.generatedSubCommand.clear()
                }
            } catch (e: Exception) {
                event.reply("Failed to generate codes due to an error: ${e.message}").setEphemeral(true).queue()
                e.printStackTrace()
            }
        })
    }

}
