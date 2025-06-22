package dev.nyon.magnetic;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
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

        resolver.addDependency(new Dependency(new DefaultArtifact("dev.nyon:konfig:3.0.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.jetbrains.kotlin:kotlin-stdlib:2.1.21"), null));
        resolver.addDependency(new Dependency(
            new DefaultArtifact("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.0"), null));
        resolver.addDependency(new Dependency(
            new DefaultArtifact("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0"), null));

        builder.addLibrary(resolver);
    }
}
