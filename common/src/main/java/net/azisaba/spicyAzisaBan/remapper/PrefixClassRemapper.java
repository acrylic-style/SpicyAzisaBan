package net.azisaba.spicyAzisaBan.remapper;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.commons.Remapper;

public class PrefixClassRemapper extends Remapper {
    private final String prefix;

    /**
     * Constructs a new PrefixClassRemapper.
     * @param prefix prefix without trailing dot.
     */
    public PrefixClassRemapper(@NotNull String prefix) {
        this.prefix = prefix.replace('.', '/');
    }

    @NotNull
    @Override
    public String map(String internalName) {
        // Packages included in JDK
        if (internalName.startsWith("java/")
                || internalName.startsWith("javax/")
                || internalName.startsWith("jdk/")
                || internalName.startsWith("sun/")
                || internalName.startsWith("com/sun/")
                || internalName.startsWith("org/ietf/jgss/")
                || internalName.startsWith("org/w3c/dom/")
                || internalName.startsWith("org/xml/sax/")
                || internalName.startsWith("org/jcp/xml/dsig/internal")
                || internalName.startsWith("org/graalvm/")
        ) return internalName;
        return prefix + "/" + internalName;
    }
}
