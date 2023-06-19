package link.danb.launcher.widgets

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import link.danb.launcher.R
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppWidgetModule {

    @Provides
    @Singleton
    fun getAppWidgetManager(application: Application): AppWidgetManager {
        return AppWidgetManager.getInstance(application)
    }

    @Provides
    @Singleton
    fun getAppWidgetHost(application: Application): AppWidgetHost {
        return AppWidgetHost(application, R.id.app_widget_host_id)
    }
}
