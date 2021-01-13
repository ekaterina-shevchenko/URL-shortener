Prerequisites
-------------
**To run with the client**
* Clone the repository containing client code.
```sh
git clone https://gitlab.db.in.tum.de/per.fuchs/cloud-based-data-processing-url-shortener-client.git
```
* After cloning the client project, the folders structure should be as follows:
	cloud-based-data-processing-url-shortener-client/
    -&nbsp;-&nbsp;-&nbsp;-&nbsp;bin/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;workloads/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;etc...
	consistent-hashing/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;src/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;pom.xml
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;etc...
	url-shortener/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;src/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;terraform/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;keypair/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;ec2.pub
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;main.tf
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;client.tf
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;client_user_data.sh
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;instance_user_data.sh
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;volume/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;sqlite_database
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;pom.xml
* You might also need to add parameter `shell=True` to the function call in cloud-based-data-processing-url-shortener-client/bin/ycsb (line 119) so that it looks like:
```sh
process = subprocess.Popen(stdout=subprocess.PIPE, shell=True, *popenargs, **kwargs)
```

**To run the project on AWS**
* Download terraform.exe executable and add its location to PATH.
* Provide AWS account credentials (in ~/.aws/credentials file or via environment variables)
* Generate ssh-key (using `ssh-keygen` called **ec2/ec2.pub** and store it to **terraform/keypair directory**)
* When infrastructure is not required anymore, destroy it running
```sh
terraform destroy
```
* **Note**: if you want to run without a client, remove `client.tf` file from terraform directory.

**To run the project locally**
* Add consistent-hashing library to your local maven repository (this is done in compile.sh script, though it is required only during the first compile)
* Edit `app.properties` file separately for each process, so that the processes grab different ports. You can point only those a few properties, which require to be rewritten. For example:
Process 1
```sh
# HTTP Server properties
server.port = 8090

# Cluster properties
cluster.port = 4445
cluster.instances = localhost:8090,localhost:8091,localhost:8092
```

Process 2
```sh
# HTTP Server properties
server.port = 8091

# Cluster properties
cluster.port = 4446
cluster.instances = localhost:8090,localhost:8091,localhost:8092
```

Process 3
```sh
# HTTP Server properties
server.port = 8092

# Cluster properties
cluster.port = 4447
cluster.instances = localhost:8090,localhost:8091,localhost:8092
```
* Run `run-local.sh` script in different command prompts so that each process has its own command prompt.
* For running client with the configurations listed above, use the following command (make sure to use python 2.7):
```sh
python ./bin/ycsb run urlshortener -P workloads/workloadshortener -threads 3 -p servers=http://localhost:8090/,http://localhost:8091/,http://localhost:8092/
```

**Adjustments**
* To load new (initial) data to the database upon starting the program, add `db_init.csv` file containing the data to the project directory:
    url-shortener/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;src/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;terraform/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;keypair/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;ec2.pub
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;main.tf
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;client.tf
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;client_user_data.sh
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;instance_user_data.sh
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;volume/
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;-&nbsp;sqlite_database
	-&nbsp;-&nbsp;-&nbsp;-&nbsp;pom.xml
    -&nbsp;-&nbsp;-&nbsp;-&nbsp;db_init.csv

**Warning**
For testing purposes, terraform script creates public S3 buckets and places EC2 instances in a public subnet. Do not upload confidential data there, since it imposes security issues.