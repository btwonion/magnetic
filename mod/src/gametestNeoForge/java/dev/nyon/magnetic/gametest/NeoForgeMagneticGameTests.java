package dev.nyon.magnetic.gametest;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod(NeoForgeMagneticGameTests.MOD_ID)
public final class NeoForgeMagneticGameTests {
    static final String MOD_ID = "magnetic_test";

    public NeoForgeMagneticGameTests(IEventBus modBus) {
        modBus.addListener(NeoForgeMagneticGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
            id("default"), new TestEnvironmentDefinition.AllOf()
        );
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
            environment,
            Identifier.withDefaultNamespace("empty"),
            40,
            0,
            true,
            Rotation.NONE
        );
        event.registerTest(id("core_gameplay"), new MagneticTestInstance(data));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    private static final class MagneticTestInstance extends GameTestInstance {
        private MagneticTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> data) {
            super(data);
        }

        @Override
        public void run(GameTestHelper helper) {
            MagneticGameTestScenario.run(helper);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            // This instance is registered directly and is never serialized.
            return FunctionGameTestInstance.CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("Magnetic core gameplay");
        }
    }
}
