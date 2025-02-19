package com.bcu.foodtable.ui.subscribeNavMenu

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bcu.foodtable.R
import com.bcu.foodtable.useful.Channel
import com.bcu.foodtable.useful.FirebaseHelper
import com.bcu.foodtable.useful.FirebaseHelper.updateFieldById
import com.bcu.foodtable.useful.RecipeItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelViewPage : AppCompatActivity() {
    lateinit var channelitem: Channel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_channel_view_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val channelName = intent.getStringExtra("channel_name") ?: ""
        CoroutineScope(Dispatchers.Main).launch {
            channelitem = FirebaseHelper.getDocumentById("channel", channelName, Channel::class.java)!!
            channelitem.let {
                Log.d("Channel_Page","Channel Loaded Successfully.")

            }
        }
    }
}