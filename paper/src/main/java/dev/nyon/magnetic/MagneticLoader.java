package dev.nyon.magnetic;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
            //? if >=1.21.6
            MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
            //? if <1.21.6
            //"https://maven-central.storage-download.googleapis.com/maven2"
        ).build());

        libraries().forEach(dependency -> resolver.addDependency(new Dependency(
            new DefaultArtifact(dependency),
            null
        )));

        builder.addLibrary(resolver);
    }

    private static List<String> libraries() {
        try (InputStream stream = MagneticLoader.class.getResourceAsStream("/magnetic-libraries.txt")) {
            if (stream == null) throw new IllegalStateException("The plugin jar does not list its libraries.");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().filter(line -> !line.isBlank()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
