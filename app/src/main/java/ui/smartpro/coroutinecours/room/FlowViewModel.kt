package ui.smartpro.coroutinecours.room

import androidx.lifecycle.MutableLiveData

//Flow
//Кроме предоставления suspend функций, Room приготовил для нас еще немного интеграции с корутинами. Он умеет возвращать Flow. И это очень важное и полезное умение. Потому что этот Flow будет следить за данными в БД и отправлять их нам, каждый раз когда данные будут изменены. Т.е. это поведение аналогичное Room + Flowable (из RxJava).

//Обернем тип возвращаемых данных в Flow:

@Query("SELECT * FROM comment")
fun getAll(): Flow<List<Comment>>
//Заметьте, что слово suspend здесь уже не нужно. В уроке про Flow я подробно рассказывал, что создание Flow происходит мгновенно и не требует отдельного потока. А вот реальная работа начинается только когда вызвать метод collect.

//Вызываем метод getAll, чтобы получить flow.

val commentsFlow = db.commentDao().getAll()
//Т.к. getAll - не suspend функция, нет необходимости вызывать ее в корутине.
// Она может быть вызвана где угодно. Повторюсь, этот код НЕ делает запрос в БД.
// Он только дает нам Flow объект, который умеет делать этот запрос, когда мы его попросим об этом.
//
//
//
//Пример
//Давайте посмотрим, как можно использовать Flow в ViewModel.
//
//У нас есть Flow, который подкидывает нам данные по мере их обновления в БД.
// И мы хотим эти данные складывать в LiveData, из которого они будут на экран попадать.
// И пользователю даже ничего нажимать не придется. Все будет обновляться автоматически.

//Создаем Flow и LiveData:

val commentsFlow = db.commentDao().getAll()
val comments = MutableLiveData<List<Comment>>()
//
//
//Код, который их свяжет, выглядит так:

viewModelScope.launch {
    commentsFlow.collect {
        comments.value = it
    }
}
//В корутине вызываем collect, чтобы Flow начал свою работу. Его работа заключается в мониторинге БД.
// Как только данные там меняются, он их нам отправляет. А мы просто передаем их в LiveData.

//Эту корутину нам надо запустить один раз при создании ViewModel. Можно сделать это в секции init
// в классе ViewModel.

val commentsFlow = db.commentDao().getAll()
val comments = MutableLiveData<List<Comment>>()
//
init {
   viewModelScope.launch {
       commentsFlow.collect {
           comments.value = it
       }
   }
}
//Flow будет работать и держать связь с БД все время пока жива ViewModel и ее viewModelScope.
//
//Выглядит удобно. Но можно сделать еще лучше.