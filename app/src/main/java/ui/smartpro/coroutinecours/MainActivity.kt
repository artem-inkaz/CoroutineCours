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
//В методе onRun стартуем корутину, а внутри нее еще одну корутину.
    private fun onRun(){
        log("onRun, start")
    // корутину c параметром start = LAZY.
    // Это не даст корутине начать работу сразу после создания.
    // В примере стартуем в onRun2()
        job = scope.launch (start = CoroutineStart.LAZY) {
            log("coroutine, start")
            TimeUnit.MILLISECONDS.sleep(1000)
            log("coroutine, end")
        }
            log("onRun, end")
    }

//11:28:30.526 onRun, start [main]
//11:28:30.532 onRun, end [main]
//11:28:32.317 onRun2, start [main]
//11:28:32.321 onRun2, end [main]
//11:28:32.328 coroutine, start [DefaultDispatcher-worker-1]
//11:28:33.331 coroutine, end [DefaultDispatcher-worker-1]
//
//Жмем кнопку Run. Метод onRun создает корутину и завершается. Но корутина не стартует.
// Через пару секунд жмем кнопку Run2, и корутина начинает работу. Метод start ничего не блокирует.
// Он стартует корутину в отдельном потоке, а метод onRun2 быстро завершается.
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