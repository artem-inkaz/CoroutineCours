package ui.smartpro.coroutinecours

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    //указываем время
    private var formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val scope = CoroutineScope(Job())

    lateinit var job: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRun.setOnClickListener {
            onRun()
        }

        btnCancel.setOnClickListener {
            onCancel()
        }
    }
//В методе onRun стартуем корутину, а внутри нее еще одну корутину.
    private fun onRun(){
        scope.launch {
            log("parent coroutine, start")

           val job= launch {
                log("child coroutine, start")
                TimeUnit.MILLISECONDS.sleep(1000)
                log("child coroutine, end")
            }
// Теперь в точке вызова join родительская корутина будет ждать, пока не выполнится дочерняя.
// join - это suspend функция, поэтому она только приостановит выполнение родительской корутины,
// но не заблокирует ее поток.
            log("parent coroutine, wait until child completes")
            job.join()

            log("parent coroutine, end")
        }
    }

//20:40:01.927 parent coroutine, start [DefaultDispatcher-worker-1]
//20:40:01.928 parent coroutine, wait for child [DefaultDispatcher-worker-1]
//20:40:01.928 child coroutine, start [DefaultDispatcher-worker-2]
//20:40:02.930 child coroutine, end [DefaultDispatcher-worker-2]
//20:40:02.931 parent coroutine, end [DefaultDispatcher-worker-2]
//код родительской корутины завершился после того, как отработала дочерняя корутина.
//
// Обратите внимание на потоки.
// Родительская корутина начала свою работу в потоке DefaultDispatcher-worker-1,
// а завершила в потоке в DefaultDispatcher-worker-2, в котором дочерняя корутина выполнялась.
// Это особенность работы suspend функций и диспетчеров.


    private fun onCancel(){
        log("onCancel")
        job.cancel()
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