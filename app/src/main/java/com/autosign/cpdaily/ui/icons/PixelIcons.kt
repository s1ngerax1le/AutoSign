package com.autosign.cpdaily.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 原生 Google Pixel Material Symbols 矢量图标集
 */
object PixelIcons {

    private fun materialIcon(name: String, block: ImageVector.Builder.() -> ImageVector.Builder): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).block().build()
    }

    val Check: ImageVector = materialIcon("Check") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(9.0f, 16.17f)
            lineTo(4.83f, 12.0f)
            lineToRelative(-1.42f, 1.41f)
            lineTo(9.0f, 19.0f)
            lineTo(21.0f, 7.0f)
            lineToRelative(-1.41f, -1.41f)
            close()
        }
    }

    val CheckCircle: ImageVector = materialIcon("CheckCircle") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(10.0f, 17.0f)
            lineToRelative(-5.0f, -5.0f)
            lineToRelative(1.41f, -1.41f)
            lineTo(10.0f, 14.17f)
            lineToRelative(7.59f, -7.59f)
            lineTo(19.0f, 8.0f)
            lineToRelative(-9.0f, 9.0f)
            close()
        }
    }

    val Bolt: ImageVector = materialIcon("Bolt") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(11.0f, 21.0f)
            horizontalLineToRelative(-1.0f)
            lineToRelative(1.0f, -7.0f)
            horizontalLineTo(7.5f)
            curveToRelative(-0.58f, 0.0f, -0.57f, -0.32f, -0.38f, -0.66f)
            lineToRelative(0.19f, -0.34f)
            curveTo(8.48f, 10.94f, 10.42f, 7.54f, 13.0f, 3.0f)
            horizontalLineToRelative(1.0f)
            lineToRelative(-1.0f, 7.0f)
            horizontalLineToRelative(3.5f)
            curveToRelative(0.49f, 0.0f, 0.56f, 0.33f, 0.47f, 0.51f)
            lineToRelative(-0.07f, 0.15f)
            curveTo(12.96f, 17.55f, 11.0f, 21.0f, 11.0f, 21.0f)
            close()
        }
    }

    val Schedule: ImageVector = materialIcon("Schedule") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(11.99f, 2.0f)
            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
            reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
            reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f)
            close()
            moveTo(12.5f, 7.0f)
            horizontalLineTo(11.0f)
            verticalLineToRelative(6.0f)
            lineToRelative(5.25f, 3.15f)
            lineToRelative(0.75f, -1.23f)
            lineToRelative(-4.5f, -2.67f)
            close()
        }
    }

    val ErrorOutline: ImageVector = materialIcon("ErrorOutline") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(11.0f, 15.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-2.0f)
            close()
            moveTo(11.0f, 7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(-2.0f)
            close()
            moveTo(11.99f, 2.0f)
            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
            reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
            close()
            moveTo(12.0f, 20.0f)
            curveToRelative(-4.42f, 0.0f, -8.0f, -3.58f, -8.0f, -8.0f)
            reflectiveCurveToRelative(3.58f, -8.0f, 8.0f, -8.0f)
            reflectiveCurveToRelative(8.0f, 3.58f, 8.0f, 8.0f)
            reflectiveCurveToRelative(-3.58f, 8.0f, -8.0f, 8.0f)
            close()
        }
    }

    val Home: ImageVector = materialIcon("Home") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.0f, 5.69f)
            lineToRelative(5.0f, 4.5f)
            verticalLineTo(18.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(-6.0f)
            horizontalLineTo(9.0f)
            verticalLineToRelative(6.0f)
            horizontalLineTo(7.0f)
            verticalLineToRelative(-7.81f)
            lineToRelative(5.0f, -4.5f)
            moveTo(12.0f, 3.0f)
            lineTo(2.0f, 12.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(6.0f)
            verticalLineToRelative(-6.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(6.0f)
            verticalLineToRelative(-8.0f)
            horizontalLineToRelative(3.0f)
            lineTo(12.0f, 3.0f)
            close()
        }
    }

    val HomeFilled: ImageVector = materialIcon("HomeFilled") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(10.0f, 20.0f)
            verticalLineToRelative(-6.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(5.0f)
            verticalLineToRelative(-8.0f)
            horizontalLineToRelative(3.0f)
            lineTo(12.0f, 3.0f)
            lineTo(2.0f, 12.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(8.0f)
            close()
        }
    }

    val Science: ImageVector = materialIcon("Science") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.8f, 18.4f)
            lineTo(14.0f, 10.67f)
            verticalLineTo(6.5f)
            lineToRelative(1.35f, -1.69f)
            curveTo(15.61f, 4.48f, 15.38f, 4.0f, 14.96f, 4.0f)
            horizontalLineTo(9.04f)
            curveTo(8.62f, 4.0f, 8.39f, 4.48f, 8.65f, 4.81f)
            lineTo(10.0f, 6.5f)
            verticalLineToRelative(4.17f)
            lineTo(4.2f, 18.4f)
            curveTo(3.71f, 19.06f, 4.18f, 20.0f, 5.0f, 20.0f)
            horizontalLineToRelative(14.0f)
            curveTo(19.82f, 20.0f, 20.29f, 19.06f, 19.8f, 18.4f)
            close()
        }
    }

    val Settings: ImageVector = materialIcon("Settings") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19.14f, 12.94f)
            curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
            curveToRelative(0.0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
            lineToRelative(2.03f, -1.58f)
            curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
            lineToRelative(-1.92f, -3.32f)
            curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
            lineToRelative(-2.39f, 0.96f)
            curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
            lineTo(14.4f, 2.81f)
            curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
            horizontalLineToRelative(-3.84f)
            curveToRelative(-0.24f, 0.0f, -0.43f, 0.17f, -0.47f, 0.41f)
            lineTo(9.25f, 5.35f)
            curveTo(8.66f, 5.59f, 8.12f, 5.92f, 7.63f, 6.29f)
            lineTo(5.24f, 5.33f)
            curveToRelative(-0.22f, -0.08f, -0.47f, 0.0f, -0.59f, 0.22f)
            lineTo(2.74f, 8.87f)
            curveTo(2.62f, 9.08f, 2.66f, 9.34f, 2.86f, 9.48f)
            lineToRelative(2.03f, 1.58f)
            curveTo(4.84f, 11.36f, 4.8f, 11.69f, 4.8f, 12.0f)
            reflectiveCurveToRelative(0.02f, 0.64f, 0.07f, 0.94f)
            lineToRelative(-2.03f, 1.58f)
            curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
            lineToRelative(1.92f, 3.32f)
            curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
            lineToRelative(2.39f, -0.96f)
            curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
            lineToRelative(0.36f, 2.54f)
            curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
            horizontalLineToRelative(3.84f)
            curveToRelative(0.24f, 0.0f, 0.44f, -0.17f, 0.47f, -0.41f)
            lineToRelative(0.36f, -2.54f)
            curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
            lineToRelative(2.39f, 0.96f)
            curveToRelative(0.22f, 0.08f, 0.47f, 0.0f, 0.59f, -0.22f)
            lineToRelative(1.92f, -3.32f)
            curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
            lineTo(19.14f, 12.94f)
            close()
            moveTo(12.0f, 15.6f)
            curveToRelative(-1.98f, 0.0f, -3.6f, -1.62f, -3.6f, -3.6f)
            reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
            reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
            reflectiveCurveTo(13.98f, 15.6f, 12.0f, 15.6f)
            close()
        }
    }

    val AccessibilityNew: ImageVector = materialIcon("AccessibilityNew") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20.5f, 6.0f)
            curveToRelative(-2.61f, 0.7f, -5.67f, 1.0f, -8.5f, 1.0f)
            reflectiveCurveToRelative(-5.89f, -0.3f, -8.5f, -1.0f)
            lineTo(3.0f, 8.0f)
            curveToRelative(1.86f, 0.5f, 4.0f, 0.83f, 6.0f, 1.0f)
            verticalLineToRelative(13.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-6.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(9.0f)
            curveToRelative(2.0f, -0.17f, 4.14f, -0.5f, 6.0f, -1.0f)
            lineToRelative(-0.5f, -2.0f)
            close()
            moveTo(12.0f, 6.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
            reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
            close()
        }
    }

    val Security: ImageVector = materialIcon("Security") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.0f, 1.0f)
            lineTo(3.0f, 5.0f)
            verticalLineToRelative(6.0f)
            curveToRelative(0.0f, 5.55f, 3.84f, 10.74f, 9.0f, 12.0f)
            curveToRelative(5.16f, -1.26f, 9.0f, -6.45f, 9.0f, -12.0f)
            verticalLineTo(5.0f)
            lineToRelative(-9.0f, -4.0f)
            close()
            moveTo(12.0f, 11.99f)
            horizontalLineToRelative(7.0f)
            curveToRelative(-0.53f, 4.12f, -3.28f, 7.79f, -7.0f, 8.94f)
            verticalLineTo(12.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(6.3f)
            lineToRelative(7.0f, -3.11f)
            verticalLineToRelative(8.8f)
            close()
        }
    }

    val Terminal: ImageVector = materialIcon("Terminal") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20.0f, 4.0f)
            horizontalLineTo(4.0f)
            curveTo(2.89f, 4.0f, 2.0f, 4.9f, 2.0f, 6.0f)
            verticalLineToRelative(12.0f)
            curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(16.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(6.0f)
            curveTo(22.0f, 4.9f, 21.11f, 4.0f, 20.0f, 4.0f)
            close()
            moveTo(20.0f, 18.0f)
            horizontalLineTo(4.0f)
            verticalLineTo(8.0f)
            horizontalLineToRelative(16.0f)
            verticalLineTo(18.0f)
            close()
            moveTo(18.0f, 17.0f)
            horizontalLineToRelative(-6.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(6.0f)
            verticalLineTo(17.0f)
            close()
            moveTo(7.5f, 17.0f)
            lineToRelative(-1.41f, -1.41f)
            lineTo(8.67f, 13.0f)
            lineToRelative(-2.59f, -2.59f)
            lineTo(7.5f, 9.0f)
            lineToRelative(4.0f, 4.0f)
            lineTo(7.5f, 17.0f)
            close()
        }
    }

    val RocketLaunch: ImageVector = materialIcon("RocketLaunch") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.0f, 2.5f)
            curveToRelative(0.0f, 0.0f, 4.5f, 2.04f, 4.5f, 10.5f)
            curveToRelative(0.0f, 2.49f, -1.04f, 5.57f, -1.6f, 7.0f)
            horizontalLineTo(9.1f)
            curveToRelative(-0.56f, -1.43f, -1.6f, -4.51f, -1.6f, -7.0f)
            curveTo(7.5f, 4.54f, 12.0f, 2.5f, 12.0f, 2.5f)
            close()
            moveTo(14.0f, 11.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
            reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
            reflectiveCurveTo(14.0f, 12.1f, 14.0f, 11.0f)
            close()
            moveTo(7.69f, 20.52f)
            curveToRelative(-0.48f, -1.23f, -1.52f, -4.17f, -1.67f, -6.87f)
            lineTo(4.0f, 15.0f)
            verticalLineToRelative(5.0f)
            lineTo(7.69f, 20.52f)
            close()
            moveTo(20.0f, 20.0f)
            verticalLineToRelative(-5.0f)
            lineToRelative(-2.02f, -1.35f)
            curveToRelative(-0.15f, 2.7f, -1.2f, 5.64f, -1.67f, 6.87f)
            lineTo(20.0f, 20.0f)
            close()
        }
    }

    val ScreenLockPortrait: ImageVector = materialIcon("ScreenLockPortrait") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(10.0f, 16.0f)
            horizontalLineToRelative(4.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
            verticalLineToRelative(-3.0f)
            curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
            verticalLineToRelative(-1.0f)
            curveToRelative(0.0f, -1.11f, -0.9f, -2.0f, -2.0f, -2.0f)
            curveToRelative(-1.11f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(1.0f)
            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
            verticalLineToRelative(3.0f)
            curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
            close()
            moveTo(10.8f, 10.0f)
            curveToRelative(0.0f, -0.66f, 0.54f, -1.2f, 1.2f, -1.2f)
            reflectiveCurveToRelative(1.2f, 0.54f, 1.2f, 1.2f)
            verticalLineToRelative(1.0f)
            horizontalLineToRelative(-2.4f)
            verticalLineToRelative(-1.0f)
            close()
            moveTo(17.0f, 1.0f)
            horizontalLineTo(7.0f)
            curveTo(5.9f, 1.0f, 5.0f, 1.9f, 5.0f, 3.0f)
            verticalLineToRelative(18.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(10.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(3.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(17.0f, 19.0f)
            horizontalLineTo(7.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(10.0f)
            verticalLineToRelative(14.0f)
            close()
        }
    }

    val StopCircle: ImageVector = materialIcon("StopCircle") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            curveToRelative(0.0f, 5.52f, 4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            curveTo(22.0f, 6.48f, 17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(16.0f, 16.0f)
            horizontalLineTo(8.0f)
            verticalLineTo(8.0f)
            horizontalLineToRelative(8.0f)
            verticalLineTo(16.0f)
            close()
        }
    }

    val ContentCopy: ImageVector = materialIcon("ContentCopy") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(16.0f, 1.0f)
            horizontalLineTo(4.0f)
            curveTo(2.9f, 1.0f, 2.0f, 1.9f, 2.0f, 3.0f)
            verticalLineToRelative(14.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(3.0f)
            horizontalLineToRelative(12.0f)
            verticalLineTo(1.0f)
            close()
            moveTo(19.0f, 5.0f)
            horizontalLineTo(8.0f)
            curveTo(6.9f, 5.0f, 6.0f, 5.9f, 6.0f, 7.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(11.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(7.0f)
            curveTo(21.0f, 5.9f, 20.1f, 5.0f, 19.0f, 5.0f)
            close()
            moveTo(19.0f, 21.0f)
            horizontalLineTo(8.0f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(11.0f)
            verticalLineTo(21.0f)
            close()
        }
    }

    val Share: ImageVector = materialIcon("Share") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(18.0f, 16.08f)
            curveToRelative(-0.76f, 0.0f, -1.44f, 0.3f, -1.96f, 0.77f)
            lineTo(8.91f, 12.7f)
            curveToRelative(0.05f, -0.23f, 0.09f, -0.46f, 0.09f, -0.7f)
            reflectiveCurveToRelative(-0.04f, -0.47f, -0.09f, -0.7f)
            lineToRelative(7.05f, -4.11f)
            curveToRelative(0.54f, 0.5f, 1.25f, 0.81f, 2.04f, 0.81f)
            curveToRelative(1.66f, 0.0f, 3.0f, -1.34f, 3.0f, -3.0f)
            reflectiveCurveToRelative(-1.34f, -3.0f, -3.0f, -3.0f)
            reflectiveCurveToRelative(-3.0f, 1.34f, -3.0f, 3.0f)
            curveToRelative(0.0f, 0.24f, 0.04f, 0.47f, 0.09f, 0.7f)
            lineTo(8.04f, 9.81f)
            curveTo(7.5f, 9.31f, 6.79f, 9.0f, 6.0f, 9.0f)
            curveToRelative(-1.66f, 0.0f, -3.0f, 1.34f, -3.0f, 3.0f)
            reflectiveCurveToRelative(1.34f, 3.0f, 3.0f, 3.0f)
            curveToRelative(0.79f, 0.0f, 1.5f, -0.31f, 2.04f, -0.81f)
            lineToRelative(7.12f, 4.16f)
            curveToRelative(-0.05f, 0.21f, -0.08f, 0.43f, -0.08f, 0.65f)
            curveToRelative(0.0f, 1.61f, 1.31f, 2.92f, 2.92f, 2.92f)
            curveToRelative(1.61f, 0.0f, 2.92f, -1.31f, 2.92f, -2.92f)
            reflectiveCurveTo(19.61f, 16.08f, 18.0f, 16.08f)
            close()
        }
    }

    val DeleteSweep: ImageVector = materialIcon("DeleteSweep") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(15.0f, 16.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-4.0f)
            close()
            moveTo(15.0f, 8.0f)
            horizontalLineToRelative(7.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-7.0f)
            close()
            moveTo(15.0f, 12.0f)
            horizontalLineToRelative(6.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(-6.0f)
            close()
            moveTo(3.0f, 18.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(6.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(8.0f)
            horizontalLineTo(3.0f)
            verticalLineToRelative(10.0f)
            close()
            moveTo(14.0f, 5.0f)
            horizontalLineToRelative(-3.0f)
            lineToRelative(-1.0f, -1.0f)
            horizontalLineTo(6.0f)
            lineTo(5.0f, 5.0f)
            horizontalLineTo(2.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(12.0f)
            close()
        }
    }

    val Pin: ImageVector = materialIcon("Pin") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(18.0f, 8.0f)
            horizontalLineToRelative(-1.0f)
            verticalLineTo(6.0f)
            curveToRelative(0.0f, -2.76f, -2.24f, -5.0f, -5.0f, -5.0f)
            reflectiveCurveTo(7.0f, 3.24f, 7.0f, 6.0f)
            verticalLineToRelative(2.0f)
            horizontalLineTo(6.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(10.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(12.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(10.0f)
            curveTo(20.0f, 8.9f, 19.1f, 8.0f, 18.0f, 8.0f)
            close()
            moveTo(12.0f, 17.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
            reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
            reflectiveCurveToRelative(2.0f, 0.9f, 2.0f, 2.0f)
            reflectiveCurveTo(13.1f, 17.0f, 12.0f, 17.0f)
            close()
            moveTo(15.1f, 8.0f)
            horizontalLineTo(8.9f)
            verticalLineTo(6.0f)
            curveToRelative(0.0f, -1.71f, 1.39f, -3.1f, 3.1f, -3.1f)
            reflectiveCurveToRelative(3.1f, 1.39f, 3.1f, 3.1f)
            verticalLineTo(8.0f)
            close()
        }
    }

    val Replay: ImageVector = materialIcon("Replay") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.0f, 5.0f)
            verticalLineTo(1.0f)
            lineTo(7.0f, 6.0f)
            lineToRelative(5.0f, 5.0f)
            verticalLineTo(7.0f)
            curveToRelative(3.31f, 0.0f, 6.0f, 2.69f, 6.0f, 6.0f)
            reflectiveCurveToRelative(-2.69f, 6.0f, -6.0f, 6.0f)
            reflectiveCurveToRelative(-6.0f, -2.69f, -6.0f, -6.0f)
            horizontalLineTo(4.0f)
            curveToRelative(0.0f, 4.42f, 3.58f, 8.0f, 8.0f, 8.0f)
            reflectiveCurveToRelative(8.0f, -3.58f, 8.0f, -8.0f)
            reflectiveCurveTo(16.42f, 5.0f, 12.0f, 5.0f)
            close()
        }
    }

    val BatteryChargingFull: ImageVector = materialIcon("BatteryChargingFull") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(15.67f, 4.0f)
            horizontalLineTo(14.0f)
            verticalLineTo(2.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineTo(8.33f)
            curveTo(7.6f, 4.0f, 7.0f, 4.6f, 7.0f, 5.33f)
            verticalLineToRelative(15.33f)
            curveTo(7.0f, 21.4f, 7.6f, 22.0f, 8.33f, 22.0f)
            horizontalLineToRelative(7.33f)
            curveToRelative(0.74f, 0.0f, 1.34f, -0.6f, 1.34f, -1.33f)
            verticalLineTo(5.33f)
            curveTo(17.0f, 4.6f, 16.4f, 4.0f, 15.67f, 4.0f)
            close()
            moveTo(11.0f, 20.0f)
            verticalLineToRelative(-5.5f)
            horizontalLineTo(9.0f)
            lineTo(13.0f, 7.0f)
            verticalLineToRelative(5.5f)
            horizontalLineToRelative(2.0f)
            lineTo(11.0f, 20.0f)
            close()
        }
    }

    val Language: ImageVector = materialIcon("Language") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(11.99f, 2.0f)
            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
            reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
            close()
            moveTo(18.92f, 8.0f)
            horizontalLineToRelative(-2.95f)
            curveToRelative(-0.32f, -1.25f, -0.78f, -2.45f, -1.38f, -3.56f)
            curveTo(16.43f, 5.07f, 17.96f, 6.35f, 18.92f, 8.0f)
            close()
            moveTo(12.0f, 4.04f)
            curveToRelative(0.83f, 1.2f, 1.48f, 2.53f, 1.91f, 3.96f)
            horizontalLineToRelative(-3.82f)
            curveTo(10.52f, 6.57f, 11.17f, 5.24f, 12.0f, 4.04f)
            close()
            moveTo(4.26f, 14.0f)
            curveTo(4.1f, 13.36f, 4.0f, 12.69f, 4.0f, 12.0f)
            reflectiveCurveToRelative(0.1f, -1.36f, 0.26f, -2.0f)
            horizontalLineToRelative(3.38f)
            curveToRelative(-0.08f, 0.66f, -0.14f, 1.32f, -0.14f, 2.0f)
            reflectiveCurveToRelative(0.06f, 1.34f, 0.14f, 2.0f)
            horizontalLineTo(4.26f)
            close()
            moveTo(5.08f, 16.0f)
            horizontalLineToRelative(2.95f)
            curveToRelative(0.32f, 1.25f, 0.78f, 2.45f, 1.38f, 3.56f)
            curveTo(7.57f, 18.93f, 6.04f, 17.65f, 5.08f, 16.0f)
            close()
            moveTo(8.03f, 8.0f)
            horizontalLineTo(5.08f)
            curveToRelative(0.96f, -1.65f, 2.49f, -2.93f, 4.33f, -3.56f)
            curveTo(8.81f, 5.55f, 8.35f, 6.75f, 8.03f, 8.0f)
            close()
            moveTo(12.0f, 19.96f)
            curveToRelative(-0.83f, -1.2f, -1.48f, -2.53f, -1.91f, -3.96f)
            horizontalLineToRelative(3.82f)
            curveTo(13.48f, 17.43f, 12.83f, 18.76f, 12.0f, 19.96f)
            close()
            moveTo(14.34f, 14.0f)
            horizontalLineTo(9.66f)
            curveToRelative(-0.09f, -0.66f, -0.16f, -1.32f, -0.16f, -2.0f)
            reflectiveCurveToRelative(0.07f, -1.35f, 0.16f, -2.0f)
            horizontalLineToRelative(4.68f)
            curveToRelative(0.09f, 0.65f, 0.16f, 1.32f, 0.16f, 2.0f)
            reflectiveCurveToRelative(-0.07f, 1.34f, -0.16f, 2.0f)
            close()
            moveTo(14.59f, 19.56f)
            curveToRelative(0.6f, -1.11f, 1.06f, -2.31f, 1.38f, -3.56f)
            horizontalLineToRelative(2.95f)
            curveTo(17.96f, 17.65f, 16.43f, 18.93f, 14.59f, 19.56f)
            close()
            moveTo(16.36f, 14.0f)
            curveToRelative(0.08f, -0.66f, 0.14f, -1.32f, 0.14f, -2.0f)
            reflectiveCurveToRelative(-0.06f, -1.34f, -0.14f, -2.0f)
            horizontalLineToRelative(3.38f)
            curveToRelative(0.16f, 0.64f, 0.26f, 1.31f, 0.26f, 2.0f)
            reflectiveCurveToRelative(-0.1f, 1.36f, -0.26f, 2.0f)
            horizontalLineTo(16.36f)
            close()
        }
    }

    val ChevronRight: ImageVector = materialIcon("ChevronRight") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(10.0f, 6.0f)
            lineTo(8.59f, 7.41f)
            lineTo(13.17f, 12.0f)
            lineToRelative(-4.58f, 4.59f)
            lineTo(10.0f, 18.0f)
            lineToRelative(6.0f, -6.0f)
            close()
        }
    }

    val ArrowBack: ImageVector = materialIcon("ArrowBack") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20.0f, 11.0f)
            horizontalLineTo(7.83f)
            lineToRelative(5.59f, -5.59f)
            lineTo(12.0f, 4.0f)
            lineToRelative(-8.0f, 8.0f)
            lineToRelative(8.0f, 8.0f)
            lineToRelative(1.41f, -1.41f)
            lineTo(7.83f, 13.0f)
            horizontalLineTo(20.0f)
            verticalLineToRelative(-2.0f)
            close()
        }
    }

    val Smartphone: ImageVector = materialIcon("Smartphone") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(17.0f, 1.01f)
            lineTo(7.0f, 1.0f)
            curveTo(5.9f, 1.0f, 5.0f, 1.9f, 5.0f, 3.0f)
            verticalLineToRelative(18.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(10.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineTo(3.0f)
            curveTo(19.0f, 1.9f, 18.1f, 1.01f, 17.0f, 1.01f)
            close()
            moveTo(17.0f, 19.0f)
            horizontalLineTo(7.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(10.0f)
            verticalLineTo(19.0f)
            close()
        }
    }

    val Android: ImageVector = materialIcon("Android") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(17.6f, 9.48f)
            lineToRelative(1.84f, -3.18f)
            curveToRelative(0.16f, -0.31f, 0.04f, -0.69f, -0.26f, -0.85f)
            curveToRelative(-0.29f, -0.15f, -0.65f, -0.06f, -0.83f, 0.22f)
            lineToRelative(-1.88f, 3.24f)
            curveToRelative(-2.86f, -1.21f, -6.08f, -1.21f, -8.94f, 0.0f)
            lineTo(5.65f, 5.67f)
            curveToRelative(-0.19f, -0.29f, -0.58f, -0.38f, -0.87f, -0.2f)
            curveTo(4.5f, 5.65f, 4.41f, 6.01f, 4.56f, 6.3f)
            lineToRelative(1.84f, 3.18f)
            curveTo(3.55f, 11.16f, 1.68f, 14.07f, 1.5f, 17.5f)
            horizontalLineToRelative(21.0f)
            curveTo(22.32f, 14.07f, 20.45f, 11.16f, 17.6f, 9.48f)
            close()
            moveTo(7.0f, 15.25f)
            curveToRelative(-0.69f, 0.0f, -1.25f, -0.56f, -1.25f, -1.25f)
            reflectiveCurveToRelative(0.56f, -1.25f, 1.25f, -1.25f)
            reflectiveCurveToRelative(1.25f, 0.56f, 1.25f, 1.25f)
            reflectiveCurveTo(7.69f, 15.25f, 7.0f, 15.25f)
            close()
            moveTo(17.0f, 15.25f)
            curveToRelative(-0.69f, 0.0f, -1.25f, -0.56f, -1.25f, -1.25f)
            reflectiveCurveToRelative(0.56f, -1.25f, 1.25f, -1.25f)
            reflectiveCurveToRelative(1.25f, 0.56f, 1.25f, 1.25f)
            reflectiveCurveTo(17.69f, 15.25f, 17.0f, 15.25f)
            close()
        }
    }

    val Fingerprint: ImageVector = materialIcon("Fingerprint") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(17.81f, 4.47f)
            curveToRelative(-0.08f, 0.0f, -0.16f, -0.02f, -0.23f, -0.06f)
            curveTo(15.66f, 3.42f, 14.0f, 3.0f, 12.01f, 3.0f)
            curveToRelative(-1.98f, 0.0f, -3.86f, 0.47f, -5.57f, 1.41f)
            curveToRelative(-0.24f, 0.13f, -0.54f, 0.04f, -0.68f, -0.2f)
            curveToRelative(-0.13f, -0.24f, -0.04f, -0.55f, 0.2f, -0.68f)
            curveTo(7.82f, 2.52f, 9.86f, 2.0f, 12.01f, 2.0f)
            curveToRelative(2.13f, 0.0f, 3.99f, 0.47f, 6.03f, 1.52f)
            curveToRelative(0.25f, 0.13f, 0.34f, 0.43f, 0.21f, 0.67f)
            curveToRelative(-0.09f, 0.18f, -0.26f, 0.28f, -0.44f, 0.28f)
            close()
            moveTo(12.0f, 6.0f)
            curveToRelative(-3.31f, 0.0f, -6.0f, 2.69f, -6.0f, 6.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-4.0f)
            curveToRelative(0.0f, -2.21f, 1.79f, -4.0f, 4.0f, -4.0f)
            reflectiveCurveToRelative(4.0f, 1.79f, 4.0f, 4.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-4.0f)
            curveToRelative(0.0f, -3.31f, -2.69f, -6.0f, -6.0f, -6.0f)
            close()
            moveTo(12.0f, 10.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(8.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-8.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(6.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(-6.0f)
            curveToRelative(0.0f, -2.21f, -1.79f, -4.0f, -4.0f, -4.0f)
            close()
        }
    }

    val Tune: ImageVector = materialIcon("Tune") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3.0f, 17.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(6.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(3.0f)
            close()
            moveTo(3.0f, 5.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(10.0f)
            verticalLineTo(5.0f)
            horizontalLineTo(3.0f)
            close()
            moveTo(13.0f, 21.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-8.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(6.0f)
            horizontalLineTo(13.0f)
            close()
            moveTo(7.0f, 9.0f)
            verticalLineToRelative(2.0f)
            horizontalLineTo(3.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(9.0f)
            horizontalLineTo(7.0f)
            close()
            moveTo(21.0f, 13.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(11.0f)
            verticalLineToRelative(2.0f)
            horizontalLineTo(21.0f)
            close()
            moveTo(15.0f, 9.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(4.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineTo(3.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(9.0f)
            close()
        }
    }

    val Info: ImageVector = materialIcon("Info") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12.0f, 2.0f)
            curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
            reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f)
            reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f)
            close()
            moveTo(13.0f, 17.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(-6.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(6.0f)
            close()
            moveTo(13.0f, 9.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineTo(7.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(2.0f)
            close()
        }
    }

    val Videocam: ImageVector = materialIcon("Videocam") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(17.0f, 10.5f)
            verticalLineTo(7.0f)
            curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
            horizontalLineTo(4.0f)
            curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
            verticalLineToRelative(10.0f)
            curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
            horizontalLineToRelative(12.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
            verticalLineToRelative(-3.5f)
            lineToRelative(4.0f, 4.0f)
            verticalLineToRelative(-11.0f)
            lineToRelative(-4.0f, 4.0f)
            close()
        }
    }
}
