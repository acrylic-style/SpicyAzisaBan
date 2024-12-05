package net.azisaba.spicyAzisaBan.util

import net.azisaba.spicyAzisaBan.ReloadableSABConfig
import net.azisaba.spicyAzisaBan.SABMessages
import net.azisaba.spicyAzisaBan.common.Actor
import net.azisaba.spicyAzisaBan.punishment.Expiration
import net.azisaba.spicyAzisaBan.punishment.Proof
import net.azisaba.spicyAzisaBan.punishment.Punishment
import net.azisaba.spicyAzisaBan.punishment.UnPunish
import net.azisaba.spicyAzisaBan.util.Util.async
import net.azisaba.spicyAzisaBan.util.Util.getProfile
import net.azisaba.spicyAzisaBan.util.Util.limit
import net.azisaba.spicyAzisaBan.util.Util.split
import util.http.DiscordWebhook
import util.kt.promise.rewrite.catch
import util.promise.rewrite.Promise
import xyz.acrylicstyle.mcutil.common.PlayerProfile
import java.awt.Color

object WebhookUtil {
    fun Punishment.sendWebhook(): Promise<Unit> {
        val url = ReloadableSABConfig.getWebhookURL(server, type)
        if (url != null && url.startsWith("http")) {
            val webhook = DiscordWebhook(url)
            return operator.getProfile().then { profile ->
                webhook.username = profile.name
                webhook.content = SABMessages.General.Webhook.punishmentAdded
                webhook.addEmbed(toEmbed(profile))
                webhook.execute()
            }.then {}.catch { it.printStackTrace() }
        }
        return Promise.resolve(null)
    }

    // meant to be used for --all option. can send up to 40 punishments.
    fun List<Punishment>.sendWebhook(): Promise<Unit> {
        if (isEmpty()) return Promise.resolve(null)
        // TODO: check server and type, or implement more smart way to send webhooks (maybe we can use #groupBy function)
        val url = ReloadableSABConfig.getWebhookURL(this[0].server, this[0].type)
        if (url != null && url.startsWith("http")) { // lazy check
            return this[0].operator.getProfile().then { profile ->
                val split = this.split(10) // send up to 10 embeds at once
                    .limit(5 - 1) // to comply with rate limit. 1st request is used by original punishment.
                split.forEachIndexed { index, list ->
                    val webhook = DiscordWebhook(url)
                    webhook.username = profile.name
                    webhook.content = SABMessages.General.Webhook.punishmentAdded
                    list.forEach { p ->
                        webhook.addEmbed(p.toEmbed(profile))
                    }
                    webhook.execute()
                }
            }.then {}.catch { it.printStackTrace() }
        }
        return Promise.resolve(null)
    }

    private fun Punishment.toEmbed(operator: PlayerProfile): DiscordWebhook.EmbedObject {
        val embed = DiscordWebhook.EmbedObject()
        embed.color = Color.RED
        embed.addField(SABMessages.General.Webhook.type, "${type.getName()} (${type.name})", false)
        embed.addField(SABMessages.General.Webhook.operator, "${operator.name} (${operator.uniqueId})", false)
        embed.addField(SABMessages.General.Webhook.server, server, false)
        embed.addField(SABMessages.General.Webhook.target, "$name ($target)", false)
        embed.addField(SABMessages.General.Webhook.punishReason, reason, false)
        embed.addField(SABMessages.General.Webhook.punishmentId, id.toString(), false)
        embed.addField(SABMessages.General.Webhook.punishmentDateTime, SABMessages.formatDate(start), false)
        if (type.name.contains("TEMP")) {
            embed.addField(SABMessages.General.Webhook.duration, Util.unProcessTime(end.serializeAsLong() - start), false)
            embed.addField(SABMessages.General.Webhook.expiration, if (end is Expiration.NeverExpire) SABMessages.General.permanent else SABMessages.formatDate(end.serializeAsLong()), false)
        }
        return embed
    }

    fun Punishment.sendReasonChangedWebhook(actor: Actor, newReason: String): Promise<Unit> {
        val url = ReloadableSABConfig.getWebhookURL(server, type)
        if (url != null && url.startsWith("http")) {
            val webhook = DiscordWebhook(url)
            return async<Unit> { context ->
                webhook.username = actor.name
                webhook.content = SABMessages.General.Webhook.punishmentReasonChanged
                val embed = DiscordWebhook.EmbedObject()
                embed.color = Color.ORANGE
                embed.addField(SABMessages.General.Webhook.type, "${type.getName()} (${type.name})", false)
                embed.addField(SABMessages.General.Webhook.operator, "${actor.name} (${actor.uniqueId})", false)
                embed.addField(SABMessages.General.Webhook.server, server, false)
                embed.addField(SABMessages.General.Webhook.target, "$name ($target)", false)
                embed.addField(SABMessages.General.Webhook.newReason, newReason, false)
                embed.addField(SABMessages.General.Webhook.oldReason, reason, false)
                embed.addField(SABMessages.General.Webhook.punishmentId, id.toString(), false)
                embed.addField(SABMessages.General.Webhook.punishmentDateTime, SABMessages.formatDate(start), false)
                if (type.name.contains("TEMP")) {
                    embed.addField(SABMessages.General.Webhook.duration, Util.unProcessTime(end.serializeAsLong() - start), false)
                    embed.addField(SABMessages.General.Webhook.expiration, if (end is Expiration.NeverExpire) SABMessages.General.permanent else SABMessages.formatDate(end.serializeAsLong()), false)
                }
                webhook.addEmbed(embed)
                try {
                    webhook.execute()
                    context.resolve()
                } catch (e: Throwable) {
                    context.reject(e)
                }
            }.catch { it.printStackTrace() }
        }
        return Promise.resolve(null)
    }

