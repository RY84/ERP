package ui

import java.awt.*
import javax.swing.JComponent
import javax.swing.Timer
import kotlin.math.PI
import kotlin.math.sin

/**
 * Płynny, lekki „oddech” z poświatą.
 * Nie pokazuje realnego progresu – to indeterminate placeholder.
 */
class GlowBar(
    private val barWidth: Int = 460,
    private val barHeight: Int = 16,
    private val radius: Int = 8
) : JComponent() {

    private var phase = 0.0
    private val fps = 60
    private val timer = Timer((1000.0 / fps).toInt()) {
        phase += 0.06
        if (phase > 2 * PI) phase -= 2 * PI
        repaint()
    }

    init {
        isOpaque = false
        preferredSize = Dimension(barWidth, barHeight)
        maximumSize   = Dimension(barWidth, barHeight)
        minimumSize   = Dimension(barWidth, barHeight)
        timer.start()
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
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val w = width
        val h = height

        // Tło
        g2.color = Theme.panel.darker()
        g2.fillRoundRect(0, 0, w, h, radius, radius)

        // Pasek (pełny – symboliczny)
        g2.color = Theme.accent
        g2.fillRoundRect(0, 0, w, h, radius, radius)

        // Poświata (pulsująca)
        val glowAlpha = (0.25 + 0.25 * (sin(phase) + 1) / 2.0).toFloat() // 0.25..0.5
        val glow = Color(Theme.accent.red, Theme.accent.green, Theme.accent.blue, (glowAlpha * 255).toInt())
        g2.color = glow
        g2.stroke = BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.drawRoundRect(3, 3, w - 6, h - 6, radius, radius)

        // Delikatna ramka
        g2.color = Color(0, 0, 0, 90)
        g2.stroke = BasicStroke(1f)
        g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius)
    }
}
