package ui.smartpro.coroutinecours

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    //указываем время
    private var formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val scope = CoroutineScope(Job())

    lateinit var job: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRun.setOnClickListener {
            onRun()
        }

        btnRun2.setOnClickListener {
            onRun2()
        }

        btnCancel.setOnClickListener {
            onCancel()
        }
    }
//В корутине запускаем две suspend функции: getData и getData2.
// Первая имитирует выполнение работы в течение 1000 мс и возвращает строку “data”,
// а вторая - в течение 1500 мс и возвращает строку “data2”.
    private fun onRun() {

    scope.launch {
        log("parent coroutine, start")
        val data = getData()
        val data2 = getData2()
        val result = "${data}, ${data2}"
        log("parent coroutine, children returned: $result")
        log("parent coroutine, end")
    }
}
//11:06:35.026 parent coroutine, start [DefaultDispatcher-worker-1]
//11:06:37.543 parent coroutine, children returned: data, data2 [DefaultDispatcher-worker-1]
//11:06:37.546 parent coroutine, end [DefaultDispatcher-worker-1]
//
//По времени в логах видно, что общее время выполнения корутины составило около 2500 мсек.
// Это верно, т.к. suspend функции были вызваны последовательно одна за другой.

    private suspend fun getData(): String{
        delay(1000)
        return "data"
    }

    private suspend fun getData2(): String {
        delay(1500)
        return "data2"
    }

    private fun onRun2(){
        log("onRun2, start")
        job.start()
        log("onRun2, end")
    }

    private fun onCancel(){
        log("onCancel")
        job.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
        scope.cancel()
    }

    private fun log(text: String) {
        Log.d("TAG", "${formatter.format(Date())} $text [${Thread.currentThread().name}]")
    }
}