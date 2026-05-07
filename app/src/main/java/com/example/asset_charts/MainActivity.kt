package com.example.asset_charts

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.CombinedChart
import com.google.firebase.database.GenericTypeIndicator
import ru.tinkoff.piapi.contract.v1.CandleInterval
import ru.tinkoff.piapi.core.InvestApi
import java.time.Instant
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var api: InvestApi
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth

    private fun getCandlesApi(figi: String, interval: CandleInterval): List<Candle> {
        val candlesDay = api.marketDataService
            .getCandlesSync(
                figi,
                Instant.now().minusSeconds(60*60*24*30),
                Instant.now(),
                interval
            )
        Log.w("info candles", "получено ${candlesDay.size} 1-дневных свечей для инструмента с figi $figi".format(candlesDay.size, figi))

        return candlesDay.map { it -> Candle(it) }.toList()
    }

    @SuppressLint("SetTextI18n")
    private fun getCandles(figi: String, interval: CandleInterval, saveToHistory: Boolean = true) {
        val candlesRef = database.reference.child("Candles").child(figi).child(interval.toString())

        candlesRef.get()
            .addOnSuccessListener {
                Log.w("info", "onSuccessListener")
                val cachedCandles = it.getValue(object : GenericTypeIndicator<List<Candle>>() {})
                Log.w("info", cachedCandles.toString())

                val candles = if (cachedCandles.isNullOrEmpty()) {
                    try {
                        getCandlesApi(figi, interval)
                    } catch (e: Exception) {
                        findViewById<TextView>(R.id.result).text = "Error: ${e.message}"
                        Log.e("getCandlesApi", "failed to load candles from API", e)
                        return@addOnSuccessListener
                    }
                } else {
                    cachedCandles
                }

                if (candles.isEmpty()) {
                    findViewById<TextView>(R.id.result).text = "No data for this ticker/interval."
                    Log.w("getCandles", "empty candle list for figi=$figi interval=$interval")
                    return@addOnSuccessListener
                }

                val chart = findViewById<CombinedChart>(R.id.chart)
                drawCandles(candles, chart)

                findViewById<TextView>(R.id.result).text = "Asset chart"

                if (saveToHistory) {
                    logCandlesToFirebase(candles, figi, interval)
                }
            }
            .addOnFailureListener { e ->
                findViewById<TextView>(R.id.result).text = "Error: ${e.message}"
                Log.e("getCandles", "failed to load candles", e)
            }
    }

    private fun logCandlesToFirebase(
        candles: List<Candle>,
        figi: String,
        interval: CandleInterval
    ) {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val email = user.email ?: "no_email"

        val baseRef = database.reference
            .child("UsersRequests")
            .child(userId)

        baseRef.child("email").setValue(email)

        val now = Date()
        val requestDateFormatter = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault())
        val requestDateKey = requestDateFormatter.format(now)

        val requestRef = baseRef
            .child("RequestsHistory")
            .child(requestDateKey)

        val intervalStr = when (interval) {
            CandleInterval.CANDLE_INTERVAL_HOUR   -> "1 hour"
            CandleInterval.CANDLE_INTERVAL_4_HOUR -> "4 hour"
            CandleInterval.CANDLE_INTERVAL_DAY    -> "1 day"
            CandleInterval.CANDLE_INTERVAL_MONTH  -> "1 month"
            else -> interval.toString()
        }

        requestRef.child("FIGI").setValue(figi)
        requestRef.child("Interval").setValue(intervalStr)

        val candlesNode = requestRef.child("Candles")

        for (c in candles) {
            val candleTime = c.time * 1000L
            val candleDate = Date(candleTime)
            val candleKey = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault()).format(candleDate)

            val data = mapOf(
                "open"   to c.open,
                "close"  to c.close,
                "low"    to c.low,
                "high"   to c.high,
                "volume" to c.volume,
                "timestamp" to c.time
            )

            candlesNode.child(candleKey).setValue(data)
        }

        val lastRequestRef = baseRef.child("LastRequest")
        lastRequestRef.child("figi").setValue(figi)
        lastRequestRef.child("interval").setValue(interval)
    }

    private fun onUserChanged() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("UsersRequests").child(userId).child("LastRequest").get().addOnSuccessListener {
            val request = it.getValue(object : GenericTypeIndicator<Request>() {}) ?: return@addOnSuccessListener
            getCandles(request.figi, request.interval, false)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val TOKEN = BuildConfig.TINKOFF_API_TOKEN

        api = InvestApiBuilder().create(TOKEN)
        FirebaseApp.initializeApp(this)
        database = FirebaseDatabase.getInstance("https://assetcharts-default-rtdb.firebaseio.com/")
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.w("user", currentUser?.email ?: "no email")

        findViewById<Button>(R.id.sign_out).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.get).setOnClickListener {
            val figiName = findViewById<EditText>(R.id.figi).text.toString().lowercase()
            val intervalStr = findViewById<EditText>(R.id.interval).text.toString()

            if (!ChartsData.FIGIs.containsKey(figiName) && !ChartsData.TimeFrame.containsKey(intervalStr)) {
                findViewById<TextView>(R.id.result).text = "The ticker and interval are entered incorrectly!"
            } else if (!ChartsData.FIGIs.containsKey(figiName)) {
                findViewById<TextView>(R.id.result).text = "The ticker is entered incorrectly!"
            } else if (!ChartsData.TimeFrame.containsKey(intervalStr)) {
                findViewById<TextView>(R.id.result).text = "The interval is entered incorrectly!"
            } else {
                val figi = ChartsData.FIGIs[figiName]
                val interval = ChartsData.TimeFrame[intervalStr]
                getCandles(figi!!, interval!!)
            }
        }

        onUserChanged()
    }
}