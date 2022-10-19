package mega.privacy.android.data.facade.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.extensions.monitor
import mega.privacy.android.data.gateway.preferences.CameraTimestampsPreferenceGateway
import mega.privacy.android.data.preferences.CameraTimestampsPreferenceDataStore
import mega.privacy.android.data.preferences.CameraTimestampsPreferenceDataStore.Companion.KEY_CAM_SYNC_TIMESTAMP
import mega.privacy.android.data.preferences.CameraTimestampsPreferenceDataStore.Companion.KEY_CAM_VIDEO_SYNC_TIMESTAMP
import mega.privacy.android.data.preferences.CameraTimestampsPreferenceDataStore.Companion.KEY_PRIMARY_HANDLE
import mega.privacy.android.data.preferences.CameraTimestampsPreferenceDataStore.Companion.KEY_SECONDARY_HANDLE
import mega.privacy.android.data.preferences.CameraTimestampsPreferenceDataStore.Companion.KEY_SEC_SYNC_TIMESTAMP
import mega.privacy.android.data.preferences.CameraTimestampsPreferenceDataStore.Companion.KEY_SEC_VIDEO_SYNC_TIMESTAMP
import mega.privacy.android.data.preferences.CameraTimestampsPreferenceDataStore.Companion.LAST_CAM_SYNC_TIMESTAMP_FILE
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class DefaultCameraTimestampsPreferenceGatewayTest {

    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val testCoroutineScope = TestScope(testCoroutineDispatcher)
    private val testDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = testCoroutineScope,
        produceFile =
        { context.preferencesDataStoreFile(LAST_CAM_SYNC_TIMESTAMP_FILE) }
    )

    private lateinit var underTest: CameraTimestampsPreferenceGateway
    private lateinit var context: Context

    private val primaryHandle = 12345678L
    private val secondaryHandle = 87654321L
    private val camSyncTimeStamp = "12345678"
    private val camVideoSyncTimeStamp = "12345678"
    private val secSyncTimeStamp = "12345678"
    private val secVideoSyncTimeStamp = "12345678"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        underTest = CameraTimestampsPreferenceDataStore(
            dataStore = testDataStore
        )
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @Test
    fun test_time_stamps_and_handles_are_backed_up() {
        testCoroutineScope.runTest {
            saveTimeStamps()
            assertThat(testDataStore.monitor(KEY_CAM_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo(camSyncTimeStamp)
            assertThat(testDataStore.monitor(KEY_CAM_VIDEO_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo(camVideoSyncTimeStamp)
            assertThat(testDataStore.monitor(KEY_SEC_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo(secSyncTimeStamp)
            assertThat(testDataStore.monitor(KEY_SEC_VIDEO_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo(secVideoSyncTimeStamp)
            assertThat(testDataStore.monitor(KEY_PRIMARY_HANDLE).firstOrNull())
                .isEqualTo(primaryHandle)
            assertThat(testDataStore.monitor(KEY_SECONDARY_HANDLE).firstOrNull())
                .isEqualTo(secondaryHandle)
        }
    }

    private suspend fun saveTimeStamps() {
        underTest.backupTimestampsAndFolderHandle(primaryHandle,
            secondaryHandle,
            camSyncTimeStamp = camSyncTimeStamp,
            camVideoSyncTimeStamp = camVideoSyncTimeStamp,
            secSyncTimeStamp = secSyncTimeStamp,
            secVideoSyncTimeStamp = secVideoSyncTimeStamp)
    }

    @Test
    fun test_that_only_primary_camera_upload_info_cleared() {
        testCoroutineScope.runTest {
            saveTimeStamps()
            underTest.clearPrimaryCameraSyncRecords()
            assertThat(testDataStore.monitor(KEY_PRIMARY_HANDLE).firstOrNull())
                .isEqualTo(0)
            assertThat(testDataStore.monitor(KEY_CAM_VIDEO_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo("")
            assertThat(testDataStore.monitor(KEY_CAM_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo("")
            assertThat(testDataStore.monitor(KEY_SEC_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo(secSyncTimeStamp)
            assertThat(testDataStore.monitor(KEY_SEC_VIDEO_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo(secVideoSyncTimeStamp)
            assertThat(testDataStore.monitor(KEY_SECONDARY_HANDLE).firstOrNull())
                .isEqualTo(secondaryHandle)
        }
    }

    @Test
    fun test_that_only_secondary_camera_upload_info_cleared() {
        testCoroutineScope.runTest {
            saveTimeStamps()
            underTest.clearSecondaryCameraSyncRecords()
            assertThat(testDataStore.monitor(KEY_PRIMARY_HANDLE).firstOrNull())
                .isEqualTo(primaryHandle)
            assertThat(testDataStore.monitor(KEY_CAM_VIDEO_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo(camVideoSyncTimeStamp)
            assertThat(testDataStore.monitor(KEY_CAM_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo(camSyncTimeStamp)
            assertThat(testDataStore.monitor(KEY_SEC_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo("")
            assertThat(testDataStore.monitor(KEY_SEC_VIDEO_SYNC_TIMESTAMP).firstOrNull())
                .isEqualTo("")
            assertThat(testDataStore.monitor(KEY_SECONDARY_HANDLE).firstOrNull())
                .isEqualTo(0)
        }
    }

    @After
    fun cleanUp() {
        testCoroutineScope.launch {
            testDataStore.edit {
                it.clear()
            }
        }
        testCoroutineScope.cancel()
        Dispatchers.resetMain()
    }
}
