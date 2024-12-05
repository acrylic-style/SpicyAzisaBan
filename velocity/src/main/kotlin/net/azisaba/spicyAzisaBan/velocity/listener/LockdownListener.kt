package net.azisaba.spicyAzisaBan.velocity.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import net.azisaba.spicyAzisaBan.SABMessages
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object LockdownListener {
    @Subscribe
    fun onLogin(e: PreLoginEvent) {
        try {
            e.result = PreLoginEvent.PreLoginComponentResult.denied(
                Component.text(SABMessages.General.failsafeKickMessage)
                    .append(Component.text("(SAB: Initialization error)").color(NamedTextColor.DARK_GRAY))
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
            e.result = PreLoginEvent.PreLoginComponentResult.denied(
                Component.text("You cannot join the server right now. Please contact the server administrator.")
                    .append(Component.text("(SAB: Initialization error)").color(NamedTextColor.DARK_GRAY))
            )
        }
    }
}
