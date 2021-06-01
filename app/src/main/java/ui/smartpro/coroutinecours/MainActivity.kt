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
import kotlin.concurrent.thread
import kotlin.coroutines.*
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    //указываем время
    private var formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

//   private val scope = CoroutineScope(Dispatchers.Unconfined)

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
   val scope = CoroutineScope(Dispatchers.Default)
        val job =  scope.launch {
            log("parent start")
            launch {
                log("child start")
                delay(1000)
                log("child end")
            }
            log("parent end")
        }
}

//И знаем, как он сработает:
//
//21:20:42.969 parent start
//21:20:42.970 parent end
//21:20:42.970 child start
//21:20:43.971 child end
//
//Код родительской корутины выполняется сразу и не ждет выполнения дочерней.
// Это, пожалуй, является одним из самых непонятных моментов, когда начинаешь изучать корутины.
// Т.е. вроде одна корутина родительская, а внутри нее дочерняя,
// но при этом родительская корутина не ждет дочернюю и никакой связи между ними не видно.
//
//
//
//Чтобы увидеть эту связь, мы будем проверять джоб родительской корутины.
// Метод job.isActive подскажет нам статус корутины - еще работает или уже завершена.
// Фокус в том, что даже если родительская корутина выполнила весь свой код, это еще не значит,
// что она завершена. Это зависит от того, завершились ли ее дочерние корутины.
// И именно в этом проявляется связь между родительской и дочерней корутинами.





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
//        job?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
//        scope.cancel()
    }

    private fun log(text: String) {
        Log.d("TAG", "${formatter.format(Date())} $text [${Thread.currentThread().name}]")
    }
}