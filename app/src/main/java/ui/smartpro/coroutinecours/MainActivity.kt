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
// В обработчике CoroutineExceptionHandler мы на вход получаем ошибку и контекст корутины,
// которая передала ошибку в этот обработчик.
// Мы из контекста достаем CoroutineName и с его помощью узнаем, какая корутина обработала ошибку.
        val handler = CoroutineExceptionHandler { context, exception ->
            log("$exception was handled in Coroutine_${context[CoroutineName]?.name}")
        }

 //Не забывайте, что контекст корутин формируется не только из того, что было передано в билдер,
        // но и того, что пришло из контекста родителя.
        // Поэтому, если вам надо поместить ваш CoroutineExceptionHandler обработчик во все корутины,
        // то просто поместите его в scope, и он будет передан во все корутины, созданные в нем.
        //
        //val handler = CoroutineExceptionHandler { context, exception ->
        //   log("coroutine exception $exception")
        //}
        // Обработчик handler мы помещаем в scope, поэтому он будет передан во все корутины.
        val scope = CoroutineScope(Job() + Dispatchers.Default + handler)

//CoroutineName - это элемент контекста.
// Помещаем в него имя корутины и передаем в launch билдеры.
// Теперь в каждой корутине мы сможем из контекста достать этот элемент и узнать, какая это корутина.

        scope.launch(CoroutineName("1")) {

            launch(CoroutineName("1_1")) {
            TimeUnit.MILLISECONDS.sleep(1000)
                log("exception")
            Integer.parseInt("a")
        }
            launch(CoroutineName("1_2")) {
               repeatIsActive()
            }

            repeatIsActive()
        }

        scope.launch(CoroutineName("2")) {
            launch(CoroutineName("2_1")) {
                repeatIsActive()
            }
            launch(CoroutineName("2_2")) {
                repeatIsActive()
            }

            repeatIsActive()
        }

// Костыль чтобы не отменялись все дочерние коррутины используя SupervisorJob
//Есть костыль, который можно использовать, чтобы сохранить связь:
//

//scope.launch { // parent coroutine
//
//   launch(SupervisorJob(coroutineContext[Job])) { // child coroutine
//
//   }
//}

//Мы в SupervisorJob передаем Job родительской корутины.
    // Это приведет к тому, что этот родительский Job станет родителем SupervisorJob. А, т.к.
    // сам SupervisorJob станет родителем для Job-а дочерней корутины, то получится такая связь:
//parentJob > supervisorJob > childJob
//
//Получилось три джоба на две корутины. Но зато связь между корутинами сохраняется -
    // при отмене родителя отменятся и его дочерние корутины.
//
//Если теперь в дочерней корутине произойдет ошибка, то childJob передаст ее в supervisorJob,
    // а тот ничего не будет отменять и дальше (в parenJjob) эту ошибку передавать не будет.
    // Т.е. ошибка будет направлена в обработчик дочерней корутиной.


}

// при возникновении ошибки корутина сообщает об этом родителю. Он отменяет всех своих детей.
// А корутина пытается сама обработать ошибку.
//
//Это был немного упрощенный алгоритм действий корутины.
// В нем не хватает одного важного этапа взаимодействия между родителем и корутиной.
// Когда корутина сообщает родителю об ошибке, она передает ему эту ошибку и спрашивает,
// сможет ли родитель ошибку обработать. Ответ родителя зависит от того, кем этот родитель является
// - скоупом или корутиной.
//
//
//
//Если родитель - скоуп, то он отвечает, что не сможет.
// Поэтому корутина пытается это сделать сама. Об этом мы подробно говорили в прошлом уроке.
// Она отправляет ошибку в обработчик CoroutineExceptionHandler.
// А если такого обработчика ей не предоставили, то ошибка уходит в глобальный обработчик,
// и приложение падает.
//
//
//
//А если родитель - корутина, то она отвечает, что сможет.
// И дочерняя корутина вместо всевозможных обработчиков передает ошибку родительской корутине.
//
//Родительская корутина, хоть и сказала, что обработать сможет, но сначала сама спрашивает у
// своего родителя, сможет ли он обработать. И если может, то передает ему ошибку.
//
//Таким образом эта ошибка поднимается наверх по иерархии вложенных корутин,
// пока не достигнет самой верхней корутины. Ее родитель - это скоуп.
// Когда самая верхняя корутина спросит его, он скажет, что ошибку обработать не сможет.
// И в итоге самая верхняя корутина отправляет ошибку в обработчики,
// как мы рассматривали в прошлом уроке.
//
//При этом каждый родитель, через который прошла ошибка,
// будет отменяться сам и отменять все свои дочерние корутины.
// Тут поведение не отличается от скоуп.

//Из объяснений выше следует, что обработчик CoroutineExceptionHandler сработает только в самой
// верхней корутине. Потому что только она получит от своего родителя отрицательный
// ответ и попытается обработать ошибку сама. Остальные корутины просто передают
// ошибку родительской корутине и сами ничего с ней делают.

//*************************************************************************************************
//Запускаем, смотрим лог:
//19:08:47.961 Coroutine_2 isActive true
//19:08:47.961 Coroutine_1 isActive true
//19:08:47.964 Coroutine_1_2 isActive true
//19:08:47.964 Coroutine_2_1 isActive true
//19:08:47.968 Coroutine_2_2 isActive true
//
//19:08:48.264 Coroutine_2 isActive true
//19:08:48.264 Coroutine_1 isActive true
//19:08:48.264 Coroutine_1_2 isActive true
//19:08:48.265 Coroutine_2_1 isActive true
//19:08:48.270 Coroutine_2_2 isActive true
//
//19:08:48.566 Coroutine_1 isActive true
//19:08:48.566 Coroutine_2_1 isActive true
//19:08:48.566 Coroutine_1_2 isActive true
//19:08:48.566 Coroutine_2 isActive true
//19:08:48.572 Coroutine_2_2 isActive true
//
//19:08:48.663 exception
//
//19:08:48.869 Coroutine_1 isActive false
//19:08:48.869 Coroutine_2 isActive false
//19:08:48.869 Coroutine_2_1 isActive false
//19:08:48.869 Coroutine_1_2 isActive false
//19:08:48.874 Coroutine_2_2 isActive false
//
//19:08:49.172 Coroutine_1 isActive false
//19:08:49.172 Coroutine_2_1 isActive false
//19:08:49.175 Coroutine_1_2 isActive false
//19:08:49.175 java.lang.NumberFormatException: For input string: "a" handled in Coroutine_1
//19:08:49.184 Coroutine_2_2 isActive false
//19:08:49.172 Coroutine_2 isActive false
//
//
//Поначалу все корутины активны. Но после того, как в корутине 1_1 была вызвана ошибка,
// все остальные корутины этого scope были отменены.
//
//Т.е. ошибка поднимается наверх до scope, отменяя все, что можно.
// И далее распространяется на остальные корутины этого scope,
// каскадно отменяя в них все дочерние корутины.



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