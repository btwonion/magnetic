@file:Suppress("unused")

package dev.nyon.magnetic

import dev.nyon.magnetic.config.config
import net.fabricmc.loader.api.FabricLoader

fun init() {
    DropEvent
    if (config.needPermission && !FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0"))
        error("To use the 'needPermission' option of Magnetic, you have to install the fabric-permissions-api.")
}
