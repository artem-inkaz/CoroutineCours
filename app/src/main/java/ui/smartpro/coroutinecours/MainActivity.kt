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
//А если у нас есть suspend функция, которая ничего не принимает на вход и возвращает результат:
    suspend fun getData(): Data
    val flow3 = ::getData.asFlow()

    //Intermediate
//Примеры Intermediate операторов - map, filter, take, zip, combine, withIndex, scan, debounce,
// distinctUntilChanged, drop, sample.
    private fun onRun2(){
//Оператор map умножит на 10 значения 1,2,3, которые придут из Flow. В итоге получился новый Flow,
// который вернет 10,20,30. При этом оператор map сам по себе не запускает Flow, к которому мы его
// применяем. Он просто создает новый Flow-обертку поверх старого. Работать он начнет,
// когда мы вызовем collect.
        val flow = flowOf(1,2,3).map{it*10}
    }

    private fun onRun3(){
        //1
        val flowStrings = flow {
            emit("abc")
            emit("def")
            emit("ghi")
        }
    }
    //2
    //нам надо эти строки привести к верхнему регистру (закапслочить) и вывести их в лог
    flowStrings.collect {
        log(it.toUpperCase())
    }
    //3
//мы создали Flow, который запрашивает элементы из другого Flow, преобразует их и отправляет получателю.
    flow {
        flowStrings.collect {
            emit(it.toUpperCase())
        }
    }

//В итоге мы можем создать кучу таких оберток, одну над другой.

// важно понимать один момент. Наш созданный Flow не запустит flowStrings.collect,
// пока сам не будет запущен. Т.е. весь этот код начнет работу только когда кто-то вызовет collect
// для нашего созданного Flow.

//    flowStrings
//    .map {
//        // ...
//    }
//    .filter {
//        // ...
//    }
//    // ...

//Упакуем наш код в extension-метод toUpperCase и получим свой оператор для Flow<String>:
    fun Flow<String>.toUpperCase(): Flow<String> = flow {
        collect {
            emit(it.toUpperCase())
        }
    }

//Теперь мы можем использовать этот оператор, чтобы Flow со строками преобразовать в Flow с теми же
// строками, но в верхнем регистре:
    flowStrings.toUpperCase().collect {
        log(it)
    }

//В качестве еще одного примера можно рассмотреть такой код:

//flow {
//   emit("start")
//   flowStrings.collect {
//       emit(it)
//   }
//   emit("end")
//}
//Создаем Flow-обертку над flowStrings. Эта обертка просто пересылает данные из flowStrings никак их не трансформируя. Но перед этой отправкой и после нее она отправляет строки start и end. Т.е. таким образом можно сделать операторы типа startWith и endWith.

//В примере выше, кстати, конструкцию

//flowStrings.collect {
//   emit(it)
//}
//можно заменить кодом

//emitAll(flowStrings)
//Метод emitAll просто переотправит все элементы из flowStrings.

//Важный момент. Когда вы используете map, filter или создаете свой оператор, не забывайте,
// что используемый в них код преобразования данных будет выполнен в suspend функции collect,
// которая начнет всю работу Flow. Это значит что ваш код не должен быть тяжелым или блокировать поток.
// Не забывайте, что suspend функция может быть запущена и в main потоке.



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