package io.github.radishsprouts.lightlite.render;

import io.github.radishsprouts.lightlite.scan.Region;
import io.github.radishsprouts.lightlite.scan.RegionMesh;

import java.util.Arrays;

/** Visible quad ranges for one frame, grouped by region (entries of a region are adjacent). */
final class DrawList {
    Region[] regions = new Region[64];
    RegionMesh[] meshes = new RegionMesh[64];
    int[] firstQuad = new int[64];
    int[] quadCount = new int[64];
    int size;
    int maxQuads;

    void clear() {
        Arrays.fill(regions, 0, size, null);
        Arrays.fill(meshes, 0, size, null);
        size = 0;
        maxQuads = 0;
    }

    void add(Region region, RegionMesh mesh, int first, int count) {
        // Extend the previous range when it continues in the same buffer
        if (size > 0 && meshes[size - 1] == mesh && firstQuad[size - 1] + quadCount[size - 1] == first) {
            quadCount[size - 1] += count;
            maxQuads = Math.max(maxQuads, quadCount[size - 1]);
            return;
        }
        if (size == regions.length) {
            int n = size * 2;
            regions = Arrays.copyOf(regions, n);
            meshes = Arrays.copyOf(meshes, n);
            firstQuad = Arrays.copyOf(firstQuad, n);
            quadCount = Arrays.copyOf(quadCount, n);
        }
        regions[size] = region;
        meshes[size] = mesh;
        firstQuad[size] = first;
        quadCount[size] = count;
        size++;
        maxQuads = Math.max(maxQuads, count);
    }
}
