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
    private val scope2 = CoroutineScope(Job()+Dispatchers.Default)

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

 //Не забывайте, что контекст корутин формируется не только из того, что было передано в билдер,
        // но и того, что пришло из контекста родителя.
        // Поэтому, если вам надо поместить ваш CoroutineExceptionHandler обработчик во все корутины,
        // то просто поместите его в scope, и он будет передан во все корутины, созданные в нем.
        //
        //val handler = CoroutineExceptionHandler { context, exception ->
        //   log("coroutine exception $exception")
        //}
        //
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)

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

//В примерах с CoroutineExceptionHandler мы убедились, что scope отменяет своих детей,
// когда в одном из них происходит ошибка. Пусть даже эта ошибка и была передана в обработчик.
// Такое поведение родителя далеко не всегда может быть удобным.
// Поэтому у нас есть возможность это отключить.
//
//Для этого надо в scope вместо обычного Job() использовать SupervisorJob().
// Он отличается от Job() тем, что не отменяет всех своих детей при возникновении ошибки в одном из них.
//
//
//
//Рассмотрим снова пример с двумя корутинами одного scope,
// но теперь в scope в качестве Job используем SupervisorJob()
//
//Запускаем, смотрим лог:
//
//14:34:36.641 second coroutine isActive true
//14:34:36.943 second coroutine isActive true
//14:34:37.244 second coroutine isActive true
//14:34:37.280 first coroutine exception java.lang.NumberFormatException: For input string: "a"
//14:34:37.546 second coroutine isActive true
//14:34:37.955 second coroutine isActive true
//
//После того, как первая корутина выбросила исключение, scope не стал отменять вторую корутину.





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