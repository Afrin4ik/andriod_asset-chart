package com.example.asset_charts

import android.content.Intent // для перехода между экранами
import androidx.appcompat.app.AppCompatActivity // базовый класс для экранов с поддержкой старых и новых версий Android
import android.os.Bundle // для получения состояния при создании экрана
import android.widget.Button // стандартный элемент интерфейса
import android.widget.EditText // стандартный элемент интерфейса
import android.widget.Toast // стандартный элемент интерфейса
import com.google.firebase.auth.FirebaseAuth // класс из Firebase для авторизации пользователя по email/паролю

class LoginActivity : AppCompatActivity() { // Объявляем свой экран LoginActivity, который расширяет AppCompatActivity — то есть становится полноценным Activity в Android
    override fun onCreate(savedInstanceState: Bundle?) { // Метод onCreate — точка входа для Activity. Система вызывает его, когда экран создаётся
        super.onCreate(savedInstanceState) // Вызываем родительскую реализацию, чтобы Android правильно настроил всё, что нужно под капотом
        setContentView(R.layout.activity_login) // «Надёргиваем» разметку экрана из файла res/layout/activity_login.xml. После этого у нас в UI появляются кнопки, поля ввода и т.д.

        findViewById<Button>(R.id.sign_in).setOnClickListener { // Ищем в разметке кнопку с ID sign_in и приводим её к типу Button. Говорим: «когда пользователь нажмёт на эту кнопку, выполни вот этот кусок кода»
            val email = findViewById<EditText>(R.id.email).text.toString() // Через findViewById находим поле для email и сразу берём text.toString() — текст, который ввёл пользователь
            val password = findViewById<EditText>(R.id.password).text.toString()
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password) // Получаем синглтон FirebaseAuth и вызываем метод signInWithEmailAndPassword, передавая почту и пароль
                .addOnCompleteListener { // Когда Firebase закончит попытку входа (успешно или с ошибкой), вызовется этот слушатель
                    if (it.isSuccessful) { // Проверяет, удалось ли войти
                        Toast(this).apply { // Toast — маленькое уведомление «Sign in successful»
                            setText("Sign in successful")
                        }.show()
                        startActivity(Intent(this, MainActivity::class.java)) // запускаем следующий экран MainActivity
                        finish() // закрываем экран логина, чтобы пользователь не смог вернуться назад кнопкой «Назад»
                    } else {
                        Toast(this).apply {
                            setText("Sign in failed")
                        }.show()
                    }
                }
        }

        findViewById<Button>(R.id.sign_up).setOnClickListener {
            val email = findViewById<EditText>(R.id.email).text.toString()
            val password = findViewById<EditText>(R.id.password).text.toString()
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast(this).apply {
                            setText("Sign up successful")
                        }.show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast(this).apply {
                            setText("Sign up failed")
                        }.show()
                    }
                }
        }
    }
}