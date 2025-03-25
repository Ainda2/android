package mega.privacy.android.domain.usecase.psa

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.repository.psa.PsaRepository
import mega.privacy.android.domain.usecase.login.IsUserLoggedInUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FetchPsaUseCaseTest {
    private lateinit var underTest: FetchPsaUseCase

    private val psaRepository = mock<PsaRepository>()
    private val isUserLoggedInUseCase = mock<IsUserLoggedInUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = FetchPsaUseCase(
            psaRepository = psaRepository,
            isUserLoggedInUseCase = isUserLoggedInUseCase,
        )
    }

    @Test
    internal fun `test that psa is returned if present`() = runTest {
        val expected = createPsaTestData()
        whenever(isUserLoggedInUseCase()).thenReturn(true)
        psaRepository.stub {
            onBlocking { fetchPsa(any()) }.thenReturn(expected)
        }

        assertThat(underTest.invoke(10L)).isEqualTo(expected)
    }

    @Test
    internal fun `test that null is returned if no psa is found`() = runTest {
        whenever(isUserLoggedInUseCase()).thenReturn(true)
        psaRepository.stub {
            onBlocking { fetchPsa(true) }.thenReturn(null)
        }

        assertThat(underTest(10L)).isNull()
    }

    @Test
    internal fun `test that cache is not refreshed if last fetch was within refresh period`() =
        runTest {
            val currentTime = 10_000_000L
            val lastFetchedTime = currentTime - (underTest.psaRequestTimeout / 2)
            val notExpected = createPsaTestData()
            whenever(isUserLoggedInUseCase()).thenReturn(true)
            psaRepository.stub {
                onBlocking { getLastPsaFetchedTime() }.thenReturn(lastFetchedTime)
                onBlocking { fetchPsa(true) }.thenReturn(notExpected)
                onBlocking { fetchPsa(false) }.thenReturn(null)
            }

            assertThat(underTest.invoke(currentTime)).isNull()
        }

    @Test
    internal fun `test that cache is refreshed if last fetch is outside refresh period`() =
        runTest {
            val currentTime = 10_000_000L
            val lastFetchedTime = currentTime - (underTest.psaRequestTimeout + 1)

            val expected = createPsaTestData()
            whenever(isUserLoggedInUseCase()).thenReturn(true)
            psaRepository.stub {
                onBlocking { getLastPsaFetchedTime() }.thenReturn(lastFetchedTime)
                onBlocking { fetchPsa(true) }.thenReturn(expected)
                onBlocking { fetchPsa(false) }.thenReturn(null)
            }

            assertThat(underTest.invoke(currentTime)).isEqualTo(expected)
        }

    @Test
    internal fun `test that last fetched time is updated if fetched`() = runTest {
        val currentTime = 10_000_000L
        val lastFetchedTime = currentTime - (underTest.psaRequestTimeout + 1)
        whenever(isUserLoggedInUseCase()).thenReturn(true)
        psaRepository.stub {
            onBlocking { getLastPsaFetchedTime() }.thenReturn(lastFetchedTime)
            onBlocking { fetchPsa(true) }.thenReturn(null)
            onBlocking { fetchPsa(false) }.thenReturn(null)
        }

        underTest(currentTime)

        verify(psaRepository).setLastFetchedTime(currentTime)
    }

    @Test
    fun `test that user is not logged in then use case returns null`() = runTest {
        whenever(isUserLoggedInUseCase()).thenReturn(false)

        assertThat(underTest.invoke(10L)).isNull()
    }

    private fun createPsaTestData() = Psa(
        id = 1,
        title = "",
        text = "",
        imageUrl = null,
        positiveText = null,
        positiveLink = null,
        url = null
    )
}