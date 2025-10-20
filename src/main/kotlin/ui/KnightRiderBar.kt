package ui

import java.awt.*
import java.awt.geom.Point2D
import javax.swing.JComponent
import javax.swing.Timer
import kotlin.math.roundToInt

/**
 * KnightRiderBar – pasek aktywności z "ognistym" efektem reflektora.
 * - pięciosegmentowy gradient (ciemny → jasny → biały → jasny → ciemny)
 * - krawędzie prawie czarne,
 * - płynny bounce L<->P,
 * - mocniejszy glow pod segmentem.
 */
class KnightRiderBar(
    private val barWidth: Int = 460,
    private val barHeight: Int = 16,
    private val chunkWidth: Int = 110,            // szerokość jadącego segmentu
    private val speedPxPerFrame: Int = 9,         // prędkość (px / tick)
    private val cornerRadius: Int = 10,           // zaokrąglenie toru
    private val tickMs: Int = 14                  // ~70 FPS
) : JComponent() {

    // Kolory z motywu
    private val trackColor = Color(0, 0, 0, 150)              // tło toru
    private val trackBorder = Color(0, 0, 0, 210)             // ramka toru
    private val accent = Theme.accent
    private val glowDim = Color(accent.red, accent.green, accent.blue, 60)
    private val glowStrong = Color(accent.red, accent.green, accent.blue, 150)

    // Stan animacji
    private var xPos = 0f
    private var dir = +1                                      // +1 w prawo, -1 w lewo
    private val timer = Timer(tickMs) { tick() }

    init {
        isOpaque = false
        preferredSize = Dimension(barWidth, barHeight + 18)
        minimumSize = preferredSize
        xPos = 0f
        timer.start()
    }

    private fun tick() {
        val max = (barWidth - chunkWidth).toFloat().coerceAtLeast(0f)
        xPos += dir * speedPxPerFrame
        if (xPos <= 0f) { xPos = 0f; dir = +1 }
        if (xPos >= max) { xPos = max; dir = -1 }
        repaint()
    }

    override fun addNotify() {
        super.addNotify()
        if (!timer.isRunning) timer.start()
    }

    override fun removeNotify() {
        timer.stop()
        super.removeNotify()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val w = barWidth
        val h = barHeight

        // Centralne pozycjonowanie toru
        val trackX = (width - w) / 2
        val trackY = (height - h) / 2

        // --- Tło toru ---
        g2.color = trackColor
        g2.fillRoundRect(trackX, trackY, w, h, cornerRadius, cornerRadius)
        g2.color = trackBorder
        g2.drawRoundRect(trackX, trackY, w, h, cornerRadius, cornerRadius)

        // --- Segment jadący ---
        val segX = (trackX + xPos.roundToInt())
        val segY = trackY
        val segW = chunkWidth
        val segH = h

        // Poświata (glow) – mocniejsza i szersza
        val glowH = 18
        val glowY = segY + segH - glowH / 3
        val glowGradient = LinearGradientPaint(
            Point2D.Float(segX.toFloat(), glowY.toFloat()),
            Point2D.Float((segX + segW).toFloat(), glowY.toFloat()),
            floatArrayOf(0f, 0.5f, 1f),
            arrayOf(glowDim, glowStrong, glowDim)
        )
        val oldComp = g2.composite
        g2.composite = AlphaComposite.SrcOver.derive(0.75f)
        g2.paint = glowGradient
        g2.fillRoundRect(segX, glowY, segW, glowH, glowH, glowH)
        g2.composite = oldComp

        // --- Główny gradient "ognisty" ---
        val barGradient = LinearGradientPaint(
            Point2D.Float(segX.toFloat(), segY.toFloat()),
            Point2D.Float((segX + segW).toFloat(), segY.toFloat()),
            floatArrayOf(0f, 0.4f, 0.5f, 0.6f, 1f),
            arrayOf(
                Color(3, 10, 4, 20), // prawie czarny (początek)
                Color(accent.red / 4, accent.green / 4, accent.blue / 4, 150),
                Color(255, 255, 255, 255), // środek – biel / jasny reflektor
                Color(accent.red / 4, accent.green / 4, accent.blue / 4, 150),
                Color(3, 10, 4, 20) // prawie czarny (koniec)
            )
        )
        g2.paint = barGradient
        g2.fillRoundRect(segX, segY, segW, segH, cornerRadius, cornerRadius)

        // Lekki obrys
        g2.color = Color(255, 255, 255, 50)
        g2.drawRoundRect(segX, segY, segW, segH, cornerRadius, cornerRadius)
    }
}
