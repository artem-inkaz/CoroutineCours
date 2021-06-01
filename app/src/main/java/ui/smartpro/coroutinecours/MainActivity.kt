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
        val handler = CoroutineExceptionHandler { context, exception ->
            log("handled $exception")
        }

        scope.launch(handler) {
            TimeUnit.MILLISECONDS.sleep(1000)
            Integer.parseInt("a")
        }

        scope.launch {
            repeat(5) {
                TimeUnit.MILLISECONDS.sleep(300)
                log("second coroutine isActive ${isActive}")
            }
        }
}

//Запускаем две корутины в одном scope. Первая корутина вызовет исключение через 1000 мсек.
// Вторая корутина 5 раз с интервалом в 300 мсек выводит в лог свой статус.
//
//Лог:
//
//15:36:34.043 second coroutine isActive true
//15:36:34.344 second coroutine isActive true
//15:36:34.646 second coroutine isActive true
//15:36:34.745 first coroutine exception java.lang.NumberFormatException: For input string: "a"
//15:36:34.947 second coroutine isActive false
//15:36:35.248 second coroutine isActive false
//
//Поначалу вторая корутина сообщает о том, что полет нормальный и все ок.
// Далее в первой корутине происходит исключение и уходит в обработчик, который пишет об этом в лог.
// А scope, узнав, что в первой корутине произошла ошибка, отменяет вторую.
// Это видно по ее статусу в логе.
//
//
//Это может быть удобным, если вам при возникновении ошибки в одной операции,
// надо отменить другие операции.
// Вы просто помещаете корутины с этими операциями в один scope.
//
//
//Учитывайте, что scope отменяет не только корутины, но и себя. А это означает,
// что в этом scope мы больше не сможем запустить корутины.







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