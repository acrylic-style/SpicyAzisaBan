package net.azisaba.spicyAzisaBan.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.remapper.JarUtils
import net.azisaba.spicyAzisaBan.velocity.listener.EventListeners
import net.azisaba.spicyAzisaBan.velocity.listener.LockdownListener
import net.azisaba.spicyAzisaBan.velocity.listener.PlayerDataUpdaterListener
import org.eclipse.aether.util.graph.visitor.NodeListGenerator
import org.eclipse.aether.util.graph.visitor.PreorderDependencyNodeConsumerVisitor
import org.slf4j.Logger
import xyz.acrylicstyle.util.maven.MavenResolver

@Plugin(
    id = "spicyazisaban",
    name = "SpicyAzisaBan",
    version = "dev",
    authors = ["AzisabaNetwork"],
    url = "https://github.com/AzisabaNetwork/SpicyAzisaBan"
)
class VelocityPlugin @Inject constructor(val server: ProxyServer, private val logger: Logger) {
    companion object {
        lateinit var instance: VelocityPlugin
    }

    init {
        instance = this
    }

    @Subscribe
    fun onProxyInitialization(@Suppress("unused") e: ProxyInitializeEvent) {
        try {
            val nodeListGenerator = NodeListGenerator()
            MavenResolver("plugins/SpicyAzisaBan/libraries")
                .addMavenCentral()
                .addDependency('c' + "om.google.guava:guava:31.0.1-jre")
                .addDependency('o' + "rg.reflections:reflections:0.10.2")
                .addDependency('o' + "rg.json:json:20210307")
                .addDependency('o' + "rg.yaml:snakeyaml:1.29")
                .addDependency('o' + "rg.mariadb.jdbc:mariadb-java-client:3.5.0")
                .resolve { node, list -> node.artifact.groupId != "log4j" }
                .forEach {
                    it.root.accept(PreorderDependencyNodeConsumerVisitor(nodeListGenerator))
                }
            val dedupe = nodeListGenerator.nodes.sortedByDescending { it.version }.distinctBy { it.artifact.groupId to it.artifact.artifactId }
            nodeListGenerator.nodes.retainAll(dedupe)
            nodeListGenerator.paths
                .map { it.toFile() }
                .map { JarUtils.remapJarWithClassPrefix(it, "-remapped", "net.azisaba.spicyAzisaBan.libs") }
                .map { it.toPath() }
                .onEach {
                    server.pluginManager.addToClasspath(this, it)
                    logger.info("Loaded library $it")
                }
            SpicyAzisaBan.debugLevel = 5
            SpicyAzisaBanVelocity(server).doEnable()
            if (!SABConfig.debugBuild) SpicyAzisaBan.debugLevel = 0
            server.eventManager.register(this, EventListeners)
            server.eventManager.register(this, PlayerDataUpdaterListener)
        } catch (e: Exception) {
            logger.error("Fatal error occurred while initializing the plugin", e)
            if (SABConfig.database.failsafe) {
                logger.info("Failsafe is enabled, locking down the server")
                server.eventManager.register(this, LockdownListener)
            }
        }
    }
}
