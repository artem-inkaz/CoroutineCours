package ui.smartpro.coroutinecours

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import ui.smartpro.coroutinecours.model.UserData
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

    private val scope = CoroutineScope(Dispatchers.Default)

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
 //   val scope = CoroutineScope(Dispatchers.Default)
         scope.launch {
            log("start coroutine")
            val data = getData()
            log("end coroutine")
             // работает в Dispatchers.Main
//            updateUI(data)

        }
}
//Логи:
//
//start coroutine [DefaultDispatcher-worker-1]
//suspend function, start [DefaultDispatcher-worker-1]
//suspend function, background work [Thread-5]
//end coroutine [DefaultDispatcher-worker-3]
//
//Билдер создает dispatchedContinuation и вызывает его метод resume.
// Диспетчер находит свободный поток (DefaultDispatcher-worker-1) и отправляет туда Continuation на выполнение.
// Код Continuation выполняется и вызывает suspend функцию.
// При вызове suspend функции никакой диспетчер не используется. Идет обычный вызов метода.
// По логам (suspend function, start) мы видим, что suspend функция была вызвана в потоке корутины.
// На этом завершилось выполнение первой части Continuation.
//
//Далее suspend функция создает отдельный поток (Thread-5) и уходит туда,
// чтобы выполнить свою фоновую работу и не блокировать поток корутины (DefaultDispatcher-worker-1).
// По завершению этой работы, она вызывает dispatchedContinuation.resume, чтобы возобновить выполнение корутины.
// Диспетчер находит свободный поток (DefaultDispatcher-worker-3) и отправляет туда Continuation на продолжение выполнения.
// Continuation в этом потоке выполняет код из следующей ветки switch.

// Резюмируя все вышесказанное, получается такая схема.
//
//Первый вызов Continuation был выполнен билдером. Диспетчер отправил его в поток DefaultDispatcher-worker-1.
// В этом потоке выполнилась первая часть кода и произошел запуск suspend функции.
//
//log("start coroutine")
//val data = getData()
//Suspend функция по окончанию работы выполнила второй вызов Continuation.
// Диспетчер отправил его в поток DefaultDispatcher-worker-3.
// В этом потоке выполнилась оставшаяся часть кода.
//
//log("end coroutine")
//В итоге получилось так, что поток начала работы корутины не совпадает с потоком окончания.
// Это выглядит крайне необычно и запутанно, если не знать, что именно происходит под капотом.
// Но вы теперь знаете, что suspend функция может сменить поток вашей корутины.
// И не забывайте, что функции join() и await(), которые мы проходили в уроке про билдеры,
// это тоже suspend функции. А также функция delay.


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
//        job?.cancel()
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