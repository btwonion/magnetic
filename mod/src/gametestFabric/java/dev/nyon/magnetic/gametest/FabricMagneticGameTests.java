package dev.nyon.magnetic.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FabricMagneticGameTests {
    @GameTest(structure = "minecraft:empty", maxTicks = 40)
    public void coreGameplay(GameTestHelper helper) {
        MagneticGameTestScenario.run(helper);
    }
}
