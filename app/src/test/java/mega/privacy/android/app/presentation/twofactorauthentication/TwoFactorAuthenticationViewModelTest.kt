package mega.privacy.android.app.presentation.twofactorauthentication

import android.graphics.Bitmap
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.twofactorauthentication.model.AuthenticationState
import mega.privacy.android.domain.exception.EnableMultiFactorAuthException
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.auth.EnableMultiFactorAuthUseCase
import mega.privacy.android.domain.usecase.GetExportMasterKeyUseCase
import mega.privacy.android.domain.usecase.auth.GetMultiFactorAuthCodeUseCase
import mega.privacy.android.domain.usecase.auth.IsMasterKeyExportedUseCase
import mega.privacy.android.domain.usecase.SetMasterKeyExportedUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class TwoFactorAuthenticationViewModelTest {
    private lateinit var underTest: TwoFactorAuthenticationViewModel
    private val enableMultiFactorAuthUseCase = mock<EnableMultiFactorAuthUseCase>()
    private val isMasterKeyExportedUseCase = mock<IsMasterKeyExportedUseCase>()
    private val getMultiFactorAuthCodeUseCase = mock<GetMultiFactorAuthCodeUseCase>()
    private val getCurrentUserEmail = mock<GetCurrentUserEmail>()
    private val qrCodeMapper = mock<QRCodeMapper>()
    private val getExportMasterKeyUseCase = mock<GetExportMasterKeyUseCase>()
    private val setMasterKeyExportedUseCase = mock<SetMasterKeyExportedUseCase>()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        underTest = TwoFactorAuthenticationViewModel(
            enableMultiFactorAuthUseCase,
            isMasterKeyExportedUseCase,
            getMultiFactorAuthCodeUseCase,
            getCurrentUserEmail,
            qrCodeMapper,
            getExportMasterKeyUseCase,
            setMasterKeyExportedUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that setMasterKeyExported should not be called when recovery key is empty and vice versa`() {
        val fakeRC = "kgQLOWMHGsdWq873562"

        fun verify(key: String?, expectedInvocation: Int) = runTest {
            whenever(getExportMasterKeyUseCase()).thenReturn(key)

            underTest.getRecoveryKey()

            advanceUntilIdle()

            verify(setMasterKeyExportedUseCase, times(expectedInvocation))
                .invoke()
        }

        verify(key = null, expectedInvocation = 0)
        verify(key = fakeRC, expectedInvocation = 1)

    }

    @Test
    fun `test that authenticationState should be Passed when submitting multi factor authentication code is successful`() =
        runTest {
            whenever(enableMultiFactorAuthUseCase(any())).thenReturn(true)
            underTest.submitMultiFactorAuthPin("")
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.authenticationState).isEqualTo(AuthenticationState.Passed)
            }
        }

    @Test
    fun `test that authenticationState should be Failed when wrong multi factor authentication pin get submitted`() =
        runTest {
            val fakeErrorCode = Random.nextInt()
            whenever(enableMultiFactorAuthUseCase(any()))
                .thenAnswer {
                    throw EnableMultiFactorAuthException(
                        errorCode = fakeErrorCode,
                        errorString = ""
                    )
                }
            underTest.submitMultiFactorAuthPin("")
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.authenticationState).isEqualTo(AuthenticationState.Failed)
            }
        }

    @Test
    fun `test that when multi factor authentication returns error should return Error state`() =
        runTest {
            val fakeErrorCode = Random.nextInt()
            whenever(enableMultiFactorAuthUseCase(any()))
                .thenAnswer {
                    throw MegaException(
                        errorCode = fakeErrorCode,
                        errorString = ""
                    )
                }
            underTest.submitMultiFactorAuthPin("")
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.authenticationState).isEqualTo(AuthenticationState.Error)
            }
        }

    @Test
    fun `test that isMasterKeyExported should be true when getting master key status is successful`() =
        runTest {
            whenever(isMasterKeyExportedUseCase()).thenReturn(true)
            underTest.getMasterKeyStatus()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isMasterKeyExported).isEqualTo(true)
            }
        }

    @Test
    fun `test that isMasterKeyExported should be false when getting master key status returns error`() =
        runTest {
            val fakeErrorCode = Random.nextInt()
            whenever(isMasterKeyExportedUseCase()).thenAnswer {
                throw MegaException(
                    errorCode = fakeErrorCode,
                    errorString = ""
                )
            }
            underTest.getMasterKeyStatus()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isMasterKeyExported).isEqualTo(false)
            }
        }

    @Test
    fun `test that authentication code should not be null when getting multi factor authentication is successful`() =
        runTest {
            val expectedAuthCode = "123456789"
            whenever(getMultiFactorAuthCodeUseCase()).thenReturn(expectedAuthCode)
            underTest.getAuthenticationCode()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.seed).isEqualTo(expectedAuthCode)
            }
        }

    @Test
    fun `test that authentication code should be null when getting multi factor authentication is not successful`() =
        runTest {
            val fakeErrorCode = Random.nextInt()
            whenever(getMultiFactorAuthCodeUseCase()).thenAnswer {
                throw MegaException(
                    errorCode = fakeErrorCode,
                    errorString = ""
                )
            }
            underTest.getAuthenticationCode()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.seed).isNull()
            }
        }

    @Test
    fun `test that userEmail should not be null when getting the current user email is successful`() {
        runTest {
            val expectedFakeEmail = "abc@mega.nz.com"
            whenever(getCurrentUserEmail()).thenReturn(expectedFakeEmail)
            underTest.getUserEmail()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.userEmail).isNotNull()
                assertThat(state.userEmail).isEqualTo(expectedFakeEmail)
            }
        }
    }

    @Test
    fun `test that isQRCodeGenerationCompleted should true when generating the QR code process is successful`() {
        runTest {
            val text = "random url"
            val width = 300
            val height = 300
            val penColor = 0xFF00000
            val bgColor = 0xFFFFFF
            val qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            whenever(
                qrCodeMapper(
                    text = text,
                    width = width,
                    height = height,
                    penColor = penColor,
                    bgColor = bgColor
                )
            ).thenReturn(qrBitmap)
            underTest.generateQRCodeBitmap(
                qrCodeUrl = text,
                width = width,
                height = height,
                penColor = penColor,
                bgColor = bgColor
            )
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isQRCodeGenerationCompleted).isEqualTo(true)
                assertThat(state.qrBitmap).isEqualTo(qrBitmap)
            }
        }
    }
}
