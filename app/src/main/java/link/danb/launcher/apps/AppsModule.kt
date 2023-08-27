package link.danb.launcher.apps

import android.app.Application
import android.content.pm.LauncherApps
import androidx.core.content.getSystemService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AppsModule {

    @Provides
    fun getLauncherApps(application: Application): LauncherApps = application.getSystemService()!!
}
