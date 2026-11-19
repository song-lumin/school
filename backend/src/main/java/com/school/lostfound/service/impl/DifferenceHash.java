/**
 * 图像差异哈希（Difference Hash, dHash）算法工具类。
 *
 * <p>功能：把任意图片压缩成一个 64 位的"指纹"（16 位十六进制字符串），
 * 两张图片指纹的汉明距离越小，图片内容越相似。
 *
 * <p>在系统中的作用：失物招领场景中，同一件失物可能被不同人重复登记。
 * 物品登记时计算图片指纹，若与库内已有物品的指纹距离小于阈值即提示"疑似重复"，
 * 供管理员合并处理，避免同一物品出现多条招领信息。
 *
 * <p>为什么选 dHash 而非 MD5 等摘要算法：MD5 要求两图逐字节相同才相等，
 * 而实际拍照存在缩放、压缩、裁剪差异；dHash 基于"相邻像素亮度相对关系"，
 * 对缩放、轻微压缩、亮度微调具有鲁棒性，是图片相似度比对的经典轻量算法。
 *
 * <p>算法原理（三步）：
 * 1. 缩采样：把图片按 9x8 网格取各单元中心点（共 72 个采样点，省去显式缩放步骤）；
 * 2. 比较：每行 9 个点产生 8 组"左邻 vs 右邻"亮度比较，8 行共 64 位；
 * 3. 编码：左比右亮记 1，否则记 0，按位拼成 64 位整数，格式化为十六进制字符串。
 *
 * <p>设计为 final + 私有构造器：纯静态工具类，禁止实例化和继承。
 */
package com.school.lostfound.service.impl;

import java.awt.image.BufferedImage;

public final class DifferenceHash {
    /** 采样网格宽度取 9：每行 9 个采样点产生 8 组相邻比较（比较次数 = 每行位数） */
    private static final int HASH_WIDTH = 9;
    /** 采样网格高度取 8：8 行 x 每行 8 位 = 64 位哈希，恰好对应一个 long */
    private static final int HASH_HEIGHT = 8;

    /** 私有构造器：工具类不允许被实例化 */
    private DifferenceHash() {
    }

    /**
     * 计算图片的差异哈希指纹（dHash）。
     *
     * @param image 待计算的图片
     * @return 64 位哈希的十六进制字符串（固定 16 个字符，如 "ff00abcd12345678"）
     * @throws IllegalArgumentException 图片为空或尺寸非法
     */
    public static String hash(BufferedImage image) {
        // 防御性校验：空图片或宽高为 0 无法采样
        if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
            throw new IllegalArgumentException("Image is empty");
        }
        long value = 0; // 哈希累加器：64 位，每完成一次比较向左腾出 1 位
        // 外层循环：逐行采样，共 8 行
        for (int y = 0; y < HASH_HEIGHT; y++) {
            // 计算第 y 行采样点的纵坐标：(y + 0.5) * 图高 / 8
            // "+0.5" 让采样点落在第 y 个网格单元的"中心"而非边缘，采样更具代表性；
            // min(...) 钳制索引，防御性保证不越界到最后一行之外
            int sampleY = Math.min(image.getHeight() - 1, (int) ((y + 0.5) * image.getHeight() / HASH_HEIGHT));
            // 内层循环：每行只循环 8 次（HASH_WIDTH - 1），
            // 因为每次迭代同时取"当前点 + 右邻点"，9 个点只需比较 8 次
            for (int x = 0; x < HASH_WIDTH - 1; x++) {
                // 当前（左）采样点的横坐标：取网格单元中心并钳制越界
                int leftX = Math.min(image.getWidth() - 1, (int) ((x + 0.5) * image.getWidth() / HASH_WIDTH));
                // 右邻采样点的横坐标：比 leftX 恰好偏移一个网格宽度
                int rightX = Math.min(image.getWidth() - 1, (int) ((x + 1.5) * image.getWidth() / HASH_WIDTH));
                // 取左右两个像素的感知亮度并比较：左亮于右记 true（本位记 1）
                boolean brighterOnLeft = luminance(image.getRGB(leftX, sampleY)) > luminance(image.getRGB(rightX, sampleY));
                // 位拼接：value 左移 1 位腾出最低位，再把本位结果(0/1)填入最低位；
                // 8 行 x 8 次比较 = 64 次移位拼接，正好填满一个 long
                value = (value << 1) | (brighterOnLeft ? 1L : 0L);
            }
        }
        // %016x：转 16 位十六进制，不足 16 位补前导 0（64 bit = 16 个 hex 字符）
        return String.format("%016x", value);
    }

    /**
     * 计算两个哈希的汉明距离（不同的位数）。
     * 距离越小图片越相似，业务上以阈值（如 <= 10）判定"疑似同一物品"。
     *
     * @param left  16 位十六进制哈希字符串
     * @param right 16 位十六进制哈希字符串
     * @throws IllegalArgumentException 任一参数不是合法的 16 位十六进制串
     */
    public static int distance(String left, String right) {
        // 正则 [0-9a-fA-F]{16}：严格校验两个入参都是 16 位十六进制，防止脏数据入库
        if (left == null || right == null || !left.matches("[0-9a-fA-F]{16}") || !right.matches("[0-9a-fA-F]{16}")) {
            throw new IllegalArgumentException("Expected two 64-bit hexadecimal image hashes");
        }
        // parseUnsignedLong：按无符号数解析。哈希最高位可能是 1，
        // 若用有符号 parseLong，超过 Long.MAX_VALUE 的值会被解析成负数导致运算歧义
        long leftValue = Long.parseUnsignedLong(left, 16);
        long rightValue = Long.parseUnsignedLong(right, 16);
        // 汉明距离经典实现：异或(XOR)后相同位变 0、不同位变 1，
        // bitCount 统计结果中 1 的个数，即为两指纹不同的位数
        return Long.bitCount(leftValue ^ rightValue);
    }

    /**
     * 计算单个像素的感知亮度（灰度化）。
     *
     * <p>使用 ITU-R BT.601 加权公式：人眼对绿色最敏感、红色次之、蓝色最弱，
     * 三个权重 0.299/0.587/0.114 之和约为 1；此处放大 1000 倍用整数运算，避免浮点误差。
     *
     * @param rgb 像素的 ARGB 整数值
     * @return 亮度值（0 ~ 255000，仅用于相对大小比较）
     */
    private static int luminance(int rgb) {
        int red = (rgb >> 16) & 0xff;  // 右移 16 位后取低 8 位 -> 红色分量 (0~255)
        int green = (rgb >> 8) & 0xff; // 右移 8 位后取低 8 位 -> 绿色分量 (0~255)
        int blue = rgb & 0xff;         // 直接取低 8 位 -> 蓝色分量 (0~255)
        return 299 * red + 587 * green + 114 * blue; // 加权求和（权重 x1000）
    }
}
