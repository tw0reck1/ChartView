package tw0reck1.chartviewapp

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import tw0reck1.chartview.BarChartView
import tw0reck1.chartview.PieChartView

import java.util.*

/** @author Adrian Tworkowski */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showRandomChart(findViewById<PieChartView>(R.id.piechart1), 10)
        showRandomChart(findViewById<PieChartView>(R.id.piechart2), 4)
        showRandomChart(findViewById<BarChartView>(R.id.barchart1), 10)
    }

    private fun showRandomChart(chart: PieChartView, count: Int) {
        val random = Random()

        val data = FloatArray(count)
        var total = 0f
        var p: Float
        for (i in 0..count - 2) {
            do {
                p = random.nextFloat()
            } while (p + total > 1f || p > 1/(count /2f))

            total += p
            data[i] = p
        }
        data[count - 1] = 1f - total

        val colors = IntArray(count)
        for (i in 0 until count) {
            colors[i] = getRandomColor(random)
        }

        chart.showChart(data.toTypedArray(), colors.toTypedArray())
    }

    private fun showRandomChart(chart: BarChartView, count: Int) {
        val random = Random()

        val data = IntArray(count)
        for (i in 0 until count) {
            data[i] = random.nextInt(251)
        }

        val colors = IntArray(count)
        for (i in 0 until count) {
            colors[i] = getRandomColor(random)
        }

        chart.showChart(data.toTypedArray(), colors.toTypedArray())
    }

    private fun getRandomColor(random: Random) = Color.rgb(random.nextInt(256),
            random.nextInt(256), random.nextInt(256))

}