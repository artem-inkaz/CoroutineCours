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

//   private val scope = CoroutineScope(Dispatchers.Unconfined)

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
   val scope = CoroutineScope(Dispatchers.Default)
        val job =  scope.launch {
            log("parent start")
            launch {
                log("child start")
                delay(1000)
                log("child end")
            }
            log("parent end")
        }
    scope.launch {
        delay(500)
        log("parent job is active: ${job.isActive}")
        delay(1000)
        log("parent job is active: ${job.isActive}")
    }
}

//Будем запускать отдельную корутину, которая в лог пишет статус джоба родительской корутины.
// Первый раз мы пишем лог, когда родительская корутина уже выполнила свой код,
// но ее дочерняя корутина еще работает. А второй лог запишется уже после того,
// как дочерняя завершила работу.
//
//Логи:
//
//22:03:03.820 parent start
//22:03:03.822 parent end
//22:03:03.823 child start
//22:03:04.327 parent job is active: true
//22:03:04.832 child end
//22:03:05.334 parent job is active: false
//
//Родительская корутина выполнила весь свой код и начала выполняться дочерняя.
// Проверяем статус родительской корутины - она все еще активна, хотя код ее завершен.
// Далее дочерняя корутина завершает работу и мы еще раз проверяем статус родительской корутины -
// теперь она завершена.
//
//Т.е. чтобы родительская корутина считалась завершенной, ей недостаточно просто выполнить весь свой код.
// Необходимо также, чтобы завершились все ее дочерние корутины.
//
//
//Каков практический смысл этого статуса? Где это используется?
//
//Например - при отмене корутин. Вызов метода cancel для родительской корутины каскадно отменит
// и все ее дочерние корутины. Но метод cancel сработает только для корутины, которая еще не завершена.
// Поэтому родительская корутина и ждет завершения дочерних,
// чтобы мы всегда могли отменить дочерние с помощью родительской.
//
//Еще один пример - метод join. В уроке про launch мы разбирали этот метод. Он будет ждать,
// чтобы корутина была именно завершена, а не просто выполнила свой код.
// Соответственно, если у ожидаемой корутины есть дочерние, то он будет ждать пока они не завершатся.





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
//        scope.cancel()
    }

    private fun log(text: String) {
        Log.d("TAG", "${formatter.format(Date())} $text [${Thread.currentThread().name}]")
    }
}