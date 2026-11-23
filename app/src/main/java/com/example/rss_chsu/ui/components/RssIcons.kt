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

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Center hole
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 15f)
                curveTo(13.6569f, 15f, 15f, 13.6569f, 15f, 12f)
                curveTo(15f, 10.3431f, 13.6569f, 9f, 12f, 9f)
                curveTo(10.3431f, 9f, 9f, 10.3431f, 9f, 12f)
                curveTo(9f, 13.6569f, 10.3431f, 15f, 12f, 15f)
                close()
            }
            // Gear outline
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.14f, 12.94f)
                curveTo(19.18f, 12.63f, 19.2f, 12.32f, 19.2f, 12f)
                curveTo(19.2f, 11.68f, 19.18f, 11.37f, 19.14f, 11.06f)
                lineTo(21.16f, 9.48f)
                curveTo(21.34f, 9.34f, 21.39f, 9.08f, 21.28f, 8.87f)
                lineTo(19.36f, 5.55f)
                curveTo(19.24f, 5.34f, 18.99f, 5.26f, 18.77f, 5.34f)
                lineTo(16.39f, 6.3f)
                curveTo(15.9f, 5.92f, 15.37f, 5.61f, 14.8f, 5.37f)
                lineTo(14.44f, 2.84f)
                curveTo(14.4f, 2.61f, 14.2f, 2.44f, 13.97f, 2.44f)
                lineTo(10.03f, 2.44f)
                curveTo(9.8f, 2.44f, 9.6f, 2.61f, 9.56f, 2.84f)
                lineTo(9.2f, 5.37f)
                curveTo(8.63f, 5.61f, 8.1f, 5.92f, 7.61f, 6.3f)
                lineTo(5.23f, 5.34f)
                curveTo(5.01f, 5.26f, 4.76f, 5.34f, 4.64f, 5.55f)
                lineTo(2.72f, 8.87f)
                curveTo(2.61f, 9.08f, 2.66f, 9.34f, 2.84f, 9.48f)
                lineTo(4.86f, 11.06f)
                curveTo(4.82f, 11.37f, 4.8f, 11.69f, 4.8f, 12f)
                curveTo(4.8f, 12.31f, 4.82f, 12.63f, 4.86f, 12.94f)
                lineTo(2.84f, 14.52f)
                curveTo(2.66f, 14.66f, 2.61f, 14.92f, 2.72f, 15.13f)
                lineTo(4.64f, 18.45f)
                curveTo(4.76f, 18.66f, 5.01f, 18.74f, 5.23f, 18.66f)
                lineTo(7.61f, 17.7f)
                curveTo(8.1f, 18.08f, 8.63f, 18.39f, 9.2f, 18.63f)
                lineTo(9.56f, 21.16f)
                curveTo(9.6f, 21.39f, 9.8f, 21.56f, 10.03f, 21.56f)
                lineTo(13.97f, 21.56f)
                curveTo(14.2f, 21.56f, 14.4f, 21.39f, 14.44f, 21.16f)
                lineTo(14.8f, 18.63f)
                curveTo(15.37f, 18.39f, 15.9f, 18.08f, 16.39f, 17.7f)
                lineTo(18.77f, 18.66f)
                curveTo(18.99f, 18.74f, 19.24f, 18.66f, 19.36f, 18.45f)
                lineTo(21.28f, 15.13f)
                curveTo(21.39f, 14.92f, 21.34f, 14.66f, 21.16f, 14.52f)
                lineTo(19.14f, 12.94f)
                close()
            }
        }.build()
    }
}
