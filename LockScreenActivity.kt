package com.company.applock

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.company.applock.databinding.ActivityLockBinding
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

class LockScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // بلاک کردن دکمه بازگشت
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // کاری نکن
            }
        })

        binding.btnUnlock.setOnClickListener {
            val code = binding.etCode.text.toString().trim()
            if (code.length < 4) return@setOnClickListener

            binding.btnUnlock.text = "در حال بررسی..."
            binding.btnUnlock.isEnabled = false

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val userId = Supabase.client.auth.currentUserOrNull()?.id
                    
                    // جستجوی رمز: یا رمز عمومی باشد (user_id=null) یا رمز خصوصی برای همین کاربر
                    val result = Supabase.client.from("access_codes").select {
                        filter {
                            eq("code", code)
                            eq("is_used", false)
                        }
                    }.decodeSingleOrNull<JsonObject>()

                    if (result != null) {
                        val codeId = result["id"]?.jsonPrimitive?.content
                        val duration = result["duration_minutes"]?.jsonPrimitive?.int ?: 0
                        
                        // علامت زدن به عنوان استفاده شده
                        if (codeId != null) {
                            Supabase.client.from("access_codes").update({
                                set("is_used", true)
                                set("used_at", System.currentTimeMillis())
                            }) { filter { eq("id", codeId) } }
                            
                            // ذخیره زمان انقضا در گوشی (مدت زمان رمز)
                            val expiryTime = System.currentTimeMillis() + (duration * 60 * 1000L)
                            val prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE)
                            prefs.edit().putLong("unlock_expiry", expiryTime).apply()

                            Toast.makeText(this@LockScreenActivity, "باز شد!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    } else {
                        Toast.makeText(this@LockScreenActivity, "رمز اشتباه یا منقضی شده است", Toast.LENGTH_LONG).show()
                        binding.btnUnlock.text = "باز کردن"
                        binding.btnUnlock.isEnabled = true
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LockScreenActivity, "خطای شبکه", Toast.LENGTH_LONG).show()
                    binding.btnUnlock.text = "باز کردن"
                    binding.btnUnlock.isEnabled = true
                }
            }
        }
    }
}
