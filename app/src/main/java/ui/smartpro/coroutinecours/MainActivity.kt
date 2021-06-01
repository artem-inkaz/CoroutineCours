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
    log("onRun start")
    try {
        scope.launch {
            Integer.parseInt("a")
        }
    } catch (e:Exception) {
        log("error $e")
    }

    log("onRun end")
}
// Будет ошибка
//Запускаем, смотрим логи:
//
//onRun start
//onRun end
//E/AndroidRuntime: FATAL EXCEPTION: DefaultDispatcher-worker-2
//  Process: com.startandroid.coroutinescourse, PID: 6718
//  java.lang.NumberFormatException: For input string: "a"
//    at java.lang.Integer.parseInt(Integer.java:615)
//    at java.lang.Integer.parseInt(Integer.java:650)
//    at com.startandroid.coroutinescourse.MainActivity$onRun$1.invokeSuspend(MainActivity.kt:59)
//    at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
//    at kotlinx.coroutines.DispatchedTask.run(Dispatched.kt:241)
//    at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:594)
//    at kotlinx.coroutines.scheduling.CoroutineScheduler.access$runSafely(CoroutineScheduler.kt:60)
//    at kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.run(CoroutineScheduler.kt:740)
//
//Приложение свалилось с крэшем. Давайте разбираться почему.
//
//
//
//Вспомним похожие простые примеры, которые мы делали в прошлых уроках.
// В них мы выяснили, что метод onRun использует билдер launch, чтобы создать и запустить корутину,
// и сам после этого сразу завершается. А корутина живет своей жизнью в отдельном потоке.
// Вот именно поэтому try-catch здесь и не срабатывает.
//
//Что делает билдер launch? Если вкратце, он формирует контекст, создает пару Continuation+Job,
// и отправляет Continuation диспетчеру, который помещает его в очередь.
// Ни в одном из этих шагов не было никакой ошибки, поэтому try-catch ничего не поймал.
// Билдер завершил свою работу, и метод onRun успешно завершился.
//
//У диспетчера есть свободный поток, который постоянно мониторит очередь.
// Он обнаруживает там Continuation и начинает его выполнение.
// И вот тут уже возникает NumberFormatException.
// Но наш try-catch до него никак не мог дотянуться.
// Т.к. он покрывал только создание и запуск корутины, но не выполнение корутины,
// т.к. выполнение ушло в отдельный поток.
//
//
//
//Обратите внимание на стэк методов в логе. Читаем их снизу вверх.
//
//CoroutineScheduler$Worker - это поток, который из очереди берет таски и выполняет их.
// Система выполняет его метод run.
//
//DispatchedTask - обертка, в которой находится Continuation.
// В его методе run происходит вызов Continuation.resumeWith.
// Это и есть тот самый resume метод,
// про который я часто говорил в прошлых уроках про Continuation и диспетчеры.
// В этом методе происходит вызов метода invokeSuspend, в котором содержится наш код корутины:
//MainActivity$onRun$1.invokeSuspend
//
//Т.е. MainActivity$onRun$1 - это Continuation класс.
// И если вы найдете его в папке \app\build\tmp\kotlin-classes\debug\<package_name>
// и декомпилируете, то сможете увидеть примерный код этого класса.
//
//Метод invokeSuspend выполняет код корутины, и в нем то и происходит исключение,
// когда котлин пытается распарсить букву в цифру (Integer.parseInt).
//
//Выполнение этого кода в потоке диспетчера мы не оборачивали в try-catch,
// поэтому исключение не поймали.




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