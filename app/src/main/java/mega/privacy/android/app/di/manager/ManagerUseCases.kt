package mega.privacy.android.app.di.manager

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.di.GetNodeModule
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.HasBackupsChildren
import mega.privacy.android.domain.usecase.MonitorUserAlertUpdates

/**
 * Manager module
 *
 * Provides dependencies used by multiple screens in the manager package
 */

@Module(includes = [GetNodeModule::class])
@InstallIn(ViewModelComponent::class, ServiceComponent::class)
abstract class ManagerUseCases {

    companion object {

        @Provides
        fun provideHasBackupsChildren(megaNodeRepository: MegaNodeRepository): HasBackupsChildren =
            HasBackupsChildren(megaNodeRepository::hasBackupsChildren)

        @Provides
        fun provideMonitorUserAlerts(notificationsRepository: NotificationsRepository): MonitorUserAlertUpdates =
            MonitorUserAlertUpdates(notificationsRepository::monitorUserAlerts)
    }
}
