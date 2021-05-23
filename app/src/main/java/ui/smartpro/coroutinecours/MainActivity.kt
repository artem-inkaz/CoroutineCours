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
//                TimeUnit.MILLISECONDS.sleep(1000)
                    delay(1000)
                //Если isActive начнет возвращать false, то мы прерываем цикл.
                log("coroutine, ${x++}, isActive=${isActive}")
            }
            log("coroutine, end")
        }

        log("onRun, end")
    }
// Когда внутри корутины используется suspend функция, поведение корутины при отмене может
// отличаться от того, что мы рассмотрели выше.
// Попробуем заменить TimeUnit.MILLISECONDS.sleep(1000) на delay(1000).
// Эта suspend функция также делает паузу в указанное количество миллисекунд.
//
//При запуске и отмене корутины логи будут следующими:
//20:29:33.578 onRun, start [main]
//20:29:33.606 onRun, end [main]
//20:29:33.612 coroutine, start [DefaultDispatcher-worker-1]
//20:29:34.619 coroutine, 0, isActive = true [DefaultDispatcher-worker-1]
//20:29:35.622 coroutine, 1, isActive = true [DefaultDispatcher-worker-3]
//20:29:36.201 onCancel [main]
//Отличие от предыдущих примеров в том, что функция delay при отмене корутины сразу прерывает
// выполнение кода корутина. Метод log после delay не вызывается и
// до проверки isActive в цикле уже не доходит.
//
//Функция delay подписывается на событие отмены корутины, и при возникновении такого события бросает
// специальный Exception. Именно поэтому выполнение кода корутины прерывается на delay и дальше не идет.
// Но этот Exception ловится и обрабатывается внутренними механизмами корутины,
// поэтому никакого крэша не происходит.
//
//Такую специфику имеет не все suspend функции, а только cancellable.


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