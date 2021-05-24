package ui.smartpro.coroutinecours

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import ui.smartpro.coroutinecours.model.UserData
import java.text.SimpleDateFormat
import java.util.*
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
//Если корутина не находит в своем контексте диспетчер, то она использует диспетчер по умолчанию.
// Этот диспетчер представляет собой пул потоков. Количество потоков равно количеству ядер процессора.
//
//Он не подходит для IO операций, но сгодится для интенсивных вычислений.
    val scope = CoroutineScope(Dispatchers.Default)

    repeat(6) {
        scope.launch {
            log("coroutine $it, start")
            TimeUnit.MILLISECONDS.sleep(100)
            log("coroutine $it, end")
        }
    }
}
//Смотрим логи:
//
//19:56:52.658 coroutine 0, start [DefaultDispatcher-worker-1]
//19:56:52.658 coroutine 2, start [DefaultDispatcher-worker-3]
//19:56:52.658 coroutine 3, start [DefaultDispatcher-worker-4]
//19:56:52.658 coroutine 1, start [DefaultDispatcher-worker-2]
//19:56:52.761 coroutine 0, end [DefaultDispatcher-worker-1]
//19:56:52.761 coroutine 3, end [DefaultDispatcher-worker-4]
//19:56:52.761 coroutine 2, end [DefaultDispatcher-worker-3]
//19:56:52.761 coroutine 1, end [DefaultDispatcher-worker-2]
//19:56:52.763 coroutine 4, start [DefaultDispatcher-worker-4]
//19:56:52.763 coroutine 5, start [DefaultDispatcher-worker-3]
//19:56:52.865 coroutine 5, end [DefaultDispatcher-worker-3]
//19:56:52.865 coroutine 4, end [DefaultDispatcher-worker-4]
//
// Корутины 0,2,3 и 1 начали работу.
// Диспетчер выдал им потоки DefaultDispatcher-worker 1, 3, 4 и 2.
// На этом свободные потоки закончились, и корутинам 5 и 4 пришлось ждать, пока потоки освободятся.

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