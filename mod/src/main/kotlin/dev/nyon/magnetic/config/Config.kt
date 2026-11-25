package dev.nyon.magnetic.config

import dev.nyon.konfig.config.loadConfig
import dev.nyon.magnetic.common.config.Config

var config: Config = loadConfig()
    set(value) {
        field = value
        invalidateIgnoredCaches()
    }
