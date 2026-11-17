package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class PosterStylePreset(val label: String, val startHex: Int, val endHex: Int, val textHex: Int) {
    LIQUID_CYAN("Liquid Cyan", 0xFF0A1628.toInt(), 0xFF1D4ED8.toInt(), 0xFFF8FAFC.toInt()),
    FROST_PEARL("iOS Pearl Frost", 0xFFE2EEFF.toInt(), 0xFFFCE7F3.toInt(), 0xFF0F172A.toInt()),
    NEON_VIOLET("Midnight Neon", 0xFF090514.toInt(), 0xFF6D28D9.toInt(), 0xFFFFFFFF.toInt()),
    CRIMSON_VINYL("Crimson Vinyl", 0xFF1F0610.toInt(), 0xFFBE123C.toInt(), 0xFFFFF1F2.toInt())
}

object PosterRenderer {

    suspend fun renderAndSavePoster(
        context: Context,
        title: String,
        artist: String,
        lyricQuote: String,
        preset: PosterStylePreset,
        isStreakPad: Boolean = false,
        streakDays: Int = 0
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val width = 1080
            val height = 1440
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    preset.startHex, preset.endHex, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Liquid Glass translucent inner card
            val cardRect = RectF(72f, 110f, width - 72f, height - 110f)
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (preset == PosterStylePreset.FROST_PEARL) {
                    Color.argb(175, 255, 255, 255)
                } else {
                    Color.argb(85, 255, 255, 255)
                }
            }
            canvas.drawRoundRect(cardRect, 56f, 56f, cardPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                color = Color.argb(150, 255, 255, 255)
            }
            canvas.drawRoundRect(cardRect, 56f, 56f, borderPaint)

            // Decorative vinyl ring
            val ringCenterY = 400f
            val discPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(220, 15, 23, 42)
            }
            canvas.drawCircle(width / 2f, ringCenterY, 175f, discPaint)
            val groovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = Color.argb(95, 56, 189, 248)
            }
            canvas.drawCircle(width / 2f, ringCenterY, 130f, groovePaint)
            canvas.drawCircle(width / 2f, ringCenterY, 92f, groovePaint)
            val centerDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = preset.endHex }
            canvas.drawCircle(width / 2f, ringCenterY, 44f, centerDot)

            val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = preset.textHex
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(200, Color.red(preset.textHex), Color.green(preset.textHex), Color.blue(preset.textHex))
                textSize = 28f
                textAlign = Paint.Align.CENTER
            }

            if (isStreakPad) {
                canvas.drawText("🔥 MUSICA STREAK PAD", width / 2f, 200f, headerPaint)
                val bigStreakPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = preset.textHex
                    textSize = 112f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("$streakDays HARI", width / 2f, 690f, bigStreakPaint)
            } else {
                canvas.drawText("MUSICA • NOW PLAYING", width / 2f, 195f, subHeaderPaint)
            }

            // Song title & artist
            canvas.drawText(title.take(36), width / 2f, 770f, headerPaint)
            canvas.drawText(artist.take(40), width / 2f, 822f, subHeaderPaint)

            // Lyric quote block
            val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = preset.textHex
                textSize = 46f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val quoteText = "“${lyricQuote.ifBlank { "Rasakan irama penuh di Musica Liquid Glass." }}”"
            val staticLayout = StaticLayout.Builder
                .obtain(quoteText, 0, quoteText.length, quotePaint, width - 220)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(12f, 1.1f)
                .build()

            canvas.save()
            canvas.translate(110f, 895f)
            staticLayout.draw(canvas)
            canvas.restore()

            val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(180, Color.red(preset.textHex), Color.green(preset.textHex), Color.blue(preset.textHex))
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Musica • Creator by XenProject", width / 2f, height - 155f, footerPaint)

            val uri = saveBitmapToMediaStore(context, bitmap, "Musica_${System.currentTimeMillis()}.png")
            bitmap.recycle()
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, displayName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Musica")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Gagal membuat entri MediaStore.")
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: throw IllegalStateException("Gagal menulis gambar ke galeri.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        return uri
    }

    fun shareImageOrText(context: Context, imageUri: Uri?, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            if (imageUri != null) {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan ke Platform"))
    }
}
