package ui.smartpro.coroutinecours.model

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

data class UserData(
        val id: Long,
        val name: String,
        val age: Int
//Чтобы иметь возможность поместить объект этого класса в контекст корутины,
        // необходимо наследовать AbstractCoroutineContextElement
) : AbstractCoroutineContextElement(UserData) {
    companion object Key: CoroutineContext.Key<UserData>
}
