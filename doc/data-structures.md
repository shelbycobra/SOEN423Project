# Data Structure Definitions

## `jsonArray` structure for `getData` and `setData` methods

```
[
    {
        DEMS.MessageKeys.SERVER_LOCATION: serverLocation, // CA, UK or US

        DEMS.MessageKeys.RECORD_ID: recordID, // MR..... or ER.....
        DEMS.MessageKeys.FIRST_NAME: firstName,
        DEMS.MessageKeys.LAST_NAME: lastName,
        DEMS.MessageKeys.EMPLOYEE_ID: employeeID, // int
        DEMS.MessageKeys.MAIL_ID: mailID,

        // if managerRecord
        DEMS.MessageKeys.PROJECTS: [
            {
                DEMS.MessageKeys.PROJECT_ID: projectID, // int
                DEMS.MessageKeys.PROJECT_CLIENT: clientName,
                DEMS.MessageKeys.PROJECT_NAME: projectName,
            },
            ...
        ],
        DEMS.MessageKeys.LOCATION: location, // CA, UK or US

        // if employeeRecord
        DEMS.MessageKeys.PROJECT_ID: projectID, // int
    },
    ...
]
```
