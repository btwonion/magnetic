package dev.nyon.magnetic.config

import dev.isxander.yacl3.api.ListOption
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.dsl.*
import dev.nyon.konfig.config.saveConfig
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

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

        val ignoreKilledEntities = rootOptions.register(
            "ignoreKilledEntities",
            ListOption.createBuilder<String>()
                .name(Component.translatable("yacl3.config.magnetic.category.general.root.option.ignoreKilledEntities"))
                .description(OptionDescription.createBuilder().text(Component.translatable("yacl3.config.magnetic.category.general.root.option.ignoreKilledEntities.description")).build())
                .controller(stringField())
                .binding(
                    emptyList(),
                    { config.ignoreKilledEntities.map(Identifier::toString) },
                    { list->
                        config.ignoreKilledEntities = list.mapNotNull { entry ->
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
