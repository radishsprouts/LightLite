package io.github.radishsprouts.lightlite.render;

import io.github.radishsprouts.lightlite.scan.Region;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import java.util.Collection;

interface MeshBackend {
    String name();

    /** Replaces the region's mesh with the builder's current content. */
    void upload(Region region, MeshBuilder mesh);

    void release(Region region);

    /** Draws or submits all regions; returns the number of draw calls issued. */
    int submit(LevelRenderContext context, Collection<Region> regions);

    void close();
}
