#!/bin/bash

# Little helper script that synchronously stops
# an EC2 instance.

# requires the following variables to be set
#   EC2_INSTANCE
#   EC2_KEY
#   EC2_CERT
#   EC2_REGION
#   MY_IP
#   HOST
#   USER

DEBUG "Stopping instance ${EC2_INSTANCE}"
ssh -t -F ./ssh/config ${HOST} "sudo shutdown -h now"
check_errs $? "Unable to stop ec2 instance"

# remove SSH access in AWS firewall
STATUS="$(${EC2_HOME}/bin/ec2-revoke --region ${EC2_REGION} -K ${EC2_KEY} -C ${EC2_CERT} ${EC2_SECURITY_GROUP} -P tcp -p 22 -s ${MY_IP}/32)"
check_errs $? "Unable to run ec2-revoke"
DEBUG "FW revoke info ${STATUS}"

STATE=''
while [ "${STATE}" != 'stopped' ]; do
  STATUS="$(${EC2_HOME}/bin/ec2-describe-instances --region ${EC2_REGION} -K ${EC2_KEY} -C ${EC2_CERT} ${EC2_INSTANCE} | grep ${EC2_INSTANCE})"
  check_errs $? "Unable to run ec2-describe-instances"
  DEBUG "Describe status ${STATUS}"
  
  STATE="$(echo ${STATUS} | cut -d' ' -f4)"
  DEBUG "State = ${STATE}"
  if [ "${STATE}" != 'stopped' ]; then
    sleep 10s
  fi
done

