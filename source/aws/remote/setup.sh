#!/bin/bash

#
# Setup file for Ubuntu 10.04 instance
#
# Run as: sudo ./setup.sh
#
# This file is run on the remote instance and basically:
#  - installs java
#  - setup firewalls.
#  - IT-vest MP controller java app as a service launching 
#    automatically when an instance boots

# for checking errors - and exiting if any - call as
#  check_errs $? 'Some error message'
check_errs()
{
  # Function. Parameter 1 is the return code
  # Para. 2 is text to display on failure.
  if [ "${1}" -ne "0" ]; then
    echo "ERROR # ${1} : ${2}"
    exit ${1}
  fi
}

EC2_CONTROLLER_APP=ec2-controller-1.0.zip

# upgrade everything
apt-get update
check_errs $? "apt-get update"

apt-get upgrade -y
check_errs $? "apt-get upgrade"


# install utility packages
apt-get install -y sysv-rc-conf secure-delete htop zip unzip hping3 ifstat sysstat
check_errs $? "apt-get tools"


# firewall
ufw --force reset
check_errs $? "ufw --force reset"

ufw allow in ssh/tcp
check_errs $? "ufw allow in ssh"

ufw allow in 8080/tcp
check_errs $? "ufw allow in 8080"

ufw allow from 10.0.0.0/8
check_errs $? "ufw allow from 10.0.0.0/8"

ufw default deny incoming
check_errs $? "ufw default deny incoming"

ufw --force enable
check_errs $? "ufw --force enable"


# add Sun Java 6 JDK
add-apt-repository "deb http://archive.canonical.com/ lucid partner"
check_errs $? "add-apt-repository"

apt-get update
check_errs $? "apt-get update"

echo sun-java6-jdk shared/accepted-sun-dlj-v1-1 boolean true | debconf-set-selections

apt-get install -y sun-java6-jdk
check_errs $? "apt-get install java"

# install IT-vest MP controller java app
mkdir itvest
check_errs $? "mkdir itvest"

mv ${EC2_CONTROLLER_APP} itvest/.
check_errs $? "mv ${EC2_CONTROLLER_APP} itvest/."

pushd itvest
check_errs $? "pushd itvest"

unzip ${EC2_CONTROLLER_APP}
check_errs $? "unzip ${EC2_CONTROLLER_APP}"

rm ${EC2_CONTROLLER_APP}
check_errs $? "rm ${EC2_CONTROLLER_APP}"

chmod 755 bin/itvest-controller
check_errs $? "chmod 755 bin/itvest-controller"

# install as service
ln -s /home/ubuntu/itvest/bin/itvest-controller /etc/init.d/itvest
check_errs $? "ln -s /home/ubuntu/itvest/bin/itvest-controller /etc/init.d/itvest"

update-rc.d itvest start 20 2 3 4 5 . stop 20 0 1 6 .
check_errs $? "update-rc.d itvest start 20 2 3 4 5 . stop 20 0 1 6 ."

popd
check_errs $? "popd itvest"

rm setup.sh
check_errs $? "unable to cleanup"
