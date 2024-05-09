package link.danb.launcher.widgets

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import link.danb.launcher.R

@Module
@InstallIn(SingletonComponent::class)
class WidgetsModule {

  @Provides
  @Singleton
  fun getAppWidgetManager(application: Application): AppWidgetManager =
    AppWidgetManager.getInstance(application)

  @Provides
  @Singleton
  fun getAppWidgetHost(application: Application): AppWidgetHost =
    AppWidgetHost(application, R.id.app_widget_host_id)
}
