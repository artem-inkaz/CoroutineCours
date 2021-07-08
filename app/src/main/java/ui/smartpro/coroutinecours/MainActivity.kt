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
//у нас есть Flow, который отправляет 3 элемента. Перед каждой отправкой он тратит секунду на то,
// чтобы создать данные.
        val flow = flow {
            delay(1000)
            emit(1)
            delay(1000)
            emit(2)
            delay(1000)
            emit(3)
        }
//Чтобы это работало параллельно, мы решаем раскидать код по отдельным корутины:
        val flow1 = flow {
            coroutineScope {
                launch {
                    delay(1000)
                    emit(1)
                }

                launch {
                    delay(1000)
                    emit(2)
                }

                launch {
                    delay(1000)
                    emit(3)
                }
            }
        }
// блок flow - это suspend функция. Мы не можем в ней напрямую стартовать корутины билдерами launch,
    // async, produce и пр, потому что этим билдерам нужен scope. Поэтому мы создаем для них scope с
    // помощью функции coroutineScope. эта функция под капотом создает корутину.
    // Эта корутина и будет являться scope для билдеров launch, которые мы поместим в coroutineScope.
//
//При этом важно понимать, что корутина, которая создается внутри coroutineScope,
    // не является какой-то бесхозной корутиной без родителя. Ее родительская корутина - это корутина,
    // которая вызывает suspend функцию coroutineScope.
    // Поэтому принцип structured concurrency тут соблюден.
//
//Теперь все данные будут подготовлены параллельно за одну секунду и отправлены нам.

//Запускаем этот Flow методом collect() и получаем крэш:
//
//java.lang.IllegalStateException: Flow invariant is violated:
//    Emission from another coroutine is detected.
//    Child of StandaloneCoroutine{Active}@ead1de6, expected child of StandaloneCoroutine{Active}@4b5ff27.
//    FlowCollector is not thread-safe and concurrent emissions are prohibited.
//    To mitigate this restriction please use 'channelFlow' builder instead of 'flow'
//
//Так происходит потому, что Flow под капотом выполняет проверку, что методы collect() и emit() были
    // выполнены в одной корутине. А в нашем случае мы вызов emit() выполняем из новой корутины.
    // Flow определил это и сказал, что мы так не договаривались.
//
//
//
//Давайте обсудим, почему так делать нельзя.
//
//Во-первых - это не потокобезопасно. Мы из разных корутин (методом emit) вызываем один и
    // тот же блок collect. Это может привести к параллельной работе с одним объектом внутри этого
    // блока из разных потоков - так делать нельзя. Об этом мы еще поговорим в отдельном уроке.
//
//Во-вторых - смена контекста. Метод collect() мы вызываем в корутине. Мы передаем туда блок кода collect,
    // который будет обрабатывать получаемые данные, и предполагаем, что этот блок кода тоже будет
    // выполнен в текущей корутине. И на самом деле так и будет, если все делать правильно и
    // в блоке flow не запускать новые корутины. Потому что, как мы уже обсудили выше,
    // метод collect() запустит блок flow, а он в свою очередь методом emit() запустит блок collect.
    // И все это выполнится в корутине, где мы вызвали метод collect().
//
//А если блок flow начинает создавать новые корутины, то в них он и запустит метод emit(),
    // а следовательно и блок collect. И в этом заключается основная проблема.
    // Мы (как получатель) ожидаем, что блок collect выполнится в нашей текущей корутине
    // (где мы запускаем метод collect()), а он выполнится неизвестно в какой корутине,
    // которую запустил блок flow.
//
//Самый очевидный пример - это когда мы хотим получать данные из Flow в Main потоке.
    // Метод collect() мы вызываем в корутине с Main диспетчером.
    // В блоке collect мы получаемые данные выводим на экран, и можем это сделать только в Main потоке.
    // Т.е.запуская метод collect() в корутине с Main потоком мы ожидаем,
    // что и блок collect будет выполнен в этой же корутине. А если блок flow вызовет emit()
    // (а следовательно и блок collect) из какой-то своей корутины
    // (например, с IO диспетчером), то наш код по выводу данных на экран не сработает,
    // т.к. будет выполнен не в Main потоке.


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