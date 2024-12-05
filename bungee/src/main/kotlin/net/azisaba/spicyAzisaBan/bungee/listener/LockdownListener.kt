package net.azisaba.spicyAzisaBan.bungee.listener

import net.azisaba.spicyAzisaBan.SABMessages
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.PreLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

object LockdownListener : Listener {
    @EventHandler
    fun onLogin(e: PreLoginEvent) {
        e.isCancelled = true
        try {
            e.setCancelReason(*TextComponent.fromLegacyText("${SABMessages.General.failsafeKickMessage} (SAB: Initialization error)"))
        } catch (ex: Exception) {
            ex.printStackTrace()
            e.setCancelReason(*TextComponent.fromLegacyText("You cannot join the server right now. Please contact the server administrator. (SAB: Initialization error)"))
        }
    }
}
