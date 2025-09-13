@file:Suppress("unused")

package dev.nyon.magnetic

import dev.nyon.magnetic.config.config
import net.fabricmc.loader.api.FabricLoader

fun init() {
    DropEvent
    if (config.permissionRequired && !FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0"))
        error("To use the 'needPermission' option of Magnetic, you have to install the fabric-permissions-api.")
}
