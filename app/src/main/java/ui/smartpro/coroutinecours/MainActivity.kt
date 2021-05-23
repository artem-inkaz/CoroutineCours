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
                TimeUnit.MILLISECONDS.sleep(1000)
            }
            val job2= launch {
                TimeUnit.MILLISECONDS.sleep(1000)
            }
// Теперь в точке вызова join родительская корутина будет ждать, пока не выполнится дочерняя.
// join - это suspend функция, поэтому она только приостановит выполнение родительской корутины,
// но не заблокирует ее поток.
            log("parent coroutine, wait until child completes")
            job.join()
            job2.join()
            log("parent coroutine, end")
        }
    }
//20:46:38.889 parent coroutine, start [DefaultDispatcher-worker-1]
//20:46:38.893 parent coroutine, wait until children complete [DefaultDispatcher-worker-1]
//20:46:40.395 parent coroutine, end [DefaultDispatcher-worker-3]
//Запускаем дочерние корутины, а потом для обеих вызываем join, тем самым дожидаясь окончания их работы.
// Дочерние корутины отработают параллельно, поэтому общее время работы родительской корутины составит 1500.


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