## Source Code for the Prototypes

The source code for the prototypes and the test bench tools are included here. The source code and tools are described in appendix A-E in the thesis.

It should be noticed that some large 3. party dependencies are
not included. These should be either manually downloaded as
described in the readme.txt file, or automatically downloaded by using
the shell file setup.sh.

### The sub folders in the source are listed below:

* **model:** This folder contains source code and build files for the original model system with no availability functionality as described in chapter 2 of the thesis.
* **terracotta:** The source code and build files for the Terracotta based prototype described in chapter 6.2 of the thesis.
* **hazelcast:** The source code and build files for the Hazelcast based prototype described in chapter 6.3 of the thesis.
* **end2end:** The source code and build files for the end-to-end based prototype described in chapter 6.4 of the thesis.
* **aws:** This folder contains shell script files automating the task of preparing and creating an Amazon EC2 image that can be used as node in the test bench. The content of this folder is described in details in appendix E of the thesis.
* **ec2-mgmt:** This folder contains the source code for the EC2 Java based con troller applications used for running the tests. This is described in details in appendix D of the thesis
