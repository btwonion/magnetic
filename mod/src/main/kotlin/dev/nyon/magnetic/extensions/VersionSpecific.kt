package dev.nyon.magnetic.extensions

import dev.nyon.magnetic.common.config.Identifier
import net.minecraft.resources.ResourceKey

typealias MinecraftIdentifier = /*? if >=1.21.11 {*/ net.minecraft.resources.Identifier /*?} else {*/ /*net.minecraft.resources.ResourceLocation *//*?}*/

fun minecraftIdentifier(namespace: String, path: String): MinecraftIdentifier =
    MinecraftIdentifier.fromNamespaceAndPath(namespace, path)

fun Identifier.toMinecraft(): MinecraftIdentifier = minecraftIdentifier(namespace, path)

fun <T : Any> ResourceKey<T>.magneticIdentifier(): MinecraftIdentifier =
    /*? if >=1.21.11 {*/ identifier() /*?} else {*/ /*location() *//*?}*/
