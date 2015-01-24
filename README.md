# AWSDemo
Amazon Web Services Demo

What I attempted to create here was a small tool that could spin up EC2 instances,
list the instances that are active, and terminate them.  I had a good deal of trouble
getting the AWS SDK set up on the command line, but was able to get things working well
in Eclipse using the Eclipse AWS SDK plugin.  

To make the program something others could demo I turned it into a jar file.  The 
jar file has a few different requirements though. 

The first requirement is to have a group called bitcrusher with the following setttings:

To set up the bitcrusher group in EC2 on the aws dashboard under Network & Security 
select "Security Groups", click "Create Security Group" set Security Group Name to "bitcrusher"
under the inbound tab select "Custom TCP Rule" under type, select "TCP" under Protocol, enter
"22" in the port range, and set source to "My IP" then click "Create".

The second requirement is that credentials are set up as follows:

If the .aws directory is nonexistant:
cd
mkdir .aws
cd .aws
touch credentials
nano credentials

Put in configuration information as:
[default]
aws_access_key_id = your access key id
aws_secret_access_key = your access key

Usage is as follows:
java -jar DemoRunner.jar <options>
Options: -l list instances, -g generate key, -i new instance, -t terminate instances

On first use generate a key, then use the same key name to create an instance. 
Follow instructions to connect. 
Then when finished terminate instances.

All of these instructions assume that you are on a linux based machine.

My main intention was to produce a tool to quickly spin up an EC2 instance with 
all of the information to ssh to that instance and then terminate the instance.

Screenshots are in the screenshots folder.