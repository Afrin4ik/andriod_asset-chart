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

    private lateinit var api: InvestApi // клиент Тинькофф API (будет инициализирован в onCreate)
    private lateinit var database: FirebaseDatabase // ссылка на Firebase Realtime Database
    private lateinit var auth: FirebaseAuth // объект авторизации Firebase

    private fun getCandlesApi(figi: String, interval: CandleInterval): List<Candle> {
        val candlesDay = api.marketDataService
            .getCandlesSync( // возвращает список gRPC‑объектов, которые мы преобразуем в наш data class Candle
                figi,
                Instant.now().minusSeconds(60*60*24*30), // запрашивает свечи за последние 30 дней
                Instant.now(), // метка времени сейчас
                interval
            )
        Log.w("info candles", "получено ${candlesDay.size} 1-дневных свечей для инструмента с figi $figi".format(candlesDay.size, figi))

        return candlesDay.map { it -> Candle(it) }.toList() // преобразует в data class Candle
    }

    @SuppressLint("SetTextI18n")
    private fun getCandles(figi: String, interval: CandleInterval, saveToHistory: Boolean = true) {
        val candlesRef = database.reference.child("Candles").child(figi).child(interval.toString()) // Пытаемся прочитать свечи из Firebase по пути /Candles/{figi}/{interval}

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
        val user = auth.currentUser ?: return // Получаем текущего авторизованного пользователя
        val userId = user.uid // ID текущего пользователя
        val email = user.email ?: "no_email" // email текущего пользователя

        // Базовая ссылка на узел пользователя
        val baseRef = database.reference
            .child("UsersRequests")
            .child(userId) // Добавляется путь UsersRequests/{userId} для хранения запросов пользователя

        baseRef.child("email").setValue(email) // Сохраняется email пользователя в узле email

        // Форматирование даты для ключа запроса
        val now = Date() // Создается текущая дата/время
        val requestDateFormatter = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault()) // Форматируется в строку вида "день-месяц-год_часы-минуты-секунды"
        val requestDateKey = requestDateFormatter.format(now)

        // Ветка запроса
        val requestRef = baseRef
            .child("RequestsHistory")
            .child(requestDateKey) // Создается путь UsersRequests/{userId}/RequestsHistory/{requestDateKey}

        // Преобразуем enum-интервал в строку
        val intervalStr = when (interval) {
            CandleInterval.CANDLE_INTERVAL_HOUR   -> "1 hour"
            CandleInterval.CANDLE_INTERVAL_4_HOUR -> "4 hour"
            CandleInterval.CANDLE_INTERVAL_DAY    -> "1 day"
            CandleInterval.CANDLE_INTERVAL_MONTH  -> "1 month"
            else -> interval.toString()
        }

        requestRef.child("FIGI").setValue(figi) // сохраняем FIGI инструмента
        requestRef.child("Interval").setValue(intervalStr) // сохраняем интервал свечей

        // Сохраняем свечи с читаемыми датами
        val candlesNode = requestRef.child("Candles")

        for (c in candles) {
            val candleTime = c.time * 1000L  // конвертируем секунды в мс (если c.time в секундах)
            val candleDate = Date(candleTime)
            val candleKey = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault()).format(candleDate)

            val data = mapOf(
                "open"   to c.open,
                "close"  to c.close,
                "low"    to c.low,
                "high"   to c.high,
                "volume" to c.volume,
                "timestamp" to c.time  // сохраняем исходный timestamp
            )

            candlesNode.child(candleKey).setValue(data)
        }

        // Обновляем последний запрос LastRequest
        val lastRequestRef = baseRef.child("LastRequest")
        lastRequestRef.child("figi").setValue(figi)
        lastRequestRef.child("interval").setValue(interval)
    }

// нигде не используется (это вспомогательная функция для логирования данных свечи в конслоль Logcat; для валидации свечей)
//    private fun printCandle(candle: HistoricCandle) {
//        val open = quotationToBigDecimal(candle.open)
//        val close = quotationToBigDecimal(candle.close)
//        val high = quotationToBigDecimal(candle.high)
//        val low = quotationToBigDecimal(candle.low)
//        val volume = candle.volume
//        val time = timestampToString(candle.time)
//        Log.w( "candle",
//            "цена открытия: $open, цена закрытия: $close, минимальная цена за 1 лот: $low, максимальная цена за 1 лот: $high, объем "
//                    + "торгов в лотах: $volume, время свечи: $time".format(
//                open,
//                close,
//                low,
//                high,
//                volume,
//                time
//            )
//        )
//        Log.w("info candles", high.toString())
//    }

    // Читает из Firebase последний запрос пользователя и показывает его, если он есть
    private fun onUserChanged() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("UsersRequests").child(userId).child("LastRequest").get().addOnSuccessListener {
            val request = it.getValue(object : GenericTypeIndicator<Request>() {}) ?: return@addOnSuccessListener
            getCandles(request.figi, request.interval, false)
        }
    }

    @SuppressLint("SetTextI18n") // Подавляем предупреждения о конкатенации строк
    // onCreate - ключевой метод жизненного цикла Activity. Система Android вызывает его в момент создания экрана
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // вызывает реализацию базового класса
        setContentView(R.layout.activity_main) // «Надёргивает» XML‑разметку activity_main.xml из папки res/layout и рисует интерфейс на экране

        // Загружаем API token из BuildConfig (установлен в build.gradle.kts из keystore.properties)
        val TOKEN = BuildConfig.TINKOFF_API_TOKEN

        api = InvestApiBuilder().create(TOKEN) // Создаём клиента Tinkoff API
        FirebaseApp.initializeApp(this) // Инициализация firebase
        database = FirebaseDatabase.getInstance("https://assetcharts-default-rtdb.firebaseio.com/") // Получаем инстанс (экземпляр) бд
        auth = FirebaseAuth.getInstance() // Получаем инстанс (экземпляр) авторизации

        val currentUser = auth.currentUser
        if (currentUser == null) { // Если нет залогиненного пользователя, переходим на экран логининга
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.w("user", currentUser?.email ?: "no email")

        // Кнопка "Выйти"
        findViewById<Button>(R.id.sign_out).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Кнопка "Получить график"
        findViewById<Button>(R.id.get).setOnClickListener {
            val figiName = findViewById<EditText>(R.id.figi).text.toString().lowercase()
            val intervalStr = findViewById<EditText>(R.id.interval).text.toString()

            // Проверяем, есть ли такие ключи в ChartsData
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

        onUserChanged() // Загружаем последний запрос пользователя (если был) (автоматически подгружает график последнего запроса при старте)
    }
}