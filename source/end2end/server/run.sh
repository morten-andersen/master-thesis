#!/bin/bash

java -cp ../common/build/classes/main:build/classes/main -Dserver-ip=192.168.1.4 -Djava.util.logging.config.file=../logging.properties dk.accel.misw.mp.model.server.Main server-0