package io.quartic.gradle

import org.gradle.api.credentials.AwsCredentials

class S3Common {
    public static final String S3_URL = "s3://quartictech-repo.s3.eu-west-2.amazonaws.com/maven2"

    public static def configureS3Maven() {
        return {
            maven {
                url S3_URL
                credentials(AwsCredentials) {
                    def fromFile = readFromCredsFile()
                    accessKey System.getenv("AWS_ACCESS_KEY_ID") ?: fromFile["aws_access_key_id"]
                    secretKey System.getenv("AWS_SECRET_ACCESS_KEY") ?: fromFile["aws_secret_access_key"]
                }
            }
        }
    }

    // We could pull in DefaultAWSCredentialsProviderChain from aws-java-sdk, but that pulls in all sorts of crap with it
    private static def readFromCredsFile() {
        def fields = [:]
        def file = new File("${System.properties["user.home"]}/.aws/credentials")
        if (file.exists()) {
            file.eachLine { line ->
                def matcher = line =~ /^(aws_access_key_id|aws_secret_access_key)\s*=\s(.*)$/
                if (matcher) {
                    fields[matcher[0][1]] = matcher[0][2]
                }
            }
        }
        return fields
    }
}
