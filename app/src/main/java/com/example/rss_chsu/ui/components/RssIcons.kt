package com.example.rss_chsu.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object RssIcons {
    val Today: ImageVector by lazy {
        ImageVector.Builder(
            name = "Today",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 3f)
                lineTo(5f, 3f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                lineTo(3f, 19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                lineTo(19f, 21f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                lineTo(21f, 5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
                close()

                moveTo(7f, 7f)
                lineTo(17f, 7f)

                moveTo(7f, 11f)
                lineTo(17f, 11f)

                moveTo(7f, 15f)
                lineTo(13f, 15f)
            }
        }.build()
    }

    val Subscriptions: ImageVector by lazy {
        ImageVector.Builder(
            name = "Subscriptions",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 11f)
                curveTo(8.42f, 11f, 12f, 14.58f, 12f, 19f)

                moveTo(4f, 4f)
                curveTo(12.28f, 4f, 19f, 10.72f, 19f, 19f)
            }
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(5f, 19f)
                curveTo(5.55f, 19f, 6f, 18.55f, 6f, 18f)
                curveTo(6f, 17.45f, 5.55f, 17f, 5f, 17f)
                curveTo(4.45f, 17f, 4f, 17.45f, 4f, 18f)
                curveTo(4f, 18.55f, 4.45f, 19f, 5f, 19f)
                close()
            }
        }.build()
    }

    val Recommendations: ImageVector by lazy {
        ImageVector.Builder(
            name = "Recommendations",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2f)
                lineTo(14.5f, 9.5f)
                lineTo(22f, 12f)
                lineTo(14.5f, 14.5f)
                lineTo(12f, 22f)
                lineTo(9.5f, 14.5f)
                lineTo(2f, 12f)
                lineTo(9.5f, 9.5f)
                close()
            }
        }.build()
    }

    val Bookmark: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bookmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 3f)
                lineTo(7f, 3f)
                curveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f)
                lineTo(5f, 21f)
                lineTo(12f, 17.5f)
                lineTo(19f, 21f)
                lineTo(19f, 5f)
                curveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f)
                close()
            }
        }.build()
    }

    val BookmarkFilled: ImageVector by lazy {
        ImageVector.Builder(
            name = "BookmarkFilled",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(17f, 3f)
                lineTo(7f, 3f)
                curveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f)
                lineTo(5f, 21f)
                lineTo(12f, 17.5f)
                lineTo(19f, 21f)
                lineTo(19f, 5f)
                curveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f)
                close()
            }
        }.build()
    }

    val Folder: ImageVector by lazy {
        ImageVector.Builder(
            name = "Folder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 19f)
                curveTo(22f, 20.1f, 21.1f, 21f, 20f, 21f)
                lineTo(4f, 21f)
                curveTo(2.9f, 21f, 2f, 20.1f, 2f, 19f)
                lineTo(2f, 5f)
                curveTo(2f, 3.9f, 2.9f, 3f, 4f, 3f)
                lineTo(9f, 3f)
                lineTo(11f, 6f)
                lineTo(20f, 6f)
                curveTo(21.1f, 6f, 22f, 6.9f, 22f, 8f)
                close()
            }
        }.build()
    }

    val Tune: ImageVector by lazy {
        ImageVector.Builder(
            name = "Tune",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
                moveTo(4f, 18f)
                lineTo(20f, 18f)

                moveTo(8f, 4f)
                lineTo(8f, 8f)
                moveTo(16f, 10f)
                lineTo(16f, 14f)
                moveTo(10f, 16f)
                lineTo(10f, 20f)
            }
        }.build()
    }
}
