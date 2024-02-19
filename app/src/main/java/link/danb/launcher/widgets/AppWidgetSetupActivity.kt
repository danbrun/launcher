package link.danb.launcher.widgets

import android.app.Activity
import android.app.ActivityOptions
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.os.UserHandle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import link.danb.launcher.R
import link.danb.launcher.extensions.allowPendingIntentBackgroundActivityStart
import link.danb.launcher.extensions.getParcelableCompat
import link.danb.launcher.widgets.AppWidgetSetupActivity.Companion.EXTRA_APP_WIDGET_SETUP_INPUT
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import link.danb.launcher.widgets.WidgetPermissionResultContract.WidgetPermissionInput

/** Activity that handles binding and configuring new app widgets. */
@AndroidEntryPoint
class AppWidgetSetupActivity : AppCompatActivity() {

  @Inject lateinit var appWidgetHost: AppWidgetHost
  @Inject lateinit var appWidgetManager: AppWidgetManager
  @Inject lateinit var widgetManager: WidgetManager

  private val widgetPermissionLauncher =
    registerForActivityResult(WidgetPermissionResultContract(), this::onPermissionResult)

  private val input: AppWidgetSetupInput by lazy {
    checkNotNull(intent.extras?.getParcelableCompat(EXTRA_APP_WIDGET_SETUP_INPUT))
  }

  private var widgetId: Int = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_WIDGET_ID)) {
      widgetId = savedInstanceState.getInt(EXTRA_WIDGET_ID)
    } else {
      widgetId = appWidgetHost.allocateAppWidgetId()
      startPermissionsActivity()
    }
  }

  @Deprecated(
    "Deprecated in Java",
    ReplaceWith(
      "super.onActivityResult(requestCode, resultCode, data)",
      "androidx.appcompat.app.AppCompatActivity",
    ),
  )
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (!isFinishing && requestCode == R.id.app_widget_configure_request_id) {
      onConfigurationResult(resultCode)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)

    outState.putInt(EXTRA_WIDGET_ID, widgetId)
  }

  private fun bindAppWidgetIfAllowed(): Boolean =
    appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, input.userHandle, input.provider, null)

  private fun startPermissionsActivity() {
    if (bindAppWidgetIfAllowed()) {
      startConfigurationActivity()
    } else {
      widgetPermissionLauncher.launch(
        WidgetPermissionInput(widgetId, input.provider, input.userHandle)
      )
    }
  }

  private fun onPermissionResult(success: Boolean) {
    if (success) {
      startConfigurationActivity()
    } else {
      onBindFailed(R.string.widget_permission_denied)
    }
  }

  private fun startConfigurationActivity() {
    try {
      appWidgetHost.startAppWidgetConfigureActivityForResult(
        this,
        widgetId,
        /* intentFlags = */ 0,
        R.id.app_widget_configure_request_id,
        ActivityOptions.makeBasic().allowPendingIntentBackgroundActivityStart().toBundle(),
      )
    } catch (exception: ActivityNotFoundException) {
      // If there is no configuration activity, return successfully.
      onBindSuccess()
    }
  }

  private fun onConfigurationResult(resultCode: Int) {
    if (resultCode == Activity.RESULT_OK) {
      onBindSuccess()
    } else {
      onBindFailed(R.string.widget_configuration_failed)
    }
  }

  private fun onBindSuccess() {
    widgetManager.notifyChange()
    Toast.makeText(this, R.string.pinned_widget, Toast.LENGTH_SHORT).show()
    setResult(Activity.RESULT_OK)
    finish()
  }

  private fun onBindFailed(@StringRes errorMessage: Int) {
    appWidgetHost.deleteAppWidgetId(widgetId)
    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    finish()
  }

  companion object {
    const val EXTRA_APP_WIDGET_SETUP_INPUT = "extra_app_widget_setup_input"
    const val EXTRA_WIDGET_ID = "extra_widget_id"
  }
}

/** Activity result contract for binding a new widget. */
class AppWidgetSetupActivityResultContract :
  ActivityResultContract<AppWidgetSetupInput, Boolean>() {

  override fun createIntent(context: Context, input: AppWidgetSetupInput): Intent =
    Intent(context, AppWidgetSetupActivity::class.java).apply {
      putExtra(EXTRA_APP_WIDGET_SETUP_INPUT, input)
    }

  override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
    resultCode == Activity.RESULT_OK

  @Parcelize
  data class AppWidgetSetupInput(val provider: ComponentName, val userHandle: UserHandle) :
    Parcelable
}

/** Activity result contract for launching a widget permission dialog. */
private class WidgetPermissionResultContract :
  ActivityResultContract<WidgetPermissionInput, Boolean>() {

  override fun createIntent(context: Context, input: WidgetPermissionInput): Intent =
    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, input.widgetId)
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, input.provider)
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, input.userHandle)
    }

  override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
    resultCode == Activity.RESULT_OK

  @Parcelize
  data class WidgetPermissionInput(
    val widgetId: Int,
    val provider: ComponentName,
    val userHandle: UserHandle,
  ) : Parcelable
}
