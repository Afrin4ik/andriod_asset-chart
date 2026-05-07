package com.example.asset_charts

import android.util.Log
import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.MethodDescriptor
import io.grpc.okhttp.OkHttpChannelBuilder
import ru.tinkoff.piapi.core.InvestApi
import ru.tinkoff.piapi.core.InvestApi.addAppNameHeader
import ru.tinkoff.piapi.core.InvestApi.addAuthHeader
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit

class InvestApiBuilder {
    private fun getInvestApiProps(): Properties {
        val loader = Thread.currentThread().contextClassLoader
        val props = Properties()
        loader?.getResourceAsStream("config.properties")?.use { resourceStream ->
            props.load(resourceStream)
        }
        Log.w("InvestApiProps", props.toString())
        return props
    }

    @Suppress("NAME_SHADOWING")
    internal class TimeoutInterceptor(private val timeout: Duration) : ClientInterceptor {
        override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: io.grpc.Channel
        ): ClientCall<ReqT, RespT> {
            var callOptions = callOptions
            if (method.type == MethodDescriptor.MethodType.UNARY) {
                callOptions =
                    callOptions.withDeadlineAfter(timeout.toMillis(), TimeUnit.MILLISECONDS)
            }
            return next.newCall(method, callOptions)
        }
    }

    private fun makeChannel(token: String, appName: String): io.grpc.Channel {
        val props = getInvestApiProps()

        val headers = io.grpc.Metadata()
        addAuthHeader(headers, token)
        addAppNameHeader(headers, appName)

        val requestTimeout = Duration.parse(props.getProperty("ru.tinkoff.piapi.core.request-timeout"))
        return OkHttpChannelBuilder
            .forTarget("invest-public-api.tinkoff.ru:443")
            .intercept(
                io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(headers),
                TimeoutInterceptor(requestTimeout))
            .useTransportSecurity()
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .maxInboundMessageSize(16777216)
            .build() as io.grpc.Channel
    }

    fun create(token: String): InvestApi {
        return InvestApi.create(makeChannel(token, "Asset_Charts"))
    }
}