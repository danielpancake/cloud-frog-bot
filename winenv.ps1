# This script is used to set environment variables for the current shell from a .env file
Import-Module $env:ChocolateyInstall\helpers\chocolateyProfile.psm1

get-content ".env" | foreach {
    $name, $value = $_.split('=')
    if ($value) {
        set-content env:$name $value
    }
}

refreshenv
