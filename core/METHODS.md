# Methods

This document lists all available JSON-RPC methods in the core service.

## Method Naming Convention

Methods follow the pattern: `<service>/<method>`

## Error Codes

| Code | Message | Description |
|------|----------|-------------|
| -32600 | Invalid Request | The JSON sent is not a valid Request object |
| -32601 | Method not found | The method does not exist / is not available |
| -32602 | Invalid params | Invalid method parameter(s) |
| -32603 | Internal error | Internal JSON-RPC error |
| -32700 | Parse error | Invalid JSON was received by the server |

## Utility

### Paginator Object

Used for paginated queries across various services.

**Fields:**
- `pageNumber` (number): The page number (1-indexed)
- `pageSize` (number): Number of items per page
- `sortKey` (string, optional): Field to sort by (e.g., "id", "display_name")
- `sortDirection` (string, optional): Sort direction - "ASC" or "DESC"
- `filters` (object, optional): Key-value pairs for filtering (e.g., {"active": "true"})

**Returns:** `object` containing:
- `page` (array): Array of items for the current page
- `totalCount` (number): Total number of items matching the criteria

## Services

### Authentication Service

#### `auth/registerType`

Registers a new credential type or updates an existing one. No error if already present.

**Parameters:**
- `code` (string): The unique code for the credential type (e.g., "BASIC", "TOKEN", "API_KEY")
- `only_one_per_user_id` (boolean): If true, only one valid credential can exist per user for this type
- `only_one_security_principal` (boolean): If true, only one valid credential can exist per security principal for this type

**Returns:** `object` with `success` boolean indicating if the operation succeeded

**Examples:**
- Basic authentication (username/password): `only_one_per_user_id = true, only_one_security_principal = true`
- Token-based authentication: `only_one_per_user_id = false, only_one_security_principal = true`
- API key authentication: `only_one_per_user_id = false, only_one_security_principal = false`

#### `auth/create`

Creates a new credential for a user. If the credential type has `only_one_per_user_id` set to true, all existing credentials for that user and type will be expired. If the credential type has `only_one_security_principal` set to true, all existing credentials with that security principal will be expired.

**Parameters:**
- `user_id` (number): The user's ID
- `credential_type_code` (string): The credential type code
- `valid_from` (string/number): Timestamp when the credential becomes valid (ISO format or epoch milliseconds)
- `valid_until` (string/number): Timestamp when the credential expires (ISO format or epoch milliseconds)
- `security_principal` (string): The security principal (e.g., username, token, API key)
- `security_credentials` (string, optional): The security credentials (e.g., hashed password, null for token-based auth)

**Returns:** `object` with `id` number - The ID of the created credential

#### `auth/expireOne`

Expires a specific credential for a user and credential type based on the security principal.

**Parameters:**
- `user_id` (number): The user's ID
- `credential_type_code` (string): The credential type code
- `security_principal` (string): The security principal to expire

**Returns:** `object` with `success` boolean indicating if the operation succeeded (true if a credential was expired, false if no matching credential was found)

#### `auth/expireAll`

Expires all valid credentials for a user and credential type.

**Parameters:**
- `user_id` (number): The user's ID
- `credential_type_code` (string): The credential type code

**Returns:** `object` with `success` boolean indicating if the operation succeeded (true if at least one credential was expired, false if no credentials were found)

#### `auth/verify`

Verifies a credential and returns the user ID if valid and not expired.

**Parameters:**
- `credential_type_code` (string): The credential type code
- `security_principal` (string): The security principal
- `security_credentials` (string, optional): The security credentials to verify (null for credential types without credentials)

**Returns:** `object` with `user_id` number if the credential is valid, or `null` if invalid/expired/not found

### Generic Service

#### `generic/ping`

Returns the current database timestamp.

**Parameters:** None

**Returns:** `string` - Current timestamp in format `YYYY-MM-DD HH:mm:ss.SSS`

#### `generic/setConfigValue`

Sets a configuration value for a namespace and key. Creates a new entry or updates an existing one.

**Parameters:**
- `namespace` (string): The configuration namespace
- `key` (string): The configuration key
- `value` (string): The configuration value

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `generic/getConfigValue`

Retrieves a configuration value for a namespace and key.

