package ui.smartpro.coroutinecours.testing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

//В этом уроке мы разбираемся зачем нужен runBlocking, чем он отличается от runBlockingTest, и
// что умеет делать TestCoroutineDispatcher.

//runBlocking
//Тестирование кода с корутинами не сильно отличается от тестирования обычного кода, особенно в
// простых случаях. Для написания тестов нам понадобится билдер runBlocking.
// Давайте на простом примере рассмотрим, почему без него не обойтись.

//У нас есть простой класс с suspend методом:

class MyClass {

   suspend fun someMethod(): String {
       return "abc"
   }

}

//Тест для метода someMethod мог бы выглядеть так:

@Test
fun testSomeMethod() {
   val actualValue = myClass.someMethod()
   assertEquals("abc", actualValue)
}
//Но не все так просто. someMethod - это suspend функция. Она не может быть вызвана в обычном коде.
// Нам нужна корутина, внутри которой мы сможем вызвать эту suspend функцию.
//
//
//
//Давайте добавим в тест обычную корутину:

@Test
fun testSomeMethod() {
   CoroutineScope(Dispatchers.Default).launch {
       val actualValue = myClass.someMethod()
       assertEquals("abc", actualValue)
   }
}
//В корутине вызываем suspend функцию.
//
//Теперь все в порядке, можно запускать тест, и он даже покажет положительный результат.
// Но на самом деле он не выполнялся.

//Чтобы проверить, работает ли тест, поменяем его так, чтобы он точно вернул отрицательный результат:

@Test
fun testSomeMethod() {
   CoroutineScope(Dispatchers.Default).launch {
       val actualValue = myClass.someMethod()
       assertEquals("abcdef", actualValue)
   }
}
//Будем проверять, что нам приходит значение "abcdef", хотя приходит "abc".
//
//Запускаем тест, а он все равно зеленый. Явно что-то не так.
// Такое ощущение, что assert просто не срабатывает.

//Добавим логирование, чтобы понять в чем проблема.

@Test
fun testSomeMethod() {
   println("test start")
   CoroutineScope(Dispatchers.Default).launch {
       println("launch")
       val actualValue = myClass.someMethod()
       assertEquals("abcdef", actualValue)
   }
   println("test end")
}
//Запускаем и видим:
//
//test start
//test end
//
//По логам видно, что launch не выполнялся. Так получилось потому что, как мы знаем, в момент
// вызова launch корутина не запускается, а передается в диспетчер, который помещает ее в
// очередь и запустит чуть позже в своем потоке.
//
//Эта схема прекрасно работает в Android, где у нас процесс приложения живет все время пока
// запущено приложение. Но процесс, который отвечает за запуск теста - не такой долгожитель.
// Он создается, выполняет тест и завершается. И его не волнует то, что какой-то диспетчер поместил
// задачу в очередь, чтобы потом в отдельном потоке ее выполнить. С точки зрения процесса,
// выполняющего тест, в коде теста был только запуск билдера launch. Тест это сделал и завершился.
// А содержимое launch до своего запуска просто не дожило и assert не выполнился.
//
//Чтобы исправить это и получить возможность запускать корутины в таких недолговечных процессах,
// мы используем runBlocking. Этот билдер создает корутину и будет держать процесс, пока она не выполнится.
// А мы внутри этой корутины можем вызывать suspend функции и вызывать свои корутины, если необходимо.

//Используем runBlocking в нашем специально сломанном тесте, который должен вернуть ошибку:

@Test
fun testSomeMethod() = runBlocking {
   val actualValue = myClass.someMethod()
   assertEquals("abcdef", actualValue)
}
//Получаем ошибку:
//expected:<abc[def]> but was:<abc[]>
//
//Теперь видно, что assert вызывается и работает.

//Фиксим проверяемое значение в тесте:

@Test
fun testSomeMethod() = runBlocking {
   val actualValue = myClass.someMethod()
   assertEquals("abc", actualValue)
}
//Теперь все в порядке - результат зеленый, тест рабочий.

//В итоге тест suspend функции выглядит как тест обычной функции. Разница только в использовании runBlocking.
//
//Если нам надо протестировать Flow, то мы будем вызывать метод Flow.collect, чтобы получить данные и
// проверить их. И т.к. collect - это suspend функция, то для ее запуска в тестах мы также
// будем использовать runBlocking.

