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
//Вложенные корутины
//Создадим в корутине еще одну корутину, а в ней еще одну.
// Посмотрим, как будет передан диспетчер от родительских корутин к дочерним.
private fun onRun() {

    val scope = CoroutineScope(Job() + Dispatchers.Main)
    log("scope, ${contextToString(scope.coroutineContext)}")

    scope.launch {
        log("coroutine, level1, ${contextToString(coroutineContext)}")

        launch {
            log("coroutine, level2, ${contextToString(coroutineContext)}")

            launch {
                log("coroutine, level3, ${contextToString(coroutineContext)}")
            }
        }
    }
}
//Логи:
//
//scope, Job = JobImpl{Active}@973e4a6, Dispatcher = Main
//coroutine, level1, Job = StandaloneCoroutine{Active}@4e813a2, Dispatcher = Main
//coroutine, level2, Job = StandaloneCoroutine{Active}@e17e833, Dispatcher = Main
//coroutine, level3, Job = StandaloneCoroutine{Active}@35f97f0, Dispatcher = Main
//
//Диспетчер Main передается по цепочке: от scope к level1, от level1 к level2, от level2 к level3.



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