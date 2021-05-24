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
//проверим что Job не передается, а диспетчер по умолчанию используется,
// если явно не указать диспетчер. А также познакомимся с пустым контекстом.
        val scope = CoroutineScope(Dispatchers.Main)
        log("scope, ${contextToString(scope.coroutineContext)}")

        scope.launch {
            log("coroutine, ${contextToString(coroutineContext)}")
        }
}
//scope, Job = JobImpl{Active}@7640e53, Dispatcher = Main
//coroutine, Job = StandaloneCoroutine{Active}@309e489, Dispatcher = Main
//
//В контексте scope есть Main диспетчер. И он же есть в контексте корутины, потому что был передан
// из контекста скопа. Системе не пришлось помещать в корутину диспетчер по умолчанию, потому что
// корутина получила диспетчер из скопа.



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