package ui.smartpro.coroutinecours

import android.telecom.Call
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*
// сервис, возвращающий список комментариев
class MainViewModel3: ViewModel() {

//Стандартный вариант использования ретрофит - это метод enqueue с колбэком:
    fun fetchData() {
        // use retrofit
        service.getComments().enqueue(object : Callback<List<Comment>> {
            //В onResponse нам придет результат в случае успеха, а в onFailure - ошибка в случае неудачи.
            // Это асинхронный способ, его можно выполнять в main потоке.
            override fun onResponse(call: Call<List<Comment>>, response: Response<List<Comment>>) {
                // ...
            }

            override fun onFailure(call: Call<List<Comment>>, t: Throwable) {
                // ...
            }
        })
    }

    //другой способ, синхронный
    fun fetchData() {
        val response = service.getComments().execute()

        // ...
    }
    //Но в main потоке так делать нельзя, потому что поток заблокируется.
// Надо выносить этот вызов в фоновый поток. Давайте посмотрим,
// как это можно сделать с помощью корутин. Так мы лучше поймем,
// в чем именно заключается интеграция корутин в ретрофит.

    //Самое простое решение - обернуть вызов в withContext. Функция withContext позволяет переключить
// поток выполнения кода, дождаться результата и вернуться обратно в свой поток.
    fun fetchData() {
        //Помещаем наш синхронный код выполнения запроса в withContext и указываем, что он должен
        // выполниться в IO потоке. withContext выполнит код и вернет нам результат в response.
        //Этот код будет ругаться, что withContext должен быть вызван в корутине или в suspend функции.
        val response = withContext(Dispatchers.IO)  {
            service.getComments().execute()
        }
    }
//добавим корутину:
    fun fetchData() {
        viewModelScope.launch {
            val response = withContext(Dispatchers.IO) {
                service.getComments().execute()
            }

            // ...
        }
    }
 //Используем viewModelScope, чтобы вызвать корутину.
    //
    //Код внутри корутины выполняется в main потоке.
// Запрос данных внутри withContext будет выполнен в IO потоке. Main поток в это время не будет заблокирован.
// Как только ретрофит вернет данные, они будут помещены в response, код снова продолжит работу
// в main потоке и мы сможем сделать все что нужно с полученными данными.

    //Таким образом мы получаем данные, используя корутины вместо колбэков. Это удобнее и читабельнее,
// особенно если нам надо сделать несколько таких вызовов.
    //При этом мы можем еще улучшить читаемость кода. Всю конструкцию withContext с
// вызовом ретрофита давайте вынесем из корутины в отдельную suspend функцию,
// которую назовем getComments.
    //
    //Код теперь выглядит так:
    fun fetchData() {
        viewModelScope.launch {
            val response = getComments()

            // ...
        }
    }

    suspend fun getComments(): Response<List<Comment>>  {
        return withContext(Dispatchers.IO) {
            service.getComments().execute()
        }
    }

    //Чтобы нам каждый раз не оборачивать получение данных в отдельную suspend функцию с withContext,
// ретрофит научился делать это сам. В этом и заключается интеграция корутин в ретрофит.
    //
    //Для этого мы в интерфейсе добавляем слово suspend к методу. А Call обертку больше не используем.

    interface CommentApiService {

       @GET("comments")
       suspend fun getComments(): List<Comment>

    }
    //Теперь вызов метода в корутине выглядит так:

    viewModelScope.launch {
       val comments = service.getComments()

       // ...
    }
    //И нам не надо создавать никаких отдельных suspend методов с withContext.
// Ретрофит делает это под капотом. Он сам перенесет операцию в фоновый поток и
// вернет нам данные в поток нашей корутины.

    //Будем получаемые данные помещать, например, в LiveData:
    val comments = MutableLiveData<List<Comment>>()

    fun fetchData() {
        viewModelScope.launch {
            val commentsData = service.getComments()
            comments.value = commentsData
        }
    }
    //Метод fetchData запускает корутину, которая получает данные с помощью ретрофит и помещает их в
// LiveData. Ну а на LiveData уже подписан какой-либо экранный элемент, который отобразит
// полученные данные.
}

