package me.justlime.redeemX.commands.subcommands

import me.justlime.redeemX.RedeemX
import me.justlime.redeemX.data.repository.ConfigRepository
import me.justlime.redeemX.data.repository.RedeemCodeRepository
import me.justlime.redeemX.enums.JMessage
import me.justlime.redeemX.enums.JPermission
import me.justlime.redeemX.enums.JSubCommand
import me.justlime.redeemX.enums.JTab
import me.justlime.redeemX.enums.JTemplate
import me.justlime.redeemX.gui.EditMessages
import me.justlime.redeemX.gui.EditRewards
import me.justlime.redeemX.gui.EditSound
import me.justlime.redeemX.models.CodePlaceHolder
import me.justlime.redeemX.models.RedeemCode
import me.justlime.redeemX.models.RedeemTemplate
import me.justlime.redeemX.utilities.JService
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ModifySubCommand(private val plugin: RedeemX) : JSubCommand {
    private val config = ConfigRepository(plugin)
    private val codeRepo = RedeemCodeRepository(plugin)
    lateinit var placeHolder: CodePlaceHolder
    override var jList: List<String> = emptyList()
    override val permission: String = JPermission.Admin.MODIFY

    override fun execute(sender: CommandSender, args: MutableList<String>): Boolean {
        placeHolder = CodePlaceHolder(sender)
        if (!hasPermission(sender)) return sendMessage(JMessage.Command.NO_PERMISSION)
        if (args.size < 4) return !sendMessage(JMessage.Command.UNKNOWN_COMMAND)
        val type = args[1]
        return when (type) {
            JTab.Type.CODE -> codeModification(sender, args)
            JTab.Type.TEMPLATE -> templateModification(sender, args)
            else -> return !sendMessage(JMessage.Command.UNKNOWN_COMMAND)
        }
    }

    override fun tabCompleter(sender: CommandSender, args: MutableList<String>): MutableList<String>? {
        val cachedCodes = codeRepo.getCachedCode()
        val cachedTemplate = config.getAllTemplates().map { it.name }
        val cachedTargetList = codeRepo.getCachedTargetList()
        val modifyOptions = mutableListOf(
            JTab.Modify.ENABLED,
            JTab.Modify.SYNC,
            JTab.Modify.SET_REDEMPTION,
            JTab.Modify.SET_PLAYER_LIMIT,
            JTab.Modify.SET_COMMAND,
            JTab.Modify.ADD_COMMAND,
            JTab.Modify.REMOVE_COMMAND,
            JTab.Modify.LIST_COMMAND,
            JTab.Modify.SET_DURATION,
            JTab.Modify.ADD_DURATION,
            JTab.Modify.REMOVE_DURATION,
            JTab.Modify.SET_PERMISSION,
            JTab.Modify.SET_PIN,
            JTab.Modify.SET_TARGET,
            JTab.Modify.ADD_TARGET,
            JTab.Modify.REMOVE_TARGET,
            JTab.Modify.LIST_TARGET,
            JTab.Modify.SET_COOLDOWN,
            JTab.Modify.SET_TEMPLATE,
            JTab.Modify.EDIT
        )
        val templateOptions = mutableListOf(
            JTab.Modify.ENABLED,
            JTab.Modify.SYNC,
            JTab.Modify.SET_REDEMPTION,
            JTab.Modify.SET_PLAYER_LIMIT,
            JTab.Modify.SET_COMMAND,
            JTab.Modify.ADD_COMMAND,
            JTab.Modify.REMOVE_COMMAND,
            JTab.Modify.LIST_COMMAND,
            JTab.Modify.SET_DURATION,
            JTab.Modify.ADD_DURATION,
            JTab.Modify.REMOVE_DURATION,
            JTab.Modify.SET_PERMISSION,
            JTab.Modify.REQUIRED_PERMISSION,
            JTab.Modify.SET_PIN,
            JTab.Modify.SET_COOLDOWN,
            JTab.Modify.SET_TEMPLATE,
            JTab.Modify.EDIT
        )
        val completions = mutableListOf<String>()

        if (!hasPermission(sender)) return mutableListOf()
        when (args.size) {
            2 -> {
                completions.addAll(mutableListOf(JTab.Type.CODE, JTab.Type.TEMPLATE))
            }

            3 -> {
                if (args[1] == JTab.Type.CODE) completions.addAll(cachedCodes)
                if (args[1] == JTab.Type.TEMPLATE) completions.addAll(cachedTemplate)
            }

            4 -> {
                if (args[1] == JTab.Type.CODE) completions.addAll(modifyOptions)
                if (args[1] == JTab.Type.TEMPLATE) completions.addAll(templateOptions)
            }

            5 -> {
                if (args[1] == JTab.Type.CODE) when (args[3]) {
                    JTab.Modify.SET_TARGET, JTab.Modify.ADD_TARGET -> return null
                    JTab.Modify.REMOVE_TARGET -> completions.addAll(cachedTargetList[args[2]] ?: emptyList())
                    //TODO Add Cached ID
                    JTab.Modify.SET_COMMAND -> completions.add("ID")
                    JTab.Modify.REMOVE_COMMAND -> completions.add("ID")
                    JTab.Modify.EDIT -> {
                        completions.add(JTab.Modify.Edit.REWARD)
                        completions.add(JTab.Modify.Edit.MESSAGE)
                        completions.add(JTab.Modify.Edit.SOUND)
                    }

                    JTab.Modify.SET_TEMPLATE -> completions.addAll(cachedTemplate)
                    else -> return mutableListOf()
                }

            }

            else -> {
                if (args[1] == JTab.Type.CODE) {
                    when (args[3]) {
                        JTab.Modify.SET_TARGET, JTab.Modify.ADD_TARGET -> return null
                        JTab.Modify.REMOVE_TARGET -> completions.addAll((cachedTargetList[args[2]] ?: emptyList()).filter { it !in args })
                    }
                }
            }

        }
        return completions
    }

    private fun codeModification(sender: CommandSender, args: MutableList<String>): Boolean {
        val code = args[2].uppercase()
        val redeemCode = codeRepo.getCode(code) ?: return !sendMessage(JMessage.Code.NOT_FOUND)

        placeHolder = CodePlaceHolder.fetchByDB(plugin, code, sender).also { it.property = args[3] }
        jList = listOf(code)

        val options = mutableListOf(
            JTab.Modify.ENABLED,
            JTab.Modify.SYNC,
            JTab.Modify.SET_PERMISSION,
            JTab.Modify.LIST_TARGET,
            JTab.Modify.LIST_COMMAND,
        )
        val optionsWithValue = mutableListOf(
            JTab.Modify.SET_TEMPLATE,
            JTab.Modify.SET_DURATION,
            JTab.Modify.ADD_DURATION,
            JTab.Modify.REMOVE_DURATION,
            JTab.Modify.SET_COOLDOWN,
            JTab.Modify.SET_REDEMPTION,
            JTab.Modify.SET_PLAYER_LIMIT,
            JTab.Modify.SET_PIN,
            JTab.Modify.EDIT
        )
        val optionsWithValues = mutableListOf(
            JTab.Modify.SET_TARGET,
            JTab.Modify.ADD_TARGET,
            JTab.Modify.REMOVE_TARGET,
            JTab.Modify.SET_COMMAND,
            JTab.Modify.ADD_COMMAND,
            JTab.Modify.REMOVE_COMMAND,
        )

        if (placeHolder.property in optionsWithValue && args.size < 5) return sendMessage(JMessage.Code.Modify.INVALID_VALUE)
        if (placeHolder.property in optionsWithValues && args.size < 5) return sendMessage(JMessage.Code.Modify.INVALID_VALUE)

        when (placeHolder.property) {
            in options -> return modify(redeemCode, placeHolder.property)
            in optionsWithValue -> return modify(redeemCode, placeHolder.property, args[4])
            in optionsWithValues -> return modify(redeemCode, placeHolder.property, args.drop(4).toMutableList())
        }
        return sendMessage(JMessage.Command.UNKNOWN_COMMAND)
    }

    private fun templateModification(sender: CommandSender, args: MutableList<String>): Boolean {
        val template = args[2].uppercase()
        val redeemTemplate = config.getTemplate(template) ?: return sendMessage(JMessage.Template.NOT_FOUND)

        placeHolder = CodePlaceHolder.applyByTemplate(redeemTemplate, sender)
        jList = listOf(template)
        val options = mutableListOf(
            JTab.Modify.ENABLED,
            JTab.Modify.SYNC,
            JTab.Modify.SET_PERMISSION,
            JTab.Modify.LIST_TARGET,
            JTab.Modify.LIST_COMMAND,
        )
        val optionsWithValue = mutableListOf(
            JTab.Modify.SET_TEMPLATE,
            JTab.Modify.SET_DURATION,
            JTab.Modify.ADD_DURATION,
            JTab.Modify.REMOVE_DURATION,
            JTab.Modify.SET_COOLDOWN,
            JTab.Modify.SET_REDEMPTION,
            JTab.Modify.SET_PLAYER_LIMIT,
            JTab.Modify.SET_PIN,
            JTab.Modify.EDIT
        )
        val optionsWithValues = mutableListOf(
            JTab.Modify.SET_COMMAND,
            JTab.Modify.ADD_COMMAND,
            JTab.Modify.REMOVE_COMMAND,
        )

        if (args.size < 4) return !sendMessage(JMessage.Command.UNKNOWN_COMMAND)
        if (placeHolder.property in optionsWithValue && args.size < 5) return sendMessage(JMessage.Template.Modify.INVALID_VALUE)
        if (placeHolder.property in optionsWithValues && args.size < 5) return sendMessage(JMessage.Template.Modify.INVALID_VALUE)

        when (placeHolder.property) {
            in options -> return modify(redeemTemplate, placeHolder.property)
            in optionsWithValue -> return modify(redeemTemplate, placeHolder.property, args[4])
            in optionsWithValues -> return modify(redeemTemplate, placeHolder.property, args.drop(4).toMutableList())
        }
        return false
    }

    private fun modify(redeemCode: RedeemCode, property: String): Boolean {
        return when (property) {
            JTab.Modify.ENABLED -> toggleEnabledStatus(redeemCode)
            JTab.Modify.SYNC -> toggleTemplateSyncStatus(redeemCode)
            JTab.Modify.SET_PERMISSION -> setPermission(redeemCode)
            JTab.Modify.LIST_TARGET -> sendMessage(JMessage.Code.Usages.TARGET)
            JTab.Modify.LIST_COMMAND -> sendMessage(JMessage.Code.Usages.COMMAND)
            else -> false
        }
    }

    private fun modify(redeemTemplate: RedeemTemplate, property: String): Boolean {
        return when (property) {
            JTab.Modify.ENABLED -> toggleEnabledStatus(redeemTemplate)
            JTab.Modify.SET_PERMISSION -> setPermission(redeemTemplate)
            JTab.Modify.REQUIRED_PERMISSION -> setPermission(redeemTemplate)
            JTab.Modify.LIST_COMMAND -> {
                val commands = redeemTemplate.commands.withIndex().joinToString("\n")
                placeHolder.sender.sendMessage(commands)
                return true
            }

            else -> false
        }
    }

    private fun modify(redeemCode: RedeemCode, property: String, value: String): Boolean {
        return when (property) {
            JTab.Modify.SET_TEMPLATE -> setTemplate(redeemCode, value)
            JTab.Modify.SET_DURATION -> adjustDuration(redeemCode, "0s", value, true)
            JTab.Modify.ADD_DURATION -> adjustDuration(redeemCode, redeemCode.duration, value, true)
            JTab.Modify.REMOVE_DURATION -> adjustDuration(redeemCode, redeemCode.duration, value, false)
            JTab.Modify.SET_COOLDOWN -> setCooldown(redeemCode, value)
            JTab.Modify.SET_REDEMPTION -> setRedemption(redeemCode, value)
            JTab.Modify.SET_PLAYER_LIMIT -> setPlayerLimit(redeemCode, value)
            JTab.Modify.SET_PERMISSION -> setPermission(redeemCode, value)
            JTab.Modify.SET_PIN -> setPin(redeemCode, value)
            JTab.Modify.EDIT -> openGUI(redeemCode, value, placeHolder.sender)
            else -> false
        }
    }

    private fun modify(redeemTemplate: RedeemTemplate, property: String, value: String): Boolean {
        return when (property) {
            JTab.Modify.SET_DURATION -> adjustDuration(redeemTemplate, "0s", value, true)
            JTab.Modify.ADD_DURATION -> adjustDuration(redeemTemplate, redeemTemplate.duration, value, true)
            JTab.Modify.REMOVE_DURATION -> adjustDuration(redeemTemplate, redeemTemplate.duration, value, false)
            JTab.Modify.SET_COOLDOWN -> setCooldown(redeemTemplate, value)
            JTab.Modify.SET_REDEMPTION -> setRedemption(redeemTemplate, value)
            JTab.Modify.SET_PLAYER_LIMIT -> setPlayerLimit(redeemTemplate, value)
            JTab.Modify.SET_PERMISSION -> setPermission(redeemTemplate, value)
            JTab.Modify.SET_PIN -> setPin(redeemTemplate, value)
            JTab.Modify.EDIT -> openGUI(redeemTemplate, value, placeHolder.sender)
            else -> false

        }
    }

    private fun modify(redeemCode: RedeemCode, property: String, value: MutableList<String>): Boolean {
        return when (property) {
            JTab.Modify.SET_TARGET -> setTarget(redeemCode, value)
            JTab.Modify.ADD_TARGET -> addTarget(redeemCode, value)
            JTab.Modify.REMOVE_TARGET -> removeTarget(redeemCode, value)
            JTab.Modify.SET_COMMAND -> setCommand(
                redeemCode, value[0].toIntOrNull() ?: return !sendMessage(JMessage.Code.Modify.INVALID_ID), value.drop(1).joinToString(" ")
            )

            JTab.Modify.ADD_COMMAND -> addCommand(redeemCode, value.drop(0).joinToString(" "))
            JTab.Modify.REMOVE_COMMAND -> removeCommand(
                redeemCode, value[0].toIntOrNull() ?: return !sendMessage(JMessage.Code.Modify.INVALID_ID)
            )

            else -> false
        }
    }

    private fun modify(redeemTemplate: RedeemTemplate, property: String, value: MutableList<String>): Boolean {
        return when (property) {
            JTab.Modify.SET_COMMAND -> setCommand(
                redeemTemplate, value[0].toIntOrNull() ?: return !sendMessage(JMessage.Code.Modify.INVALID_ID), value.drop(1).joinToString(" ")
            )

            JTab.Modify.ADD_COMMAND -> addCommand(redeemTemplate, value.drop(0).joinToString(" "))
            JTab.Modify.REMOVE_COMMAND -> removeCommand(
                redeemTemplate, value[0].toIntOrNull() ?: return !sendMessage(JMessage.Code.Modify.INVALID_ID)
            )

            else -> false
        }
    }

    private fun sendMessage(key: String): Boolean {
        placeHolder.sentMessage = config.getMessage(key, placeHolder)
        config.sendMsg(key, placeHolder)
        return true
    }

    private fun upsertCode(redeemCode: RedeemCode): Boolean {
        redeemCode.modified = JService.getCurrentTime()
        val success = codeRepo.upsertCode(redeemCode)
        if (!success) {
            sendMessage(JMessage.Code.Modify.FAILED)
            return false
        }
        return true
    }

    private fun upsetCodes(template: RedeemTemplate): Boolean {
        val redeemCode: List<RedeemCode> = codeRepo.getCodesByTemplate(template.name, true)
        if (redeemCode.isEmpty()) return false
        val codes: MutableList<String> = mutableListOf()
        redeemCode.forEach { codes.add(it.code); if (!codeRepo.templateToRedeemCode(it, template)) return false }
        placeHolder.code = codes.joinToString(" ")

        codeRepo.upsertCodes(redeemCode)
        return true
    }

    private fun upsertTemplate(template: RedeemTemplate): Boolean {
        val success = config.modifyTemplate(template)
        if (upsetCodes(template)) config.sendMsg(JMessage.Template.Modify.CODES_MODIFIED, placeHolder)

        if (success) {
            config.sendMsg(JMessage.Template.Modify.SUCCESS, placeHolder)
            return true
        } else {
            config.sendMsg(JMessage.Template.Modify.FAILED, placeHolder)
            return false
        }
    }

    private fun openGUI(redeemCode: RedeemCode, value: String, sender: CommandSender): Boolean {
        if (sender !is Player) return sendMessage(JMessage.Command.RESTRICTED_TO_PLAYERS)
        when (value) {
            JTab.Modify.Edit.REWARD -> {
                if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncRewards == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
                EditRewards.code(plugin, redeemCode, sender)
            }

            JTab.Modify.Edit.MESSAGE -> {
                if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncMessages == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
                EditMessages.code(plugin, redeemCode)
            }

            JTab.Modify.Edit.SOUND -> {
                if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncSound == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
                EditSound.code(plugin, redeemCode)
            }
        }
        return upsertCode(redeemCode)
    }

    private fun openGUI(redeemTemplate: RedeemTemplate, value: String, sender: CommandSender): Boolean {
        if (sender !is Player) return sendMessage(JMessage.Command.RESTRICTED_TO_PLAYERS)
        when (value) {
            JTab.Modify.Edit.REWARD -> {
                if (redeemTemplate.syncRewards) return sendMessage(JMessage.Template.Modify.SYNC_LOCKED)
                EditRewards.template(plugin, redeemTemplate, sender)
            }

            JTab.Modify.Edit.MESSAGE -> {
                if (redeemTemplate.syncMessages) return sendMessage(JMessage.Template.Modify.SYNC_LOCKED)
                EditMessages.template(plugin, redeemTemplate)
            }

            JTab.Modify.Edit.SOUND -> {
                if (redeemTemplate.syncSound) return sendMessage(JMessage.Template.Modify.SYNC_LOCKED)
                EditSound.template(plugin, redeemTemplate)
            }
        }
        return upsertTemplate(redeemTemplate)
    }

    private fun setCommand(redeemCode: RedeemCode, id: Int, command: String): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncCommands == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        if (command.isEmpty()) return !sendMessage(JMessage.Code.Modify.INVALID_VALUE)
        if (id >= redeemCode.commands.size) return !sendMessage(JMessage.Code.Modify.INVALID_ID)
        redeemCode.commands[id] = command
        placeHolder.command = command
        placeHolder.commandId = id.toString()
        sendMessage(JMessage.Code.Modify.SET_COMMAND)
        return upsertCode(redeemCode)
    }

    private fun setCommand(redeemTemplate: RedeemTemplate, id: Int, command: String): Boolean {
        if (command.isBlank()) return config.sendMsg(JMessage.Template.Modify.INVALID_VALUE, placeHolder) != Unit
        if (id > redeemTemplate.commands.size) return config.sendMsg(JMessage.Template.Modify.INVALID_VALUE, placeHolder) != Unit
        redeemTemplate.commands[id] = command
        return upsertTemplate(redeemTemplate)
    }

    private fun addCommand(redeemCode: RedeemCode, command: String): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncCommands == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        placeHolder.command = command
        if (command.isBlank()) return !sendMessage(JMessage.Code.Modify.INVALID_VALUE)
        redeemCode.commands.add(command)
        placeHolder.commandId = (redeemCode.commands.size - 1).toString()
        sendMessage(JMessage.Code.Modify.ADD_COMMAND)
        return upsertCode(redeemCode)
    }

    private fun addCommand(redeemTemplate: RedeemTemplate, command: String): Boolean {
        if (command.isBlank()) return config.sendMsg(JMessage.Template.Modify.INVALID_VALUE, placeHolder) != Unit
        redeemTemplate.commands.add(command)
        return upsertTemplate(redeemTemplate)
    }

    private fun removeCommand(redeemCode: RedeemCode, id: Int): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncCommands == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        if (redeemCode.commands.isEmpty() || id >= redeemCode.commands.size || id < 0) return !sendMessage(JMessage.Code.Modify.INVALID_ID)
        placeHolder.commandId = id.toString()
        placeHolder.command = redeemCode.commands[id]
        redeemCode.commands.removeAt(id)
        sendMessage(JMessage.Code.Modify.REMOVE_COMMAND)
        return upsertCode(redeemCode)
    }

    private fun removeCommand(redeemTemplate: RedeemTemplate, id: Int): Boolean {
        if (id > redeemTemplate.commands.size) return config.sendMsg(JMessage.Template.Modify.INVALID_VALUE, placeHolder) != Unit
        redeemTemplate.commands.removeAt(id)
        return upsertTemplate(redeemTemplate)
    }

    private fun setPin(redeemCode: RedeemCode, value: String): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncPin == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        placeHolder.pin = value
        redeemCode.pin = value.toIntOrNull() ?: return !sendMessage(JMessage.Code.Modify.INVALID_VALUE)
        sendMessage(JMessage.Code.Modify.SET_PIN)
        return upsertCode(redeemCode)
    }

    private fun setPin(redeemTemplate: RedeemTemplate, value: String): Boolean {
        redeemTemplate.pin = value.toIntOrNull() ?: return !sendMessage(JMessage.Template.Modify.INVALID_VALUE)
        sendMessage(JMessage.Code.Modify.SET_PIN)
        return upsertTemplate(redeemTemplate)
    }

    private fun setRedemption(redeemCode: RedeemCode, value: String): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncRedemption == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        placeHolder.redemptionLimit = value
        redeemCode.redemption = value.toIntOrNull() ?: return !sendMessage(JMessage.Code.Modify.INVALID_VALUE)
        sendMessage(JMessage.Code.Modify.SET_REDEMPTION)
        return upsertCode(redeemCode)
    }

    private fun setRedemption(redeemTemplate: RedeemTemplate, value: String): Boolean {
        redeemTemplate.redemption = value.toIntOrNull() ?: return !sendMessage(JMessage.Template.Modify.INVALID_VALUE)
        sendMessage(JMessage.Code.Modify.SET_REDEMPTION)
        return upsertTemplate(redeemTemplate)
    }

    private fun setPlayerLimit(redeemCode: RedeemCode, value: String): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncPlayerLimit == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        placeHolder.playerLimit = value
        redeemCode.playerLimit = value.toIntOrNull() ?: return !sendMessage(JMessage.Code.Modify.INVALID_VALUE)
        sendMessage(JMessage.Code.Modify.SET_PLAYER_LIMIT)
        return upsertCode(redeemCode)
    }

    private fun setPlayerLimit(redeemTemplate: RedeemTemplate, value: String): Boolean {
        redeemTemplate.playerLimit = value.toIntOrNull() ?: return !sendMessage(JMessage.Template.Modify.INVALID_VALUE)
        sendMessage(JMessage.Code.Modify.SET_PLAYER_LIMIT)
        return upsertTemplate(redeemTemplate)
    }

    private fun toggleEnabledStatus(redeemCode: RedeemCode): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncEnabledStatus == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        redeemCode.enabledStatus = !redeemCode.enabledStatus
        placeHolder.status = redeemCode.enabledStatus.toString()
        sendMessage(JMessage.Code.Modify.ENABLED_STATUS)
        return upsertCode(redeemCode)
    }

    private fun toggleEnabledStatus(redeemTemplate: RedeemTemplate): Boolean {
        redeemTemplate.defaultEnabledStatus = !redeemTemplate.defaultEnabledStatus
        placeHolder.status = redeemTemplate.defaultEnabledStatus.toString()
        sendMessage(JMessage.Code.Modify.ENABLED_STATUS)
        return upsertTemplate(redeemTemplate)
    }

    private fun setPermission(redeemCode: RedeemCode, value: String = ""): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncPermission == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)

        //Set Custom Permission
        if (value.isNotBlank()) {
            redeemCode.permission = value.replace("{code}", redeemCode.code)
            placeHolder.permission = redeemCode.permission
            sendMessage(JMessage.Code.Modify.SET_PERMISSION)
            return upsertCode(redeemCode)
        }

        //Toggle Permission
        if (redeemCode.permission.isNotBlank()) {
            redeemCode.permission = ""
            placeHolder.permission = config.getMessage(JMessage.Code.Placeholder.DISABLED, placeHolder)
            sendMessage(JMessage.Code.Modify.SET_PERMISSION)
            return upsertCode(redeemCode)
        }

        val codePermission = config.getTemplateValue(redeemCode.template, JTemplate.PERMISSION_VALUE.property)
        redeemCode.permission = codePermission.replace("{code}", redeemCode.code.lowercase())
        placeHolder.permission = redeemCode.permission
        sendMessage(JMessage.Code.Modify.SET_PERMISSION)
        return upsertCode(redeemCode)
    }

    private fun setPermission(redeemTemplate: RedeemTemplate, value: String = ""): Boolean {
        //Set Custom Permission
        if (value.isNotBlank()) {
            redeemTemplate.permissionValue = value
            redeemTemplate.permissionRequired = true
            placeHolder.permission = redeemTemplate.permissionValue
            sendMessage(JMessage.Template.Modify.SET_PERMISSION)
            return upsertTemplate(redeemTemplate)
        }

        //Toggle Permission
        if (redeemTemplate.permissionRequired) {
            redeemTemplate.permissionRequired = false
            placeHolder.permission = config.getMessage(JMessage.Code.Placeholder.DISABLED, placeHolder)
            sendMessage(JMessage.Template.Modify.SET_PERMISSION)
            return upsertTemplate(redeemTemplate)
        }

        redeemTemplate.permissionRequired = true
        placeHolder.permission = redeemTemplate.permissionValue
        sendMessage(JMessage.Template.Modify.SET_PERMISSION)
        return upsertTemplate(redeemTemplate)
    }

    private fun adjustDuration(redeemCode: RedeemCode, existingDuration: String, duration: String, isAdding: Boolean): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncDuration == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        if (!JService.isDurationValid(duration)) return sendMessage(JMessage.Code.Modify.INVALID_VALUE)
        redeemCode.duration = JService.adjustDuration(existingDuration, duration, isAdding)
        placeHolder.duration = redeemCode.duration
        sendMessage(JMessage.Code.Modify.SET_DURATION)
        return upsertCode(redeemCode)
    }

    private fun adjustDuration(redeemTemplate: RedeemTemplate, existingDuration: String, duration: String, isAdding: Boolean): Boolean {
        if (!JService.isDurationValid(duration)) return sendMessage(JMessage.Template.Modify.INVALID_VALUE)
        redeemTemplate.duration = JService.adjustDuration(existingDuration, duration, isAdding)
        placeHolder.duration = redeemTemplate.duration
        sendMessage(JMessage.Template.Modify.SET_DURATION)
        return upsertTemplate(redeemTemplate)
    }

    private fun setCooldown(redeemCode: RedeemCode, duration: String): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncCooldown == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        placeHolder.cooldown = duration
        if (!JService.isDurationValid(duration)) return !sendMessage(JMessage.Code.Modify.INVALID_VALUE)
        redeemCode.cooldown = duration
        if (duration.isBlank()) redeemCode.cooldown = "0s"
        sendMessage(JMessage.Code.Modify.SET_COOLDOWN)
        return upsertCode(redeemCode)
    }

    private fun setCooldown(redeemTemplate: RedeemTemplate, duration: String): Boolean {
        placeHolder.cooldown = duration
        if (!JService.isDurationValid(duration)) return !sendMessage(JMessage.Template.Modify.INVALID_VALUE)
        redeemTemplate.cooldown = duration
        if (duration.isBlank()) redeemTemplate.cooldown = "0s"
        sendMessage(JMessage.Template.Modify.SET_COOLDOWN)
        return upsertTemplate(redeemTemplate)
    }

    private fun toggleTemplateSyncStatus(redeemCode: RedeemCode): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncLockedStatus == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        placeHolder.templateLocked = redeemCode.sync.toString()
        if (redeemCode.template.isBlank()) return !sendMessage(JMessage.Template.NOT_FOUND)
        redeemCode.sync = !redeemCode.sync
        sendMessage(JMessage.Code.Modify.SYNC_STATUS)
        return upsertCode(redeemCode)
    }

    private fun setTemplate(redeemCode: RedeemCode, template: String): Boolean {

        redeemCode.template = template
        placeHolder.template = template
        val templateState = config.getTemplate(redeemCode.template) ?: return sendMessage(JMessage.Template.NOT_FOUND)
        sendMessage(JMessage.Code.Modify.SET_TEMPLATE)
        if (codeRepo.templateToRedeemCode(redeemCode, templateState)) sendMessage(JMessage.Code.Modify.SYNC)
        return upsertCode(redeemCode)
    }

    private fun addTarget(redeemCode: RedeemCode, targetList: MutableList<String>): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncTarget == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        if (targetList.size < 1) return !sendMessage(JMessage.Code.Modify.INVALID_VALUE)
        redeemCode.target.addAll(targetList)
        placeHolder.target = targetList.joinToString(", ")
        sendMessage(JMessage.Code.Modify.ADD_TARGET)
        return upsertCode(redeemCode)
    }

    private fun removeTarget(redeemCode: RedeemCode, targetList: MutableList<String>): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncTarget == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        if (targetList.size < 1) return !sendMessage(JMessage.Code.Modify.INVALID_VALUE)
        redeemCode.target.removeAll(targetList)
        placeHolder.target = targetList.joinToString(", ")
        sendMessage(JMessage.Code.Modify.REMOVE_TARGET)
        return upsertCode(redeemCode)
    }

    private fun setTarget(redeemCode: RedeemCode, targetList: MutableList<String>): Boolean {
        if (redeemCode.sync && config.getTemplate(redeemCode.template)?.syncTarget == true) return sendMessage(JMessage.Code.Modify.SYNC_LOCKED)
        redeemCode.target.clear()
        redeemCode.target.addAll(targetList)
        placeHolder.target = targetList.joinToString(", ")
        sendMessage(JMessage.Code.Modify.SET_TARGET)
        return upsertCode(redeemCode)
    }
}
