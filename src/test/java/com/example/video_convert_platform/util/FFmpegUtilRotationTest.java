package com.example.video_convert_platform.util;

import com.example.video_convert_platform.util.FFmpegUtil.VideoStreamInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FFmpegUtilRotationTest {

    @Test
    void parseVideoStreamInfo_ShouldUseDisplaySize_WhenRotationIsQuarterTurn() {
        String output = "width=320\nheight=180\nrotation=90";

        VideoStreamInfo info = FFmpegUtil.parseVideoStreamInfo(output);

        assertEquals(320, info.getCodedWidth());
        assertEquals(180, info.getCodedHeight());
        assertEquals(90, info.getRotationDegrees());
        assertArrayEquals(new int[]{180, 320}, info.getDisplayResolution());
        assertTrue(info.hasRotationMetadata());
    }

    @Test
    void parseVideoStreamInfo_ShouldNormalizeLegacyRotateTag() {
        String output = "width=1920\nheight=1080\nTAG:rotate=-90";

        VideoStreamInfo info = FFmpegUtil.parseVideoStreamInfo(output);

        assertEquals(270, info.getRotationDegrees());
        assertArrayEquals(new int[]{1080, 1920}, info.getDisplayResolution());
    }

    @Test
    void orientTargetDimensions_ShouldSwapPresetForPortraitDisplay() {
        int[] target = FFmpegUtil.orientTargetDimensions(640, 360, 1080, 1920);

        assertArrayEquals(new int[]{360, 640}, target);
    }

    @Test
    void computeBoundedDisplayDimensions_ShouldPreservePortraitOrientation() {
        int[] target = FFmpegUtil.computeBoundedDisplayDimensions(2160, 3840, 1920, 1080);

        assertArrayEquals(new int[]{1080, 1920}, target);
    }

    @Test
    void buildScaleFilter_ShouldNormalizeSampleAspectRatio() {
        assertEquals("scale=360:640,setsar=1", FFmpegUtil.buildScaleFilter(360, 640));
    }
}
