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

        btnRun2.setOnClickListener {
            onRun2()
        }

        btnCancel.setOnClickListener {
            onCancel()
        }
    }
//С помощью async мы можем вызвать эти функции параллельно, чтобы сократить общее время их выполнения.
// Помещаем вызов каждой suspend функции в отдельную async корутину.
// И методом await получаем результат.
    private fun onRun() {

    scope.launch {
        log("parent coroutine, start")
        val data = async { getData()}
        val data2 = async { getData2()}
        log("parent coroutine, wait until children return result")
        val result = "${data.await()}, ${data2.await()}"
        log("parent coroutine, children returned: $result")
        log("parent coroutine, end")
    }
}
//Логи:
//
//11:00:52.751 parent coroutine, start [DefaultDispatcher-worker-1]
//11:00:52.758 parent coroutine, wait until children return result [DefaultDispatcher-worker-1]
//11:00:54.268 parent coroutine, children returned: data, data2 [DefaultDispatcher-worker-3]
//11:00:54.269 parent coroutine, end [DefaultDispatcher-worker-3]
//
//Теперь логи показывают, что общее время выполнения родительской корутины составило около 1500 мсек.
// Наши suspend функции выполнялись параллельно в разных async корутинах,
// а родительская корутина дождалась выполнения обеих и получила результаты выполнения.
//
//А теперь снова представьте, что родительская корутина выполняется в main потоке.
// А suspend-функции - это получение данных с сервера с помощью, например, Retrofit.
// Вы выполняете запросы в фоновых потоках, получаете данные в main потоке и
// отображаете их не в логе, а на экране. И никаких колбэков.
// В этом и есть одна из основных возможностей корутин - лаконичный асинхронный код.
//Lazy
//async корутину также можно запускать в режиме Lazy. Метод await стартует ее выполнение.

    private suspend fun getData(): String{
        delay(1000)
        return "data"
    }

    private suspend fun getData2(): String {
        delay(1500)
        return "data2"
    }

    private fun onRun2(){
        log("onRun2, start")
        job.start()
        log("onRun2, end")
    }

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