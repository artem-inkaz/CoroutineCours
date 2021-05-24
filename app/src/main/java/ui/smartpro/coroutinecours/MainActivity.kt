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
//В методе onRun стартуем корутину, а внутри нее еще одну корутину.
    private fun onRun() {

    scope.launch {
        log("parent coroutine, start")
//создаем и стартуем дочернюю корутину. Билдер возвращает нам Deferred объект
        val deferred = async() {
            log("child coroutine, start")
            TimeUnit.MILLISECONDS.sleep(1000)
            "async result"
        }
        log("parent coroutine, wait until child returns result")
// метод await приостановит выполнение родительской корутины, пока не выполнится дочерняя,
// и вернет результат дочерней корутины.
// Дочерняя корутина имитирует работу в течение одной секунды, а затем возвращает строку "async result".
        val result = deferred.await()
        log("parent coroutine, child returns: $result")
        log("parent coroutine, end")
    }
}
//parent coroutine, start [DefaultDispatcher-worker-1]
//parent coroutine, wait until child returns result [DefaultDispatcher-worker-1]
//child coroutine, start [DefaultDispatcher-worker-2]
//child coroutine, end [DefaultDispatcher-worker-2]
//parent coroutine, child returns: async result [DefaultDispatcher-worker-2]
//parent coroutine, end [DefaultDispatcher-worker-2]
//
//Все аналогично примеру с launch+join. Билдер async создает и запускает дочернюю корутину.
// Родительская корутина продолжает выполняться и останавливается на методе await.
// Теперь она в ожидании завершения дочерней корутины.
//
//Дочерняя корутина выполняется в отдельном потоке.
// По ее завершению метод await возвращает результат ее работы (строка “async result”), и
// возобновляет выполнение кода родительской корутины.


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