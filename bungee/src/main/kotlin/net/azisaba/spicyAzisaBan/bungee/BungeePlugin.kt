package net.azisaba.spicyAzisaBan.bungee

import net.azisaba.spicyAzisaBan.SABConfig
import net.azisaba.spicyAzisaBan.SpicyAzisaBan
import net.azisaba.spicyAzisaBan.bungee.listener.EventListeners
import net.azisaba.spicyAzisaBan.bungee.listener.LockdownListener
import net.azisaba.spicyAzisaBan.bungee.listener.PlayerDataUpdaterListener
import net.azisaba.spicyAzisaBan.remapper.JarUtils
import net.blueberrymc.nativeutil.NativeUtil
import net.md_5.bungee.api.plugin.Plugin
import org.eclipse.aether.util.graph.visitor.NodeListGenerator
import org.eclipse.aether.util.graph.visitor.PreorderDependencyNodeConsumerVisitor
import xyz.acrylicstyle.util.maven.MavenResolver
import java.io.File
import java.net.URLClassLoader
import java.util.logging.Logger

class BungeePlugin: Plugin() {
    companion object {
        private val LOGGER = Logger.getLogger("SpicyAzisaBan")
        lateinit var instance: BungeePlugin

        init {
            val dataFolder = File("plugins/SpicyAzisaBan")
            LOGGER.info("Data folder: ${dataFolder.absolutePath}")
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
            val urls = nodeListGenerator.paths
                .map { it.toFile() }
                .map { JarUtils.remapJarWithClassPrefix(it, "-remapped", "net.azisaba.spicyAzisaBan.libs") }
                .map { it.toURI().toURL() }
                .toTypedArray()
            val cl = BungeePlugin::class.java.classLoader
            val libraryLoader = URLClassLoader(urls)
            NativeUtil.setObject(cl::class.java.getDeclaredField("libraryLoader"), cl, libraryLoader)
            LOGGER.info("Loaded libraries (" + urls.size + "):")
            urls.forEach { url ->
                LOGGER.info(" - ${url.path}")
            }
        }
    }

    init {
        instance = this
    }

    override fun onEnable() {
        try {
            SpicyAzisaBanBungee().doEnable()
            proxy.pluginManager.registerListener(this, EventListeners)
            proxy.pluginManager.registerListener(this, PlayerDataUpdaterListener)
            logger.info("Hewwwwwwwwwoooooo!")
        } catch (e: Exception) {
            logger.severe("Fatal error occurred while initializing the plugin")
            e.printStackTrace()
            if (SABConfig.database.failsafe) {
                logger.info("Failsafe is enabled, locking down the server")
                proxy.pluginManager.registerListener(this, LockdownListener)
            }
        }
    }

    override fun onDisable() {
        SpicyAzisaBan.instance.shutdownTimer()
        logger.info("Closing database connection")
        SpicyAzisaBan.instance.connection.close()
        SpicyAzisaBan.debugLevel = 0
        logger.info("Goodbye, World!")
    }
}
