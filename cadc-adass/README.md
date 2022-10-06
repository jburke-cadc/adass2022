# TBD

This module provides ???. It uses the persistence layer code (rather than the web service) for the various functions.

## Usage

```
Usage: TBD <command> [-v|--verbose|-d|--debug] [-h|--help]
Where command is:

--import-users                                  : Import and create Users, Groups, and vault folders
    --file=<import-properties-file>             : Config file
    --outfile=<tsv of user info>                : Username, password, email, and vault folder
    [--dry-run] 
--send-email                                    : Send an email to selected users
    --file=<email-properties-file>              : Config file with email details
    --outfile=<tsv of user info>                : Username, password, email, and vault folder
    [--dry-run]     
        
-v|--verbose                    : Verbose mode print progress and error messages
-d|--debug                      : Debug mode print all the logging messages
-h|--help                       : Print this message and exit
```

## config file

pretalx.apiUrl = <pretalx endpoint to retrieve all submisions>
cadc-cert-gen.exec = <path to cadc-cert-gen>
cadc-cert-gen.cert = <path to caller cert>
cadc-cert-gen.signingCert = <path to signing cert>
cadc-cert-gen.server = <database server>
cadc-cert-gen.archive = <database table>
vault.rootFolder = <root vault folder>
