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
       job = scope.launch {
           log("coroutine, start")
           var x = 0
           while (x < 5) {
               TimeUnit.MILLISECONDS.sleep(1000)
               log("coroutine, ${x++}")
           }
           log("coroutine, end")
       }
        log("onRun, end")
    }
//11:24:53.416 onRun, start [main]
//11:24:53.466 onRun, end [main]
//11:24:53.474 coroutine, start [DefaultDispatcher-worker-1]
//11:24:54.475 coroutine, 0 [DefaultDispatcher-worker-1]
//11:24:55.476 coroutine, 1 [DefaultDispatcher-worker-1]
//11:24:56.135 onCancel [main]
//11:24:56.480 coroutine, 2 [DefaultDispatcher-worker-1]
//11:24:57.484 coroutine, 3 [DefaultDispatcher-worker-1]
//11:24:58.486 coroutine, 4 [DefaultDispatcher-worker-1]
//11:24:58.486 coroutine, end [DefaultDispatcher-worker-1]



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