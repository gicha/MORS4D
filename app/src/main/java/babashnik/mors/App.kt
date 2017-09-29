package babashnik.mors

import android.app.Application
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class App : Application() {
    private var retrofit: Retrofit? = null

    override fun onCreate() {
        super.onCreate()

        retrofit = Retrofit.Builder()
                .baseUrl("https://static.daniils.ru/") //Базовая часть адреса
                .addConverterFactory(GsonConverterFactory.create()) //Конвертер, необходимый для преобразования JSON'а в объекты
                .build()
        api = retrofit!!.create(Api::class.java) //Создаем объект, при помощи которого будем выполнять запросы
    }

    companion object {

        var api: Api? = null
            private set
    }
}