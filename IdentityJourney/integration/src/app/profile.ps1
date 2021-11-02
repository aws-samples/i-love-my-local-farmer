# Azure Functions profile.ps1
# This profile.ps1 will get executed every "cold start" of your Function App.
# "cold start" occurs when:
#
# * A Function App starts up for the very first time
# * A Function App starts up after being de-allocated due to inactivity
#
# You can define helper functions, run commands, or specify environment variables
# NOTE: any variables defined that are not environment variables will get reset after the first execution

# Authenticate with Azure PowerShell using MSI.
# Remove this if you are not planning on using MSI or Azure PowerShell.
if ($env:MSI_SECRET) {
    Disable-AzContextAutosave -Scope Process | Out-Null
    Connect-AzAccount -Identity
}

# Remove the # from the lines below starting at 28 and paste in the SCIM Job Id
# As you add move functions - copy and paste those lines for new Global Variables

# Provide the SCIM Job ID for each function.  
# This can be retrieved from the Azure Portal at - portal.azure.com
#   Go to Active Directory
#       Select Enterprise Applications
#           Select Provisioning
#               Select View Technical Information

#[System.Environment]::SetEnvironmentVariable("JobIdIncrementalSyncAWS", "scim.1ffec608964c4aaa8f1e125baacd6ed2.68840c83-adad-43c3-b864-e6427de24c57")
#[System.Environment]::SetEnvironmentVariable("JobIdResetSyncAWS", "scim.1ffec608964c4aaa8f1e125baacd6ed2.68840c83-adad-43c3-b864-e6427de24c57")
#[System.Environment]::SetEnvironmentVariable("JobIdIncrementalSyncGCP", "")
#[System.Environment]::SetEnvironmentVariable("JobIdResetSyncGCP", "")
#[System.Environment]::SetEnvironmentVariable("JobIdIncrementalSyncGitHub", "")
#[System.Environment]::SetEnvironmentVariable("JobIdResetSyncGitHub", "")


# Uncomment the next line to enable legacy AzureRm alias in Azure PowerShell.
# Enable-AzureRmAlias

# You can also define functions or aliases that can be referenced in any of your PowerShell functions.
