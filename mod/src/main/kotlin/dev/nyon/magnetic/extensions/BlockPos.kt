package dev.nyon.magnetic.extensions

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

fun BlockPos.centerVec(): Vec3 = Vec3(x + 0.5, y + 0.5, z + 0.5)