This project contains bash shell files for creating an EC2 image:
	ec2-image-create.sh

First download the Amazon EC2 API tools into the ec2-api-tools folder. See
the ec2-api-tools/readme.txt file for details.

The ec2-env file must be modified to point to Amazon EC2 tools and user certs.

The new image is modified as follows:
  - java is installed
  - the ec2-controller application is uploaded and installed as a service
  - the firewall is set up.

Finally the script prints the EC2 AMI of the new image. This should be
used in the ec2-mgmt-console AwsSettings.properties file.
