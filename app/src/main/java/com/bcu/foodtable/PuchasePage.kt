package com.bcu.foodtable

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

class PuchasePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_puchase_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btn5000Minus:Button = findViewById(R.id.minus1)
        val btn10000Minus:Button = findViewById(R.id.minus2)
        val btn50000Minus:Button = findViewById(R.id.minus3)
        val btn100000Minus:Button = findViewById(R.id.minus4)

        val btn5000Plus:Button = findViewById(R.id.plus1)
        val btn10000Plus:Button = findViewById(R.id.plus2)
        val btn50000Plus:Button = findViewById(R.id.plus3)
        val btn100000Plus:Button = findViewById(R.id.plus4)

        val purchaseBtn:Button = findViewById(R.id.purchaseBtn)

        val moneyTotal:TextView = findViewById(R.id.moneyText)
        val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
        var moneyValue=0;
        btn5000Minus.setOnClickListener {
            if (moneyValue <= 5000) {
                moneyValue = 0
            }else{
                moneyValue -= 5000
            }
            val formattedValue = formatter.format(moneyValue)
            moneyTotal.text = formattedValue
        }

        btn10000Minus.setOnClickListener {
            if (moneyValue <= 10000) {
                moneyValue = 0
            }else{
                moneyValue -= 10000
            }
            val formattedValue = formatter.format(moneyValue)
            moneyTotal.text = formattedValue
        }
        btn50000Minus.setOnClickListener {
            if (moneyValue <= 50000) {
                moneyValue = 0
            }else{
                moneyValue -= 50000
            }
            val formattedValue = formatter.format(moneyValue)
            moneyTotal.text = formattedValue
        }
        btn100000Minus.setOnClickListener {
            if (moneyValue <= 100000) {
                moneyValue = 0
            }else{
                moneyValue -= 100000
            }
            val formattedValue = formatter.format(moneyValue)
            moneyTotal.text = formattedValue
        }


        btn5000Plus.setOnClickListener {
            moneyValue+=5000
            val formattedValue = formatter.format(moneyValue)
            moneyTotal.text = formattedValue
        }
        btn10000Plus.setOnClickListener {
            moneyValue+=10000
            val formattedValue = formatter.format(moneyValue)
            moneyTotal.text = formattedValue
        }
        btn50000Plus.setOnClickListener {
            moneyValue+=50000
            val formattedValue = formatter.format(moneyValue)
            moneyTotal.text = formattedValue
        }
        btn100000Plus.setOnClickListener {
            moneyValue+=100000
            val formattedValue = formatter.format(moneyValue)
            moneyTotal.text = formattedValue
        }
        purchaseBtn.setOnClickListener{
            val intent = Intent(this@PuchasePage, PurchaseConfirmActivity::class.java)
            intent.putExtra("price", moneyValue.toString())
            Log.d("Purchase","SEND COST STRING $moneyValue ")
            this.startActivity(intent)  // 새로운 액티비티로 전환
            purchaseBtn.isClickable = false
            purchaseBtn.isActivated = false
        }
    }
}