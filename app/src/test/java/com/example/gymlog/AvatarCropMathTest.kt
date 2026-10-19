package com.example.gymlog

import com.example.gymlog.ui.settings.AvatarCropMath
import com.example.gymlog.ui.settings.AvatarCropTransform
import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarCropMathTest {
    @Test
    fun squareImageAtDefaultScaleUsesTheWholePicture() {
        val rect = AvatarCropMath.sourceRect(
            imageWidth = 1000,
            imageHeight = 1000,
            cropSizePx = 500f,
            transform = AvatarCropTransform(1f, 0f, 0f),
        )
        assertEquals(0f, rect.left, 0.01f)
        assertEquals(0f, rect.top, 0.01f)
        assertEquals(1000f, rect.right, 0.01f)
        assertEquals(1000f, rect.bottom, 0.01f)
    }

    @Test
    fun wideImageStartsOnTheCenterAndPanReachesTheLeftEdge() {
        val center = AvatarCropMath.sourceRect(
            imageWidth = 2000,
            imageHeight = 1000,
            cropSizePx = 500f,
            transform = AvatarCropTransform(1f, 0f, 0f),
        )
        assertEquals(500f, center.left, 0.01f)
        assertEquals(0f, center.top, 0.01f)
        assertEquals(1500f, center.right, 0.01f)

        val left = AvatarCropMath.sourceRect(
            imageWidth = 2000,
            imageHeight = 1000,
            cropSizePx = 500f,
            transform = AvatarCropTransform(1f, 250f, 0f),
        )
        assertEquals(0f, left.left, 0.01f)
        assertEquals(1000f, left.right, 0.01f)
    }

    @Test
    fun zoomKeepsTheCenterAndStaysInsideThePicture() {
        val zoomed = AvatarCropMath.zoomAround(
            imageWidth = 1000,
            imageHeight = 1000,
            cropSizePx = 500f,
            current = AvatarCropTransform(1f, 0f, 0f),
            zoom = 2f,
            panX = 0f,
            panY = 0f,
            focalXFromCenter = 0f,
            focalYFromCenter = 0f,
        )
        assertEquals(2f, zoomed.userScale, 0.01f)
        val rect = AvatarCropMath.sourceRect(1000, 1000, 500f, zoomed)
        assertEquals(250f, rect.left, 0.01f)
        assertEquals(250f, rect.top, 0.01f)
        assertEquals(750f, rect.right, 0.01f)
        assertEquals(750f, rect.bottom, 0.01f)
    }

    @Test
    fun panCannotExposeEmptySpace() {
        val clamped = AvatarCropMath.clamp(
            imageWidth = 1000,
            imageHeight = 1000,
            cropSizePx = 500f,
            userScale = 1f,
            offsetX = 400f,
            offsetY = -400f,
        )
        assertEquals(0f, clamped.offsetX, 0.01f)
        assertEquals(0f, clamped.offsetY, 0.01f)
    }

    @Test
    fun largePhotosAreSampledDownBeforeCropping() {
        assertEquals(1, AvatarCropMath.sampleSize(1000, 800))
        assertEquals(2, AvatarCropMath.sampleSize(4096, 3000))
        assertEquals(4, AvatarCropMath.sampleSize(5000, 4000))
    }
}
