package link.danb.launcher.profiles

import android.app.Application
import android.os.UserManager
import androidx.core.content.getSystemService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ProfilesModule {

  @Provides
  fun getUserManager(application: Application): UserManager = application.getSystemService()!!
}
