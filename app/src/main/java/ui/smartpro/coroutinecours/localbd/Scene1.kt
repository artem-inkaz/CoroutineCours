package ui.smartpro.coroutinecours.localbd

import androidx.lifecycle.ViewModel

//Сценарий 1
//На экране отображается список пользователей. Мы можем добавлять нового пользователя или удалить
// сразу всех. Работаем только с локальной БД, на сервер не ходим.
//
//Нам нужны три операции:
//- получение данных
//- добавление одной записи
//- удаление данных
//
//Давайте создавать объекты, которые обеспечат такой функционал.
// Если смотреть на картинку архитектуры выше, то мы пойдем справа налево: DAO > Repository > UseCase > ViewModel
//
//Обращаю ваше внимание, что это не урок по архитектуре приложений.
// Цель урока - показать, как корутины могут быть встроены в архитектуру.

//Data Source
//DAO для работы с БД:
@Dao
interface UserDao {

   @Query("SELECT * FROM user")
   fun getAll(): Flow<List<UserDb>>

   @Insert
   suspend fun insert(user: UserDb)

   @Query("DELETE FROM user")
   suspend fun deleteAll()

}
//Получение данных оформляем как Flow. А вставку и удаление делаем suspend функциями.

//Repository
//Далее - репозиторий для работы с данными пользователей.
//
//Сразу поясню немного по архитектуре. Я здесь придерживался схемы, которая часто используется
// для организации типов данных. Если у нас есть тип данных User, который мы используем в UI части,
// то для базы данных должен быть отдельный тип UserDb. Для Retrofit, если бы он тут использовался,
// тоже использовался бы отдельный тип UserApi. За пределы UserRepository может выйти только тип User.
// Для конвертации одного типа данных в другой внутри репозитория используем маппинги.

class UserRepository(
   private val userDao: UserDao,
   private val userMapperDbToUi: UserMapperDbToUi,
   private val userMapperUiToDb: UserMapperUiToDb
) {

   fun getUsers(): Flow<List<User>> {
        return userDao.getAll().map { userMapperDbToUi.transformList(it) }
   }

   suspend fun addUser(user: User) {
       userDao.insert(userMapperUiToDb.transform(user))
   }

   suspend fun deleteAllUsers() {
       userDao.deleteAll()
   }

}
//Методы репозитория полностью повторяют соответствующие методы из DAO и используют маппинги для
// конвертации UserDb в User и наоборот.
//
//Обратите внимание на использование suspend. Чтобы вызывать suspend функции DAO (insert и deleteAll),
// нам нужно использовать слово suspend и в функциях репозитория, т.к. suspend функция может быть
// вызвана только из корутины или другой suspend функции.

//UseCase
//Переходим к UseCase. Их будет три: получение пользователей, удаление пользователей,
// добавление пользователя.

//Получение пользователей:

class GetUsersUseCase(private val userRepository: UserRepository) {

   fun execute(): Flow<List<User>> {
       return userRepository.getUsers()
   }

}
//Просто пробрасываем Flow из репозитория.

//Удаление пользователей:

class DeleteAllUsersUseCase(private val userRepository: UserRepository) {

   suspend fun execute() {
       userRepository.deleteAllUsers()
   }
}
//Снова просто вызываем метод репозитория. А для этого нам надо объявлять метод execute как suspend.

//Добавление пользователя:

class AddUserUseCase(
   private val userRepository: UserRepository,
   private val userValidator: UserValidator
) {

   suspend fun execute(user: User): Result<Unit> {

       if (!userValidator.isValid(user)) {
           return Error(IllegalArgumentException("User is not valid"))
       }

       try {
           userRepository.addUser(user)
       } catch (e: Exception) {
           return Error(e)
       }

       return Success(Unit)
   }
}
//Чтобы сделать пример интереснее, я добавил немного логики в этот UseCase.
//
//Он возвращает тип Result (Success|Error).
//
//Перед добавлением данных пользователя в БД мы выполняем проверку, что эти данные валидны.
// Если данные не валидны, мы сразу возвращаем Error и ничего не вставляем в БД.
//
//Вставка данных в БД обернута в try-catch, чтобы в случае возникновения ошибки получить объект Error.
//
//Если все прошло хорошо, то возвращаем пустой Success.
//
//Метод execute помечаем как suspend, потому что мы в нем вызываем suspend метод userRepository.addUser.

//ViewModel
//Используем созданные UseCase в ViewModel:

class MainViewModel : ViewModel(...) {

   // ...

   val users = getUsersUseCase.execute().asLiveData()

   fun onAddClick() {
       viewModelScope.launch {
           val result = addUsersUseCase.execute(User(...))

           if (result is Error) {
               showError(result.exception)
           }
       }
   }


   fun onClearClick() {
       viewModelScope.launch {
           deleteAllUsersUseCase.execute()
       }
   }

   // ...

}
//Flow из getUsersUseCase сразу конвертируем в LiveData.
//
//По нажатию на кнопку Add запускаем корутину, а в ней вызываем suspend метод addUsersUseCase.execute.
// Если результат - Error, то отображаем ошибку.
//
//По нажатию на кнопку Clear запускаем корутину,
// а в ней вызываем suspend метод deleteAllUsersUseCase.execute.
//
//Т.к. мы используем Room, то добавление и удаление пользователей автоматически будут триггерить
// Flow из getUsersUseCase, и в LiveData придут обновленные данные.

//Как видим, код ViewModel не сильно отличается от примеров из уроков про Room и Retrofit.
// Мы стартуем корутины и в них работаем с suspend функциями.
// Код репозиториев и UseCase выглядит вполне обычным, с тем лишь отличием,
// что их функции объявлены как suspend.