package io.github.radishsprouts.lightlite.render;

import io.github.radishsprouts.lightlite.scan.RegionMesh;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

interface MeshBackend {
    String name();

    /** Replaces the mesh's contents with the builder's current output. */
    void upload(RegionMesh mesh, MeshBuilder builder);

    void release(RegionMesh mesh);

    /** Draws or submits the frame's visible ranges; returns the number of draw calls issued. */
    int submit(LevelRenderContext context, DrawList draws);

    void close();
}
