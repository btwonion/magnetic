package dev.nyon.magnetic.config.screen

import dev.isxander.yacl3.api.ListOption
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.dsl.*
import dev.nyon.konfig.config.saveConfig
import dev.nyon.magnetic.common.conditions.ConditionChain
import dev.nyon.magnetic.common.config.Config
import dev.nyon.magnetic.common.config.Identifier
import dev.nyon.magnetic.config.config
import dev.nyon.magnetic.config.invalidateIgnoredCaches
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

private val defaults = Config()

fun generateConfigScreen(parent: Screen? = null): Screen = YetAnotherConfigLib("magnetic") {
    val general by categories.registering {
        val conditionStatement by rootOptions.registering {
            binding(defaults.conditionStatement.raw, { config.conditionStatement.raw }, { config.conditionStatement = ConditionChain(it) })
            controller = stringField()
            descriptionBuilder {
                addDefaultText(1)
            }
        }

        val expAllowed by rootOptions.registering {
            binding(defaults.expAllowed, { config.expAllowed }, { config.expAllowed = it })
            controller = tickBox()
            descriptionBuilder {
                addDefaultText(1)
            }
        }

        val itemsAllowed by rootOptions.registering {
            binding(defaults.itemsAllowed, { config.itemsAllowed }, { config.itemsAllowed = it })
            controller = tickBox()
            descriptionBuilder {
                addDefaultText(1)
            }
        }

        val buckets by groups.registering {
            val enabled by options.registering {
                binding(defaults.buckets.enabled, { config.buckets.enabled }, { config.buckets.enabled = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val abilityTimeout by options.registering {
                binding(defaults.buckets.abilityTimeout, { config.buckets.abilityTimeout }, { config.buckets.abilityTimeout = it })
                controller = numberField(-1, Long.MAX_VALUE)
                descriptionBuilder {
                    addDefaultText(1)
                }
            }
        }

        val leafDecay by groups.registering {
            val enabled by options.registering {
                binding(defaults.leafDecay.enabled, { config.leafDecay.enabled }, { config.leafDecay.enabled = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val abilityTimeout by options.registering {
                binding(defaults.leafDecay.abilityTimeout, { config.leafDecay.abilityTimeout }, { config.leafDecay.abilityTimeout = it })
                controller = numberField(0, Long.MAX_VALUE)
                descriptionBuilder {
                    addDefaultText(1)
                }
            }
        }

        val ignoreEntities = rootOptions.register(
            "ignoreEntities",
            ListOption.createBuilder<String>()
                .name(Component.translatable("yacl3.config.magnetic.category.general.root.option.ignoreEntities"))
                .description(OptionDescription.createBuilder().text(Component.translatable("yacl3.config.magnetic.category.general.root.option.ignoreEntities.description")).build())
                .controller(stringField())
                .binding(
                    defaults.ignoreEntities.map(Identifier::toString),
                    { config.ignoreEntities.map(Identifier::toString) },
                    { list->
                        config.ignoreEntities = list.mapNotNull { entry ->
                            runCatching { Identifier.parse(entry) }.getOrNull()
                        }
                    }
                )
                .initial("")
                .build()
        )

        val ignoreBlocks = rootOptions.register(
            "ignoreBlocks",
            ListOption.createBuilder<String>()
                .name(Component.translatable("yacl3.config.magnetic.category.general.root.option.ignoreBlocks"))
                .description(OptionDescription.createBuilder().text(Component.translatable("yacl3.config.magnetic.category.general.root.option.ignoreBlocks.description")).build())
                .controller(stringField())
                .binding(
                    defaults.ignoreBlocks.map(Identifier::toString),
                    { config.ignoreBlocks.map(Identifier::toString) },
                    { list ->
                        config.ignoreBlocks = list.mapNotNull { entry ->
                            runCatching { Identifier.parse(entry) }.getOrNull()
                        }
                    }
                )
                .initial("")
                .build()
        )

        val ignoredEntitiesRangeMin by rootOptions.registering {
            binding(defaults.ignoredEntitiesRangeMin, { config.ignoredEntitiesRangeMin }, { config.ignoredEntitiesRangeMin = it })
            controller = numberField(-1.0, Double.MAX_VALUE)
            descriptionBuilder {
                addDefaultText(1)
            }
        }

        val fullInventoryAlert by groups.registering {
            val soundAlertEnabled by options.registering {
                binding(defaults.fullInventoryAlert.soundAlert.enabled, { config.fullInventoryAlert.soundAlert.enabled }, { config.fullInventoryAlert.soundAlert.enabled = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val soundAlertCooldownInSeconds by options.registering {
                binding(defaults.fullInventoryAlert.soundAlert.cooldownInSeconds, { config.fullInventoryAlert.soundAlert.cooldownInSeconds }, { config.fullInventoryAlert.soundAlert.cooldownInSeconds = it })
                controller = numberField(0, Int.MAX_VALUE)
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val textAlertEnabled by options.registering {
                binding(defaults.fullInventoryAlert.textAlert.enabled, { config.fullInventoryAlert.textAlert.enabled }, { config.fullInventoryAlert.textAlert.enabled = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val textAlertCooldownInSeconds by options.registering {
                binding(defaults.fullInventoryAlert.textAlert.cooldownInSeconds, { config.fullInventoryAlert.textAlert.cooldownInSeconds }, { config.fullInventoryAlert.textAlert.cooldownInSeconds = it })
                controller = numberField(0, Int.MAX_VALUE)
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val titleAlertEnabled by options.registering {
                binding(defaults.fullInventoryAlert.titleAlert.enabled, { config.fullInventoryAlert.titleAlert.enabled }, { config.fullInventoryAlert.titleAlert.enabled = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val titleAlertCooldownInSeconds by options.registering {
                binding(defaults.fullInventoryAlert.titleAlert.cooldownInSeconds, { config.fullInventoryAlert.titleAlert.cooldownInSeconds }, { config.fullInventoryAlert.titleAlert.cooldownInSeconds = it })
                controller = numberField(0, Int.MAX_VALUE)
                descriptionBuilder {
                    addDefaultText(1)
                }
            }
        }

        val animation by groups.registering {
            val enabled by options.registering {
                binding(defaults.animation.enabled, { config.animation.enabled }, { config.animation.enabled = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val blocksPerSecond by options.registering {
                binding(defaults.animation.blocksPerSecond, { config.animation.blocksPerSecond }, { config.animation.blocksPerSecond = it })
                controller = numberField(0.5, Double.MAX_VALUE)
                descriptionBuilder {
                    addDefaultText(1)
                }
            }

            val canOtherPlayersPickup by options.registering {
                binding(defaults.animation.canOtherPlayersPickup, { config.animation.canOtherPlayersPickup }, { config.animation.canOtherPlayersPickup = it })
                controller = tickBox()
                descriptionBuilder {
                    addDefaultText(1)
                }
            }
        }
    }

    save {
        saveConfig(config)
        invalidateIgnoredCaches()
    }
}.generateScreen(parent)
