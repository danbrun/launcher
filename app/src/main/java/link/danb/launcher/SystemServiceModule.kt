package link.danb.launcher

import android.app.Application
import android.content.pm.LauncherApps
import android.os.UserManager
import androidx.core.content.getSystemService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class SystemServiceModule {

    @Provides
    @Singleton
    fun getLauncherApps(application: Application): LauncherApps = application.getSystemService()!!

    @Provides
    @Singleton
    fun getUserManager(application: Application): UserManager = application.getSystemService()!!
}
