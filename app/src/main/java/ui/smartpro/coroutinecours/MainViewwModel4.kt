package ui.smartpro.coroutinecours

class MainViewwModel4 {

    //Response
    //Чтобы поймать ошибки при получении данных, нужно просто обернуть вызов suspend функции в
    // try-catch.

    viewModelScope.launch {
       try {
           val commentsData = service.getComments()
       } catch (e: Exception) {
           // ...
       }
    }
    //
    //
    //Но как вы, возможно, знаете, ретрофит предоставляет возможность разделять обычные ошибки и
    // HTTP ошибки (например HTTP 404). Для этого надо обернуть тип данных в Response:

    interface CommentApiService {

       @GET("comments")
       suspend fun getComments(): Response<List<Comment>>

    }

    //Теперь мы вместо данных будем получать объект обертку Response:

    viewModelScope.launch {
       try {
           val response = service.getComments()
           if (response.isSuccessful) {
               val commentsData = response.body()
           }
       } catch (e: Exception) {
           // ...
       }
    }
    //Если все прошло хорошо (isSuccessful), то наши данные лежат в reposnse.body.
    //Если произошла HTTP ошибка, то данные о ней будут можно найти в response.
    //Если произошло другая ошибка, то она будет поймана в catch.
}