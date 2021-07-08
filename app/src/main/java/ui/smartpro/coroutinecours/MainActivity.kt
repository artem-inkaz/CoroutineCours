package ui.smartpro.coroutinecours

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

    private fun onRun2(){

//flowOn
//Оператор channelFlow позволял нам самим создать Flow, чтобы выполнять его код в корутине с
// нужным нам контекстом. Но что, если нам дали уже готовый Flow. Как нам попросить выполнить
// его блок flow в отдельной корутине?
//
//Если бы нам пришлось делать это самим, то мы бы сделали так:

flow {
   coroutineScope {
       val channel = produce<Int>(neededContext) {
           flow.collect {
               send(it)
           }
       }
       channel.consumeEach {
           emit(it)
       }
   }
}
//Снова используем produce. При его вызове указываем контекст, который нужен - neededContext.
// Внутри мы методом collect запускаем Flow, который нам дали. Он выполнится в корутине produce и
// оттуда отправит данные в канал методом send. А мы их получим в consumeEach и методом emit
// отправим получателю.

//Т.е. разница с прошлым примером в том, что мы в новой корутине не сами создаем данные,
// а получаем их из потока flow, который нам дали. И при получении сразу отправляем их в канал.
//
//Именно так и работает оператор flowOn. Мы используем его, когда хотим сообщить Flow,
// какой контекст ему использовать для кода создания данных:

//val ioFlow = flow.flowOn(Dispatchers.IO)
//Вызов ioFlow.collect() приведет к тому, что flow начнет работу в IO потоке. А все данные мы будем
// получать в текущей корутине.

//В прошлом уроке мы рассматривали Intermediate операторы. Если помните, каждый из них создает Flow
// обертку и мы можем комбинировать их. Оператор flowOn также создает Flow обертку и может быть
// встроен в цепочку операторов в любом месте.
//
//Пример:

flow {
   // ... IO thread
}
.map {
   // ... IO thread
}
.flowOn(Dispatchers.IO)
.onEach {
   // ... Main thread
}
.flowOn(Dispatchers.Main)
//Здесь билдер flow и оператор map будут выполнены в IO потоке. А оператор onEach - в Main потоке.
// Т.е. оператор flowOn влияет на все предыдущие операторы в цепочке, пока не встретит другой flowOn.

//Почему flowOn влияет на все предыдущие операторы, а не только на тот, к которому он был применен?
// Рассмотрим на примере выше.
//
//У нас есть цепочка flow > map > flowOn. flowOn создает Flow обертку. Эта обертка запустит map в
// IO потоке. Map в свою очередь также является оберткой, которая будет запускать flow.
    // Но в отличие от flowOn, map - это простая обертка, которая лишь выполняет преобразование данных
    // и не создает новых корутин. Поэтому map просто запустит flow в той же IO корутине, где сам map
    // и выполняется. В итоге flow выполнится в той корутине с IO диспетчером, которую запустил flowOn.
//
//В итоге все данные будут получены в той корутине, в которой мы вызываем метод collect().
    // И для этого нам не надо использовать никакие операторы.

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