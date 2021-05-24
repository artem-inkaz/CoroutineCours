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

    private fun onRun() {
//Кроме Job в context чаще всего хранится диспетчер, который задает поток корутины.
        //Чтобы сформировать Context, который будет содержать в себе Job и диспетчер
    val context = Job() + Dispatchers.Default
    //сформированный контекст может быть использован при создании scope
    val scope = CoroutineScope(context)
    //Теперь контекст созданного scope будет содержать в себе указанные Job и диспетчер
    val scope2 = CoroutineScope(Job() + Dispatchers.Default)
        log("context = $context")
//любой элемент, который можно поместить в context, сам по себе также является context-ом. Т.е. Job - это просто Context с одним элементом.
    val scope4 = CoroutineScope(Job())
// Dispatchers.Default - это Context с одним элементом
// scope поймет, что ему передают контекст с одним элементом - диспетчером
    val scope3 =    CoroutineScope(Dispatchers.Default)

}


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