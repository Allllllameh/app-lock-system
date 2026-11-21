package com.company.applock

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(50,50,50,50) }
        val email = EditText(this).apply { hint = "ایمیل کارمند" }
        val pass = EditText(this).apply { hint = "رمز عبور"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val btn = Button(this).apply { text = "ورود و ارسال لیست" }

        btn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    Supabase.client.auth.signInWith(Email) {
                        this.email = email.text.toString()
                        password = pass.text.toString()
                    }
                    Toast.makeText(this@MainActivity, "لاگین موفق!", Toast.LENGTH_SHORT).show()

                    val pm = packageManager
                    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    val appsJson = buildJsonArray {
                        apps.forEach { app ->
                            if (pm.getLaunchIntentForPackage(app.packageName) != null) {
                                add(buildJsonObject {
                                    put("appName", pm.getApplicationLabel(app).toString())
                                    put("packageName", app.packageName)
                                })
                            }
                        }
                    }

                    val userId = Supabase.client.auth.currentUserOrNull()?.id
                    if (userId != null) {
                        Supabase.client.from("devices").upsert(
                            buildJsonObject {
                                put("user_id", userId)
                                put("installed_apps", appsJson)
                                put("device_name", android.os.Build.MODEL)
                            }
                        )
                        Toast.makeText(this@MainActivity, "لیست ارسال شد!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "خطا: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        layout.addView(email); layout.addView(pass); layout.addView(btn)
        setContentView(layout)
    }
}
