package com.pla.ulartangga

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator

class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Ukuran papan 10x10
    private val cols = 10
    private val rows = 10

    // Listener interaksi user (tekan / geser / usap)
    var onStepRequested: (() -> Unit)? = null

    // Data bidak yang ditampilkan
    var tokens: List<PlayerToken> = emptyList()
        set(value) { field = value; invalidate() }

    var activePlayer: Int = 1
        set(value) { field = value; invalidate() }

    var allowUserStep: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    // Posisi animasi bidak (dalam "satuan kotak", misal 2.5 artinya di antara kotak 2 dan 3)
    private val animPositions = mutableMapOf<Int, Float>()

    private val paintCell = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintSnake = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintLadder = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintToken = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintTokenText = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintGlow = Paint(Paint.ANTI_ALIAS_FLAG)

    private var cellW = 0f
    private var cellH = 0f

    private val cellColors = intArrayOf(
        0xFFFFCDD2.toInt(), 0xFFF8BBD0.toInt(), 0xFFE1BEE7.toInt(), 0xFFD1C4E9.toInt(),
        0xFFBBDEFB.toInt(), 0xFFB3E5FC.toInt(), 0xFFB2DFDB.toInt(), 0xFFC8E6C9.toInt(),
        0xFFFFF9C4.toInt(), 0xFFFFE0B2.toInt()
    )

    private val snakeColors = intArrayOf(
        0xFFE53935.toInt(), 0xFF8E24AA.toInt(), 0xFF00897B.toInt(),
        0xFFF4511E.toInt(), 0xFF3949AB.toInt(), 0xFF43A047.toInt(),
        0xFFD81B60.toInt(), 0xFF6D4C41.toInt(), 0xFF00ACC1.toInt()
    )

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (allowUserStep) {
                val pos = findTokenAt(e.x, e.y)
                if (pos != null || isAnywhereTouch(e.x, e.y)) {
                    onStepRequested?.invoke()
                    return true
                }
            }
            return false
        }

        override fun onFling(
            e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
        ): Boolean {
            if (allowUserStep) {
                onStepRequested?.invoke()
                return true
            }
            return false
        }
    })

    private fun findTokenAt(x: Float, y: Float): Int? {
        // Cek apakah sentuhan di sekitar bidak aktif
        for (t in tokens) {
            if (t.player != activePlayer) continue
            val cx = cellCenterX(t.position)
            val cy = cellCenterY(t.position)
            val dx = x - cx
            val dy = y - cy
            if (dx * dx + dy * dy < (cellW * 0.8f) * (cellW * 0.8f)) return t.player
        }
        return null
    }

    private fun isAnywhereTouch(x: Float, y: Float): Boolean = true // tekan di mana saja = langkah

    init {
        isClickable = true
        paintText.textAlign = Paint.Align.LEFT
        paintText.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paintBorder.style = Paint.Style.STROKE
        paintBorder.strokeWidth = 2f
        paintBorder.color = 0x33000000
        paintSnake.style = Paint.Style.STROKE
        paintSnake.strokeCap = Paint.Cap.ROUND
        paintLadder.style = Paint.Style.STROKE
        paintLadder.strokeCap = Paint.Cap.ROUND
        paintToken.style = Paint.Style.FILL
        paintTokenText.textAlign = Paint.Align.CENTER
        paintTokenText.color = Color.WHITE
        paintTokenText.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paintGlow.style = Paint.Style.FILL
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellW = w.toFloat() / cols
        cellH = h.toFloat() / rows
        paintText.textSize = cellH * 0.22f
        paintTokenText.textSize = cellH * 0.55f
        invalidate()
    }

    // Mapping nomor kotak -> (baris, kolom) dengan pola zigzag klasik
    private fun cellRowCol(n: Int): Pair<Int, Int> {
        val idx = n - 1
        val rowFromBottom = idx / 10
        val colRaw = idx % 10
        val col = if (rowFromBottom % 2 == 0) colRaw else (9 - colRaw)
        val rowFromTop = 9 - rowFromBottom
        return Pair(rowFromTop, col)
    }

    private fun cellLeft(n: Int): Float = cellRowCol(n).second * cellW
    private fun cellTop(n: Int): Float = cellRowCol(n).first * cellH
    private fun cellCenterX(n: Int): Float = cellLeft(n) + cellW / 2f
    private fun cellCenterY(n: Int): Float = cellTop(n) + cellH / 2f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cellW == 0f || cellH == 0f) return

        // 1) gambar kotak 1..100
        for (n in 1..100) {
            val left = cellLeft(n)
            val top = cellTop(n)
            val rect = RectF(left, top, left + cellW, top + cellH)
            paintCell.color = cellColors[(n - 1) % cellColors.size]
            canvas.drawRect(rect, paintCell)
            canvas.drawRect(rect, paintBorder)

            paintText.color = 0xFF3E2723.toInt()
            canvas.drawText(n.toString(), left + cellW * 0.08f, top + cellH * 0.28f, paintText)
        }

        // 2) gambar tangga
        for ((from, to) in SnakeLadderData.ladders) {
            drawLadder(canvas, from, to)
        }

        // 3) gambar ular
        for ((idx, entry) in SnakeLadderData.snakes.entries.withIndex()) {
            drawSnake(canvas, entry.key, entry.value, snakeColors[idx % snakeColors.size])
        }

        // 4) gambar bidak
        drawTokens(canvas)
    }

    private fun drawLadder(canvas: Canvas, from: Int, to: Int) {
        val x1 = cellCenterX(from); val y1 = cellCenterY(from)
        val x2 = cellCenterX(to);   val y2 = cellCenterY(to)
        val dx = x2 - x1; val dy = y2 - y1
        val len = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (len == 0f) return
        val nx = -dy / len; val ny = dx / len
        val w = Math.max(6f, cellW * 0.18f)

        paintLadder.color = 0xFF8D5524.toInt()
        paintLadder.strokeWidth = w * 0.28f
        canvas.drawLine(x1 + nx * w / 2, y1 + ny * w / 2, x2 + nx * w / 2, y2 + ny * w / 2, paintLadder)
        canvas.drawLine(x1 - nx * w / 2, y1 - ny * w / 2, x2 - nx * w / 2, y2 - ny * w / 2, paintLadder)

        paintLadder.color = 0xFFA1887F.toInt()
        paintLadder.strokeWidth = w * 0.22f
        val steps = Math.max(3, (len / (cellW * 0.6f)).toInt())
        for (i in 1 until steps) {
            val t = i.toFloat() / steps
            val cx = x1 + dx * t
            val cy = y1 + dy * t
            canvas.drawLine(cx + nx * w / 2, cy + ny * w / 2, cx - nx * w / 2, cy - ny * w / 2, paintLadder)
        }

        drawBadge(canvas, x2, y2, to.toString(), 0xFF2E7D32.toInt())
    }

    private fun drawSnake(canvas: Canvas, from: Int, to: Int, color: Int) {
        val x1 = cellCenterX(from); val y1 = cellCenterY(from)
        val x2 = cellCenterX(to);   val y2 = cellCenterY(to)
        val mx = (x1 + x2) / 2 + cellW * 0.6f
        val my = (y1 + y2) / 2 - cellH * 0.6f
        val w = Math.max(5f, cellW * 0.2f)

        val path = Path()
        path.moveTo(x1, y1)
        path.quadTo(mx, my, x2, y2)

        // shadow
        paintSnake.color = 0x44000000
        paintSnake.strokeWidth = w + 4f
        val shadowPath = Path(path)
        shadowPath.offset(2f, 3f)
        canvas.drawPath(shadowPath, paintSnake)

        // body
        paintSnake.color = color
        paintSnake.strokeWidth = w
        canvas.drawPath(path, paintSnake)

        // dots
        val dots = 6
        for (i in 1 until dots) {
            val t = i.toFloat() / dots
            val px = (1 - t) * (1 - t) * x1 + 2 * (1 - t) * t * mx + t * t * x2
            val py = (1 - t) * (1 - t) * y1 + 2 * (1 - t) * t * my + t * t * y2
            paintCell.color = 0xFFFFEB3B.toInt()
            canvas.drawCircle(px, py, w * 0.22f, paintCell)
        }

        // head
        paintCell.color = color
        canvas.drawCircle(x1, y1, w * 0.9f,