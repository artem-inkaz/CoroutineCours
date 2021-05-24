package ui.smartpro.coroutinecours

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import ui.smartpro.coroutinecours.model.UserData
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
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

private fun onRun() {

    val scope = CoroutineScope(
            Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    repeat(6) {
        scope.launch {
            log("coroutine $it, start")
            TimeUnit.MILLISECONDS.sleep(100)
            log("coroutine $it, end")
        }
    }
}
//6 корутин придут к диспетчеру, у которого есть только один поток.
//
//Логи:
//
//20:26:39.502 coroutine 0, start [pool-1-thread-1]
//20:26:39.604 coroutine 0, end [pool-1-thread-1]
//20:26:39.605 coroutine 1, start [pool-1-thread-1]
//20:26:39.706 coroutine 1, end [pool-1-thread-1]
//20:26:39.707 coroutine 2, start [pool-1-thread-1]
//20:26:39.808 coroutine 2, end [pool-1-thread-1]
//20:26:39.808 coroutine 3, start [pool-1-thread-1]
//20:26:39.911 coroutine 3, end [pool-1-thread-1]
//20:26:39.911 coroutine 4, start [pool-1-thread-1]
//20:26:40.013 coroutine 4, end [pool-1-thread-1]
//20:26:40.013 coroutine 5, start [pool-1-thread-1]
//20:26:40.114 coroutine 5, end [pool-1-thread-1]
//
//Корутинам пришлось выстраиваться в очередь и выполнятся последовательно.


//простой метод, чтобы доставать из контекста и выводить в лог Job и диспетчер.
// Это поможет нам наглядно понять, что происходит с контекстом
    private fun contextToString(context: CoroutineContext) : String =
            "Job = ${context[Job]}, Dispatchers = ${context[ContinuationInterceptor]}"

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