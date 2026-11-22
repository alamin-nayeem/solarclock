package com.example.solarclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.widget.RemoteViews
import java.util.Calendar
import kotlin.math.*

class ClockWidget : AppWidgetProvider() {
    private val tick = "com.example.solarclock.TICK"

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        val bmp = render(520)
        ids.forEach { id ->
            val v = RemoteViews(ctx.packageName, R.layout.widget)
            v.setImageViewBitmap(R.id.img, bmp)
            mgr.updateAppWidget(id, v)
        }
        val next = Calendar.getInstance().apply { add(Calendar.MINUTE, 1); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .setAndAllowWhileIdle(AlarmManager.RTC, next.timeInMillis, pi(ctx))
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == tick) {
            val m = AppWidgetManager.getInstance(ctx)
            onUpdate(ctx, m, m.getAppWidgetIds(ComponentName(ctx, ClockWidget::class.java)))
        }
    }

    override fun onDisabled(ctx: Context) {
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi(ctx))
    }

    private fun pi(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 0, Intent(ctx, ClockWidget::class.java).setAction(tick),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun render(n: Int): Bitmap {
        val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); val s = n.toFloat(); val m = s / 2
        val cal = Calendar.getInstance()
        val sec = cal.get(Calendar.SECOND) + cal.get(Calendar.MILLISECOND) / 1000f
        val min = cal.get(Calendar.MINUTE) + sec / 60f
        val hr = cal.get(Calendar.HOUR) + min / 60f
        val T = (PI * 2).toFloat()
        val aS = hr / 12 * T; val aE = min / 60 * T; val aM = sec / 60 * T
        val R = s * .27f; val ER = s * .16f; val MR = s * .032f
        fun pt(cx: Float, cy: Float, a: Float, r: Float) = PointF(cx + sin(a) * r, cy - cos(a) * r)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        p.shader = RadialGradient(m, m, m, 0xFF10143A.toInt(), 0xFF05060F.toInt(), Shader.TileMode.CLAMP)
        c.drawCircle(m, m, m, p); p.shader = null

        fun dots(cx: Float, cy: Float, r: Float, col: Int, dr: Float) {
            p.color = col
            for (i in 0 until 60) { val q = pt(cx, cy, i / 60f * T, r); c.drawCircle(q.x, q.y, if (i % 5 == 0) dr * 1.8f else dr, p) }
        }
        dots(m, m, R, 0xBF8F97C2.toInt(), s * .0022f)
        p.color = 0xFFE8ECFF.toInt(); p.style = Paint.Style.STROKE
        for (i in 0 until 12) {
            p.strokeWidth = if (i % 3 == 0) s * .004f else s * .002f
            val a = pt(m, m, i / 12f * T, R); val b = pt(m, m, i / 12f * T, R * .94f)
            c.drawLine(a.x, a.y, b.x, b.y, p)
        }
        p.style = Paint.Style.FILL

        val sun = pt(m, m, aS, R)
        val e0 = pt(0f, 0f, aE, ER); val ex = sun.x + e0.x; val ey = sun.y + e0.y
        val m0 = pt(0f, 0f, aM, MR); val mx = ex + m0.x; val my = ey + m0.y
        dots(sun.x, sun.y, ER, 0xBF78B4FF.toInt(), s * .0022f)
        dots(ex, ey, MR, 0xBFDCDCE6.toInt(), s * .0016f)

        // Sun
        val sr = s * .05f
        p.shader = RadialGradient(sun.x, sun.y, sr * 3, 0x8CFFBE28.toInt(), 0x00FFAA00, Shader.TileMode.CLAMP)
        c.drawCircle(sun.x, sun.y, sr * 3, p)
        p.shader = RadialGradient(sun.x - sr * .3f, sun.y - sr * .3f, sr, 0xFFFFF6B0.toInt(), 0xFFFF9A00.toInt(), Shader.TileMode.CLAMP)
        c.drawCircle(sun.x, sun.y, sr, p); p.shader = null

        fun sphere(x: Float, y: Float, r: Float, earth: Boolean) {
            c.save(); c.clipPath(Path().apply { addCircle(x, y, r, Path.Direction.CW) })
            if (earth) {
                p.color = 0xFF1F5FB0.toInt(); c.drawCircle(x, y, r, p)
                val land = listOf(
                    Pair(floatArrayOf(-.55f, -.35f, .3f, .22f), 0xFF3F8A3C.toInt()),
                    Pair(floatArrayOf(-.5f, .35f, .18f, .35f), 0xFF2F7D38.toInt()),
                    Pair(floatArrayOf(.05f, -.45f, .25f, .15f), 0xFF4D9440.toInt()),
                    Pair(floatArrayOf(.15f, .15f, .25f, .4f), 0xFFB79A52.toInt()),
                    Pair(floatArrayOf(.55f, -.3f, .3f, .2f), 0xFF7DA24A.toInt()))
                land.forEach { (l, col) -> p.color = col; c.drawOval(x + (l[0] - l[2]) * r, y + (l[1] - l[3]) * r, x + (l[0] + l[2]) * r, y + (l[1] + l[3]) * r, p) }
                p.color = 0xFFF4F8FF.toInt()
                c.drawOval(x - r * .6f, y - r * 1.15f, x + r * .6f, y - r * .85f, p)
                c.drawOval(x - r * .6f, y + r * .85f, x + r * .6f, y + r * 1.15f, p)
                p.color = 0x8CFFFFFF.toInt()
                c.drawOval(x - r * .6f, y - r * .1f, x + r * .2f, y + r * .05f, p)
                c.drawOval(x + r * .1f, y + r * .5f, x + r * .8f, y + r * .65f, p)
            } else {
                p.color = 0xFFB4B4BA.toInt(); c.drawCircle(x, y, r, p)
                p.color = 0x70505058.toInt()
                c.drawCircle(x - r * .2f, y - r * .2f, r * .35f, p); c.drawCircle(x + r * .3f, y + r * .2f, r * .25f, p)
            }
            val d = hypot(sun.x - x, sun.y - y).coerceAtLeast(1f)
            val gx = x + (sun.x - x) / d * r * .5f; val gy = y + (sun.y - y) / d * r * .5f
            p.shader = RadialGradient(gx, gy, r * 1.6f,
                intArrayOf(Color.argb(0, 0, 0, 10), Color.argb(38, 0, 0, 10), Color.argb(225, 0, 0, 10)),
                floatArrayOf(.12f, .34f, 1f), Shader.TileMode.CLAMP)
            c.drawRect(x - r, y - r, x + r, y + r, p); p.shader = null
            c.restore()
            if (earth) { p.style = Paint.Style.STROKE; p.strokeWidth = r * .15f; p.color = 0x7378B4FF; c.drawCircle(x, y, r, p); p.style = Paint.Style.FILL }
        }
        sphere(ex, ey, s * .016f, true)
        sphere(mx, my, s * .006f, false)
        return bmp
    }
}
