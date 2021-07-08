package ui.smartpro.coroutinecours

import android.location.Criteria
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
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

//поиск авиарейсов. Предположим, что мы написали некую функцию агрегатор,
// которая по определенным критериям ищет рейсы.
    fun serchFlight(criteria: Criteria):  List<Flight> {

        val results = mutableListOf<Flight>()

        for ( airCarrier in  getAirCarriers()) {
            //Найденный рейс добавляется в коллекцию, которая затем возвращается,
                // как результат работы функции.
            val flight = airCarrier.searchFlight(criteria)
            results.add(flight) 
        }
        //Вызываем ее, получаем полный список рейсов через какое-то время и выводим их на экран.
        return results

    }
 //Вызов
    val flights = searchFlight(criteria)
    showFlights(flights)

 //Было бы круто выдавать рейсы по одному, по мере их нахождения у авиаперевозчиков.
    // то позволит начать выводить на экран первые результаты поиска почти сразу же после старта,
    // а не ждать пока поиск полностью завершится.
 fun searchFlight(criteria: Criteria, onFind: (Flight) -> Unit) {
     for (airCarrier in getAirCarriers()) {
         val flight = airCarrier.searchFlight(criteria)
         onFind(flight)
     }
 }
//Вызов
    searchFlight(criteria) { flight ->
        showFlight(flight)
    }

//наша функция при запуске не выполняла поиск, а возвращала объект-функцию, которая умеет выполнять поиск.
// И клиент мог бы у себя в коде ее хранить и передавать, как обычный объект.
// А когда нужно, он мог бы ее запускать.
fun searchFlight(criteria: Criteria): ((Flight) -> Unit) -> Unit {
    return { onFind ->
        for (airCarrier in getAirCarriers()) {
            val flight = airCarrier.searchFlight(criteria)
            onFind(flight)
        }
    }
}

    //Вызов
    val search = searchFlight(criteria)

/// ...

    search.invoke { flight ->
        showFlight(flight)
    }

//Давайте снова перепишем нашу функцию, но в этот раз используем Flow.
fun searchFlight(criteria: Criteria): Flow<Flight> {
    return flow {
        for (airCarrier in getAirCarriers()) {
            val flight = airCarrier.searchFlight(criteria)
            emit(flight)
        }
    }
}

    //Вызов
    val searchFlow = searchFlight(criteria)

// ...

    scope.launch {
        searchFlow.collect { flight ->
            showFlight(flight)
        }
    }

// Работа с Flow
    //1. Создается объект Flow, он хранит в себе блок flow, в котором содержится код по
// генерации данных и отправке их в метод emit.
    val flow = flow {
        // flow block
        for (i in 1..10) {
            delay(100)
            emit(i)
        }
    }
//2. Чтобы запустить работу и начать получать данные Flow, мы вызываем его метод collect и
// на вход этому методу передаем блок collect, в который мы хотели бы получать данные,
// создаваемые в блоке flow.
    flow.collect { i ->
        // collect block
        log(i)
    }

    //Отличие от каналов
    //И с каналом и с Flow можно отправлять/получать данные. Возможно, именно поэтому их можно
    // спутать при поверхностном рассмотрении.
    //
    //Но, канал - это просто потокобезопасный инструмент для передачи данных между корутинами.
    // Он не создает ничего. Только передает. Т.е. должны существовать отправитель и получатель.
    // Они работают в своих корутинах независимо друг от друга, используя канал для обмена данными.
    //
    //А Flow - это генератор данных. В этом случае нет явного отправителя.
    // Вместо него есть создатель. Он создает Flow и дает его получателю.
    // Получатель запускает Flow в своей корутине и, можно сказать, сам же становится отправителем.

//Расширенная suspend функция
//Есть еще один интересный момент, который я хотел бы обсудить. Ничего нового сейчас не скажу, но акцентирую внимание на том, что может быть неочевидно.
//
//Обычно suspend функция возвращает нам одно значение. И пока мы ждем это значение, корутина приостанавливается. Flow позволяет расширить это поведение. Он делает так, что мы можем получать последовательность (поток) данных вместо одного значения. И это будет происходит в suspend режиме. Т.е. корутина будет приостанавливаться на время ожидания каждого элемента.

//В принципе мы уже это видели в этом уроке ранее, но давайте взглянем еще раз на примере с поиском рейсов.
//
//Есть такая suspend функция:

//suspend fun searchFlight(criteria: Criteria): List<Flight>
//Она по заданным критериям возвращает список полетов.
//
//Мы запускаем ее в корутине и ждем, пока она отработает и вернет нам данные.

//launch {
//    val flights = searchFlight(criteria)
//}
//Ничего необычного. suspend функция возвращает одно значение. И пока мы ждем это значение, корутина приостанавливается.

//Переписываем функцию, чтобы она возвращала Flow:

//fun searchFlight5(criteria: Criteria): Flow<Flight>
//Теперь эта функция не обязана быть suspend.

//И вызываем ее в корутине

//launch {
//    searchFlight(criteria).collect { flight ->
//        showFlight(flight)
//    }
//}
//В этом случае suspend функцией является метод collect().
// Но он не возвращает нам одно единственное значение.
// Он позволяет нам получить последовательность данных.
// И он приостанавливает корутину на время следующего элемента.
// А когда последовательность закончится, корутина пойдет дальше.

//Т.е. Flow расширяет возможности suspend функций,
// позволяя нам получать последовательность данных в suspend режиме.

// Flow является холодным источником данных. Он для каждого получателя будет генерировать данные заново.
//
//Т.е. каждый вызов collect будет приводить к тому, что снова будет запускаться блок flow и
// генерировать элементы. Потому что collect - это всего лишь вызов блока кода flow.

    private fun getAirCarriers() {

    }

    @ExperimentalCoroutinesApi
    private fun onRun() {

        scope.launch(CoroutineName("1")) {

 //trySend
//Кроме suspend метода send, канал дает нам возможность попытаться отправить данные обычным
        // (не suspend) методом.
        // В этом случае если получатель не готов к приему, то данные просто не уйдут.
//
//Раньше для этого использовался метод offer, но сейчас он объявлен устаревшим.
        // Вместо него используем trySend.
//
//В качестве результата он вернет ChannelResult,
        // из которого можно понять, удалось ли отправить данные.
        }
}


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

//                if (result != null) {
//                    continuation.resume(result)
//                } else {
                    // async code
 //               }

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