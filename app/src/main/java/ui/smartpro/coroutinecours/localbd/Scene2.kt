package ui.smartpro.coroutinecours.localbd

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Job
import java.util.prefs.Preferences

//Cценарий 2
//Во втором сценарии рассмотрим получение данных с сервера и сохранение их в БД.
//
//С точки зрения UI нам нужна только одна операция - получение данных. А внутри нее уже будет
// происходить работа с БД и сервером.

//Идем по той же схеме, DAO/ApiService > Repository > UseCase > ViewModel

//DataSource
//ApiService для получения данных с сервера:

interface UserApiService {

   @GET("users")
   suspend fun fetchUsers(): List<UserApi>

}
//Помечаем метод как suspend

//DAO для работы с БД:

@Dao
interface UserDao {

   @Query("SELECT * FROM user")
   suspend fun getAll(): List<UserDb>

   @Insert
   suspend fun insertAll(users: List<UserDb>)

   @Query("DELETE FROM user")
   suspend fun deleteAll()

}
//Три suspend метода: чтение, вставка и удаление. Обратите внимание, чтение на этот раз не Flow,
// а suspend. Т.е. в этом сценарии нам не нужна подписка на данные. Нужен способ получить данные
// в коде здесь и сейчас.

//Repository
//Репозиторий:

class UserRepository(
   private val userDao: UserDao,
   private val userApiService: UserApiService,
   private val userMapperDbToUi: UserMapperDbToUi,
   private val userMapperApiToDb: UserMapperApiToDb
) {

   suspend fun getUsers(): List<User> {
        return userMapperDbToUi.transformList(userDao.getAll())
   }

   suspend fun fetchUsers() {
       val usersApi = userApiService.fetchUsers()
       val usersDb = userMapperApiToDb.transformList(usersApi)
       userDao.deleteAll()
       userDao.insertAll(usersDb)
   }

}
//Метод getUsers просто использует соответствующий метод DAO для получения данных, плюс маппинг.
//
//А вот метод fetchUsers интереснее. Он ничего не возвращает, а только обновляет данные в БД.
// Для этого он получает данные с сервера, конвертирует их и вставляет в БД,
// предварительно удалив все старые данные.
//
//Т.е. в одном suspend методе мы вызываем три suspend метода.

//UseCase
//Переходим к UseCase:

class FetchOrGetUsersUseCase(
   private val userRepository: UserRepository,
   private val networkHelper: NetworkHelper,
   private val preferences: Preferences
) {

   suspend fun execute(): List<User> {
       if (networkHelper.isWiFi()) {
           try {
               userRepository.fetchUsers()
           } catch (e: Exception) {
               // ...
           }
           preferences.lastUpdated()
       }
       return userRepository.getUsers()
   }

}
//Сначала проверяем, что мы подключены к WiFi. В этом случае вызываем метод fetchUsers, который
// загрузит данные с сервера и запишет их в БД. Т.к. при этом мы совершаем запрос к серверу,
// то вполне могут возникнуть ошибки. Поэтому вызов оборачиваем в try-catch.
//
//Если обновление данных прошло успешно, то методом lastUpdated фиксируем время обновления данных.
// Это нужно, например, если мы хотим где-нибудь отображать, насколько актуальны данные в БД.
//
//В конце мы читаем данные из БД методом getUsers и возвращаем их. Если не было WiFi,
// то эти данные просто будут не самыми свежими.

//ViewModel
//Смотрим на код ViewModel:

class MainViewModel : ViewModel() {

   val users = MutableLiveData<List<User>>()

   fun getData() {
       viewModelScope.launch {
           showLoading()
           val usersData = fetchOrGetUsersUseCase.execute()
           hideLoading()
           users.value = usersData
       }
   }

}
//Запускаем корутину. В ней показываем индикатор загрузки, используем UseCase, чтобы получить данные,
// скрываем индикатор загрузки, помещаем данные в LiveData.
//
//Код ViewModel снова не особо сложный. В корутине вызываем suspend метод, внутри которого крутится вся
// логика и вызывается несколько других suspend методов.
//
//Этот сценарий вполне можно было сделать по-другому: разбить FetchOrGetUsersUseCase на два разных UseCase.
// Первый читает данные из БД с помощью Flow. А второй отвечает только за получение данных с сервера и
// обновление их в БД, и ничего не возвращает. В связке они будут отлично работать.

//Давайте отдельно обсудим некоторые вопросы, которые могут возникнуть при написании такого кода.

//suspend функции
//В примерах выше для работы с БД и сетью мы использовали Room и Retrofit. Они поддерживают Flow и
// suspend функции из коробки.
//
//Если же у нас есть просто долгий синхронный код или код с колбэком, то мы можем сами создать
// для них suspend обертку. В случае синхронных функций можно просто использовать withContext,
// как мы это делали в Уроках 24 и 25. А для колбэков используйте suspendCoroutine или
// suspendCancellableCoroutine, которые мы разбирали в Уроках 3 и 16.

//Обработка исключений
//Чтобы поймать исключение, пришедшее из suspend, мы просто оборачиваем вызов suspend функции в try-catch.
// Корутина при этом отменена не будет.

//Отмена корутины
//Как мы знаем, если закрыть экран, то ViewModel будет уничтожена. А следовательно будет отменен
// viewModelScope и все его корутины. Как поведет себя suspend функция в этом случае?
// Выбросит исключение CancellationException (если она cancellable, Урок 16).
//
//Flow прекратит свою работу, если его collect был вызван в корутине, которую мы отменяем (Урок 22).
//
//И не забывайте, что asLiveData под капотом использует билдер liveData, который НЕ использует viewModelScope.
// Вместо этого он создает свой Scope, который привязан к LifeCycle подписчика его LiveData.
// Если перевести на русский, то Flow, к которому применили asLiveData, будет работать,
// пока есть подписчик на его LiveData.

//Защита от повторных вызовов
//Вернемся к примеру получения данных с сервера:

class MainViewModel : ViewModel() {

   val users = MutableLiveData<List<User>>()

   fun getData() {
       viewModelScope.launch {
           showLoading()
           val usersData = fetchOrGetUsersUseCase.execute()
           hideLoading()
           users.value = usersData
       }
   }

}
//Если метод getData вызывается по нажатию на кнопку, и пользователь будет жать эту кнопку несколько раз,
// то мы получим множественные вызовы. Каждый такой вызов создает новую корутину.
// Каждая корутина запрашивает данные.
//
//В итоге будет куча лишних одновременных вызовов. И индикатор загрузки будет моргать.
// Давайте это исправим.

//Перед тем, как вызывать новую корутину, будем проверять, что прошлая уже завершилась.
// В этом нам поможет Job:

var getDataJob: Job? = null
//
fun getData() {
   if (getDataJob?.isActive == true) return

   getDataJob = viewModelScope.launch {
       showLoading()
       val usersData = fetchOrGetUsersUseCase.execute()
       hideLoading()
       users.value = usersData
   }
}
//Схема очень похожа на работу с Disposable в RxJava. Когда мы запускаем корутину, мы сохраняем ее Job.
// При следующем вызове getData мы выполняем проверку.
// Если Job еще работает, значит прошлая корутина все еще получает данные. Ничего не делаем.
// А если Job уже завершился, значит прошлая корутина уже отработала и можно запросить данные снова.