package dev.nyon.magnetic;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@SuppressWarnings({ "UnstableApiUsage", "unused" })
public class MagneticLoader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder builder) {
        final MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder(
            "nyonReleases",
            "default",
            "https://repo.nyon.dev/releases"
        ).build());
        resolver.addRepository(new RemoteRepository.Builder(
            "central",
            "default",
            MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
        ).build());

        var dependencies = Arrays.asList(
            "dev.nyon:konfig:3.0.0",
            "org.jetbrains.kotlin:kotlin-stdlib:2.1.21",
            "org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0",
            "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"
        );
        dependencies.forEach(dependency -> resolver.addDependency(new Dependency(
            new DefaultArtifact(dependency),
            null
        )));

        builder.addLibrary(resolver);
    }
}
