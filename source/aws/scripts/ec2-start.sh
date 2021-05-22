#!/bin/bash

# Little helper script that synchronously creates
# and launches an EC2 instance.

# requires the following variables to be set
#   EC2_AMI
#   EC2_KEY
#   EC2_CERT
#   EC2_SSH_KEY
#   EC2_REGION
#   EC2_SECURITY_GROUP
#   MY_IP

# create ec2 security group
STATUS="$(${EC2_HOME}/bin/ec2-add-group --region ${EC2_REGION} -K ${EC2_KEY} -C ${EC2_CERT} ${EC2_SECURITY_GROUP} -d 'it-vest')"
check_errs $? "Unable to run ec2-add-group"

DEBUG "Creating instance of ${EC2_AMI}"
STATUS="$(${EC2_HOME}/bin/ec2-run-instances ${EC2_AMI} --instance-type m1.small --region ${EC2_REGION} --key ${EC2_KEYPAIR} --group ${EC2_SECURITY_GROUP} -K ${EC2_KEY} -C ${EC2_CERT})"
check_errs $? "Unable to run ec2-run-instances"
DEBUG "Start status ${STATUS}"

NEW_INSTANCE="$(echo ${STATUS} | cut -d' ' -f6)"
echo ${NEW_INSTANCE}

# loop until state is running
STATE=''
while [ "${STATE}" != 'running' ]; do
  STATUS="$(${EC2_HOME}/bin/ec2-describe-instances --region ${EC2_REGION} -K ${EC2_KEY} -C ${EC2_CERT} ${NEW_INSTANCE} | grep ${NEW_INSTANCE})"
  check_errs $? "Unable to run ec2-describe-instances"
  DEBUG "Describe status ${STATUS}"
  
  STATE="$(echo ${STATUS} | cut -d' ' -f6)"
  DEBUG "State = ${STATE}"
  if [ "${STATE}" != 'running' ]; then
    sleep 30s
  fi
done

HOST="$(echo ${STATUS} | cut -d' ' -f4)"
echo ${HOST}

# setup SSH access in AWS firewall
STATUS="$(${EC2_HOME}/bin/ec2-authorize --region ${EC2_REGION} -K ${EC2_KEY} -C ${EC2_CERT} ${EC2_SECURITY_GROUP} -P tcp -p 22 -s ${MY_IP}/32)"
# just ignore errors in modifying the AWS firewall, as it might already
# contain the SSH entry
DEBUG "FW info ${STATUS}"
