package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.exception.account.ConfirmChangeEmailException
import mega.privacy.android.domain.exception.account.CreateAccountException
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateAccountUseCaseTest {
    private lateinit var underTest: CreateAccountUseCase

    private val accountRepository = mock<AccountRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CreateAccountUseCase(accountRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository)
    }


    @Test
    fun `test that account is created successfully`() = runTest {
        val email = "test@test.com"
        val password = "password"
        val firstName = "FirstName"
        val lastName = "LastName"
        val sessionKey = "sessionKey"

        val expected = EphemeralCredentials(
            email = email,
            password = password,
            session = sessionKey,
            firstName = firstName,
            lastName = lastName
        )

        whenever(
            accountRepository.createAccount(
                email = any(),
                password = any(),
                firstName = any(),
                lastName = any(),
            )
        ).thenReturn(expected)

        val actual = underTest.invoke(
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName
        )

        verify(accountRepository).createAccount(
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName
        )
        assertThat(actual).isEqualTo(expected)
    }


    @Test
    fun `test that account is not created when backend throws already exists exception`() =
        runTest {
            val email = "test@test.com"
            val password = "password"
            val firstName = "FirstName"
            val lastName = "LastName"

            whenever(
                accountRepository.createAccount(
                    email = any(),
                    password = any(),
                    firstName = any(),
                    lastName = any(),
                )
            ).thenAnswer {
                throw CreateAccountException.AccountAlreadyExists
            }

            assertThrows<CreateAccountException.AccountAlreadyExists> {
                underTest.invoke(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName
                )
            }
        }

    @Test
    fun `test that account is not created when backend throws unknown exception`() =
        runTest {
            val email = "test@test.com"
            val password = "password"
            val firstName = "FirstName"
            val lastName = "LastName"

            whenever(
                accountRepository.createAccount(
                    email = any(),
                    password = any(),
                    firstName = any(),
                    lastName = any(),
                )
            ).thenAnswer {
                throw CreateAccountException.Unknown(mock())
            }

            assertThrows<CreateAccountException.Unknown> {
                underTest.invoke(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName
                )
            }
        }
}