**Parameters:**
- `namespace` (string): The configuration namespace
- `key` (string): The configuration key

**Returns:** `string` - The configuration value, or `null` if not found

#### `generic/getAllConfigValues`

Retrieves all configuration values across all namespaces.

**Parameters:** None

**Returns:** `object` - A key-value object where keys are in the format `namespace.key` and values are the configuration values

#### `generic/localize`

Saves a translation for a key and language code. Creates a new entry or updates an existing one.

**Parameters:**
- `key` (string): The translation key
- `languageCode` (string): The language code (e.g., "en", "es", "fr")
- `translation` (string): The translated text

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `generic/translate`

Retrieves a translation for a key and language code.

**Parameters:**
- `key` (string): The translation key
- `languageCode` (string): The language code (e.g., "en", "es", "fr")

**Returns:** `string` - The translated text, or `null` if not found

### User Registry Service

#### `user/create`

Creates a new user in the system.

**Parameters:**
- `user` (object): User object containing:
  - `display_name` (string): The user's display name
  - `active` (boolean): Whether the user is active (optional, defaults to true)

**Returns:** `object` with `id` number - The ID of the created user

#### `user/update`

Updates an existing user's information.

**Parameters:**
- `user` (object): User object containing:
  - `id` (number): The user's ID (required)
  - `display_name` (string): The user's display name
  - `active` (boolean): Whether the user is active

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `user/activate`

Activates a user account.

**Parameters:**
- `id` (number): The user's ID

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `user/deactivate`

Deactivates a user account.

**Parameters:**
- `id` (number): The user's ID

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `user/findById`

Retrieves a user by their ID.

**Parameters:**
- `id` (number): The user's ID

**Returns:** `object` - The user object with `id`, `display_name`, and `active` fields, or `null` if not found

#### `user/findBy`

Retrieves a paginated list of users with optional filtering and sorting.

