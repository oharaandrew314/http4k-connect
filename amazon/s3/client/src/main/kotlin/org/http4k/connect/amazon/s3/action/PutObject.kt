package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.s3.S3BucketAction
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Headers
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import java.io.InputStream

@Http4kConnectAction
data class PutObject(
    val key: BucketKey,
    val content: InputStream,
    val headers: Headers = emptyList(),
    val tags: List<Tag> = emptyList(),
) : S3BucketAction<Unit> {
    override fun toRequest() = Request(PUT, Uri.of("/$key")).headers(headers + headersFor(tags)).body(content)

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful || status.redirection -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }
}

private fun headersFor(tags: Collection<Tag>) = if (tags.isEmpty()) {
    emptyList()
} else {
    listOf(Pair("x-amz-tagging", tags.joinToString("&") { (key, value) -> "$key=$value" }))
}