//runBlockingTest
//Создатели корутин предоставили нам еще пару инструментов для написания тестов.
// И хотя они все еще в экспериментальном статусе, давайте рассмотрим их.
//
//Добавьте зависимость:

//testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.2'
//
//
//Один из инструментов - это runBlockingTest. В чем его отличие от runBlocking?
//
//Рассмотрим такую функцию:

suspend fun someMethod(): String {
   delay(5000)
   return "abc"
}
//Время работы этой функции в runBlocking составит 5 секунд. А в runBlockingTest - миллисекунды.
// Потому что диспетчер, который работает под капотом runBlockingTest умеет проматывать время из delay.

//Еще одно важное отличие от runBlocking состоит в том, что в runBlockingTest корутина выполняется сразу,
// а не уходит в диспетчер на последующее выполнение.
//
//Рассмотрим пример, чтобы увидеть эту разницу в поведении:

suspend fun someMethod(): String {
   return coroutineScope {
       println("before launch")
       launch {
           println("launch")
       }
       println("after launch")
       "abc"
   }
}
//Есть suspend функция, внутри которой мы вызываем корутину launch. По логам мы увидим,
// в каком порядке будет выполнен код.

//Вызвав эту функцию в runBlocking, мы в логах видим:
//
//before launch
//after launch
//launch
//
//Это стандартное и привычное поведение корутин. Его мы уже обсуждали чуть ранее в этом уроке.
// Билдер launch создает корутину, а выполняется она чуть позже.

//Вызвав эту функцию в runBlockingTest, мы в логах видим:
//
//before launch
//launch
//after launch
//
//Диспетчер внутри runBlockingTest выполняет переданную ему корутину сразу, а не отправляет ее в очередь.

//TestCoroutineDispatcher
//В паре предыдущих примеров мы уже говорили о диспетчере внутри runBlockingTest.
// Это TestCoroutineDispatcher. У него есть пара интересных методов, влияющих на поведение корутин.

//pause/resume
//В качестве примера рассмотрим метод в ViewModel:

fun fetchData() {
   showLoading = true

   viewModelScope.launch {
        val data = serviceApi.getData()
        showLoading = false

        // ...
    }
}
//Он включает флаг отображения индикатора загрузки и запускает корутину, в которой грузит данные
// и затем выключает флаг.

//Напишем тест для этого метода. в котором будем проверять, что флаг сначала ставится в true,
// а затем в false. Но перед этим нужна небольшая подготовка.
//
//Под капотом viewModelScope работает диспетчер Main. В Android такой диспетчер есть,
// а вот в unit-тестах - нет. Поэтому тест при запуске будет выдавать ошибку:
//Exception in thread "Test worker @coroutine#1" java.lang.IllegalStateException: Module with the
// Main dispatcher had failed to initialize. For tests Dispatchers.setMain from kotlinx-coroutines-test
// module can be used
//
//Нам предлагают использовать метод Dispatchers.setMain, чтобы глобально заменить Main диспетчер в тестах.
// Используем TestCoroutineDispatcher:

val testDispatcher = TestCoroutineDispatcher()

@Before
fun setUp() {
   Dispatchers.setMain(testDispatcher)
}

@After
fun tearDown() {
   Dispatchers.resetMain()
   testDispatcher.cleanupTestCoroutines()
}
//Ставим его как Main до запуска теста и сбрасываем после.
//
//ViewModelScope теперь будет использовать этот диспетчер. Но нам также надо использовать этот же
// экземпляр диспетчера и в runBlockingTest. Иначе runBlockingTest создаст свой экземпляр
// TestCoroutineDispatcher. И получится, что корутина runBlockingTest работает в одном
// TestCoroutineDispatcher, а корутина в ViewModel в другом TestCoroutineDispatcher.
// Это неправильно. Для корректной работы с TestCoroutineDispatcher нужно, чтобы все наши корутины
// выполнялись в одном его экземпляре. Поэтому созданный TestCoroutineDispatcher будем не только
// ставить как Main, но и передавать в runBlockingTest.

//В итоге метод теста будет выглядеть так:

