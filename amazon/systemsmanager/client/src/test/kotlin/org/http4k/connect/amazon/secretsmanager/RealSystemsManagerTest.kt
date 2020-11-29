package org.http4k.connect.amazon.secretsmanager

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.RealAwsEnvironment
import org.http4k.connect.amazon.configAwsEnvironment

class RealSystemsManagerTest : SystemsManagerContract(JavaHttpClient()), RealAwsEnvironment {
    override val aws get() = configAwsEnvironment(service)
}