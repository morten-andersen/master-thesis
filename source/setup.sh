#!/bin/bash

pushd aws/ec2-api-tools
./setup.sh
popd

pushd hazelcast/hazelcast-1.9.3
./setup.sh
popd

pushd terracotta/terracotta-3.5.1
./setup.sh
popd

