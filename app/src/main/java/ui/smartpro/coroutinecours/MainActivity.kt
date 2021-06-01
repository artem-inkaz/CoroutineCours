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
// В обработчике CoroutineExceptionHandler мы на вход получаем ошибку и контекст корутины,
// которая передала ошибку в этот обработчик.
// Мы из контекста достаем CoroutineName и с его помощью узнаем, какая корутина обработала ошибку.
        val handler = CoroutineExceptionHandler { context, exception ->
            log("$exception was handled in Coroutine_${context[CoroutineName]?.name}")
        }

 //Не забывайте, что контекст корутин формируется не только из того, что было передано в билдер,
        // но и того, что пришло из контекста родителя.
        // Поэтому, если вам надо поместить ваш CoroutineExceptionHandler обработчик во все корутины,
        // то просто поместите его в scope, и он будет передан во все корутины, созданные в нем.
        //
        //val handler = CoroutineExceptionHandler { context, exception ->
        //   log("coroutine exception $exception")
        //}
        // Обработчик handler мы помещаем в scope, поэтому он будет передан во все корутины.
        val scope = CoroutineScope(Job() + Dispatchers.Default + handler)

//CoroutineName - это элемент контекста.
// Помещаем в него имя корутины и передаем в launch билдеры.
// Теперь в каждой корутине мы сможем из контекста достать этот элемент и узнать, какая это корутина.

        scope.launch(CoroutineName("1")) {

//Это приводит к тому, что ошибка из async не пойдет вверх по иерархии корутин,
// т.к. supervisorJob ее туда не пропустит. При этом он скажет async корутине, что не может обработать ошибку.
// Это означает, что async корутине надо сделать это самостоятельно.
// Но она (в отличие от launch) не умеет отправлять ошибку в обработчики.
// Она просто сохраняет это исключение внутри себя, чтобы выбросить его,
// когда вызовут ее метод await.
                val deferred = async(SupervisorJob(coroutineContext[Job])) {
                    // exсeption
                }

                // ...

                val result = deferred.await()

                // ...


        }


}
//Есть какая-то suspend функция, которая перед тем, как начать асинхронную работу, проверяет кэш.
// Если она находит там результат, то сразу отправляет его обратно в корутину.
//
//Если этот код выбросит исключение, то это будет равносильно тому,
// что suspend функция в корутине выбросила исключение.
    suspend fun someFunction(): String =
            suspendCancellableCoroutine { continuation ->

                val result = getFromCache()

                if (result != null) {
                    continuation.resume(result)
                } else {
                    // async code
                }

            }

// ВАЖНо
//Асинхронная часть
//Если же ошибка произойдет в асинхронной части и не будет там поймана в try-catch,
// то приложение свалится с крэшем. И даже если обернуть suspend функцию в try-catch или использовать
// CoroutineExceptionHandler, это не поможет.
//
//Потому что это был отдельный поток. И выполнялась там не корутина,
// которая автоматически ловит все ошибки своего кода, а обычный код.
// А непойманное исключение в обычном коде приводит к крэшу.


//Расширение для CoroutineScope. Мы сможем запускать его прямо в корутине.
// Оно будет периодически выводить в лог имя и isActive статус корутины
   fun CoroutineScope.repeatIsActive() {
       repeat(5) {
           TimeUnit.MILLISECONDS.sleep(300)
           log("Coroutine_${coroutineContext[CoroutineName]?.name} isActive $isActive")
       }
   }


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