package dev.nyon.magnetic.config.screen

import dev.isxander.yacl3.api.ListOption
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.dsl.*
import dev.nyon.konfig.config.saveConfig
import dev.nyon.magnetic.config.Identifier
import dev.nyon.magnetic.extensions.IdentifierSerializer
import dev.nyon.magnetic.config.config
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

fun generateConfigScreen(parent: Screen? = null): Screen = YetAnotherConfigLib("magnetic") {
    val general by categories.registering {
        val enchantmentRequired by rootOptions.registering {
            binding(true, { config.enchantmentRequired }, { config.enchantmentRequired = it })
            controller = tickBox()
            descriptionBuilder {
                addDefaultText(1)
            }
        }

        val sneakRequired by rootOptions.registering {
            binding(false, { config.sneakRequired }, { config.sneakRequired = it })
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

        val ignoreEntities = rootOptions.register(
            "ignoreKilledEntities",
            ListOption.createBuilder<String>()
                .name(Component.translatable("yacl3.config.magnetic.category.general.root.option.ignoreEntities"))
                .description(OptionDescription.createBuilder().text(Component.translatable("yacl3.config.magnetic.category.general.root.option.ignoreEntities.description")).build())
                .controller(stringField())
                .binding(
                    emptyList(),
                    { config.ignoreEntities.map(Identifier::toString) },
                    { list->
                        config.ignoreEntities = list.mapNotNull { entry ->
                            runCatching { IdentifierSerializer.decodeFromString(entry) }.getOrNull()
                        }
                    }
                )
                .initial("")
                .build()
        )

        val ignoreRangedWeapons by rootOptions.registering {
            binding(true, { config.ignoreRangedWeapons }, { config.ignoreRangedWeapons = it })
            controller = tickBox()
            descriptionBuilder {
                addDefaultText(1)
            }
        }
    }

    save { saveConfig(config) }
}.generateScreen(parent)
