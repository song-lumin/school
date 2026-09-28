package com.school.lostfound.service;

import com.school.lostfound.service.impl.DifferenceHash;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifferenceHashTest {

    @Test
    void identicalImagesHaveTheSameHash() {
        BufferedImage image = imageWithColumnGradient();

        assertEquals(DifferenceHash.hash(image), DifferenceHash.hash(image));
    }

    @Test
    void smallBrightnessChangeHasSmallHashDistance() {
        BufferedImage original = imageWithColumnGradient();
        BufferedImage changed = imageWithColumnGradient();
        for (int y = 0; y < changed.getHeight(); y++) {
            for (int x = 0; x < changed.getWidth(); x++) {
                Color color = new Color(changed.getRGB(x, y));
                int red = Math.min(255, color.getRed() + 2);
                changed.setRGB(x, y, new Color(red, red, red).getRGB());
            }
        }

        assertTrue(DifferenceHash.distance(
                DifferenceHash.hash(original), DifferenceHash.hash(changed)) <= 4);
    }

    @Test
    void oppositeSolidImagesDifferInAllBits() {
        BufferedImage leftToRight = imageWithColumnGradient(false);
        BufferedImage rightToLeft = imageWithColumnGradient(true);

        assertEquals(64, DifferenceHash.distance(
                DifferenceHash.hash(leftToRight), DifferenceHash.hash(rightToLeft)));
    }

    private BufferedImage imageWithColumnGradient() {
        return imageWithColumnGradient(false);
    }

    private BufferedImage imageWithColumnGradient(boolean reverse) {
        BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int value = reverse ? 230 - x * 10 : x * 10;
                image.setRGB(x, y, new Color(value, value, value).getRGB());
            }
        }
        return image;
    }

}
