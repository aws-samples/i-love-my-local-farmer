## Overview
- **Updated 8/5/2022 to Support V4 of Azure Functions**
- Automatic SCIM provisioning of Azure AD to AWS SSO with PowerShell using an Azure Function with KeyVault integration using a Timer (cron job) Function
- This solution uses Windows WSL or a Mac due to the running of shell scripts to prompt for values

## Prerequisites
- Configure **AWS Single Sign-On** with the steps outlined in this [article](https://aws.amazon.com/blogs/aws/the-next-evolution-in-aws-single-sign-on/)
- Configure **On-Demand SCIM provisioning of Azure AD to AWS SSO with PowerShell** with the steps outlined in this [article](https://aws.amazon.com/blogs/security/on-demand-scim-provisioning-of-azure-ad-to-aws-sso-with-powershell/)

## Dependencies

- Azure Cli - install from this [link](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
- For Windows 10 with WSL then run the following to ensure zip is installed: apt-get install zip
- The current timer is scheduled to run every 3 minutes - "schedule": "0 */3 * * * *" 
  - To update this value - change the function.json file from 3 to a 2,3 4, 20 etc to have it run every 2,3, 4 or 20 minutes
    - Configure the Cron job with examples from this [article](https://github.com/atifaziz/NCrontab)
- The first time the function is invoked - it will pull down it's Az dependencies automatically, listed in the requirements.psd1 file in this project
    - Please Allow 10 to 15 minutes for this process to complete - you will see initial errors while this is being setup

## Configure Function to trigger SCIM by Job ID
There are multiple functions which are run by a specific job ID -
1. Incremental - will run on a timer every (user defined) minutes triggering the Azure API to sync users
2. Initial - will do a run / reset on all users at midnight. This is used to remove the 'Watermark' assigned to synced users so all of them will be sent over again.
    - The Job Restart Sync is based on the Watermark [article](https://docs.microsoft.com/en-us/graph/api/resources/synchronization-synchronizationjobrestartcriteria?view=graph-rest-beta)


![Profile](./images/Profile.PNG)
![Incremental](./images/Incremental.PNG)
![Reset](./images/Reset.PNG)

## Steps
- Import the main.json file and deploy from Azure.  
    - I recommend using the Azure Portal -  search for Template Specs.  Import the template and Deploy
    - The template asks for two parameters - I  recommend naming the Resource Group and the Project the same and both in lower for ease of deployment
      - User - this is the Object ID of the person deploying and is retrieved from Azure AD, Users, Profile and is a one time use only
      - Project - **Lowercase Only** this is the name of the function - this must be in lowercase only as it creates a Storage Account for the function as part of the process
      

      ![DeployTemplate](./images/TemplateSpecDeploy.PNG)


## Scripts
- Before running the following - login to Azure with the Az Cli using the following command **az login**
- Once the base infrastructure has been deployed run the following which will upload the zip, deploying the functions and the user will be prompted for the values which are stored in Keyvault
- Run the configure.sh script  (If you receive an error - ensure you run chmod +x configure.h)
  - ./tools/configure.sh

  - Items to be filled in are :
      - PROJECT :
      - RESOURCE_GROUP :
      - x-tenant-id : 
      - x-display-name : 
      - x-app-id : 
      - x-client-secret : 

![Entering in values](./images/Config_Deploy1.PNG)
![Entering in values](./images/Config_Deploy2.PNG)

## Check the status of the function
  - To verify the function is working properly - go to the Azure Portal
    - Go to the Resource Group that was deployed
    - Click on the Function App 
    - In the Menu, Select Functions
    - Select the Function you are looking to monitor
    - In the left menu - select Monitor and then on the top menu select Logs
    - This will display the output of the function running
    - If there are any errors, please allow 10-20 minutes for the function to pull down it's initial dependency for the Az module



![Function Output](./images/Function_Monitor.PNG)

## Add additional Functions for SCIM Endpoints 
The App comes with one function, however, to create additional functions to trigger different SCIM endpoints, just copy and paste the function and rename it. This customer had multiple SCIM endpoints that they wanted to trigger so I have added 3 Incremental Functions and 3 Reset functions to trigger Azure, GCP and Github. 

1. To add extra SCIM endpoint functions, copy the folder and rename to the new endpoint
2. Open profile.ps1 and update the new Environment Variable with the SCIM Job ID
3. Open the run.ps1 inside the folder that was copied and **update line (179)** - $jobId = $env:JobIdIncrementalSync with the new environment variable
4. Repeat step 2 if you also have copied the reset folder.  **Update line 184** - $jobId = $env:JobIdResetSyncMSFT with the new environment variable

![Multiple Functions](./images/MultipleFunctions.PNG)

When adding additional functions, you will need to update the main.json template to a [maximum of 10](https://docs.microsoft.com/en-us/azure/azure-functions/functions-app-settings#functions_worker_process_count_). Both of these values to 4.

![Update Values](./images/WorkerProcess.PNG)
