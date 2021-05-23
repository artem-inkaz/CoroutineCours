package ui.smartpro.coroutinecours

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

        btnCancel.setOnClickListener {
            onCancel()
        }
    }

    private fun onRun(){
        log("onRun, start")

        scope.launch {
            log("coroutine, start")
            TimeUnit.MILLISECONDS.sleep(1000)
            log("coroutine, end")
        }

        log("onRun, middle")

        scope.launch {
            log("coroutine2, start")
            TimeUnit.MILLISECONDS.sleep(1500)
            log("coroutine2, end")
        }

        log("onRun, end")
    }
//Метод onRun быстро выполнился, создав и отправив на выполнение пару корутин.
// Корутины стартовали практически одновременно, в отдельных потоках сделали свою работу и завершились.
// Между собой они никак не связаны.
//
//Эти примеры показывают, что билдер launch быстро выполняется, не блокируя и не задерживая основной поток.
// А корутины уходят в отдельный поток и выполняются там.
//11:07:54.350 onRun, start [main]
//11:07:54.351 onRun, middle [main]
//11:07:54.352 onRun, end [main]
//11:07:54.352 coroutine2, start [DefaultDispatcher-worker-4]
//11:07:54.354 coroutine, start [DefaultDispatcher-worker-1]
//11:07:55.355 coroutine, end [DefaultDispatcher-worker-1]
//11:07:55.855 coroutine2, end [DefaultDispatcher-worker-4]



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