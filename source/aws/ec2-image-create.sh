#!/bin/bash

# This script launches an EC2 instance (based on the 
# EC2 AMI defined in <ec2-env>)
# This new instance is then modified:
#  - java is installed
#  - the ec2-controller application is uploaded and installed as a service
#  - the firewall is set up.

# check for env controller file pointing to all the dependencies
if [ ! -f ./ec2-env ]; then
	echo "The ec2-env file is not found, copy the template from ec2-env.template"
	exit ${1}
fi

# includes
source ./ec2-env
source ./scripts/func

# check for all the dependencies
if [ ! -f ${EC2_KEY} ]; then
	echo "The ${EC2_KEY} file is not found"
	exit ${1}
fi
if [ ! -f ${EC2_CERT} ]; then
	echo "The ${EC2_CERT} file is not found"
	exit ${1}
fi
if [ ! -f ./ssh/aws-itvest.pem ]; then
	echo "The ./ssh/aws-itvest.pem file is not found"
	exit ${1}
fi
if [ ! -f ${EC2_CONTROLLER_APP} ]; then
	echo "The EC2-controller application has not been build ${EC2_CONTROLLER_APP}"
	exit ${1}
fi

if [ -f ssh/known_hosts ]; then
	rm ssh/known_hosts
fi

# a temporary EC2 security group
export EC2_SECURITY_GROUP="it-vest-$(date +%Y%m%d_%H%M)"

# create the ec2 instance and receive the host name
echo "Creating EC2 Instance"
STATUS="$(./scripts/ec2-start.sh)"
check_errs $? "Unable to create EC2 instance"

export EC2_INSTANCE="$(echo ${STATUS} | cut -d' ' -f1)"
export HOST="$(echo ${STATUS} | cut -d' ' -f2)"

echo "EC2 Instance: ${EC2_INSTANCE} created - Host: ${HOST}"

# upload the controller app
scp -F ./ssh/config ${EC2_CONTROLLER_APP} ${HOST}:.
check_errs $? "Unable to upload the controller app"

# upload the setup script
scp -F ./ssh/config remote/setup.sh ${HOST}:.
check_errs $? "Unable to upload the setup script"

# and run it
ssh -t -F ./ssh/config ${HOST} sudo bash ./setup.sh
check_errs $? "Unable to run the remote setup script"

# stop the instance
./scripts/ec2-stop.sh
check_errs $? "Unable to stop the EC2 instance"

# create an AMI
STATUS="$(${EC2_HOME}/bin/ec2-create-image ${EC2_INSTANCE} -n "it-vest-$(date +%Y%m%d_%H%M)" --no-reboot --region ${EC2_REGION} -K ${EC2_KEY} -C ${EC2_CERT})"
check_errs $? "Unable to create an AMI from the EC2 instance"
EC2_AMI="$(echo ${STATUS} | cut -d' ' -f2)"
echo "EC2 new IT-vest AMI: ${EC2_AMI} created"

# terminate the instance
STATUS="$(${EC2_HOME}/bin/ec2-terminate-instances ${EC2_INSTANCE} --region ${EC2_REGION} -K ${EC2_KEY} -C ${EC2_CERT})"
check_errs $? "Unable to terminate the EC2 instance"

# remove ec2 security group
STATUS="$(${EC2_HOME}/bin/ec2-delete-group --region ${EC2_REGION} -K ${EC2_KEY} -C ${EC2_CERT} ${EC2_SECURITY_GROUP})"
check_errs $? "Unable to run ec2-delete-group"

# finally modify the AwsSettings.properties file in the ec2-mgmt-console application
sed -r "s|^region\s?=.+$|region = ${EC2_REGION_WS}|" ${EC2_MGMT_CONTROLLER_AWS_SETTINGS} -i
check_errs $? "Unable to insert region in AwsSettings.properties"

sed -r "s/^instance-type\s?=.+$/instance-type = ${EC2_INSTANCE_TYPE}/" ${EC2_MGMT_CONTROLLER_AWS_SETTINGS} -i
check_errs $? "Unable to insert instance-type in AwsSettings.properties"

sed -r "s/^ami\s?=.+$/ami = ${EC2_AMI}/" ${EC2_MGMT_CONTROLLER_AWS_SETTINGS} -i
check_errs $? "Unable to insert instance-type in AwsSettings.properties"
