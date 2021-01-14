# Create EC2 instance running client application
resource "aws_instance" "client" {
  ami                         = "ami-0be2609ba883822ec"
  instance_type               = "t2.micro"
  availability_zone           = "us-east-1a"
  key_name                    = aws_key_pair.key_pair.key_name
  associate_public_ip_address = "true"
  vpc_security_group_ids      = [aws_security_group.sg_web_in.id, aws_security_group.sg_web_out.id]
  subnet_id                   = aws_subnet.public_subnet.id
  user_data                   = file("client_user_data.sh")
  private_ip                  = "192.1.0.100"
  iam_instance_profile        = aws_iam_instance_profile.client_profile.name
}

# Create S3 bucket with public reading access
resource "aws_s3_bucket" "s3_bucket_client" {
  bucket        = "url-shortener-client-bucket-sources"
  acl           = "public-read"
  force_destroy = "true"
}

# By default, AWS enables all four options when you create a new S3 bucket via
# the AWS Management Console. However, you need to enable Block Public Access
# explicitly when working with Terraform.
# resource "aws_s3_bucket_public_access_block" "s3_bucket_pab_client" {
#   bucket = aws_s3_bucket.s3_bucket_client.id
#
#   block_public_acls   = true
#   block_public_policy = true
#   ignore_public_acls      = true
#   restrict_public_buckets = true
# }

resource "aws_s3_bucket_object" "client_code_upload" {
  for_each = fileset(path.module, "../../cloud-based-data-processing-url-shortener-client/**/*")
  bucket = aws_s3_bucket.s3_bucket_client.bucket
  key    = each.value
  source = "${path.module}/${each.value}"
}

# Create IAM profile role which grants access to S3 buckets
resource "aws_iam_role" "iam_role_client" {
  name                  = "iam_role_client"
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
resource "aws_iam_instance_profile" "client_profile" {
  name = "client_profile"
  role = aws_iam_role.iam_role.id
}

# Create IAM role policy which grants access to S3 buckets
resource "aws_iam_role_policy" "iam_role_policy_client" {
  name   = "iam_role_policy_client"
  role   = aws_iam_role.iam_role.id
  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": ["arn:aws:s3:::url-shortener-client-bucket-sources"]
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": ["arn:aws:s3:::url-shortener-client-bucket-sources/*"]
    }
  ]
}
EOF
}