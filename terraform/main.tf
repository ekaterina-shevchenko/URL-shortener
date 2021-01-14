# Configure the AWS provider
provider "aws" {
  region  = "us-east-1"
  version = "~> 3.20.0"
}

# Create a VPC
resource "aws_vpc" "vpc" {
  cidr_block           = "192.1.0.0/16"
  enable_dns_hostnames = "true"
}

# Attach IGW to the VPC
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.vpc.id
}

# Create a private subnet
resource "aws_subnet" "public_subnet" {
  vpc_id                  = aws_vpc.vpc.id
  cidr_block              = "192.1.0.0/24"
  availability_zone       = "us-east-1a"
  map_public_ip_on_launch = "true"
}

# Define a route table
resource "aws_route_table" "route_table" {
  vpc_id = aws_vpc.vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }
}

# Assign the route table to the public subnet
resource "aws_route_table_association" "web_public_rt_association" {
  subnet_id      = aws_subnet.public_subnet.id
  route_table_id = aws_route_table.route_table.id
}

# Create an EC2 instance
resource "aws_instance" "instance_1" {
  ami                         = "ami-0be2609ba883822ec"
  instance_type               = "t2.micro"
  availability_zone           = "us-east-1a"
  key_name                    = aws_key_pair.key_pair.key_name
  associate_public_ip_address = "true"
  vpc_security_group_ids      = [aws_security_group.sg_web_in.id, aws_security_group.sg_web_out.id]
  subnet_id                   = aws_subnet.public_subnet.id
  user_data                   = file("instance_user_data.sh")
  private_ip                  = "192.1.0.101"
  iam_instance_profile        = aws_iam_instance_profile.instance_profile.name
}

# Create an EBS volume and attach it to EC2 instance
resource "aws_ebs_volume" "i1_volume" {
  type              = "gp2"
  size              = 30
  availability_zone = "us-east-1a"
  encrypted         = "false"
}

# Attach volume 1 to EC2 instance 1
resource "aws_volume_attachment" "ebs_attachment_1" {
  device_name  = "/dev/sdh"
  volume_id    = aws_ebs_volume.i1_volume.id
  instance_id  = aws_instance.instance_1.id
  force_detach = true
}

resource "aws_instance" "instance_2" {
  ami                         = "ami-0be2609ba883822ec"
  instance_type               = "t2.micro"
  availability_zone           = "us-east-1a"
  key_name                    = aws_key_pair.key_pair.key_name
  associate_public_ip_address = "true"
  vpc_security_group_ids      = [aws_security_group.sg_web_in.id, aws_security_group.sg_web_out.id]
  subnet_id                   = aws_subnet.public_subnet.id
  user_data                   = file("instance_user_data.sh")
  private_ip                  = "192.1.0.102"
  iam_instance_profile        = aws_iam_instance_profile.instance_profile.name
}

# Create an EBS volume and attach it to EC2 instance
resource "aws_ebs_volume" "i2_volume" {
  type              = "gp2"
  size              = 30
  availability_zone = "us-east-1a"
  encrypted         = "false"
}

# Attach volume 2 to EC2 instance 2
resource "aws_volume_attachment" "ebs_attachment_2" {
  device_name  = "/dev/sdh"
  volume_id    = aws_ebs_volume.i2_volume.id
  instance_id  = aws_instance.instance_2.id
  force_detach = true
}

resource "aws_instance" "instance_3" {
  ami                         = "ami-0be2609ba883822ec"
  instance_type               = "t2.micro"
  availability_zone           = "us-east-1a"
  key_name                    = aws_key_pair.key_pair.key_name
  associate_public_ip_address = "true"
  vpc_security_group_ids      = [aws_security_group.sg_web_in.id, aws_security_group.sg_web_out.id]
  subnet_id                   = aws_subnet.public_subnet.id
  user_data                   = file("instance_user_data.sh")
  private_ip                  = "192.1.0.103"
  iam_instance_profile        = aws_iam_instance_profile.instance_profile.name
}

# Create an EBS volume and attach it to EC2 instance
resource "aws_ebs_volume" "i3_volume" {
  type              = "gp2"
  size              = 30
  availability_zone = "us-east-1a"
  encrypted         = "false"
}

# Attach volume 3 to EC2 instance 3
resource "aws_volume_attachment" "ebs_attachment_3" {
  device_name  = "/dev/sdh"
  volume_id    = aws_ebs_volume.i3_volume.id
  instance_id  = aws_instance.instance_3.id
  force_detach = true
}

# Create S3 bucket with public reading access
resource "aws_s3_bucket" "s3_bucket" {
  bucket        = "url-shortener-instance-bucket-sources"
  acl           = "public-read"
  force_destroy = "true"
}

# By default, AWS enables all four options when you create a new S3 bucket via
# the AWS Management Console. However, you need to enable Block Public Access
# explicitly when working with Terraform.
# resource "aws_s3_bucket_public_access_block" "s3_bucket_pab" {
#   bucket = aws_s3_bucket.s3_bucket.id
#
#   block_public_acls   = true
#   block_public_policy = true
#   ignore_public_acls      = true
#   restrict_public_buckets = true
# }

# Upload source code to S3 bucket
resource "aws_s3_bucket_object" "source_code_upload" {
  bucket        = aws_s3_bucket.s3_bucket.id
  key           = "url-shortener-1.0.jar"
  source        = "${path.module}/../url-shortener/target/url-shortener-1.0.jar"
  force_destroy = true
}

# Upload DB state to S3 bucket
# resource "aws_s3_bucket_object" "dbstate_upload" {
#   bucket        = aws_s3_bucket.s3_bucket.id
#   key           = "sqlite_database"
#   source        = "${path.module}/../volume/sqlite_database"
#   force_destroy = true
# }

# Define security group allowing inbound public access
resource "aws_security_group" "sg_web_in" {
  name        = "web-security-group-all-in"
  description = "Allow access to our webserver"
  vpc_id      = aws_vpc.vpc.id

  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Define security group allowing outbound access to internet
resource "aws_security_group" "sg_web_out" {
  name        = "web-security-group-all-out"
  description = "Allow access to internet"
  vpc_id      = aws_vpc.vpc.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Add key-pair
resource "aws_key_pair" "key_pair" {
  key_name   = "ec2"
  public_key = file("keypair/ec2.pub")
}

# Create IAM profile role which grants access to S3 buckets
resource "aws_iam_role" "iam_role" {
  name                  = "iam_role"
  path                  = "/"
  force_detach_policies = true
  assume_role_policy    = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

# Create IAM instance profile
resource "aws_iam_instance_profile" "instance_profile" {
  name = "instance_profile"
  role = aws_iam_role.iam_role.id
}

# Create IAM role policy which grants access to S3 buckets
resource "aws_iam_role_policy" "iam_role_policy" {
  name   = "iam_role_policy"
  role   = aws_iam_role.iam_role.id
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": ["arn:aws:s3:::url-shortener-instances-bucket-sources"]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": ["arn:aws:s3:::url-shortener-instances-bucket-sources/*"]
    }
  ]
}
EOF
}