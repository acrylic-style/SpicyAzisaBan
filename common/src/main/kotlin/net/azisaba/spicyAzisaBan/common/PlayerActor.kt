package net.azisaba.spicyAzisaBan.common

import net.azisaba.spicyAzisaBan.common.chat.Component
import net.azisaba.spicyAzisaBan.common.title.Title
import net.azisaba.spicyAzisaBan.util.Util.convert

interface PlayerActor: Actor, PlayerConnection {
    /**
     * Get the current server of a player. Returns null if a player has not been connected to any server.
     */
    fun getServer(): ServerInfo?

    /**
     * Disconnects a player with provided reason.
     */
    fun disconnect(reason: Component)

    /**
     * Disconnects a player with provided reason.
     */
    fun disconnect(vararg reason: Component)

    fun disconnect(reason: net.kyori.adventure.text.Component) = disconnect(reason.convert())

    /**
     * Connect the player to specific server. If BungeeCord, the method might return before the player connects to the
     * target server.
     * @throws IllegalArgumentException if invalid server was specified
     */
    @Throws(IllegalArgumentException::class)
    fun connect(server: ServerInfo)

    fun sendTitle(title: Title)

    fun clearTitle()

    fun isOnline(): Boolean
}
