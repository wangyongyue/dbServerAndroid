package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        DBServer.setupContext(applicationContext)
        DBServer.register(User())

        server.delete {
            return@delete User::name.name equal "wwww"
        }
        server.insert<User> {
            it.name = "wwww11"
            it.age = 10
        }

        server.commit()

        val user = server.select<User> {
            return@select User::age.name less 10 limit 1
        }
        Log.v("sdf", "${user?.count()}")
        Log.v("sdf", "${user?.last()?.name}")
        Log.v("sdf", "${user?.last()?.age}")

    }

    val server = DBServer("User")
}

class User : JSONInterface{

    @DBJSON("User","name")
    var name: String? = null

    @DBJSON("User","age")
    var age: Int? = null
}

