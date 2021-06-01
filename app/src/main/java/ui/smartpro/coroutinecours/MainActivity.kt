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

            val channel = Channel<Int>()
//случай, когда отправитель хочет отправить данные, но получатель еще не готов их принять.
            launch {
                delay(300)
                log("send 5")
                channel.send(5)
                log("send, done")
            }

            launch {
                delay(1000)
                log("receive")
                val i = channel.receive()
                log("receive $i, done")
            }
        }
}

//Две корутины, стартующие в одно время и работающие параллельно. Первая корутина отправляет данные
// в канал через 300 мсек после запуска. Но вторая будет готова эти данные получать только через 1000 мсек.
// В итоге первая корутина будет приостановлена на 700 мсек.
//
//Как это будет работать изнутри.
//
//Первая корутина вызывает метод send, чтобы отправить данные. Метод send проверяет,
// есть ли получатель ожидающий данных. Т.е. кто-нибудь, кто вызвал метод receive.
// Такого пока нет. Значит, просто сразу передать данные уже ожидающему получателю не получится.
// Придется ждать вызова receive. Для этого надо уходить в полноценный suspend вызов.
//
//Используется классическая схема создания suspend функции. Вызывается штука, похожая на suspendCoroutine,
// чтобы получить continuation текущей (первой) корутины. Данные (число 5) и
// continuation пакуются в контейнер SendElement, и этот контейнер сохраняется внутри канала.
// На этом вызов send завершен. Выполнение кода первой корутины приостановлено suspend функцией send
// пока кто-нибудь не выполнит continuation.resume.
//
//Вторая корутина вызывает метод receive, чтобы получить данные.
// Метод receive проверяет, не отправлял ли кто данные.
// Для этого он смотрит на наличие в канале контейнера SendElement.
// Такой контейнер есть, т.к. метод send поместил его туда ранее.
// Из контейнера извлекаются данные и continuation.
// Данные используются в качестве результата вызова метода receive,
// т.е. вторая корутина получает эти данные. А у continuation вызывается resume,
// чтобы возобновить выполнение первой корутины.
//
//Лог:
//15:28:09.511 send 5
//15:28:10.211 receive
//15:28:10.211 receive 5, done
//15:28:10.212 send, done
//
//Видно, что метод send приостановил первую корутину на время ожидания получателя.
// Через 700 мсек получатель во второй корутине вызвал метод receive и мгновенно получил данные,
// т.к. отправитель эти даные уже поместил в канал.
// Забрав данные, получатель возобновил выполнение первой корутины.




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