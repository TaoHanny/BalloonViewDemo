package com.instwall.balloonviewdemo.view.pathlayout;

import android.graphics.Path;

public class PathRegionGenerators {

    public static PathRegionGenerator createBitmapPathRegionGenerator() {

        return new PathRegionGenerator() {

            @Override
            public PathRegion generatorPathRegion(Path path, int clipType, int width, int height) {
                return new BitmapPathRegion(path, clipType, width, height);
            }
        };
    }

    public static PathRegionGenerator createBitmapPathRegionGenerator(final int inSampleSize) {
        return new PathRegionGenerator() {
            @Override
            public PathRegion generatorPathRegion(Path path, int clipType, int width, int height) {
                return new BitmapPathRegion(path, width, height, inSampleSize);
            }
        };
    }

    public static PathRegionGenerator createNativePathRegionGenerator() {
        return new PathRegionGenerator() {
            @Override
            public PathRegion generatorPathRegion(Path path, int clipType, int width, int height) {
                return new NativePathRegion(path, clipType);
            }
        };
    }

}
