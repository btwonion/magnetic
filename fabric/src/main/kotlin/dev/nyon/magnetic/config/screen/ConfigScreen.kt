package dev.nyon.magnetic.config.screen

import dev.isxander.yacl3.api.ListOption
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.dsl.*
import dev.nyon.konfig.config.saveConfig
import dev.nyon.magnetic.config.Identifier
import dev.nyon.magnetic.config.conditions.ConditionChain
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.IdentifierSerializer
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

fun generateConfigScreen(parent: Screen? = null): Screen = YetAnotherConfigLib("magnetic") {

    save { saveConfig(config) }
}.generateScreen(parent)
