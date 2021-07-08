package ui.smartpro.coroutinecours

import android.telecom.Call
import org.w3c.dom.Comment

interface CommentApiService {

    @GET("comments")
    fun getComments(): Call<List<Comment>>
}