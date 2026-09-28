package com.school.lostfound.service.impl;

import java.awt.image.BufferedImage;

public final class DifferenceHash {
    private static final int HASH_WIDTH = 9;
    private static final int HASH_HEIGHT = 8;

    private DifferenceHash() {
    }

    public static String hash(BufferedImage image) {
        if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
            throw new IllegalArgumentException("Image is empty");
        }
        long value = 0;
        for (int y = 0; y < HASH_HEIGHT; y++) {
            int sampleY = Math.min(image.getHeight() - 1, (int) ((y + 0.5) * image.getHeight() / HASH_HEIGHT));
            for (int x = 0; x < HASH_WIDTH - 1; x++) {
                int leftX = Math.min(image.getWidth() - 1, (int) ((x + 0.5) * image.getWidth() / HASH_WIDTH));
                int rightX = Math.min(image.getWidth() - 1, (int) ((x + 1.5) * image.getWidth() / HASH_WIDTH));
                boolean brighterOnLeft = luminance(image.getRGB(leftX, sampleY)) > luminance(image.getRGB(rightX, sampleY));
                value = (value << 1) | (brighterOnLeft ? 1L : 0L);
            }
        }
        return String.format("%016x", value);
    }

    public static int distance(String left, String right) {
        if (left == null || right == null || !left.matches("[0-9a-fA-F]{16}") || !right.matches("[0-9a-fA-F]{16}")) {
            throw new IllegalArgumentException("Expected two 64-bit hexadecimal image hashes");
        }
        long leftValue = Long.parseUnsignedLong(left, 16);
        long rightValue = Long.parseUnsignedLong(right, 16);
        return Long.bitCount(leftValue ^ rightValue);
    }

    private static int luminance(int rgb) {
        int red = (rgb >> 16) & 0xff;
        int green = (rgb >> 8) & 0xff;
        int blue = rgb & 0xff;
        return 299 * red + 587 * green + 114 * blue;
    }
}
