package com.school.lostfound.service;

import com.school.lostfound.service.impl.DifferenceHash;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 差异哈希算法（DifferenceHash）的单元测试。
 *
 * <p>测试模块：DifferenceHash —— 图片感知哈希工具，用于以图搜图时计算图像指纹
 * （64 位 hash）及两个指纹之间的汉明距离（distance）。</p>
 * <p>覆盖场景：</p>
 * <ul>
 *   <li>同一图片哈希稳定（确定性）</li>
 *   <li>轻微亮度变化只带来很小的汉明距离（感知鲁棒性）</li>
 *   <li>内容完全相反的两张图片距离达到最大值 64（区分度）</li>
 * </ul>
 * <p>方式：纯单元测试，用程序生成的渐变图片作为输入，不依赖外部资源。</p>
 */
class DifferenceHashTest {

    /**
     * 验证同一张图片两次计算得到相同哈希。
     * 预期：两次 hash 结果相等。
     * 为什么：哈希的确定性是一切比较的前提；结果不稳定则搜图完全不可靠。
     */
    @Test
    void identicalImagesHaveTheSameHash() {
        BufferedImage image = imageWithColumnGradient();

        // 同一对象计算两次，结果必须完全一致
        assertEquals(DifferenceHash.hash(image), DifferenceHash.hash(image));
    }

    /**
     * 验证算法对轻微亮度变化的鲁棒性。
     * 预期：全图每个像素仅 +2 亮度后，汉明距离不超过 4（几乎不变的感知内容）。
     * 为什么：真实拍摄/压缩常带来轻微色差，算法必须对这类噪声不敏感。
     */
    @Test
    void smallBrightnessChangeHasSmallHashDistance() {
        BufferedImage original = imageWithColumnGradient();
        BufferedImage changed = imageWithColumnGradient();
        // 把每个像素的亮度提高 2（限制在 255 以内），模拟轻微曝光差异
        for (int y = 0; y < changed.getHeight(); y++) {
            for (int x = 0; x < changed.getWidth(); x++) {
                Color color = new Color(changed.getRGB(x, y));
                int red = Math.min(255, color.getRed() + 2);
                changed.setRGB(x, y, new Color(red, red, red).getRGB());
            }
        }

        // 哈希距离应很小（<=4 位差异），判定为"近似同一张图"
        assertTrue(DifferenceHash.distance(
                DifferenceHash.hash(original), DifferenceHash.hash(changed)) <= 4);
    }

    /**
     * 验证算法的区分度：内容完全相反的图片距离达到最大。
     * 预期：左亮右暗 与 右亮左暗 的渐变图，64 位哈希全部翻转，距离恰为 64。
     * 为什么：与鲁棒性测试互为补充，确认"相似图距离小"不是因为哈希退化成了常数。
     */
    @Test
    void oppositeSolidImagesDifferInAllBits() {
        BufferedImage leftToRight = imageWithColumnGradient(false); // 从左到右变亮
        BufferedImage rightToLeft = imageWithColumnGradient(true);  // 从右到左变亮，方向完全相反

        // 方向相反的渐变在 8x8 网格上的每一对比较都翻转 → 距离为满值 64
        assertEquals(64, DifferenceHash.distance(
                DifferenceHash.hash(leftToRight), DifferenceHash.hash(rightToLeft)));
    }

    // 生成 24x24 的列渐变测试图（默认从左到右变亮）
    private BufferedImage imageWithColumnGradient() {
        return imageWithColumnGradient(false);
    }

    // 生成 24x24 的列渐变图；reverse=true 时方向反转（从右到左变亮）
    private BufferedImage imageWithColumnGradient(boolean reverse) {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int value = reverse ? 230 - x * 10 : x * 10; // 每列灰度递增/递减
                image.setRGB(x, y, new Color(value, value, value).getRGB());
            }
        }
        return image;
    }

}
