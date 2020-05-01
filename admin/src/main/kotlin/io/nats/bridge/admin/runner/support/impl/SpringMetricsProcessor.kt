package io.nats.bridge.admin.runner.support.impl

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.ImmutableTag
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.nats.bridge.TimeSource
import io.nats.bridge.metrics.Metrics
import io.nats.bridge.metrics.MetricsProcessor
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong


class SpringMetricsProcessor(private val metricsRegistry: MeterRegistry, private val metrics: Metrics,
                             private val every: Int,
                             private val everyDuration: Duration,
                             private val timeSource: TimeSource,
                             private val name: () -> String) : MetricsProcessor {

    private var index = 0
    private var lastTime = 0L
    private val counters: MutableMap<String, Counter> = mutableMapOf()
    private val gaugeMap: MutableMap<String, AtomicLong> = mutableMapOf()
    private val timerMap: MutableMap<String, Timer> = mutableMapOf()

    override fun process() {
        index++
        if (index % every == 0) {
            val currentTime = timeSource.time
            val duration: Long = currentTime - lastTime
            if (duration > everyDuration.toMillis()) {
                lastTime = currentTime
                doProcess()
            }
        }
    }

    private fun doProcess() {

        metrics.counters().forEach { c ->
            if (!counters.containsKey(c.id()))
                counters[c.id()] = metricsRegistry.counter(name() + "_" + c.metricName(), c.dimensions().map { it -> ImmutableTag(it.key, it.value) })
            counters[c.id()]?.increment(c.value.toDouble())
        }

        metrics.gauges().forEach { g ->
            if (!gaugeMap.containsKey(g.id())) {
                val tags = g.dimensions().map { it -> ImmutableTag(it.key, it.value) }
                val value = AtomicLong()
                metricsRegistry.gauge(name() + "_" + g.metricName(), tags, value)
                gaugeMap[g.id()] = value
            }
        }

        metrics.timers().forEach { t ->

            if (!timerMap.containsKey(t.id()))
                timerMap[t.id()] = metricsRegistry.timer(name() + "_" + t.metricName(), t.dimensions().map { it -> ImmutableTag(it.key, it.value) })
            timerMap[t.id()]?.record(Duration.ofMillis(t.value))

        }

    }

}