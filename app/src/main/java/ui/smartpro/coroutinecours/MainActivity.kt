package ui.smartpro.coroutinecours

import android.accounts.NetworkErrorException
import android.location.Criteria
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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

//Получатель вызывает метод collect(), чтобы получить данные. Этот метод вызовет блок flow,
// который начнет создавать данные. Созданные данные отправляются в метод emit(),
// который переадресует их получателю в блок collect.
// Т.е. вся эта цепочка вызовов (collect() -> блок flow -> emit() -> блок collect) будет происходить
// последовательно в одной корутине, где был вызван метод collect().

    @ExperimentalCoroutinesApi
    private fun onRun() {
//Основной способ создания Flow - это билдер flow
//В билдер мы передаем блок кода, который будет запущен, когда получатель запустит этот Flow.
// Методом emit мы отправляем данные получателю.
        val flow =flow{
            emit("a")
            emit("b")
            emit("c")
        }
//Также существуют билдеры-обертки asFlow и flowOf, которые избавляют нас от написания очевидного
// и простого кода, а под капотом используют билдер flow.
//
//Пример создания Flow из коллекции с помощью asFlow:

        val flow1 = listOf("a","b","c").asFlow()

// Билдер flowOf может сделать то же самое еще лаконичнее. На вход он принимает vararg:

        val flow2 = flowOf("a","b","c")
}

    private fun onRun2() {

//retry
//Оператор retry перезапустит Flow в случае ошибки. Как и оператор catch, он срабатывает только для
    // тех ошибок, которые произошли в предшествующих ему операторах.
//
//Пример:

launch {
   flow
       .retry(2)
       .collect {
           log("collect $it")
       }
}
//На вход оператор retry принимает количество попыток. Если все попытки закончатся ошибкой,
    // то retry просто ретранслирует эту ошибку дальше. Поэтому добавляйте catch после retry
    // если хотите ошибку в итоге поймать.
//
//
//
//Если на вход retry не передавать количество попыток, то по умолчанию оно будет равно Long.MAX_VALUE.
    // Упорству такого retry можно только позавидовать.
//
//
//
//Кроме количества попыток, в оператор retry можно передать блок кода. На вход в этот блок retry
    // даст нам пойманную ошибку. А от нас он ждет Boolean ответ, который определяет,
    // надо ли пытаться перезапускать Flow

launch {
   flow
       .retry(2) {
           it !is ArithmeticException
       }
       .collect {
           log("collect $it")
       }
}
//В блоке retry мы проверяем тип ошибки. Если это не ArithmeticException, то вернется true - надо
    // перезапускать Flow. А если ошибка - ArithmeticException, то вернется false - Flow не будет
    // перезапущен, а ошибка пойдет дальше, и ее надо будет ловить.

//Если вам нужно, чтобы retry выждал какую-то паузу прежде чем перезапустить Flow,
    // можно использовать delay:

launch {
   flow
       .retry(2) {
           if (it is NetworkErrorException) {
               delay(5000)
               true
           }
           false
       }
       .collect {
           log("collect $it")
       }
}
//Здесь мы проверяем тип ошибки. Если это NetworkErrorException, то ждем 5 сек и возвращаем true,
    // чтобы перезапустить Flow. В любом другом случае мы сразу возвращаем false - Flow не будет
    // перезапущен, а ошибка пойдет дальше.

    }
    @InternalCoroutinesApi
    private suspend fun onRun3(){
    }




    // можно coroutineScope и все его содержимое вынести в отдельную suspend функцию
    private suspend fun twoCoroutines() {
        coroutineScope {
            launch(CoroutineName("1_1")) {
                // ...
            }

            launch(CoroutineName("1_2")) {
                // exception
            }
        }
    }

//Есть какая-то suspend функция, которая перед тем, как начать асинхронную работу, проверяет кэш.
// Если она находит там результат, то сразу отправляет его обратно в корутину.
//
//Если этот код выбросит исключение, то это будет равносильно тому,
// что suspend функция в корутине выбросила исключение.
    suspend fun someFunction(): String =
            suspendCancellableCoroutine { continuation ->

//                val result = getFromCache()

//                if (result != null) {
//                    continuation.resume(result)
//                } else {
                    // async code
 //               }

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