/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Copyright(C) Chris2018998,All rights reserved.
 *
 * Project owner contact:Chris2018998@tom.com.
 *
 * Project Licensed under Apache License v2.0.
 */
package org.stone.springboot.controller.pusher;

import io.prometheus.client.Gauge;
import org.stone.beeop.BeeObjectPoolMonitorVo;

/**
 * Gauge type for Prometheus
 *
 * @author Chris Liao
 * @version 1.0
 */
public class BeeOPPrometheusGauge {

    private static final Gauge poolName = Gauge.build()
            .name("beeop_pool_name")
            .help("beecp pool name.")
            .register();

    private static final Gauge poolMode = Gauge.build()
            .name("beeop_pool_mode")
            .help("beecp pool mode.")
            .register();

    private static final Gauge poolState = Gauge.build()
            .name("beeop_pool_state")
            .help("beecp pool state.")
            .register();

    private static final Gauge poolMaxSize = Gauge.build()
            .name("beeop_pool_max_size")
            .help("beecp pool max size.")
            .register();

    private static final Gauge poolIdleSize = Gauge.build()
            .name("beeop_pool_idle_size")
            .help("beecp pool dile size.")
            .register();

    private static final Gauge poolBorrowedSize = Gauge.build()
            .name("beeop_pool_borrowed_size")
            .help("beecp pool borrowed size.")
            .register();

    private static final Gauge poolSemaphoreWaitingSize = Gauge.build()
            .name("beeop_pool_semaphore_waiting_size")
            .help("beecp pool semaphore waiting size.")
            .register();

    private static final Gauge poolTransferWaitingSize = Gauge.build()
            .name("beeop_pool_transfer_waiting_size")
            .help("beecp pool transfer waiting size.")
            .register();

    private static final Gauge poolCreatingCount = Gauge.build()
            .name("beeop_pool_creating_count")
            .help("beecp pool creating count.")
            .register();

    private static final Gauge poolCreatingTimeoutCount = Gauge.build()
            .name("beeop_pool_creating_timeout_count")
            .help("beecp pool creating timeout_count.")
            .register();

    public void fill(BeeObjectPoolMonitorVo vo) {
        poolMaxSize.set(vo.getPoolMaxSize());
        poolIdleSize.set(vo.getIdleSize());
        poolBorrowedSize.set(vo.getBorrowedSize());
        poolSemaphoreWaitingSize.set(vo.getSemaphoreWaitingSize());
        poolTransferWaitingSize.set(vo.getTransferWaitingSize());
        poolCreatingCount.set(vo.getCreatingCount());
        poolCreatingTimeoutCount.set(vo.getCreatingTimeoutCount());
    }
}
