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
        val loader = Thread.currentThread().contextClassLoader // получает ClassLoader текущего потока, чтобы найти ресурсы в resources/
        val props = Properties() // пустой объект Properties
        loader?.getResourceAsStream("config.properties")?.use { resourceStream -> // ищет в classpath файл config.properties
            props.load(resourceStream) // загружает пары ключ=значение из потока
        }
        Log.w("InvestApiProps", props.toString())
        return props
    }

    // Этот перехватчик (ClientInterceptor) добавляет индивидуальный таймаут на каждый unary вызов (одно запрос‑одно ответ)
    @Suppress("NAME_SHADOWING")
    internal class TimeoutInterceptor(private val timeout: Duration) : ClientInterceptor {
        override fun <ReqT, RespT> interceptCall(
            method: MethodDescriptor<ReqT, RespT>,
            callOptions: CallOptions,
            next: io.grpc.Channel
        ): ClientCall<ReqT, RespT> {
            var callOptions = callOptions
            if (method.type == MethodDescriptor.MethodType.UNARY) { // если метод — UNARY (не стриминговый), то ...
                callOptions =
                    callOptions.withDeadlineAfter(timeout.toMillis(), TimeUnit.MILLISECONDS) // создаёт новую копию CallOptions с установленным временем ожидания timeout в миллисекундах
            }
            return next.newCall(method, callOptions) // пропускает вызов дальше по цепочке с обновлёнными опциями
        }
    }

    private fun makeChannel(token: String, appName: String): io.grpc.Channel {
        val props = getInvestApiProps() // загружаем настройки из config.properties

        val headers = io.grpc.Metadata() // создаём пустой объект Metadata
        addAuthHeader(headers, token) // добавляем токен
        addAppNameHeader(headers, appName) // добавляем пользовательский заголовок с именем приложения

        val requestTimeout = Duration.parse(props.getProperty("ru.tinkoff.piapi.core.request-timeout"))
        return OkHttpChannelBuilder
            .forTarget("invest-public-api.tinkoff.ru:443") // цель gRPC (хост и порт)
            .intercept(
                io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(headers), // автоматически прикрепляет метаданные (заголовки) ко всем вызовам
                TimeoutInterceptor(requestTimeout)) // каждый unary‑запрос с нужным таймаутом
            .useTransportSecurity() //  включаем TLS
            .keepAliveTimeout(10, TimeUnit.SECONDS) // таймаут между keep‑alive пингами (10 с)
            .maxInboundMessageSize(16777216) // максимальный размер входящего сообщения 16MB
            .build() as io.grpc.Channel // строим канал и приводим к io.grpc.Channel
    }

    fun create(token: String): InvestApi {
        return InvestApi.create(makeChannel(token, "Asset_Charts")) // строит высокоуровневый клиент, готовый делать запросы к API
    }
}