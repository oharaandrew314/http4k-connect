package org.http4k.connect.amazon

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.aws.AwsCredentials
import org.http4k.cloudnative.env.Environment
import org.http4k.connect.amazon.core.model.ProfileName
import org.http4k.core.with
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.Path

class ProfileCredentialsProviderTest {

    private val profileFile = Files.createTempFile("credentials", "ini").also {
        it.toFile().writeText("""
            [default]
            aws_access_key_id = key123
            aws_secret_access_key = secret123
            
            [dev]
            aws_access_key_id = key456
            aws_secret_access_key = secret456
        """)
    }

    @AfterEach
    fun cleanup() {
        profileFile.toFile().delete()
    }

    private fun getCredentials(name: ProfileName): AwsCredentials? = CredentialsChain.Profile(
        profileName = name,
        credentialsPath = profileFile
    ).invoke()

    @Test
    fun `default profile in custom file`() {
        assertThat(
            getCredentials(ProfileName.of("default")),
            equalTo(AwsCredentials("key123", "secret123"))
        )
    }

    @Test
    fun `custom profile in custom file`() {
        assertThat(
            getCredentials(ProfileName.of("dev")),
            equalTo(AwsCredentials("key456", "secret456"))
        )
    }

    @Test
    fun `missing profile`() {
        assertThat(
            getCredentials(ProfileName.of("missing")),
            absent()
        )
    }

    @Test
    fun `missing file`() {
        assertThat(
            CredentialsChain.Profile(Environment.EMPTY.with(AWS_CREDENTIAL_PROFILES_FILE of Path("foobar")))(),
            absent()
        )
    }

    @Test
    fun `credentials are cached`() {
        val expected = AwsCredentials("key123", "secret123")
        val chain = CredentialsChain.Profile(
            profileName = ProfileName.of("default"),
            credentialsPath = profileFile
        )

        assertThat(chain.invoke(), equalTo(expected))

        profileFile.toFile().writeText("""
            [default]
            aws_access_key_id = key1456
            aws_secret_access_key = secret456
        """)

        assertThat(chain.invoke(), equalTo(expected))
    }
}
