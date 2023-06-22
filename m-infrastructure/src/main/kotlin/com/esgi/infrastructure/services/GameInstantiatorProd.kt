package com.esgi.infrastructure.services

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import com.amazonaws.services.ecs.model.*
import com.esgi.applicationservices.services.GameInstantiator
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("prod")
@Service
@Primary
class GameInstantiatorProd(
    private val tcpService: TcpService
): GameInstantiator {
    private fun getGameSecurityGroup(): String {
        val name = "game-security-group"

        val ec2Client = AmazonEC2ClientBuilder.standard()
            .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
            .withRegion(Regions.EU_WEST_3)
            .build()

        val describeSecurityGroupsRequest = DescribeSecurityGroupsRequest()
            .withGroupNames(name)
        val describeSecurityGroupsResult = ec2Client.describeSecurityGroups(describeSecurityGroupsRequest)
        if (describeSecurityGroupsResult.securityGroups.isNotEmpty()) {
            return describeSecurityGroupsResult.securityGroups[0].groupId
        }

        throw Exception("GameSecurityGroup not found")
    }

    private fun waitForTaskToBeRunning(ecsClient: AmazonECS, clusterName: String, taskArn: String): String {
        val timeout = 60 * 1000

        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            val describeTasksRequest = DescribeTasksRequest()
                .withCluster(clusterName)
                .withTasks(taskArn)
            val describeTasksResult = ecsClient.describeTasks(describeTasksRequest)
            val task = describeTasksResult.tasks[0]
            if (task.lastStatus == "RUNNING") {
                Thread.sleep(10000)
                return task.containers[0].networkInterfaces[0].privateIpv4Address
            }
            Thread.sleep(1000)
        }

        throw Exception("Timeout while waiting for task to be running")
    }

    override fun instanciateGame() {
        // Create an instance of the Amazon ECS client
        val ecsClient: AmazonECS = AmazonECSClientBuilder.standard()
            .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
            .withRegion(Regions.EU_WEST_3)
            .build()

        val clusterName = "fpr-backend-cluster"

        val describeServicesRequest = DescribeServicesRequest()
            .withCluster(clusterName)
            .withServices("fpr-backend-service")
        val describeServicesResult = ecsClient.describeServices(describeServicesRequest)

        val networkConfiguration = describeServicesResult.services[0].networkConfiguration

        val subnetId = networkConfiguration.awsvpcConfiguration.subnets[0]
        val securityGroupId = getGameSecurityGroup()

        val containerImage = "075626265631.dkr.ecr.eu-west-3.amazonaws.com/fpr-games-repository:448a82c3-d29c-4921-8b45-480ae59a7cf1"

        val containerName = "fpr-game-default-task"

        val describeTaskDefinitionRequest = DescribeTaskDefinitionRequest()
            .withTaskDefinition("fpr-game-task")

        val describeTaskDefinitionResult = ecsClient.describeTaskDefinition(describeTaskDefinitionRequest)
        val taskDefinition = describeTaskDefinitionResult.taskDefinition

        val containerDefinition = taskDefinition.containerDefinitions.find { it.name == containerName }
        if (containerDefinition != null) {
            // Update the container image
            containerDefinition.image = containerImage

            // Register the updated task definition
            val registerTaskDefinitionRequest = RegisterTaskDefinitionRequest()
                .withFamily("fpr-game-test")
                .withContainerDefinitions(containerDefinition)
                .withCpu(taskDefinition.cpu)
                .withMemory(taskDefinition.memory)
                .withRequiresCompatibilities(taskDefinition.requiresCompatibilities)
                .withExecutionRoleArn(taskDefinition.executionRoleArn)
                .withTaskRoleArn(taskDefinition.taskRoleArn)
                .withNetworkMode(taskDefinition.networkMode)
                .withVolumes(taskDefinition.volumes)
            val registerTaskDefinitionResult = ecsClient.registerTaskDefinition(registerTaskDefinitionRequest)
            val updatedTaskDefinitionArn = registerTaskDefinitionResult.taskDefinition.taskDefinitionArn

//         Create a task request
            val request = RunTaskRequest()
                .withCluster(clusterName)
                .withTaskDefinition(updatedTaskDefinitionArn)
                .withLaunchType("FARGATE")
                .withNetworkConfiguration(
                    NetworkConfiguration()
                        .withAwsvpcConfiguration(
                            AwsVpcConfiguration()
                                .withSubnets(subnetId)
                                .withAssignPublicIp("ENABLED")
                                .withSecurityGroups(securityGroupId)
                        )
                )

            // Run the task
            val runTaskResult: RunTaskResult = ecsClient.runTask(request)

            println(runTaskResult)

            val taskArn = runTaskResult.tasks[0].taskArn

            val ipAddress = waitForTaskToBeRunning(ecsClient, clusterName, taskArn)

            println("Container IP address: $ipAddress")

            tcpService.init_test(ipAddress)
        } else {
            throw Exception("Container definition not found")
        }
    }
}