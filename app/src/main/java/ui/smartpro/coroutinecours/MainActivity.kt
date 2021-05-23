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

    private fun onRun(){
        log("onRun, start")

        job = scope.launch {
            log("coroutine, start")
            var x = 0
            while (x < 5 && isActive) {
                TimeUnit.MILLISECONDS.sleep(1000)
                //Если isActive начнет возвращать false, то мы прерываем цикл.
                log("coroutine, ${x++}, isActive=${isActive}")
            }
            log("coroutine, end")
        }

        log("onRun, end")
    }
// После нажатия на кнопку Cancel, метод isActive начинает возвращать false.
// Это означает, что корутина больше не активна. Ее выполнение или ее результат более никому не нужны.
// Можно прервать ее работу, что мы и делаем в условии цикла.
//11:27:45.001 onRun, start [main]
//11:27:45.048 onRun, end [main]
//11:27:45.053 coroutine, start [DefaultDispatcher-worker-1]
//11:27:46.053 coroutine, 0, isActive = true [DefaultDispatcher-worker-1]
//11:27:47.055 coroutine, 1, isActive = true [DefaultDispatcher-worker-1]
//11:27:47.665 onCancel [main]
//11:27:48.057 coroutine, 2, isActive = false [DefaultDispatcher-worker-1]
//11:27:48.057 coroutine, end [DefaultDispatcher-worker-1]



    private fun onCancel(){
        log("onCancel")
        job.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        log("onDestroy")
        scope.cancel()
    }
// Попробуем нажать кнопку Run и через пару секунд закрыть приложение.
//11:32:14.242 onRun, start [main]
//11:32:14.284 onRun, end [main]
//11:32:14.293 coroutine, start [DefaultDispatcher-worker-1]
//11:32:15.294 coroutine, 0, isActive = true [DefaultDispatcher-worker-1]
//11:32:16.295 coroutine, 1, isActive = true [DefaultDispatcher-worker-1]
//11:32:16.355 onDestroy [main]
//11:32:17.297 coroutine, 2, isActive = false [DefaultDispatcher-worker-1]
//11:32:17.297 coroutine, end [DefaultDispatcher-worker-1]
//
//Логи показывают, что корутина была отменена. Scope сделал свое дело.

    private fun log(text: String) {
        Log.d("TAG", "${formatter.format(Date())} $text [${Thread.currentThread().name}]")
    }
}