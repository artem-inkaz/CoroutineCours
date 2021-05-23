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
        log("onRun, Start")
//используется фоновый поток из диспетчера по умолчанию
        scope.launch {
            log("coroutine, start")
            TimeUnit.MILLISECONDS.sleep(1000)
            log("coroutine, end")
        }

        log("onRun, end")
    }
//11:00:55.994 onRun, start [main] полностью выполнился еще до того, как начала работать корутина
//11:00:56.062 onRun, end [main] основной код идет дальше, и метод onRun завершается.
//11:00:56.075 coroutine, start [DefaultDispatcher-worker-1] Т.е. билдер launch не блокирует поток, где он был запущен. Он создает корутину и отправляет ее на запуск в отдельном потоке (это видно по логам coroutine)
//11:00:57.076 coroutine, end [DefaultDispatcher-worker-1]



    private fun onCancel(){}

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
        scope.cancel()
    }

    private fun log(text: String) {
        Log.d("TAG", "${formatter.format(Date())} $text [${Thread.currentThread().name}]")
    }
}