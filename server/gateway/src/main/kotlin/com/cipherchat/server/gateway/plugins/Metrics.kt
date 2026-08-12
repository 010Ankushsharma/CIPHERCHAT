package com.cipherchat.server.gateway.plugins

import io.ktor.server.application.Application
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Application.configureMetrics() {
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    // JVM binders — gives Grafana dashboards memory, GC, thread,
    // and CPU data without any manual instrumentation in route handlers.
    listOf(
        ClassLoaderMetrics(),
        JvmMemoryMetrics(),
        JvmGcMetrics(),
        JvmThreadMetrics(),
        ProcessorMetrics(),
    ).forEach { it.bindTo(prometheusRegistry) }

    install(MicrometerMetrics) {
        registry = prometheusRegistry
        // Distribute request duration into histogram buckets matching
        // the spec's "Cold Start < 2 sec" and "Instant Messaging" perf
        // goals — having these buckets lets Grafana alert on p99
        // latency crossing the 2s threshold, not just average latency.
        distributionStatisticConfig = io.micrometer.core.instrument.distribution.DistributionStatisticConfig.Builder()
            .percentilesHistogram(true)
            .serviceLevelObjectives(
                50.0, 100.0, 200.0, 500.0, 1000.0, 2000.0
            )
            .build()
    }

    routing {
        // /metrics is intentionally unauthenticated — Prometheus scrapes
        // it from within the cluster network (never exposed to the public
        // internet in a real deployment). If the gateway is behind a load
        // balancer with a public IP, add network-level protection (VPC
        // security group, not application-level auth) rather than
        // bolting authentication onto a metrics endpoint.
        get("/metrics") {
            call.respond(prometheusRegistry.scrape())
        }
    }
}
