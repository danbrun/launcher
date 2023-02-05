package link.danb.launcher

import android.app.Application
import android.content.Context
import android.content.pm.LauncherApps
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class LauncherModule {

    @Provides
    @Singleton
    fun getLauncherApps(application: Application): LauncherApps =
        application.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
}
