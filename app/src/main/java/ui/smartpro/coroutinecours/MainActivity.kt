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

            launch {
                log("child coroutine, start")
                TimeUnit.MILLISECONDS.sleep(1000)
                log("child coroutine, end")
            }
            log("parent coroutine,end")
        }
    }
// Билдер, вызванный в корутине не задерживает выполнение этой корутины.
// Он только создает и запускает дочернюю корутину, которая уходит работать в свой поток.
// А родительская корутина продолжает работу.
//20:32:16.221 parent coroutine, start [DefaultDispatcher-worker-1]
//20:32:16.222 parent coroutine, end [DefaultDispatcher-worker-1]
//20:32:16.222 child coroutine, start [DefaultDispatcher-worker-3]
//20:32:17.224 child coroutine, end [DefaultDispatcher-worker-3]
//Родительская корутина выполнила весь свой код не дожидаясь выполнения дочерней.
// А дочерняя корутина в отдельном потоке выполнила свой код.
//
//Хоть родительская корутина и выполнила сразу же весь свой код,
// но ее статус поменяется на Завершена только когда выполнится дочерняя корутина.
// Потому что родительская корутина подписывается на дочерние и ждет их завершения,
// прежде чем официально завершиться. Т.е. то, что корутина выполнила свой код, может и не означать,
// что она имеет статус Завершена.



    private fun onCancel(){
        log("onCancel")
//        job.cancel()
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