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

// channelFlow
//Чтобы лучше понять, что именно делает channelFlow, давайте сами сделаем его упрощенную версию.

//Основная проблема, с которой мы столкнулись в прошлом примере - это то, что в блоке flow нельзя
// вызывать emit из другой корутины. Т.е. можно запустить другую корутину и создать там данные,
// а вот отправить их оттуда получателю методом emit нельзя, т.к., тем самым, мы меняем корутину,
// в которой будет запущен код получателя (блок collect). Это значит, что нам надо вызов emit
// вернуть в нашу текущую корутину.
//
//Т.е. план такой:
//1) в нашей текущей корутине в блоке flow создаем новую корутину
//2) в новой корутине создаем данные
//3) передаем эти данные из новой корутины в текущую корутину
//4) в текущей корутине запускаем метод emit() чтобы отправить данные получателю
//Профит!
//
//Основная сложность в этом плане - пункт 3. Нам надо передать данные из одной корутины в другую.
// И это может быть не одно значение, а последовательность значений, т.к. мы работаем с Flow.
// Т.е. простым async-await тут не отделаться.
//
//И тут мы вспоминаем, что для безопасной и удобной передачи последовательности данных из одной
// корутины в другую отлично подходят каналы!

//В уроке про каналы мы рассматривали билдер produce. Он создает связку: корутина + канал.
//
//val channel = produce<Int> {
   // send(...)
//}
//Билдер produce возвращает нам канал. А блок кода, который мы передаем в produce,
// будет выполнен в новой корутине. Там мы сможем вызывать метод send для отправки данных в канал.
//
//И это как раз то, что нам нужно чтобы из текущей корутины запустить новую корутину для создания
// данных. А чтобы получать данные из канала обратно в текущую корутину, можно использовать consumeEach:
//
//channel.consumeEach {
//   // ...
//}
//
//Давайте вставим эту конструкцию в flow билдер.
//
flow {
   coroutineScope {
       val channel = produce<Int> {
           // ...
       }
       channel.consumeEach {
           // ...
       }
   }
}
//Несмотря на то, что coroutineScope под капотом создает новую корутину, Flow считает,
// что все происходящее внутри coroutineScope происходит в текущей корутине. А значит,
// мы можем вызвать там метод emit. Это учитывается в подкапотных проверках и не приведет к ошибке.
//
//
//
//Итак, produce создает нам новую корутину. Туда мы поместим код создания данных. А методом send
// будем отправлять их в канал.
//
flow {
   coroutineScope {
       val channel = produce<Int> {
           launch {
               delay(1000)
               send(1)
           }
           launch {
               delay(1000)
               send(2)
           }
           launch {
               delay(1000)
               send(3)
           }
       }
       channel.consumeEach {
           // ...
       }
   }
}
//В produce создаем три новые корутины, чтобы создавать данные параллельно.
// Если вам не нужна такая параллельность, то можно все сделать прямо в produce без создания новых
// дополнительных корутин.
//
//
//
//Созданные данные мы отправляем в канал методом send. Получать их будем в consumeEach.
// Напомню, что consumeEach является suspend функцией и выполняется в текущей корутине,
// не создавая никаких новых корутин. А, значит, мы можем в этой функции вызвать метод emit(),
// чтобы перенаправить эти данные получателю Flow:
//
flow {
   coroutineScope {

       val channel = produce<Int> {
           launch {
               delay(1000)
               send(1)
           }
           launch {
               delay(1000)
               send(2)
           }
           launch {
               delay(1000)
               send(3)
           }
       }
       channel.consumeEach {
           emit(it)
       }

   }
}
//В итоге мы получили Flow, внутри которого будут созданы корутина и канал.
// Когда мы запустим его методом collect(), данные будут создаваться в отдельной корутине и с
// помощью канала вернутся в нашу текущую корутину.

//Именно так и работает channelFlow. Пример вызова:
//
val flow = channelFlow {
   launch {
       delay(1000)
       send(1)
   }
   launch {
       delay(1000)
       send(2)
   }
   launch {
       delay(1000)
       send(3)
   }
}
//Все лишнее спрятано под капотом, нам остается только написать блок кода, который будет создавать
// данные и отправить их методом send. Т.е. тот же код, что мы использовали в produce в нашем примере.
//
//В билдеры launch мы можем передавать контекст (диспетчер, хэндлер и т.п.), который нам нужен.


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