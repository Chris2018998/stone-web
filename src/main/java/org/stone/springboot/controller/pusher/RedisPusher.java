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

import org.stone.springboot.controller.PoolSnapshot;
import org.stone.springboot.controller.PoolSnapshotPusher;

/**
 * pusher implementation for redis
 *
 * @author Chris Liao
 */
public class RedisPusher implements PoolSnapshotPusher {

    /**
     * push snapshot of pools to redis server
     *
     * @param snapshot is runtime monitor object
     * @throws Exception when push fail
     */
    public void push(PoolSnapshot snapshot) throws Exception {

    }
}