@Test
fun test() = runBlockingTest(testDispatcher) {
   classForTest.fetchData()
   assertTrue(classForTest.showLoading)
   assertFalse(classForTest.showLoading)
}
//В тесте мы запускаем метод fetchData и хотим проверить, что этот метод сначала поставил флаг в
// true, а затем в false.
//
//Запускаем тест и он выполняется неуспешно. Так произошло потому что fetchData сразу выполнил весь
// свой код, включая код корутины. И значение true в showLoading мы просто не успели поймать.

//В этом случае нам поможет метод pauseDispatcher. Он притормозит выполнение кода:

@Test
fun test() = runBlockingTest(testDispatcher) {
   testDispatcher.pauseDispatcher()

   classForTest.fetchData()
   assertTrue(classForTest.showLoading)

   testDispatcher.resumeDispatcher()
   assertFalse(classForTest.showLoading)
}
//Методом pauseDispatcher мы переводим диспетчер в режим паузы.
//
//Запускаем fetchData. В нем мы ставим флаг в true, а билдер launch отправляет корутину диспетчеру.
// Но диспетчер не будет сразу ее выполнять, т.к. он на паузе. Он будет ждать, пока мы не снимем
// паузу (или пока не закончится код внутри runBlockingTest).
//
//Запуск корутины отложен. Это дает нам возможность проверить, что showLoading установлен в true.
//
//Далее мы снимаем паузу методом resumeDispatcher. Корутина выполняется и мы проверяем, что флаг
// установлен в false.

//advanceTimeBy
//Если у нас в коде есть вызовы delay, то TestCoroutineDispatcher дает возможность не только
// автоматической, но и ручной перемотки времени.
//
//В качестве тестируемого кода рассмотрим такой метод в ViewModel:

fun showAndHideDialog() {
   showDialog = false

   viewModelScope.launch {
       delay(1000)
       showDialog = true
       delay(3000)
       showDialog = false
   }
}
//Мы ждем 1000 мсек, показываем диалог на 3 секунды, затем скрываем его.

//Пишем тест для этого метода:

@Test
fun test() = runBlockingTest(testDispatcher) {
   testDispatcher.pauseDispatcher()

   classForTest.showAndHideDialog()
   assertFalse(classForTest.showDialog)

   testDispatcher.advanceTimeBy(1000)
   assertTrue(classForTest.showDialog)

   testDispatcher.advanceTimeBy(3000)
   assertFalse(classForTest.showDialog)

}
//Сначала ставим диспетчер на паузу. Это позволит нам вручную перематывать время в delay.
//
//Затем запускаем метод и проверяем, что флаг установлен в false.
//
//Далее методом advanceTimeBy проматываем время на 1000 мсек. Это вынуждает диспетчер запустить
// корутину (которую ему отправил launch) и перемотать виртуальное время для delay(1000).
// Выполнение корутины остановится на delay(3000). Это дает нам возможность проверить,
// что флаг был установлен в true.
//
//Далее снова мотаем время на 3000, чтобы завершился delay(3000), а за ним и корутина. И проверяем,
// что флаг установлен в false.

//Прочее
//Напоследок напишу несколько интересных моментов, касающихся тестов.
//
//В последнем примере можно было бы advanceTimeBy(3000) заменить на метод resumeDispatcher(), т.к.
// после delay(3000) нам уже не нужны были остановки. Диспетчер, снятый с паузы, сам перемотал бы
// delay(3000) и код корутины завершился.
//
//У TestCoroutineDispatcher есть поле currentTime. Оно отображает виртуальное время, которое проходит,
// когда диспетчер перематывает delay время. Если рассматривать последний пример теста, то этот тест
// выполняется за миллисекунды. Но если в конце теста вывести в лог значение currentTime,
// то оно покажет 4000 мсек. Потому что было два delay: 1000 и 3000 мсек.
//
//В коде внутри runBlockingTest мы можем вместо testDispatcher.pauseDispatcher() писать просто
// pauseDispatcher(). runBlockingTest сам передаст этот вызов под капот диспетчеру TestCoroutineDispatcher.
// Это же актуально и для методов resumeDispatcher, advanceTimeBy и currentTime.
//
//Ну и золотое правило, которое мы соблюдаем еще со времен шедулеров RxJava - диспетчеры надо инджектить,
// а не использовать напрямую. На эту тему есть полезная статья