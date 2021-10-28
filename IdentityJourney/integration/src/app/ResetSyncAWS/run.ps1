param($Timer)

# Get the current universal time in the default string format.
$currentUTCtime = (Get-Date).ToUniversalTime()

# The 'IsPastDue' property is 'true' when the current function invocation is later than scheduled.
if ($Timer.IsPastDue) {
    Write-Host "PowerShell timer is running late!"
}

#region Internal
function Get-GraphServicePrincipal() {

    [OutputType([string])]
    param (
        [Parameter(Mandatory = $true)]
        [ValidateNotNull()]
        [securestring]$AccessToken,

        [Parameter(Mandatory = $true)]
        [ValidateNotNull()]
        [string]$DisplayName
    )

    begin {
        Write-Host ("Initiating function " + $MyInvocation.MyCommand + " begin")
        $params = @{
            Method  = "GET"
            Headers = @{
                "Authorization" = ("Bearer " + (ConvertFrom-SecureString -SecureString $accessToken -AsPlainText))
                "Accept"        = "application/json"
            }
        }
    }

    process {
        Write-Host ("Initiating function " + $MyInvocation.MyCommand + " process")
        try {
            $response = Invoke-RestMethod @params -Uri "https://graph.microsoft.com/beta/servicePrincipals"
            $response.value | ForEach-Object {
                if ($_.displayName -like $displayName) {
                    $objectId = $_.id
                }
            }
            if ($response.'@odata.nextLink') {
                Write-Host "NextLink Section"
                $nextLink = $response.'@odata.nextLink'
                while ($nextLink -ne $null) {
                    $output = Invoke-RestMethod @params -Uri $nextLink
                    $output.value | ForEach-Object {
                        if ($_.displayName -like $displayName) {
                            $objectId = $_.id
                        }
                    }
                    $nextLink = $output.'@odata.nextLink'

                }
            }
        }
        catch {
            Write-Error -ErrorRecord $_ -ErrorAction Stop
        }
    }

    end {
        Write-Host ("Initiating function " + $MyInvocation.MyCommand + " end")
        return $objectId
    }

}
function New-GraphAccessToken() {

    [OutputType([securestring])]
    param (
        [Parameter(Mandatory = $true)]
        [string]$TenantId,

        [Parameter(Mandatory = $true)]
        [string]$ApplicationId,

        [Parameter(Mandatory = $true)]
        [securestring]$ClientSecret
    )

    begin {
        Write-Host ("Initiating function " + $MyInvocation.MyCommand + " begin")
        $params = @{
            Method  = "POST"
            Uri     = ("https://login.microsoftonline.com/" + $tenantId + "/oauth2/token")
            Headers = @{
                "Content-Type" = "application/x-www-form-urlencoded"
                "Accept"       = "application/json"
            }
            Body    = @{
                "resource"      = "https://graph.microsoft.com"
                "grant_type"    = "client_credentials"
                "client_id"     = "$applicationId"
                "client_secret" = "$(ConvertFrom-SecureString -SecureString $clientSecret -AsPlainText)"
            }
        }
    }

    process {
        Write-Host ("Initiating function " + $MyInvocation.MyCommand + " process")
        try {
            $response = Invoke-RestMethod @params
            $accessToken = ConvertTo-SecureString -String "$($response.access_token)" -AsPlainText -Force
        }
        catch {
            Write-Error -ErrorRecord $_ -ErrorAction Stop
        }
    }

    end {
        Write-Host ("Initiating function " + $MyInvocation.MyCommand + " end")
        return $accessToken
    }

}
function Restart-GraphSynchronizationJob() {

    [OutputType([string])]
    param (
        [Parameter(Mandatory = $true)]
        [ValidateNotNull()]
        [securestring]$AccessToken,

        [Parameter(Mandatory = $true)]
        [ValidateNotNull()]
        [string]$ServicePrincipalId,

        [Parameter(Mandatory = $true)]
        [ValidateNotNull()]
        [string]$JobId
    )

    begin {
        Write-Host ("Initiating function " + $MyInvocation.MyCommand + " begin")
        $params = @{
            Method  = "POST"
            Uri     = ("https://graph.microsoft.com/beta/servicePrincipals/" + $servicePrincipalId + "/synchronization/jobs/" + $JobId + "/restart")
            Headers = @{
                "Authorization" = ("Bearer " + (ConvertFrom-SecureString -SecureString $accessToken -AsPlainText))
            }
            Body    = (@{
                    "criteria" = @{
                        "resetScope" = "Watermark"
                    } 
            } | ConvertTo-Json -Depth 5)
        }
    }

    process {
        Write-Host ("Initiating function " + $MyInvocation.MyCommand + " process")
        try {
            $response = Invoke-RestMethod @params
        }
        catch {
            Write-Error -ErrorRecord $_ -ErrorAction Stop
        }
    }

    end {
        Write-Host ("Initiating function " + $MyInvocation.MyCommand + " end")
        return $response
    }

}
function Initialization {

    param ()

    begin {
        $tenantId = $env:X_TENANT_ID
        $displayName = $env:X_DISPLAY_NAME
        $applicationId = $env:X_APPLICATION_ID
        $clientSecret = ConvertTo-SecureString -String "$env:X_CLIENT_SECRET" -AsPlainText -Force
    }

    process {
        $accessToken = New-GraphAccessToken -TenantId $tenantId -ApplicationId $applicationId -ClientSecret $clientSecret
        $servicePrincipalId = Get-GraphServicePrincipal -AccessToken $accessToken -DisplayName $displayName
        $jobId = $env:JobIdResetSyncAWS
        Restart-GraphSynchronizationJob -AccessToken $accessToken -ServicePrincipalId $servicePrincipalId -JobId $jobId
    }

    end {
    }

}
#endregion

Initialization

# Write an information log with the current time.
Write-Host "PowerShell timer trigger function ran! TIME: $currentUTCtime"
