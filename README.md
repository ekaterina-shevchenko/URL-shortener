Prerequisites
-------------
\
**To run with the client**

* Clone the repository containing client code.
```sh
git clone https://gitlab.db.in.tum.de/per.fuchs/cloud-based-data-processing-url-shortener-client.git
```
* If you are running the client on Windows, you might also need to add parameter `shell=True` to the function call in cloud-based-data-processing-url-shortener-client/bin/ycsb (line 119) so that it looks like:
```sh
process = subprocess.Popen(stdout=subprocess.PIPE, shell=True, *popenargs, **kwargs)
```
\
**To run the project on AWS**

* Download terraform.exe executable and add its location to PATH.
* Provide AWS account credentials (in ~/.aws/credentials file or via environment variables)
* Generate ssh-key (using `ssh-keygen` called **ec2/ec2.pub** and store it to **terraform/keypair directory**)
* When infrastructure is not required anymore, destroy it running
```sh
terraform destroy
```
* **Note**: if you want to run without a client, remove `client.tf` file from terraform directory.

\
**To run the project locally**

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
* Run the program in different command prompts so that each process has its own command prompt. If you have edited properties in url-shortener/src/main/resources,
you can simply run `run-local.sh`. Otherwise, you can create a new properties file in the project directory and run the program
providing the file, for this run:
```sh
mvn clean package exec:java -Dapp.properties=app.properties -pl url-shortener
```
* For running client with the configurations listed above, use the following command (make sure to use python 2.7 and run it from the client's project directory):
```sh
python ./bin/ycsb run urlshortener -P workloads/workloadshortener -threads 3 -p servers=http://localhost:8090/,http://localhost:8091/,http://localhost:8092/
```
\
**Adjustments**

* To load new (initial) data to the database upon starting the program, add `db_init.csv` file containing the data to the directory from which the program will be executed. Make sure that the file does not contain header.
* To make the application using sqlite_database, having been already filled with data, add sqlite_database file to the folder `volume/`. For running on AWS, additionally uncomment the following code snippet in `terraform/main.tf` file:
```sh
# Upload DB state to S3 bucket
resource "aws_s3_bucket_object" "dbstate_upload" {
  bucket        = aws_s3_bucket.s3_bucket.id
  key           = "sqlite_database"
  source        = "${path.module}/../volume/sqlite_database"
  force_destroy = true
}
```
\
**Warning**

For testing purposes, terraform script creates public S3 buckets and places EC2 instances in a public subnet. Do not upload confidential data there, since it imposes security issues.