package ui.smartpro.coroutinecours

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import ui.smartpro.coroutinecours.model.UserData
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.*
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    //указываем время
    private var formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

   private val scope = CoroutineScope(Job()+Dispatchers.Default)

    private var job: Job? = null

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
    log("onRun start")
    try {
        thread {
            Integer.parseInt("a")
        }
    } catch (e:Exception) {
        log("error $e")
    }

    log("onRun end")
}
//Вместо корутины мы просто создаем новый поток. И в нем выполняем код, который выбросит исключение.
// Корутина работает именно по этому принципу, если рассматривать ее упрощенно.
//
//
//
//Смотрим лог:
//
//onRun start
//onRun end
//E/AndroidRuntime: FATAL EXCEPTION: Thread-2
//  Process: com.startandroid.coroutinescourse, PID: 6881
//  java.lang.NumberFormatException: For input string: "a"
//    at java.lang.Integer.parseInt(Integer.java:615)
//    at java.lang.Integer.parseInt(Integer.java:650)
//    at com.startandroid.coroutinescourse.MainActivity$onRun$1.invoke(MainActivity.kt:59)
//    at com.startandroid.coroutinescourse.MainActivity$onRun$1.invoke(MainActivity.kt:21)
//    at kotlin.concurrent.ThreadsKt$thread$thread$1.run(Thread.kt:30)
//
//Похоже на предыдущий случай. Система выполнила у потока метод run.
// А тот выполнил код внутри блока thread, получил исключение и крэшнул приложение.
//
//А наш try-catch снова не у дел. Потому что он покрывал создание и запуск нового потока,
// но не выполнение кода в нем.




//простой метод, чтобы доставать из контекста и выводить в лог Job и диспетчер.
// Это поможет нам наглядно понять, что происходит с контекстом
    private fun contextToString(context: CoroutineContext) : String =
            "Job = ${context[Job]}, Dispatchers = ${context[ContinuationInterceptor]}"

//suspend функция приостанавливает код корутины, выполняет свою работу в фоновом потоке,
// а затем возобновляет корутину. Поток корутины при этом не блокируется.
// suspend функция код должен быть запущен в корутине, и вне корутины ее не запустить.
    private suspend fun getData(): String =
        suspendCoroutine {
            //покажет из какого потока была запущена эта функция
            log("suspend function, start")
            thread {
                //в каком потоке она выполняет свою работу
                log("suspend function, background work")
                TimeUnit.MILLISECONDS.sleep(3000)
                it.resume("Data!")
            }
        }

    private suspend fun getData2(): String {
        delay(1500)
        return "data2"
    }

//Данные, которые будут получены сразу отображаем на экране.
    private fun updateUI(data:String) {
        label.text = data
    }

    private fun onRun2(){
        log("onRun2, start")
//        job?.start()
        log("onRun2, end")
    }

    private fun onCancel(){
        log("onCancel")
 //       job?.cancel()
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