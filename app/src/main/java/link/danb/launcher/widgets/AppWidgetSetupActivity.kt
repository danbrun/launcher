package link.danb.launcher.widgets

import android.app.Activity
import android.app.ActivityOptions
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import link.danb.launcher.R
import link.danb.launcher.extensions.allowPendingIntentBackgroundActivityStart
import link.danb.launcher.extensions.getParcelableCompat
import link.danb.launcher.widgets.AppWidgetSetupActivity.Companion.EXTRA_WIDGET_HANDLE
import link.danb.launcher.widgets.AppWidgetSetupActivity.Companion.EXTRA_WIDGET_PROVIDER
import link.danb.launcher.widgets.AppWidgetSetupActivity.Companion.EXTRA_WIDGET_USER
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupInput
import link.danb.launcher.widgets.AppWidgetSetupActivityResultContract.AppWidgetSetupResult
import javax.inject.Inject

/** Activity that handles binding and configuring new app widgets. */
@AndroidEntryPoint
class AppWidgetSetupActivity : AppCompatActivity() {

  @Inject lateinit var appWidgetHost: AppWidgetHost
  @Inject lateinit var appWidgetManager: AppWidgetManager

  private val widgetPermissionLauncher =
    registerForActivityResult(WidgetPermissionResultContract(), this::onPermissionResult)

  private lateinit var widgetHandle: WidgetHandle

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_WIDGET_HANDLE)) {
      widgetHandle = savedInstanceState.getParcelableCompat(EXTRA_WIDGET_HANDLE)!!
    } else {
      widgetHandle =
        WidgetHandle(
          appWidgetHost.allocateAppWidgetId(),
          intent.extras?.getParcelableCompat(EXTRA_WIDGET_PROVIDER)!!,
          intent.extras?.getParcelableCompat(EXTRA_WIDGET_USER)!!
        )
      startPermissionsActivity()
    }
  }

  @Deprecated(
    "Deprecated in Java",
    ReplaceWith(
      "super.onActivityResult(requestCode, resultCode, data)",
      "androidx.appcompat.app.AppCompatActivity"
    )
  )
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (!isFinishing && requestCode == R.id.app_widget_configure_request_id) {
      onConfigurationResult(resultCode)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)

    outState.putParcelable(EXTRA_WIDGET_HANDLE, widgetHandle)
  }

  private fun bindAppWidgetIfAllowed(): Boolean =
    appWidgetManager.bindAppWidgetIdIfAllowed(
      widgetHandle.id,
      widgetHandle.user,
      widgetHandle.info.provider,
      null
    )

  private fun startPermissionsActivity() {
    if (bindAppWidgetIfAllowed()) {
      startConfigurationActivity()
    } else {
      widgetPermissionLauncher.launch(widgetHandle)
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
        widgetHandle.id,
        /* intentFlags = */ 0,
        R.id.app_widget_configure_request_id,
        ActivityOptions.makeBasic().allowPendingIntentBackgroundActivityStart().toBundle()
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
    setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_WIDGET_HANDLE, widgetHandle))
    finish()
  }

  private fun onBindFailed(@StringRes errorMessage: Int) {
    appWidgetHost.deleteAppWidgetId(widgetHandle.id)
    setResult(
      Activity.RESULT_CANCELED,
      Intent().apply {
        putExtra(EXTRA_WIDGET_HANDLE, widgetHandle)
        putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
      }
    )
    finish()
  }

  companion object {
    const val EXTRA_WIDGET_PROVIDER = "extra_widget_provider"
    const val EXTRA_WIDGET_USER = "extra_widget_user"
    const val EXTRA_WIDGET_HANDLE = "extra_widget_handle"
    const val EXTRA_ERROR_MESSAGE = "extra_bind_widget_message"
  }
}

/** Activity result contract for binding a new widget. */
class AppWidgetSetupActivityResultContract :
  ActivityResultContract<AppWidgetSetupInput, AppWidgetSetupResult>() {

  override fun createIntent(context: Context, input: AppWidgetSetupInput): Intent =
    Intent(context, AppWidgetSetupActivity::class.java).apply {
      putExtra(EXTRA_WIDGET_PROVIDER, input.providerInfo)
      putExtra(EXTRA_WIDGET_USER, input.user)
    }

  override fun parseResult(resultCode: Int, intent: Intent?): AppWidgetSetupResult =
    intent!!.extras!!.let {
      AppWidgetSetupResult(
        resultCode == Activity.RESULT_OK,
        it.getParcelableCompat(EXTRA_WIDGET_HANDLE)!!,
        it.getInt(AppWidgetSetupActivity.EXTRA_ERROR_MESSAGE)
      )
    }

  data class AppWidgetSetupInput(val providerInfo: AppWidgetProviderInfo, val user: UserHandle)

  data class AppWidgetSetupResult(
    val success: Boolean,
    val widgetHande: WidgetHandle,
    @StringRes val errorMessage: Int
  )
}

/** Activity result contract for launching a widget permission dialog. */
private class WidgetPermissionResultContract : ActivityResultContract<WidgetHandle, Boolean>() {

  override fun createIntent(context: Context, input: WidgetHandle): Intent =
    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, input.id)
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, input.info.provider)
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, input.info.profile)
    }

  override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
    resultCode == Activity.RESULT_OK
}
