package ui.smartpro.coroutinecours.room

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers

//В качестве примера у нас есть Dao, возвращающий список комментариев.

@Dao
interface CommentDao {

   @Query("SELECT * FROM comment")
   fun getAll(): List<Comment>

}
//И у нас есть ViewModel с методом fetchData, который вызывается по нажатию кнопки Refresh или
// какому-либо другому событию. В нем мы планируем получать данные из БД:

class MainViewModel : ViewModel() {

    fun fetchData() {
        // use room
    }

    // ...
}

//Чтобы получить данные, надо вызвать этот метод:

fun fetchData() {
    val commentsData = db.commentDao().getAll()

    // ...
}
//Но вызывать такой код в main потоке нельзя. Получим ошибку:
//
//error java.lang.IllegalStateException:
// Cannot access database on the main thread since it may potentially lock the UI for a long period
// of time.
//
//Надо выносить в фоновый поток. Давайте посмотрим, как можно сделать это с помощью корутин.
// Так мы лучше поймем, в чем заключается интеграция корутин в Room.

//Самое простое решение - обернуть вызов в withContext. Функция withContext позволяет переключить
// поток выполнения кода, дождаться результата и вернуться обратно в свой поток.

val commentsData = withContext(Dispatchers.IO) {
   db.commentDao().getAll()
}
//Помещаем код получения данных в withContext и указываем, что он должен выполниться в IO потоке.
// withContext выполнит код и вернет нам результат в commentsData.

//Но т.к. withContext - suspend функция, мы не можем вызвать ее где угодно.

fun fetchData() {
    val commentsData = withContext(Dispatchers.IO) {
        db.commentDao().getAll()
    }

    // ...
}
//Этот код будет ругаться, что withContext должен быть вызван в корутине или в suspend функции.
//
//Ок, добавим корутину:

fun fetchData() {
    viewModelScope.launch {
        val commentsData = withContext(Dispatchers.IO) {
            db.commentDao().getAll()
        }

        // ...
    }
}
//Используем viewModelScope, чтобы вызвать корутину.
//
//Код внутри корутины выполняется в main потоке.
// Получение данных в withContext будет выполнено в IO потоке. Main поток в это время не будет
// заблокирован. Как только Room вернет данные, они будут помещены в commentsData,
// код снова продолжит работу в main потоке и мы сможем сделать все что нужно с полученными данными.

//Таким образом мы получаем данные, используя корутины. При этом мы можем еще улучшить читаемость кода.
// Всю конструкцию withContext с вызовом Room давайте вынесем из корутины в отдельную suspend функцию,
// которую назовем getComments.
//
//Код теперь выглядит так:

fun fetchData() {
    viewModelScope.launch {
        val commentsData = getComments()

        // ...
    }
}
//
suspend fun getComments(): List<Comment> {
    return withContext(Dispatchers.IO) {
        db.commentDao().getAll()
    }
}
//Корутина стала лаконичнее и читабельнее.

//Чтобы нам каждый раз не оборачивать получение данных в отдельную suspend функцию с withContext,
// Room научился делать это сам. В этом и заключается интеграция корутин в Room.
//
//Для этого надо в Dao интерфейсе добавить слово suspend к методу:

@Dao
interface CommentDao {

   @Query("SELECT * FROM comment")
   suspend fun getAll(): List<Comment>

}
//Теперь вызов этой функции в корутине выглядит так:

viewModelScope.launch {
   val commentsData = db.commentDao().getAll()

   // ...
}
//И нам не надо создавать никаких отдельных suspend методов с withContext.
// Room делает это под капотом. Он сам перенесет операцию в фоновый поток и вернет нам данные
// в поток нашей корутины.

//Давайте немного расширим пример. Будем получаемые данные помещать, например, в LiveData:

val comments = MutableLiveData<List<Comment>>()

fun fetchData() {
   viewModelScope.launch {
       val commentsData = db.commentDao().getAll()
       comments.value = commentsData
   }
}
//Метод fetchData запускает корутину, которая получает данные с помощью Room и помещает их в LiveData.
// Ну а на LiveData уже подписан какой-либо экранный элемент, который отобразит полученные данные.