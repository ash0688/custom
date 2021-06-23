EDPathFinder installation is required. 
Set your variables at Main2 class and build. 
Run.

Be sure to reset csv to its original state before running again.
Be sure to whitelist 9999 port on firewall when executing.

If you wanna get the fresh systemsPopulated json from EDSM dumps - be sure to split it into 2 parts before processing as it is huge and takes time to parse.
Then prepare xls (cut not needed columns to match the app input format for this app).

***Systems in the csv are sorted by Population. It is not recommended to process more than 500 systems at a time due to API limitations.
