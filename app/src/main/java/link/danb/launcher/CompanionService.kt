package link.danb.launcher

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CompanionBroadcastReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_MY_PACKAGE_REPLACED -> {
        if (CompanionService.tabService == null) {
          context.startForegroundService(Intent(context, CompanionService::class.java))
        }
      }
    }
  }
}

class CompanionService : Service() {

  override fun onCreate() {
    super.onCreate()

    tabService = this

    NotificationManagerCompat.from(baseContext)
      .createNotificationChannel(
        NotificationChannelCompat.Builder(
            TAB_SERVICE_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW,
          )
          .setName(getString(R.string.tab_service))
          .build()
      )

    val notification =
      Notification.Builder(baseContext, TAB_SERVICE_CHANNEL_ID)
        .setContentTitle(getString(R.string.tab_service_title))
        .setContentText(getString(R.string.tab_service_text))
        .build()

    ServiceCompat.startForeground(
      this,
      R.id.tab_service,
      notification,
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
          0
        }
      } else {
        0
      },
    )

    embeddedServer(Netty, port = 47051, module = { module() }).start()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onDestroy() {
    super.onDestroy()

    tabService = null
    tabStateFlow.value = listOf()
  }

  companion object {
    var tabService: CompanionService? = null

    val tabStateFlow: MutableStateFlow<List<TabInfo>> = MutableStateFlow(listOf())

    private const val TAB_SERVICE_CHANNEL_ID = "tab_service"
  }

  private fun Application.module() {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }

    routing {
      get { call.respondText("Tab service is running!") }

      post {
        try {
          val tabState = call.receive<TabEvent>()
          when {
            tabState.updated != null -> {
              tabStateFlow.value =
                tabStateFlow.value.filter { it.id != tabState.updated.info.id } +
                  tabState.updated.info
            }

            tabState.removed != null -> {
              tabStateFlow.value = tabStateFlow.value.filter { it.id != tabState.removed.id }
            }

            else -> {
              throw IllegalArgumentException()
            }
          }
          call.respond(HttpStatusCode.OK)
        } catch (e: BadRequestException) {
          log.atWarn().log("failed ${e.cause} ${e.message}")
        }
      }
    }
  }
}

@Serializable
data class TabInfo(
  val id: Int,
  val url: String,
  val title: String = url,
  val capture: String? = null,
)

@Serializable data class TabUpdatedEvent(val info: TabInfo)

@Serializable data class TabRemovedEvent(val id: Int)

@Serializable
data class TabEvent(val updated: TabUpdatedEvent? = null, val removed: TabRemovedEvent? = null)