**Parameters:**
- `paginator` (object): Pagination options (see [Paginator Object](#paginator-object))

**Returns:** `object` containing:
- `page` (array): Array of user objects for the current page
- `totalCount` (number): Total number of users matching the criteria

#### `user/setAttribute`

Sets an attribute for a user. Creates a new attribute or updates an existing one.

**Parameters:**
- `user_id` (number): The user's ID
- `name` (string): The attribute name
- `value` (string): The attribute value

**Returns:** `object` with `success` boolean indicating if operation succeeded

#### `user/getAttribute`

Retrieves an attribute value for a user.

**Parameters:**
- `user_id` (number): The user's ID
- `name` (string): The attribute name

**Returns:** `string` - The attribute value, or `null` if not found

### Audit Service

#### `audit/log`

Logs an audit event. Creates a log type entry if it doesn't exist, then inserts an audit log entry.

**Parameters:**
- `user_id` (number, optional): The user ID associated with the event. Use `null` for system events
- `type` (string): The type of audit event (e.g., "LOGIN", "LOGOUT", "SYSTEM")
- `remote_address` (string): The remote address of the client making the request
- `payload` (string): The payload data for the audit event

**Returns:** `object` with `id` number - The ID of the created audit log entry

#### `audit/list`

Retrieves a paginated list of audit log entries with optional filtering and sorting.

**Parameters:**
- `paginator` (object): Pagination options (see [Paginator Object](#paginator-object))

**Available filters:**
- `type_code` (string, optional): Filter by audit log type code (e.g., "LOGIN", "LOGOUT")
- `user_id` (string, optional): Filter by user ID

**Available sort keys:**
- `instant_at`: Sort by timestamp (default)
- `type_code`: Sort by type code
- `user_id`: Sort by user ID

**Returns:** `object` containing:
- `page` (array): Array of audit log entries for the current page. Each entry contains:
  - `id` (number): The audit log entry ID
  - `instant_at` (string): The timestamp of the event
  - `user_id` (number or null): The user ID, or null if it's a system event
  - `type_code` (string): The type code of the audit event
  - `payload` (string): The payload data
- `totalCount` (number): Total number of audit log entries matching the criteria

### Authorization Service

#### `authorization/createRole`

Creates a new role in the system.

**Parameters:**
- `role` (object): Role object containing:
  - `code` (string): The unique role code (e.g., "ADMIN", "USER")
  - `name` (string): The display name of the role

**Returns:** `object` with `id` number - The ID of the created role

**Notes:**
- Role codes must be unique. Attempting to create a duplicate code will result in an error.

#### `authorization/updateRole`

Updates an existing role's information.

**Parameters:**
- `role` (object): Role object containing:
  - `id` (number): The role's ID (required)
  - `code` (string): The role code
  - `name` (string): The display name of the role

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `authorization/authorize`

Authorizes a user to have a specific role for a specified time period. Validates that the authorization period does not overlap with any existing authorization for the same user and role.

**Parameters:**
- `user_id` (number): The user's ID
- `role_id` (number): The role's ID
- `valid_from` (string/number): Timestamp when the authorization becomes valid (ISO format or epoch milliseconds)
- `valid_until` (string/number): Timestamp when the authorization expires (ISO format or epoch milliseconds)

**Returns:** `object` with `id` number - The ID of the created authorization

**Errors:**
- Throws an error if the time period overlaps with an existing authorization for the same user/role combination
- Throws an error if the user does not exist
- Throws an error if the role does not exist

**Overlap Detection:**
- Two authorizations overlap if their time ranges intersect in any way
- Non-overlapping authorizations can be created for the same user/role combination

#### `authorization/deauthorize`

Removes all authorizations for a specific user and role.

**Parameters:**
- `user_id` (number): The user's ID
- `role_id` (number): The role's ID

**Returns:** `object` with `success` boolean indicating if the operation succeeded

**Notes:**
- This removes all active and expired authorizations for the specified user/role combination
- Returns `false` if no authorizations existed for the combination

#### `authorization/isUserInRole`

Checks if a user currently has a specific role based on the current timestamp.

**Parameters:**
- `user_id` (number): The user's ID
- `role_id` (number): The role's ID

**Returns:** `object` with `result` boolean - `true` if the user has an active authorization for the role at the current time, `false` otherwise

**Notes:**
- Only considers authorizations where `valid_from` <= current time <= `valid_until`

#### `authorization/isUserInAnyRoles`

Checks if a user currently has any of the specified roles based on the current timestamp.

**Parameters:**
- `user_id` (number): The user's ID
- `role_ids` (array): An array of role IDs to check

**Returns:** `object` with `result` boolean - `true` if the user has at least one active authorization for any of the specified roles at the current time, `false` otherwise

**Notes:**
- Returns `false` if the role_ids array is empty
- Only considers authorizations where `valid_from` <= current time <= `valid_until`

#### `authorization/isUserInAllRoles`

Checks if a user currently has all of the specified roles based on the current timestamp.

**Parameters:**
- `user_id` (number): The user's ID
- `role_ids` (array): An array of role IDs to check

**Returns:** `object` with `result` boolean - `true` if the user has active authorizations for all of the specified roles at the current time, `false` otherwise

**Notes:**
- Returns `true` if the role_ids array is empty
- Only considers authorizations where `valid_from` <= current time <= `valid_until`

#### `authorization/addFunctionToRole`

Links a function to a role.

**Parameters:**
- `role_id` (number): The role's ID
- `function_id` (number): The function's ID

**Returns:** `object` with `success` boolean indicating if the operation succeeded

**Errors:**
- Throws an error if the role does not exist
- Throws an error if the function does not exist
- Throws an error if the function is already linked to the role

#### `authorization/removeFunctionFromRole`

Unlinks a function from a role.

**Parameters:**
- `role_id` (number): The role's ID
- `function_id` (number): The function's ID

**Returns:** `object` with `success` boolean indicating if the operation succeeded

**Errors:**
- Throws an error if the role does not exist
- Throws an error if the function does not exist

#### `authorization/getFunctionsByRole`

Retrieves all functions linked to a role.

**Parameters:**
- `role_id` (number): The role's ID

**Returns:** `array` - Array of function objects with `id`, `name`, and `url` fields

#### `authorization/getMenuTree`

Retrieves the menu tree for a user based on their authorized functions.

**Parameters:**
- `user_id` (number): The user's ID

**Returns:** `array` - Array of menu objects in hierarchical structure based on the user's authorized functions

### Function & Menu Service

#### `generic/createFunction`

Creates a new function in the system.

**Parameters:**
- `function` (object): Function object containing:
  - `name` (string): The function name (required)
  - `url` (string, optional): The function URL

**Returns:** `object` with `id` number - The ID of the created function

#### `generic/updateFunction`

Updates an existing function.

**Parameters:**
- `function` (object): Function object containing:
  - `id` (number, required): The function ID
  - `name` (string): The function name
  - `url` (string, optional): The function URL

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `generic/createMenu`

Creates a new menu entry.

**Parameters:**
- `menu` (object): Menu object containing:
  - `function_id` (number): The associated function ID (required)
  - `parent_id` (number, optional): The parent menu ID for hierarchical menus

**Returns:** `object` with `id` number - The ID of the created menu entry

#### `generic/updateMenu`

Updates an existing menu entry.

**Parameters:**
- `menu` (object): Menu object containing:
  - `id` (number, required): The menu ID
  - `function_id` (number): The associated function ID
  - `parent_id` (number, optional): The parent menu ID for hierarchical menus

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `generic/getMenuTree`

Retrieves the complete menu tree.

**Parameters:** None

**Returns:** `array` - Array of menu objects in hierarchical structure

#### `generic/findFunctionByName`

Finds a function by name.

**Parameters:**
- `name` (string): The function name

**Returns:** `object` - The function object with `id`, `name`, and `url` fields, or `null` if not found

#### `generic/getAllFunctions`

Retrieves all functions.

**Parameters:** None

**Returns:** `array` - Array of all function objects with `id`, `name`, and `url` fields

### Remote Endpoints Service

#### `remote_endpoints/create`

Creates a new remote endpoint.

**Parameters:**
- `name` (string): The name of the remote endpoint

**Returns:** `object` with `id` number - The ID of the created remote endpoint

#### `remote_endpoints/update`

Updates an existing remote endpoint's name.

**Parameters:**
- `id` (number): The remote endpoint's ID
- `name` (string): The new name for the remote endpoint

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `remote_endpoints/ping`

Updates the last communication timestamp and current address for a remote endpoint.

**Parameters:**
- `id` (number): The remote endpoint's ID
- `current_address` (string): The current address of the remote endpoint

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `remote_endpoints/list`

Lists all remote endpoints with their details.

**Parameters:** None

**Returns:** `array` - Array of remote endpoint objects containing id, name, current_address, token, and last_communication_at

#### `remote_endpoints/generateToken`

Generates or retrieves the authentication token for a remote endpoint.

**Parameters:**
- `id` (number): The remote endpoint's ID

**Returns:** `object` with `token` string - The authentication token

#### `remote_endpoints/getByToken`

Retrieves a remote endpoint by its authentication token.

**Parameters:**
- `token` (string): The authentication token

**Returns:** `object` - The remote endpoint object with id, name, current_address, and last_communication_at, or `null` if not found

### Messages Service

#### `messages/publish`

Publishes messages with a topic and list of payloads.

**Parameters:**
- `topic` (string): The message topic
- `payloads` (array): List of payload strings

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `messages/poll`

Retrieves messages for a specific topic ordered by creation time.

**Parameters:**
- `topic` (string): The message topic to filter by

**Returns:** `array` - Array of message objects containing id, topic, payload, and created_at

### Files Service

#### `files/create`

Creates a new file in the system.

**Parameters:**
- `file` (object): File object containing:
  - `name` (string): The file name (required)
  - `content_type` (string, optional): The content type of the file
  - `payload` (string, optional): The file content as base64 string

**Returns:** `object` with `id` number - The ID of the created file

#### `files/delete`

Deletes a file by its ID.

**Parameters:**
- `id` (number): The file's ID

**Returns:** `object` with `success` boolean indicating if the operation succeeded

#### `files/list`

Retrieves a paginated list of files with optional filtering and sorting.

**Parameters:**
- `paginator` (object): Pagination options (see [Paginator Object](#paginator-object))

**Available filters:**
- `name` (string, optional): Filter by file name (partial match)

**Returns:** `object` containing:
- `page` (array): Array of file objects for the current page
- `totalCount` (number): Total number of files matching the criteria

#### `files/searchByName`

Searches for files by name with partial matching.

**Parameters:**
- `name` (string): The name or partial name to search for

**Returns:** `array` - Array of file objects with id, name, content_type, and created_at

