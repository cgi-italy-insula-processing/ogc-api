# Supported OGC API Processes endpoints

- GET /
- GET /conformance
- GET /api
- POST /processes
- GET /processes
- GET /processes/{processId}
- GET /processes/{processId}/package
- POST /processes/{processId}/execution
- PUT /processes/{processId}
- DELETE /processes/{processId}
- GET /jobs
- GET /jobs/{jobId}
- DELETE /jobs/{jobId}
- GET /jobs/{jobId}/results
- GET /jobs/{jobId}/results/{outputId}


# Differences from the official OpenAPI Specification

Reference repository: https://github.com/opengeospatial/ogcapi-processes



## Common

- relative path references are sometimes not handled correctly by the tool used by ESA MAAP to generated the Server Stub (openapi generator https://openapi-generator.tech/).

  For this reason, all `$ref` values have been modified to become absolute paths, by prefixing the original values with the token `_OGC_API_PROCESSES_SPECS_ROOT_`. This token should be replaced with absolute path to the folder containing the specifications.

- Only the schema files strictly necessary to support the API endpoints from section `OGC API Processes endpoints` have been kept. 



## ogcapi-processes.yaml

### paths
- reference to `paths/processes-workflows/pExecution-workflows.yaml` replaced with `paths/processes-core/pExecution.yaml`, since OGC API Processes Part 3 is not supported
- added endpoint `/jobs/{jobId}/results/{outputId}` and reference to `/paths/processes-core/pOutputResult.yaml` since OGC API Processes Part 1 defines that endpoint but it is missing in the OpenAPI specification.

### schemas
- `bbox-processes` schema name renamed to `bbox` (fix)
- add explicit reference to the following schema:
  - `bbox-def-crs` 
  - `input` 


### responses
- add explicit reference to the following schema:
  -  `ProcessSummary`



## parameters/processes-core/outputId.yaml

- Definition of parameter `outputId`, not present in the original ogcapi-processes repo (fix).



## parameters/processes-core/processId-path.yaml

- `processId` schema set to `int64` (fix)



## parameters/unspecified/f-metadata.yaml

- `html` content schema not present, Requirement Class `HTML` is not supported



## paths/processes-core/pExecution.yaml

### post

- parameters:
	- `prefer-header-execution.yaml` parameter not present, since synchronous execution is not supported
	
- responses:
  - status 200 and `rExecuteSync.yaml` schema not present, since synchronous execution is not supported
  - status 204 and `rEmpty.yaml` schema not present since synchronous execution is not supported
- callbacks:
  - field not present, since Requirements Class `Callback` is not supported



## paths/processes-core/pJobResults.yaml

### get

- `prefer-header-results.yaml` parameter not present, since CWL outputs are always of type file and can be retrieved only by reference



## paths/processes-core/pOutputResult.yaml

- Definition of endpoint `/jobs/{jobId}/results/{outputId}`, not present in the original ogcapi-processes repo (fix).



## paths/processes-dru/pPackage.yaml

### get

- requestBody/content:
  - `application/cwl`, `application/cwl+json`, `application/cwl+yaml` no present, since the CWL will be deployed through the `ogcapppkg.yaml`



## paths/processes-dru/pProcessListDeploy.yaml

#### get

- parameters:
  - added reference to `parameters/processes-core/limit.yaml` to handle pagination on GET `/processes` requests (fix) 

### post

- parameters:
	- `w-param.yaml` schema not present, since the CWL will always have one Workflow
- requestBody/content:
	- `application/cwl`, `application/cwl+json`, `application/cwl+yaml` no present, since the CWL will be deployed through the `ogcapppkg.yaml`
- responses:
	- 201: reference to `../../schemas/processes-core/processSummary.yaml` schema replaced with `../../responses/processes-core/rProcessSummary.yaml` response type (fix)
	- 400: reference to `"../../responses/processes-dru/rWorkflowNotFound.yaml"` replaced with `../../responses/common-core/rNotFound.yaml` since `rWorkflowNotFound.yaml` is not present in the ogcapi-processes repo (fix)



## paths/processes-dru/pProcessDescriptionReplaceUndeploy.yaml

### put

- 400: reference to `"../../responses/processes-dru/rWorkflowNotFound.yaml"` replaced with `../../responses/common-core/rNotFound.yaml` since `rWorkflowNotFound.yaml` is not present in the ogcapi-processes repo (fix)



## responses/common-core/rAPI.yaml

- `text/html` content schema not present, Requirement Class `HTML` is not supported



## responses/common-core/rException.yaml

- `text/html` content schema not present, Requirement Class `HTML` is not supported



## responses/common-core/rLandingPage.yaml

- `text/html` content schema not present, Requirement Class `HTML` is not supported



## responses/common-core/rNotAcceptable.yaml

- `text/html` content schema not present, Requirement Class `HTML` is not supported



## responses/common-core/rNotAllowed.yaml

- `text/html` content schema not present, Requirement Class `HTML` is not supported



## responses/common-core/rNotFound.yaml

- `text/html` content schema not present, Requirement Class `HTML` is not supported



## responses/common-core/rServerError.yaml

- `text/html` content schema not present, Requirement Class `HTML` is not supported



## responses/processes-core/rOutputResult.yaml

- Defines the response of a get job output request, not present in the original ogcapi-processes repo (fix) 



## responses/processes-core/rProcessSummary.yaml

- New schema file, not present in the original ogcapi-processes repo (fix). References `processSummary.yaml` schema



## responses/processes-dru/rDuplicateProcess.yaml

- `text/html` content schema not present, Requirement Class `HTML` is not supported



## responses/processes-dru/rImmutableProcess.yaml

- `text/html` content schema not present, Requirement Class `HTML` is not supported



## schemas/processes-core/bbox.yaml

- array size constraints defined in `properties/bbox/oneOf` is not handled correctly by the tool used by ESA MAAP to generated the Server Stub (openapi generator https://openapi-generator.tech/).
For this reason those constraints have been removed from the schema used to generate the ESA MAAP Server Stub. Constraints are enforced by the business logic.



## schemas/processes-core/bbox-def-crs.yaml

- the top level `anyOf` doesn't seem to be handled correctly by the tool used by ESA MAAP to generated the Server Stub (openapi generator https://openapi-generator.tech/).
For this reason only the most generic schema has been kept: `uri` format with a default value



## schemas/processes-core/execute.yaml

- `outputs` property is not present, since request of individual output is not supported in asynchronous mode execution
- `subscriber` property is not present, since Requirements Class `Callback` is not supported



## schemas/processes-core/inlineOrRefData.yaml

- `qualifiedInputValue.yaml` schema is not present, since there is no CWL type that directly maps to this schema



## schemas/processes-core/inputValueNoObject.yaml

- `binaryInputValue.yaml` schema is not present, since there is no CWL type that directly maps to this schema



## schemas/processes-core/metadata.yaml

- the combination of oneOf/allOf is not handled correctly by the tool used by ESA MAAP to generated the Server Stub (openapi generator https://openapi-generator.tech/).
For this reason the two items of the array nested within the `oneOf` have been externalized in two separate schema files: `metadata-1.yaml` and `metadata-2.yaml`. The generated code is equivalent to the one modeled by the original schema.



## schemas/processes-core/schema.yaml

- schema named `schema` are not handled correctly by the tool used by ESA MAAP to generated the Server Stub (openapi generator https://openapi-generator.tech/).

  For this reason the file `schema.yaml` has been renamed to `processesCoreSchema.yaml`.

- polymorphic primitive types are not handled correctly by the tool used by ESA MAAP to generated the Server Stub (openapi generator https://openapi-generator.tech/). 

  For this reason the default value `true` for property `additionalProperties` has been removed.




## schemas/processes-dru/ogcapppkg.yaml

- property `properties/processDescription/required` declared as array (fix)

- the only supported schema for `executionUnit` is `link` since the CWL is expected to be provided by reference.

## paths/processes-dru/pProcessListDeploy.yaml

- parameters:
    - `limit.yaml` added to support the `limit` query parameter and let the user specify the maximum number of results to return in the response. 
       This is not present in the original ogcapi-processes repository.