package com.example.climbit.activity

import android.content.Context
import android.content.Intent
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.example.climbit.R

open class BaseActivity : AppCompatActivity() {
    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val background = AppCompatResources.getDrawable(this, R.drawable.background)
        findViewById<View>(android.R.id.content).background = background
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (this !is MainActivity) {
            menuInflater.inflate(R.menu.main_menu, menu)
            return true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_home) {
            startActivity(Intent(this, MainActivity::class.java))
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun alert(msg: CharSequence) {
        runOnUiThread {
            toast?.also {
                it.cancel()
            }

            toast = Toast.makeText(this, msg, Toast.LENGTH_LONG).also {
                it.show()
            }
        }
    }

    fun alertError(msg: CharSequence) {
        alert("Error: $msg")
    }

    fun withConfirmation(fn: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.confirm_your_action)

        builder.setNegativeButton(R.string.cancel, null)
        builder.setPositiveButton(R.string.confirm) { _, _ ->
            fn()
        }

        val dialog = builder.create()
        dialog.show()
    }

    @Suppress("Deprecation")
    fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun vibrateShort(vibrator: Vibrator) {
        vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun vibrateShort() {
        vibrateShort(getVibrator())
    }

    fun reloadActivity() {
        finish()
        startActivity(intent)
    }
}
