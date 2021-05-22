#!/bin/bash

if [ "${1}" == "" ]; then
	echo 'Usage ./stop.sh <host>'
	exit 1
fi

source ./env

export HOST=${1}

# stop the ec2 instance
echo "Stopping EC2 Instance"
./ec2-stop.sh
check_errs $? "Unable to stop EC2 instance"
echo "EC2 Instance stopped"
