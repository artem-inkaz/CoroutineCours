package ui.smartpro.coroutinecours

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import ui.smartpro.coroutinecours.model.UserData
import java.lang.Exception
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

   private val scope = CoroutineScope(Job()+Dispatchers.Default)
    private val scope2 = CoroutineScope(Job()+Dispatchers.Default)

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

        scope.launch(CoroutineName("1")) {

//Можно выделить два основных назначения coroutineScope:
//1) создает свой scope, что позволяет ограничить распространение ошибки
//2) позволяет выносить вызов корутин в отдельные (suspend) функции
//Ошибка в корутине 1_2 приведет к тому, что отменятся все остальные корутины.
//
//Но что если для нас корутины 1_1 и 1_2 не критичны, и при их ошибках нам не надо отменять все вокруг.
// Для этого мы можем поместить их в coroutineScope
            // try catch
            try {
                twoCoroutines()
            } catch (e: Exception) {
                // ...
            }

            launch(CoroutineName("1_3")) {
                // ...
            }

            launch(CoroutineName("1_4")) {
                // ...
            }
        }
}

//coroutineScope - это suspend функция, внутри которой запускается специальная корутина ScopeCoroutine.
// Код, который мы пишем в блоке coroutineScope, становится кодом этой корутины и выполняется именно в ней.
//
//Следовательно, в примере выше у нас между корутинами 1 и 1_2 образуется связь:
//Coroutine_1 > ScopeCoroutine > Coroutine_1_2
//а не:
//Coroutine_1 > Coroutine_1_2
//
//Т.е. coroutineScope вклинивает новую корутину ScopeCoroutine между нашими родительской и
// дочерней корутиной, тем самым меняя связь между ними, и, как следствие,
// поведение при возникновении ошибки.
//
//Основная особенность ScopeCoroutine в том, что она не передает ошибку родителю.
// Т.е. если в корутине 1_2 произойдет ошибка, то она пойдет в ScopeCoroutine,
// которая отменит себя и своих детей (т.е. корутину 1_1), но своему родителю (корутине 1)
// она ошибку передавать не будет. Т.е. ошибка не пойдет дальше вверх по иерархии корутин.
// Поэтому корутина 1 не отменится сама и не отменит все свои дочерние корутины (1_3 и 1_4).
//
//Но! Ошибка из корутины 1_2 не пропадает просто так. ScopeCoroutine,
// хоть и не передаст ее своему родителю, но она передаст ее в свою обертку - suspend функцию coroutineScope.
// И suspend функция выбросит эту ошибку в наш код в месте своего вызова.
// И если не поймать ее там, то мы получим стандартное поведение при ошибке в корутине 1.
// И в результате все поотменяется
//Чтобы этого не произошло, обернем coroutineScope в try-catch

    // можно coroutineScope и все его содержимое вынести в отдельную suspend функцию
    private suspend fun twoCoroutines() {
        coroutineScope {
            launch(CoroutineName("1_1")) {
                // ...
            }

            launch(CoroutineName("1_2")) {
                // exception
            }
        }
    }




//Есть какая-то suspend функция, которая перед тем, как начать асинхронную работу, проверяет кэш.
// Если она находит там результат, то сразу отправляет его обратно в корутину.
//
//Если этот код выбросит исключение, то это будет равносильно тому,
// что suspend функция в корутине выбросила исключение.
    suspend fun someFunction(): String =
            suspendCancellableCoroutine { continuation ->

//                val result = getFromCache()

                if (result != null) {
                    continuation.resume(result)
                } else {
                    // async code
                }

            }

// ВАЖНо
//Асинхронная часть
//Если же ошибка произойдет в асинхронной части и не будет там поймана в try-catch,
// то приложение свалится с крэшем. И даже если обернуть suspend функцию в try-catch или использовать
// CoroutineExceptionHandler, это не поможет.
//
//Потому что это был отдельный поток. И выполнялась там не корутина,
// которая автоматически ловит все ошибки своего кода, а обычный код.
// А непойманное исключение в обычном коде приводит к крэшу.


//Расширение для CoroutineScope. Мы сможем запускать его прямо в корутине.
// Оно будет периодически выводить в лог имя и isActive статус корутины
   fun CoroutineScope.repeatIsActive() {
       repeat(5) {
           TimeUnit.MILLISECONDS.sleep(300)
           log("Coroutine_${coroutineContext[CoroutineName]?.name} isActive $isActive")
       }
   }


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
        scope.cancel()
    }

    private fun log(text: String) {
        Log.d("TAG", "${formatter.format(Date())} $text [${Thread.currentThread().name}]")
    }
}