package dev.nyon.magnetic.config.screen

import dev.isxander.yacl3.api.ListOption
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.dsl.*
import dev.nyon.konfig.config.saveConfig
import dev.nyon.magnetic.config.Identifier
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.extensions.IdentifierSerializer
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

        val ignoredEntitiesRangeMin by rootOptions.registering {
            binding(15.0, { config.ignoredEntitiesRangeMin }, { config.ignoredEntitiesRangeMin = it })
            controller = numberField(-1.0, Double.MAX_VALUE)
            descriptionBuilder {
                addDefaultText(1)
            }
        }

        val fullInventoryAlert by groups.registering {
            val soundAlertEnabled by options.registering {
                binding(true, { config.fullInventoryAlert.soundAlert.enabled }, { config.fullInventoryAlert.soundAlert.enabled = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val soundAlertCooldownInSeconds by options.registering {
                binding(5, { config.fullInventoryAlert.soundAlert.cooldownInSeconds }, { config.fullInventoryAlert.soundAlert.cooldownInSeconds = it })
                controller = numberField(0, Int.MAX_VALUE)
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val textAlertEnabled by options.registering {
                binding(true, { config.fullInventoryAlert.textAlert.enabled }, { config.fullInventoryAlert.textAlert.enabled = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val textAlertCooldownInSeconds by options.registering {
                binding(60, { config.fullInventoryAlert.textAlert.cooldownInSeconds }, { config.fullInventoryAlert.textAlert.cooldownInSeconds = it })
                controller = numberField(0, Int.MAX_VALUE)
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val titleAlertEnabled by options.registering {
                binding(true, { config.fullInventoryAlert.titleAlert.enabled }, { config.fullInventoryAlert.titleAlert.enabled = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val titleAlertCooldownInSeconds by options.registering {
                binding(5, { config.fullInventoryAlert.titleAlert.cooldownInSeconds }, { config.fullInventoryAlert.titleAlert.cooldownInSeconds = it })
                controller = numberField(0, Int.MAX_VALUE)
                descriptionBuilder {
                    addDefaultText(1)
                }
            }
        }

        val animation by groups.registering {
            val enabled by options.registering {
                binding(true, { config.animation.enabled }, { config.animation.enabled = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val blocksPerSecond by options.registering {
                binding(1.0, { config.animation.blocksPerSecond }, { config.animation.blocksPerSecond = it })
                controller = numberField(0.5, Double.MAX_VALUE)
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val canOtherPlayersPickup by options.registering {
                binding(false, { config.animation.canOtherPlayersPickup }, { config.animation.canOtherPlayersPickup = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }
        }
    }

    save { saveConfig(config) }
}.generateScreen(parent)
