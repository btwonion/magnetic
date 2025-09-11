package dev.nyon.magnetic.config

import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation

@Serializable(with = ResourceLocationSerializer::class)
data class Identifier(val original: ResourceLocation, val isTag: Boolean)