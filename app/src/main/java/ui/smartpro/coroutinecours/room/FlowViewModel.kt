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

//liveData
//В Уроке 23 я рассказывал про билдер liveData. Он хорошо подходит под наш сценарий:

val commentsFlow = db.commentDao().getAll()
val comments = liveData {
   commentsFlow.collect {
       emit(it)
   }
}
//Билдер liveData создает корутину и возвращает LiveData в переменную comments. Он взял на себя код из секции init.
//
//Кроме того, код внутри liveData начнет работу только тогда, когда у LiveData comments появится подписчик. А в прошлом примере он начинал работу в любом случае.
//
//Кода стало чуть меньше. Но и это не предел.

//asLiveData
//Разработчики гугл понимали, что передача данных из Flow в LiveData будет достаточно часто используемым сценарием, и упаковали его в удобное расширение asLiveData.
//
//В итоге этот же код можно переписать так:

val comments = db.commentDao().getAll().asLiveData()
//Всего одна строка кода, и получившаяся LiveData содержит актуальные данные из БД.

//Изменение данных
//Использовать suspend можно не только для чтения данных, но и их изменения.

//Пример:

@Insert
suspend fun insert(comment: Comment)
//Этот метод теперь можно вызывать в main корутине. Давайте используем его в примере с чтением данных.

//Пусть у нас на экране есть список комментариев и возможность добавить новый с помощью кнопки Send. Простая версия кода в ViewModel будет выглядеть так:

val comments = db.commentDao().getAll().asLiveData()
//
fun onSendClick() {
   viewModelScope.launch {
       db.commentDao().insert(Comment(...))
   }
}
//По нажатию кнопки Send вызывается метод onSendClick в котором мы в корутине вызываем метод insert, чтобы добавить новый комментарий в БД.
//
//comments - это LiveData с данными из БД. Как только мы изменим данные методом insert, Flow получит обновленный список и поместит их в comments. А оттуда они уже пойдут на экран. Нам ничего не надо вручную запрашивать.
//
//Это очень удобно. Принцип Single Source of Truth в действии.