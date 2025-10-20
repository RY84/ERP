package ui

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.Timer
import javax.swing.border.EmptyBorder

/**
 * Prosty panel wykonujący płynne cross-fade między dwoma komponentami.
 * Działa bez `pow()` i bez problematycznego konstruktora Timera.
 *
 * Użycie:
 *   val host = CrossfadePanel()
 *   host.showInitial(loginPanel)
 *   host.crossfadeTo(otherPanel, durationMs = 380, fps = 60)
 */
class CrossfadePanel : JPanel(BorderLayout()) {

    private var current: Component? = null

    // Dane animacji
    private var animTimer: Timer? = null
    private var imgFrom: BufferedImage? = null
    private var imgTo: BufferedImage? = null
    private var alpha: Float = 1f

    init {
        isOpaque = false
        border = EmptyBorder(0, 0, 0, 0)
    }

    /** Pokazuje pierwszy, początkowy komponent bez animacji. */
    fun showInitial(comp: Component) {
        stopAnim()
        removeAll()
        current = comp
        add(comp, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    /**
     * Cross-fade do nowego komponentu.
     * @param nextComp   docelowy komponent
     * @param durationMs czas animacji w ms
     * @param fps        klatki na sekundę (max 60)
     */
    fun crossfadeTo(nextComp: Component, durationMs: Int = 380, fps: Int = 60) {
        val fromComp = current
        if (fromComp == null) {
            // nic jeszcze nie było – pokaż po prostu next
            showInitial(nextComp)
            return
        }

        // Złap zrzuty aktualnego i docelowego stanu
        imgFrom = snapshot(fromComp)
        // Wstaw docelowy komponent tymczasowo (niewidoczny), by zrobić zrzut
        add(nextComp, BorderLayout.CENTER)
        layout.layoutContainer(this)
        imgTo = snapshot(nextComp)
        remove(nextComp) // wyjmij – pojawi się po animacji

        // Przygotowanie animacji
        val dt = (1000f / fps.coerceAtMost(60)).toInt().coerceAtLeast(1)
        val totalSteps = (durationMs / dt).coerceAtLeast(1)
        var step = 0
        alpha = 0f

        stopAnim()
        animTimer = Timer(dt) {
            step++
            // ease-out cubic bez pow(): 1 - (1 - t)^3
            val t = (step.toFloat() / totalSteps).coerceIn(0f, 1f)
            alpha = 1f - (1f - t) * (1f - t) * (1f - t)
            repaint()

            if (step >= totalSteps) {
                stopAnim()
                finish(nextComp)
            }
        }.also { it.start() }
    }

    /** Dorysowujemy cross-fade, gdy trwa animacja. */
    override fun paintComponent(g: Graphics) {
        val a = animTimer
        if (a != null && a.isRunning && imgFrom != null && imgTo != null) {
            val g2 = g as Graphics2D
            g2.color = background
            g2.fillRect(0, 0, width, height)

            // Rysuj obraz „from”
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - alpha)
            g2.drawImage(imgFrom, 0, 0, width, height, null)

            // Rysuj obraz „to”
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
            g2.drawImage(imgTo, 0, 0, width, height, null)

        } else {
            super.paintComponent(g)
        }
    }

    // ===== prywatne =====

    private fun stopAnim() {
        animTimer?.stop()
        animTimer = null
        imgFrom = null
        imgTo = null
        alpha = 1f
    }

    private fun finish(nextComp: Component) {
        removeAll()
        current = nextComp
        add(nextComp, BorderLayout.CENTER)
        revalidate()
        repaint()
    }

    private fun snapshot(comp: Component): BufferedImage {
        // Renderuj komponent do obrazu w jego aktualnym rozmiarze
        val w = this.width.coerceAtLeast(1)
        val h = this.height.coerceAtLeast(1)
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        // Narysuj tło hosta + sam komponent
        this.paintAll(g2) // tło panelu
        val off = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB).createGraphics()
        comp.paint(off)
        g2.drawImage((off.deviceConfiguration.createCompatibleImage(w, h, Transparency.TRANSLUCENT)).also { _ ->
            // uproszczenie: rysujemy bezpośrednio w off (nie klonujemy)
        }, 0, 0, null)
        off.dispose()

        // Prostszą i pewniejszą wersję – po prostu paint samego komponentu:
        g2.composite = AlphaComposite.SrcOver
        comp.paint(g2)
        g2.dispose()
        return img
    }
}
