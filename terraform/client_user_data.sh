#! /bin/bash
sudo yum install java-1.8.0-openjdk-devel -y

cd /opt || exit
sudo wget https://downloads.apache.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz
sudo tar -xvzf apache-maven-3.6.3-bin.tar.gz

curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

sudo aws s3 cp s3://url-shortener-client-bucket-sources /client/ --recursive
cd /client/cloud-based-data-processing-url-shortener-client || exit
sudo chown ec2-user /client
sudo chmod 777 /client -R
PATH=$PATH:/opt/apache-maven-3.6.3/bin
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.272.b10-1.amzn2.0.1.x86_64
sudo update-alternatives --install "/usr/bin/mvn" "mvn" "/opt/apache-maven-3.6.3/bin/mvn" 1

python ./bin/ycsb run urlshortener -P workloads/workloadshortener -threads 3 -p servers=http://192.1.0.101:8080/,http://192.1.0.102:8080/,http://192.1.0.103:8080/
