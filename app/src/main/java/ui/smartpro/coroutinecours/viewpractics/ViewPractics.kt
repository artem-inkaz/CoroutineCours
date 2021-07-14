package ui.smartpro.coroutinecours.viewpractics

import android.animation.ObjectAnimator
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import kotlinx.coroutines.flow.MutableStateFlow

//В этом уроке создаем suspend функции и Flow из колбэков View компонентов

//В прошлых примерах мы использовали Flow и suspend функции для работы с сетью, диском, вычислениями
// и прочими тяжелыми операциями. Но их вполне можно применять и при работе с UI.
// В основном это касается работы с различными listener для View компонентов.
//
//Давайте рассмотрим несколько сценариев.

//Listener to suspend function
//View listener может быть конвертирован в suspend функцию. Это даст лучшую читаемость кода.

//Рассмотрим простой пример:

btn.text = "New text"
log("new width is ${btn.width}")
//Мы меняем текст у кнопки и сразу после этого хотим узнать ее новую ширину.
//
//Этот код не сработает как ожидалось. Мы получим старую ширину кнопки.
// Потому что setText не выполнится сразу в момент вызова кода. Реальное обновление кнопки будет
// помещено в очередь и произойдет чуть позже.

//У нас есть способ узнать, когда именно это произойдет. Для этого мы можем использовать OnLayoutChangeListener:

val listener = object : View.OnLayoutChangeListener {
   override fun onLayoutChange(view: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
       view?.removeOnLayoutChangeListener(this)
       log("new width is ${btn.width}")
   }
}
btn.addOnLayoutChangeListener(listener)

btn.text = "New text"
log("old width is ${btn.width}")
//Создаем listener и вешаем его на кнопку. Он сработает, когда произойдут реальные изменения layout кнопки.
// Внутри отписываем listener и выводим в лог новую ширину.
//
//В итоге этот код поместит обновления кнопки в очередь и выведет в лог старую ширину.
// Затем сработает listener после того, как кнопка будет обновлена и перерисована.
// И в нем мы уже будем видеть новую ширину кнопки.

//Этот OnLayoutChangeListener может быть упакован в отдельную suspend функцию:

suspend fun View.awaitLayoutChange() = suspendCancellableCoroutine<Unit> { cont ->

   val listener = object : View.OnLayoutChangeListener {

       override fun onLayoutChange(view: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
           view?.removeOnLayoutChangeListener(this)
           cont.resume(Unit)
       }
   }

   addOnLayoutChangeListener(listener)

   cont.invokeOnCancellation { removeOnLayoutChangeListener(listener) }
}
//Используем suspendCancellableCoroutine для создания отменяемой suspend функции.
// Создаем listener и вешаем его на View, для которого будет вызвана эта suspend функция.
// В onLayoutChange отписываем listener от View и сообщаем корутине, что можно продолжать работу.
//
//invokeOnCancellation будет вызван при отмене корутины. В этом случае отписываем listener от View.

//Теперь наш пример выглядит так:

lifecycleScope.launch {
   btn.text = "New text"
   log("old width is ${btn.width}")
   btn.awaitLayoutChange()
   log("new width is ${btn.width}")
}
//Используем корутину, т.к. нам теперь необходимо вызывать suspend функцию awaitLayoutChange.
// Эта функция приостановит код корутины, пока кнопка не будет реально обновлена.
// После чего мы запрашиваем новую ширину кнопки.
//
//Т.е. listener мы поместили в suspend функцию, и код стал читабельнее.

//Аналогично можно обернуть listener анимации, чтобы можно было запускать анимацию и дожидаться ее окончания

lifecycleScope.launch {
   val animator = ObjectAnimator.ofFloat(imageView, View.ALPHA, 0f, 1f)
   animator.start()
   animator.awaitEnd()
}
//awaitEnd - suspend функция, которая приостановит корутину, пока анимация не завершится.
// Если у нас есть несколько анимаций, то мы сможем запускать их параллельно, последовательно,
// с задержкой, с повторами и т.д. В этом помогут корутины и подобные suspend функции.
// Более подробно об этом можно прочесть в статье.
// Если вас интересуют более сложные примеры с анимацией и RecyclerView, то посмотрите эту статью.
// Там используется та же схема, что и в простом примере, который мы рассмотрели.

