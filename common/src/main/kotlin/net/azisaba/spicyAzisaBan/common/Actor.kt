package net.azisaba.spicyAzisaBan.common

import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.common.chat.Component
import java.util.UUID

interface Actor {
    val name: String
    val uniqueId: UUID
    fun sendMessage(component: Component)
    fun sendMessage(vararg components: Component)
    fun hasPermission(permission: String): Boolean

    fun sendMessage(component: net.kyori.adventure.text.Component) =
        sendMessage(SpicyAzisaBan.instance.convertComponent(component))

    object Dummy : Actor {
        override val name = "dummy player"
        override val uniqueId = UUID(0, 0)

        override fun sendMessage(component: Component) = Unit

        override fun sendMessage(vararg components: Component) = Unit

        override fun hasPermission(permission: String): Boolean = false
    }
}
