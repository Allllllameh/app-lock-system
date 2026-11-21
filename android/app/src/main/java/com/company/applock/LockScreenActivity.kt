package com.company.applock
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

class LockScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50, 50, 50, 50); setBackgroundColor(0xFF000000.toInt()) }
        val title = TextView(this).apply { text = "برنامه قفل است.\nرمز را وارد کنید:"; setTextColor(0xFFFFFFFF.toInt()); textSize = 18f }
        val input = EditText(this).apply { hint = "رمز"; setTextColor(0xFFFFFFFF.toInt()) }
        val btnUnlock = Button(this).apply { text = "باز کردن" }

        btnUnlock.setOnClickListener { verifyCode(input.text.toString()) }
        layout.addView(title); layout.addView(input); layout.addView(btnUnlock)
        setContentView(layout)
    }

    private fun verifyCode(code: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = Supabase.client.from("access_codes").select {
                    filter { eq("code", code); eq("is_used", false) }
                }.decodeSingleOrNull<JsonObject>()

                if (result != null) {
                    Toast.makeText(this@LockScreenActivity, "رمز صحیح!", Toast.LENGTH_SHORT).show()
                    val codeId = result["id"]?.jsonPrimitive?.content
                    if (codeId != null) {
                        Supabase.client.from("access_codes").update({ set("is_used", true) }) { filter { eq("id", codeId) } }
                    }
                    finish()
                } else {
                    Toast.makeText(this@LockScreenActivity, "رمز اشتباه است!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LockScreenActivity, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
