package net.azisaba.spicyAzisaBan.velocity.listener

import com.velocitypowered.api.event.EventTask
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.api.proxy.Player
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.ServerInfo
import net.azisaba.spicyAzisaBan.punishment.PunishmentChecker
import net.azisaba.spicyAzisaBan.struct.PlayerData
import net.azisaba.spicyAzisaBan.util.Util.send
import net.azisaba.spicyAzisaBan.util.Util.translate
import net.azisaba.spicyAzisaBan.velocity.VelocityPlayerActor
import net.azisaba.spicyAzisaBan.velocity.util.VelocityUtil.toVelocity
import net.kyori.adventure.text.Component
import util.kt.promise.rewrite.catch

object EventListeners {
    @Subscribe
    fun onLogin(e: LoginEvent): EventTask = EventTask.async {
        var denied = false
        PunishmentChecker.checkLockdown(VelocityPlayerActor(e.player)) { reason ->
            e.result = ResultedEvent.ComponentResult.denied(Component.textOfChildren(*reason.toVelocity()))
            denied = true
        }
        if (!denied) {
            PlayerData.createOrUpdate(VelocityPlayerActor(e.player)).catch { it.printStackTrace() }
            PunishmentChecker.checkGlobalBan(VelocityPlayerActor(e.player)) { reason ->
                e.result = ResultedEvent.ComponentResult.denied(Component.textOfChildren(*reason.toVelocity()))
            }
        }
    }

    @Subscribe
    fun onServerPreConnect(e: ServerPreConnectEvent): EventTask = EventTask.async {
        if (e.result.server.isEmpty) return@async
        PunishmentChecker.checkLocalBan(
            ServerInfo(e.result.server.get().serverInfo.name, e.result.server.get().serverInfo.address),
            VelocityPlayerActor(e.player)
        ) {
            e.result = ServerPreConnectEvent.ServerResult.denied()
        }.complete()
    }

    @Subscribe
    fun onServerPostConnect(e: ServerPostConnectEvent): EventTask = EventTask.async {
        if (e.player.currentServer.isEmpty) return@async
        PunishmentChecker.checkLocalBan(
            ServerInfo(e.player.currentServer.get().serverInfo.name, e.player.currentServer.get().serverInfo.address),
            VelocityPlayerActor(e.player)
        ) {
            SpicyAzisaBan.LOGGER.warning("Kicking player ${e.player.username} because they are banned from ${e.player.currentServer.get().serverInfo.name}")
            e.player.disconnect(SABMessages.General.error.translate())
        }.complete()
    }

    @Suppress("DEPRECATION")
    @Subscribe
    fun onPlayerChat(e: PlayerChatEvent): EventTask = EventTask.async {
        val actor = VelocityPlayerActor(e.player)
        PunishmentChecker.checkMute(actor, e.message) { reason ->
            e.result = PlayerChatEvent.ChatResult.denied()
            if (e.player.protocolVersion.ordinal >= ProtocolVersion.valueOf("MINECRAFT_1_19_1").ordinal) {
                actor.disconnect(reason)
            } else {
                actor.send(reason)
            }
        }
    }

    @Subscribe
    fun onCommandExecute(e: CommandExecuteEvent): EventTask = EventTask.async {
        val source = e.commandSource
        if (source !is Player) return@async
        val actor = VelocityPlayerActor(source)
        PunishmentChecker.checkMute(actor, "/${e.command}") { reason ->
            e.result = CommandExecuteEvent.CommandResult.denied()
            if (source.protocolVersion.ordinal >= ProtocolVersion.valueOf("MINECRAFT_1_19_1").ordinal) {
                actor.disconnect(reason)
            } else {
                actor.send(reason)
            }
        }
    }
}
