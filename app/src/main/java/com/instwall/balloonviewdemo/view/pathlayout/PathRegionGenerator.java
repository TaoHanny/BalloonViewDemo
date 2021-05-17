package com.instwall.balloonviewdemo.view.pathlayout;

import android.graphics.Path;

public interface PathRegionGenerator {

    PathRegion generatorPathRegion(Path path, int clipType, int width, int height);

}
