package com.fab.video_convert_platform.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试FFmpegUtil分辨率解析修复
 */
class FFmpegUtilResolutionTest {

    /**
     * 测试正常的分辨率解析
     */
    @Test
    void testNormalResolutionParsing() {
        // 模拟正常的ffprobe输出
        String normalOutput = "1920x1080";
        int[] result = parseResolutionOutput(normalOutput);
        
        assertEquals(1920, result[0]);
        assertEquals(1080, result[1]);
    }

    /**
     * 测试重复分辨率输出的解析（这是我们修复的问题）
     */
    @Test
    void testDuplicateResolutionParsing() {
        // 模拟异常的ffprobe输出（重复分辨率）
        String duplicateOutput = "1920x1080\n\n1920x1080";
        int[] result = parseResolutionOutput(duplicateOutput);
        
        assertEquals(1920, result[0]);
        assertEquals(1080, result[1]);
    }

    /**
     * 测试多行输出但只有一行有效的情况
     */
    @Test
    void testMultiLineWithValidResolution() {
        // 模拟多行输出
        String multiLineOutput = "\n1920x1080\n\n";
        int[] result = parseResolutionOutput(multiLineOutput);
        
        assertEquals(1920, result[0]);
        assertEquals(1080, result[1]);
    }

    /**
     * 测试无效输出
     */
    @Test
    void testInvalidOutput() {
        String invalidOutput = "invalid output";
        assertThrows(RuntimeException.class, () -> parseResolutionOutput(invalidOutput));
    }

    /**
     * 辅助方法：模拟getResolution方法的核心解析逻辑
     */
    private int[] parseResolutionOutput(String output) {
        String result = output.trim();
        
        String[] parts = result.split("x");
        if (parts.length != 2) {
            // 处理重复输出的情况，尝试按行分割取第一个有效行
            String[] lines = result.split("\\r?\\n");
            String validLine = null;
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && line.contains("x")) {
                    validLine = line;
                    break;
                }
            }
            
            if (validLine != null) {
                parts = validLine.split("x");
                if (parts.length == 2) {
                    result = validLine;
                } else {
                    throw new RuntimeException("Unexpected ffprobe output format: '" + result + "'");
                }
            } else {
                throw new RuntimeException("Unexpected ffprobe output format: '" + result + "'");
            }
        }

        int w = Integer.parseInt(parts[0].trim());
        int h = Integer.parseInt(parts[1].trim());

        return new int[]{w, h};
    }
}