    fun UnPunish.sendWebhook(): Promise<Unit> {
        val url = ReloadableSABConfig.getWebhookURL(punishment.server, punishment.type)
        if (url != null && url.startsWith("http")) {
            val webhook = DiscordWebhook(url)
            return punishment.operator.getProfile().then { profile ->
                val unPunishOpProfile = operator.getProfile().complete()
                webhook.username = unPunishOpProfile.name
                webhook.content = SABMessages.General.Webhook.punishmentRemoved
                val embed = DiscordWebhook.EmbedObject()
                embed.color = Color.GREEN
                embed.addField(SABMessages.General.Webhook.type, "${punishment.type.getName()} (${punishment.type.name})", false)
                embed.addField(SABMessages.General.Webhook.server, punishment.server, false)
                embed.addField(SABMessages.General.Webhook.operator, "${unPunishOpProfile.name} (${unPunishOpProfile.uniqueId})", false)
                embed.addField(SABMessages.General.Webhook.unpunishReason, reason, false)
                embed.addField(SABMessages.General.Webhook.unpunishId, id.toString(), false)
                embed.addField(SABMessages.General.Webhook.punishOperator, "${profile.name} (${profile.uniqueId})", false)
                embed.addField(SABMessages.General.Webhook.target, "${punishment.name} (${punishment.target})", false)
                embed.addField(SABMessages.General.Webhook.punishReason, punishment.reason, false)
                embed.addField(SABMessages.General.Webhook.punishmentId, punishment.id.toString(), false)
                embed.addField(SABMessages.General.Webhook.punishmentDateTime, SABMessages.formatDate(punishment.start), false)
                if (punishment.type.name.contains("TEMP")) {
                    embed.addField(SABMessages.General.Webhook.duration, Util.unProcessTime(punishment.end.serializeAsLong() - punishment.start), false)
                    embed.addField(SABMessages.General.Webhook.expiration, if (punishment.end is Expiration.NeverExpire) SABMessages.General.permanent else SABMessages.formatDate(punishment.end.serializeAsLong()), false)
                }
                webhook.addEmbed(embed)
                webhook.execute()
            }.then {}.catch { it.printStackTrace() }
        }
        return Promise.resolve(null)
    }

    fun Proof.sendWebhook(actor: Actor, content: String, color: Color? = null): Promise<Unit> {
        val url = ReloadableSABConfig.getWebhookURL(punishment.server, punishment.type)
        if (url != null && url.startsWith("http")) {
            val webhook = DiscordWebhook(url)
            return async<Unit> { context ->
                webhook.username = actor.name
                webhook.content = content
                val embed = DiscordWebhook.EmbedObject()
                embed.color = color
                embed.addField(SABMessages.General.Webhook.type, "${punishment.type.getName()} (${punishment.type.name})", false)
                embed.addField(SABMessages.General.Webhook.operator, "${actor.name} (${actor.uniqueId})", false)
                embed.addField(SABMessages.General.Webhook.server, punishment.server, false)
                embed.addField(SABMessages.General.Webhook.proofText, text, false)
                embed.addField(SABMessages.General.Webhook.proofId, id.toString(), false)
                embed.addField(SABMessages.General.Webhook.target, "${punishment.name} (${punishment.target})", false)
                embed.addField(SABMessages.General.Webhook.punishmentId, punishment.id.toString(), false)
                embed.addField(SABMessages.General.Webhook.punishReason, punishment.reason, false)
                embed.addField(SABMessages.General.Webhook.viewableByTarget, public.toString(), false)
                if (text.startsWith("https://") &&
                    !text.contains(" ") &&
                    (text.endsWith(".png", true) || text.endsWith(".jpg", true) || text.endsWith(".gif", true))
                ) {
                    embed.image = DiscordWebhook.EmbedObject.Image(text)
                }
                webhook.addEmbed(embed)
                try {
                    webhook.execute()
                    context.resolve()
                } catch (e: Throwable) {
                    context.reject(e)
                }
            }.catch { it.printStackTrace() }
        }
        return Promise.resolve(null)
    }
}
