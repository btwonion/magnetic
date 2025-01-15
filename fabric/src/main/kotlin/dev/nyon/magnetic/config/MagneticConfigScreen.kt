package dev.nyon.magnetic.config

import dev.isxander.yacl3.dsl.YetAnotherConfigLib
import dev.isxander.yacl3.dsl.controller
import dev.isxander.yacl3.dsl.descriptionBuilder
import dev.isxander.yacl3.dsl.tickBox
import dev.nyon.konfig.config.saveConfig
import net.minecraft.client.gui.screens.Screen

fun generateConfigScreen(parent: Screen? = null): Screen = YetAnotherConfigLib("magnetic") {
    val general by categories.registering {
        val needEnchantment by rootOptions.registering {
            binding(true, { config.needEnchantment }, { config.needEnchantment = it })
            controller = tickBox()
            descriptionBuilder {
                addDefaultText(1)
            }
        }

        val needSneak by rootOptions.registering {
            binding(false, { config.needSneak }, { config.needSneak = it })
            controller = tickBox()
            descriptionBuilder {
                addDefaultText(1)
            }
        }

        val expAllowed by rootOptions.registering {
            binding(false, { config.expAllowed }, { config.expAllowed = it })
            controller = tickBox()
            descriptionBuilder {
                addDefaultText(1)
            }
        }

        val itemsAllowed by rootOptions.registering {
            binding(false, { config.itemsAllowed }, { config.itemsAllowed = it })
            controller = tickBox()
            descriptionBuilder {
                addDefaultText(1)
            }
        }
    }

    save { saveConfig(config) }
}.generateScreen(parent)
