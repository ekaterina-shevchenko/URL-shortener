#! /bin/bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

aws s3 cp s3://url-shortener-instances-bucket-sources /urlshortener/ --recursive
cd /urlshortener || exit

sudo mkfs -t ext4 /dev/sdh
sudo mkdir -p volume
sudo mount /dev/sdh volume/

sudo yum install java-1.8.0-openjdk -y
sudo java -jar -Djava.net.preferIPv4Stack=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 url-shortener-1.0.jar