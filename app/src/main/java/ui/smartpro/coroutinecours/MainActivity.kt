package ui.smartpro.coroutinecours

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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

        scope.launch(CoroutineName("1")) {
//Канал с размером буфера = 64 по умолчанию. Это значение можно поменять в JVM
            val channel = Channel<Int>(2)
            launch {
                repeat(7) {
                delay(300)
                log("send $it")
                channel.send(it)
            }
                log("close")
//cancel
//Кроме закрытия канала, его можно отменить. Это то же самое, что и закрытие,
// только с очисткой буфера.
//
//Рассмотрим тот же пример, что и ранее с фиксированным размером буфера.
// Но теперь будем не закрывать канал, а отменять его методом cancel.
               channel.cancel()

            }

            launch {

//Этот цикл будет получать данные из канала, пока отправитель не закроет канал.
// После чего цикл завершится, и код пойдет дальше. И никакие ошибки ловить не придется.
                for (element in channel) {
                    log("receive")
                    delay(1000)
                }
//Также, на стороне получателя мы можем использовать метод chanel.isClosedForReceive,
// чтобы понять, что канал закрыт и данных в нем не осталось.
                }
        }
}

// Лог:
//14:34:54.596 send 0
//14:34:54.597 received 0
//14:34:54.898 send 1
//14:34:55.199 send 2
//14:34:55.501 send 3
//14:34:55.599 received 1
//14:34:55.901 send 4
//14:34:56.601 received 2
//14:34:56.902 send 5
//14:34:57.603 received 3
//14:34:57.904 send 6
//14:34:58.605 received 4
//14:34:58.605 cancel
//
//В прошлом примере после закрытия канала получатель мог достать данные из буфера.
// А после отмены уже не может, значения 5 и 6 для получателя утеряны.
//
//Еще одно отличие cancel от close в том, что при попытке вызвать receive для отмененного канала
// будет выброшено исключение CancellationException, а не ClosedReceiveChannelException

//Получатель закрывает канал
//Разумеется, отменить/закрыть канал может и корутина получатель.
// При этом в корутине отправителе метод send выбросит, соответственно,
// исключение CancellationException (что приведет к отмене корутины) / ClosedSendChannelException
// (что приведет к крэшу, если исключение никак не обрабатывается).
//
//Отправитель может использовать метод channel.isClosedForSend(), чтобы определить,
// можно ли отправлять данные в канал.


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