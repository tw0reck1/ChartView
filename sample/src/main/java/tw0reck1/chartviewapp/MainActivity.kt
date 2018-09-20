package tw0reck1.chartviewapp

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import tw0reck1.chartview.PieChartView

import java.util.*

/** @author Adrian Tworkowski */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showRandomChart(findViewById(R.id.piechart1), 10)
        showRandomChart(findViewById(R.id.piechart2), 4)
        showRandomChart(findViewById(R.id.piechart3), 6)
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
        for (i in 0..count - 1) {
            colors[i] = getRandomColor(random)
        }

        chart.showChart(data.toTypedArray(), colors.toTypedArray())
    }

    private fun getRandomColor(random: Random) = Color.rgb(random.nextInt(256),
            random.nextInt(256), random.nextInt(256))

}