//Listener to Flow
//Если listener предполагает отправку нам нескольких значений, то мы можем сделать из него Flow.
//
//Самый очевидный пример - это обработчик нажатия на кнопку:

val flow = callbackFlow {
   btn.setOnClickListener {
       trySend(Unit)
   }
   awaitClose { btn.setOnClickListener(null) }
}
//Билдер callbackFlow мы разбирали в Уроке 21. Под капотом он создает комбинацию Flow + корутина + канал.
// Его отличие от channelFlow в том, что он требует вызова awaitClose в конце, иначе выдаст ошибку.
//
//Код внутри callbackFlow будет выполнен, когда получатель подпишется (collect) на этот созданный Flow.
// Listener будет создан и повешен на кнопку. Метод awaitClose приостановит код.
//
//По нажатию на кнопку данные будут отправляться (trySend) получателю. Когда получатель отпишется,
// то метод awaitClose возобновит работу и отвяжет listener от кнопки.
//
//В результате мы получим Flow, который будет сообщать о нажатиях на кнопку.

//Почему здесь используется trySend, а не привычный нам send?
// Потому что код в setOnClickListener будет вызываться вне корутины,
// а значит не может вызывать suspend функции. Фактически мы здесь просто передали канал кнопке.
// И она по нажатию пытается передать туда Unit.

//Еще один распространенный пример, это отслеживание изменений в EditText:

val flow = callbackFlow<String> {

   val textWatcher = object: TextWatcher {

       override fun afterTextChanged(editable: Editable?) {
           editable?.toString()?.let{ trySend(it)}
       }

       override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
       override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
   }

   editText.addTextChangedListener(textWatcher)
   awaitClose { editText.removeTextChangedListener(textWatcher) }
}
//Схема та же, просто другой listener, который мониторит EditText и отправляет новые значения получателю.
//
//В результате у нас есть Flow, который шлет нам изменения текста.
// Нам остается только добавить нужные операторы: map, filter, debounce и прочее.
// Такой Flow может быть использован для удобной реализации поиска или фильтра.

//Когда мы используем схему MVVM, то не очень понятно куда девать эти Flow,
// привязанные к View компонентам. Их нельзя передавать в модель.
// Потому что модель не хранит никакие ссылки на View.
// А эти Flow через listener будут хранить ссылки на компоненты View.
//
//Есть еще одно решение, как listener можно обернуть в Flow.

//Listener to Flow in ViewModel
//Чтобы не создавать Flow на стороне View, мы можем создать его в модели.
// Раньше для этих целей использовались каналы с оберткой asFlow. View передавал данные из
// listener сразу в модель, модель внутри себя помещала их в канал, а канал передавал их в Flow.
// В итоге получалась трансформация обычных данных в Flow.
//
//С выходом SharedFlow и StateFlow схема упростилась. Рассмотрим на примере поиска.

//В модели у нас есть StateFlow и метод search, которые помещает данные в StateFlow.

val _searchQuery = MutableStateFlow("")

fun search(query: String) {
   _searchQuery.value = query
}
//В View мы вешаем listener на EditText и в нем просто вызываем метод модели search и передаем туда данные.

editText.addTextChangedListener { model.search(it.toString()) }
//Метод addTextChangedListener - это Kotlin расширение из androidx.core:core-ktx.
// Под капотом у него используется все тот же TextWatcher.
//
//В итоге в модели у нас есть StateFlow, который предоставляет данные из EditText.

//Мы можем добавить к этому Flow операторов и попросить при каждом новом значении выполнять поиск,
// например, с помощью UseCase:

val searchResultFlow = _searchQuery.asStateFlow()
   .debounce(500)
   .filter { it.length > 3 }
   .mapLatest { query ->
       searchUseCase.execute(query)
   }
//В итоге searchResultFlow будет возвращать нам готовый результат поиска. Его уже можно использовать,
// как нам требуется. Например, конвертировать в LiveData.