package ui.smartpro.coroutinecours

import android.location.Criteria
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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

        lifecycleScope.launch {
            while (true) {
                delay(1000)
                log("work")
            }
        }

//lifecycleScope может быть использован в MVP для вызова корутин в презентере
// (если конечно вы умышленно не держите презентер живым при поворотах экрана)
//
//
//
//У lifecycleScope есть билдеры launchWhenCreated, launchWhenStarted и
// launchWhenResumed для вызова корутин с отслеживанием текущего состояния LifeCycle.

//MainScope
//Еще одна возможность создания scope - это MainScope.
//
//Пример:

//val scope = MainScope()
//Под капотом похожий набор компонентов:

//ContextScope(SupervisorJob() + Dispatchers.Main)
//Но его главное отличие от двух предыдущих - он не привязан ни к чему.
//
//viewModelScope был привязан к модели. lifecycleScope - к LifecycleOwner.
// И нам не надо было думать о том, когда отменять эти scope.
// Они автоматически отменялись объектом, к которому они привязаны.
//
//А MainScope - сам по себе, и может быть создан и использован где угодно.
// Но нам надо самим следить за тем, когда его отменять.
// Т.е. если у нас есть какой-то свой объект с жизненным циклом и ему надо запускать корутины,
// то мы можем при его создании создавать ему MainScope, а при его уничтожении - отменять этот scope.

//Свой Scope
//Если вам не подходят все вышеописанные scope, то всегда можно создать свой с помощью CoroutineScope.
// И в нем самому указать Job нужного типа, диспетчер, Handler и прочее.

//val scope = CoroutineScope(Job() + Dispatchers.Main + handler)

//Также этот вариант подходит, когда нам в нашем объекте надо создать несколько scope с одним
// жизненным циклом, но разным диспетчером или типом Job, например.
//
//Главное - это привязка к какому-либо Lifecycle, чтобы корутины не оставались без присмотра.
// Хотя я видел примеры создания глобального scope, который был один на все приложение и использовался
// для различных вещей не привязанных к экрану, типа нотификаций.

        btnRun.setOnClickListener {

        }

        btnRun2.setOnClickListener {

        }

        btnCancel.setOnClickListener {
            onCancel()
        }
    }

    private fun onRun(){
        log("before")

        scope.launch {
            log("launch")
        }

        log("after")
    }
//Если в scope используется диспетчер Main, то мы в логах получим такой результат:
//before
//after
//launch

//А если мы используем диспетчер Main.immediate, то лог будет следующий:
//before
//launch
//after

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