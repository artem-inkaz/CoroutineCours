package ui.smartpro.coroutinecours

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import java.util.*

class MainViewModel: ViewModel() {

    //жизненный цикл ViewModel (модели) заканчивается, когда будет закрыт экран, к которому она была
    // привязана. Корутины, которые мы запускаем в модели, должны иметь тот же жизненный цикл,
    // чтобы они не продолжали свою работу после закрытия экрана.
    //
    //Чтобы достичь этого, нам надо привязать жизненный цикл scope к жизненному циклу модели.
    // Т.е. создавать scope при создании модели и отменять его (cancel) по окончании жизни модели
    // (в методе onCleared). И чтобы нам самим не приходилось делать это в каждой нашей модели,
    // существует готовое решение - viewModelScope.
    //В любом месте ViewModel мы можем использовать viewModelScope, чтобы запустить новую корутину:
//    viewModelScope.launch {
//       // ...
//    }
    //Заглянув в исходники, увидим следующий код:
    //CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))

    init {
        log("launch")
        viewModelScope.launch {
            while (true) {
// delay - это suspend функция, которая прервет работу корутины, как только мы эту корутину отменим.
    // Поэтому нам тут можно самим не выполнять проверку isActive.
                delay(1000)
                log("work")
            }
        }
    }

    private fun log(text: String) {
        Log.d("TAG", "${formatter.format(Date())} $text [${Thread.currentThread().name}]")
    }
}

//Activity onCreate
//launch
//work
//work
//work
//work
//Activity onDestroy, isFinishing = true

//Проверим, что случится с этой же корутиной при повороте экрана:
//
//Activity onCreate
//launch
//work
//work
//work
//Activity onDestroy, isFinishing = false
//Activity onCreate
//work
//work
//work
//work

//Activity создается, корутина запускается (launch) и начинает работу (work).
// Далее при повороте экрана Activity пересоздается, а корутина продолжает работать без пересоздания,
// потому что модель в момент поворота продолжает жить, и viewModelScope не отменяется.
//
//Также корутина продолжит работать если свернуть приложение.