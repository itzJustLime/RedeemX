package me.justlime.redeemX.commands.subcommands

import me.justlime.redeemX.RedeemX
import me.justlime.redeemX.commands.JSubCommand
import me.justlime.redeemX.data.repository.ConfigRepository
import me.justlime.redeemX.data.repository.RedeemCodeRepository
import me.justlime.redeemX.enums.JConfig
import me.justlime.redeemX.enums.JMessage
import me.justlime.redeemX.enums.JPermission
import me.justlime.redeemX.enums.JTab
import me.justlime.redeemX.models.CodePlaceHolder
import me.justlime.redeemX.utilities.JService
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

//rcx preview code <code> [command|reward|message]
//rcx preview template <template> [command|reward|message]
class PreviewSubCommand(val plugin: RedeemX) : JSubCommand {
    override var jList: List<String> = emptyList()
    override val permission: String = JPermission.Admin.PREVIEW
    lateinit var placeHolder: CodePlaceHolder
    private val config = ConfigRepository(plugin)
    private val codeRepo = RedeemCodeRepository(plugin)

    override fun sendMessage(key: String): Boolean {
        placeHolder.sentMessage = config.getMessage(key, placeHolder)
        config.sendMsg(key, placeHolder)
        return true
    }

    override fun execute(sender: CommandSender, args: MutableList<String>): Boolean {
        if (sender !is Player) return !sendMessage(JMessage.Command.RESTRICTED_TO_PLAYERS)
        placeHolder = CodePlaceHolder(sender, args)
        if (!hasPermission(sender)) return sendMessage(JMessage.Command.NO_PERMISSION)
        if (args.size < 3) return !sendMessage(JMessage.Command.UNKNOWN_COMMAND)
        val type = args[1]
        val code = args[2]
        val console = plugin.server.consoleSender
        when (type) {
            JTab.Type.CODE -> {
                val redeemCode = plugin.redeemCodeDB.get(code) ?: return !sendMessage(JMessage.Code.NOT_FOUND)
                placeHolder = CodePlaceHolder.applyByRedeemCode(redeemCode, sender)
                if (args.size > 3) {
                    when (args[3]) {
                        "command" -> placeHolder.sender.sendMessage(placeHolder.command)
                        "reward" -> placeHolder.sender.sendMessage(redeemCode.rewards.toString())
                        "message" -> placeHolder.sender.sendMessage(redeemCode.messages)
                        else -> return !sendMessage(JMessage.Command.UNKNOWN_COMMAND)
                    }
                } else {
                    redeemCode.commands.forEach {
                        console.server.dispatchCommand(console, JService.applyColors(JService.applyPlaceholders(it, placeHolder) {
                            plugin.server.pluginManager.isPluginEnabled("PlaceholderAPI")
                        }))
                    }
                    if (redeemCode.rewards.size > getEmptySlotSize(sender) && config.getConfigValue(JConfig.Rewards.DROP) == "false") {
                        config.sendMsg(JMessage.Redeem.FULL_INVENTORY, placeHolder)
                        return true
                    } else redeemCode.rewards.forEach { item ->
                        val remaining = sender.inventory.addItem(item)
                        if (remaining.isEmpty()) return@forEach
                        if (config.getConfigValue(JConfig.Rewards.SOUND) == "true") sender.playSound(
                            sender.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f
                        )

                        // If there are remaining items (inventory was full), drop them
                        remaining.values.forEach { droppedItem ->
                            sender.world.dropItem(sender.location, droppedItem)
                        }
                    }
                    config.sendTemplateMsg(redeemCode.template, placeHolder)
                    sendMessage(JMessage.Code.Preview.PREVIEW)
                    return true
                }
            }

            JTab.Type.TEMPLATE -> {
                val template = config.getTemplate(code) ?: return !sendMessage(JMessage.Template.NOT_FOUND)
                placeHolder = CodePlaceHolder.applyByTemplate(template, sender)
                if (args.size > 3) {
                    when (args[3]) {
                        "command" -> {}
                        "reward" -> {}
                        "message" -> {}
                        else -> return !sendMessage(JMessage.Command.UNKNOWN_COMMAND)
                    }
                } else {
                    template.commands.forEach {
                        console.server.dispatchCommand(console, JService.applyColors(JService.applyPlaceholders(it, placeHolder) {
                            plugin.server.pluginManager.isPluginEnabled("PlaceholderAPI")
                        }))
                    }

                    config.sendTemplateMsg(template.name, placeHolder)

                    sendMessage(JMessage.Code.Preview.PREVIEW)
                    return true
                }
            }

            else -> return !sendMessage(JMessage.Command.UNKNOWN_COMMAND)
        }

        return true
    }

    override fun tabCompleter(sender: CommandSender, args: MutableList<String>): MutableList<String> {
        if (!hasPermission(sender)) return mutableListOf()
        val cachedCodes = codeRepo.getCachedCode()
        val cachedTemplate = config.getAllTemplates().map { it.name }
        return when (args.size) {
            2 -> mutableListOf("code", "template")
            3 -> {
                val list = mutableListOf<String>()
                if (args[1] == "code") list.addAll(cachedCodes)
                if (args[1] == "template") list.addAll(cachedTemplate)
                list
            }

            4 -> {
                val list = mutableListOf<String>()
                if (args[1] == "code") list.addAll(listOf("command", "reward", "message"))
                if (args[1] == "template") list.addAll(listOf("command", "reward", "message"))
                list
            }

            else -> mutableListOf()
        }
    }

    private fun getEmptySlotSize(sender: Player): Int {
        return sender.inventory.filter { it == null }.size
    }
}

