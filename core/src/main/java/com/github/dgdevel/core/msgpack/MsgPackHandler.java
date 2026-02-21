package com.github.dgdevel.core.msgpack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dgdevel.core.common.Paginator;
import com.github.dgdevel.core.db.DatabaseManager;
import com.github.dgdevel.core.model.Function;
import com.github.dgdevel.core.model.Menu;
import com.github.dgdevel.core.model.Role;
import com.github.dgdevel.core.model.User;
import com.github.dgdevel.core.registry.AuthenticationRegistry;
import com.github.dgdevel.core.registry.AuthorizationRegistry;
import com.github.dgdevel.core.registry.GenericRegistry;
import com.github.dgdevel.core.registry.UserRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.value.ValueType;
import org.msgpack.value.ValueFactory;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MsgPackHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, java.util.function.Function<Object[], Object>> methods = new HashMap<>();
    private final List<MethodDescriptor> methodDescriptors = new ArrayList<>();
    private final DatabaseManager databaseManager;
    private final UserRegistry userRegistry;
    private final AuthenticationRegistry authenticationRegistry;
    private final AuthorizationRegistry authorizationRegistry;
    private final GenericRegistry genericRegistry;

    private static class MethodDescriptor {
        String name;
        String description;
        List<Map<String, Object>> params;
        java.util.function.Function<Object[], Object> implementation;

        MethodDescriptor(String name, String description, List<Map<String, Object>> params,
                         java.util.function.Function<Object[], Object> implementation) {
            this.name = name;
            this.description = description;
            this.params = params;
            this.implementation = implementation;
        }
    }

    private void registerMethod(String name, String description, List<Map<String, Object>> params,
                                 java.util.function.Function<Object[], Object> implementation) {
        methodDescriptors.add(new MethodDescriptor(name, description, params, implementation));
        methods.put(name, implementation);
    }

    public MsgPackHandler(
        DatabaseManager databaseManager,
        UserRegistry userRegistry,
        AuthenticationRegistry authenticationRegistry,
        AuthorizationRegistry authorizationRegistry,
        GenericRegistry genericRegistry) {
        this.databaseManager = databaseManager;
        this.userRegistry = userRegistry;
        this.authenticationRegistry = authenticationRegistry;
        this.authorizationRegistry = authorizationRegistry;
        this.genericRegistry = genericRegistry;
        registerMethods();
    }

    private void registerMethods() {
        registerMethod("generic/ping", "Returns the current database timestamp",
            List.of(),
            params -> {
                try {
                    return databaseManager.getCurrentTimestamp().toString();
                } catch (Exception e) {
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
            });

        registerMethod("generic/setConfigValue",
            "Sets a configuration value for a namespace and key",
            List.of(
                Map.of("name", "namespace", "type", "string", "required", true, "description", "The configuration namespace"),
                Map.of("name", "key", "type", "string", "required", true, "description", "The configuration key"),
                Map.of("name", "value", "type", "string", "required", true, "description", "The configuration value")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    String namespace = paramArray[0].toString();
                    String key = paramArray[1].toString();
                    String value = paramArray[2].toString();
                    boolean success = databaseManager.setConfigValue(namespace, key, value);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
            });

        registerMethod("generic/getConfigValue",
            "Retrieves a configuration value for a namespace and key",
            List.of(
                Map.of("name", "namespace", "type", "string", "required", true, "description", "The configuration namespace"),
                Map.of("name", "key", "type", "string", "required", true, "description", "The configuration key")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    String namespace = paramArray[0].toString();
                    String key = paramArray[1].toString();
                    return databaseManager.getConfigValue(namespace, key);
                } catch (Exception e) {
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
            });

        registerMethod("generic/getAllConfigValues",
            "Retrieves all configuration values across all namespaces",
            List.of(),
            params -> {
                try {
                    return databaseManager.getAllConfigValues();
                } catch (Exception e) {
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
            });

        registerMethod("generic/localize",
            "Saves a translation for a key and language code",
            List.of(
                Map.of("name", "key", "type", "string", "required", true, "description", "The translation key"),
                Map.of("name", "languageCode", "type", "string", "required", true, "description", "The language code (e.g., 'en', 'es', 'fr')"),
                Map.of("name", "translation", "type", "string", "required", true, "description", "The translated text")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    String key = paramArray[0].toString();
                    String languageCode = paramArray[1].toString();
                    String translation = paramArray[2].toString();
                    boolean success = databaseManager.setLocalization(key, languageCode, translation);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
            });

        registerMethod("generic/translate",
            "Retrieves a translation for a key and language code",
            List.of(
                Map.of("name", "key", "type", "string", "required", true, "description", "The translation key"),
                Map.of("name", "languageCode", "type", "string", "required", true, "description", "The language code (e.g., 'en', 'es', 'fr')")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    String key = paramArray[0].toString();
                    String languageCode = paramArray[1].toString();
                    return databaseManager.getTranslation(key, languageCode);
                } catch (Exception e) {
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
            });

        registerMethod("user/create",
            "Creates a new user in the system",
            List.of(
                Map.of("name", "user", "type", "object", "required", true, "description", "User object containing display_name and optionally active")
            ),
            params -> {
                try {
                    User user = objectMapper.convertValue(params[0], User.class);
                    Long id = userRegistry.create(user);
                    return Map.of("id", id);
                } catch (Exception e) {
                    throw new RuntimeException("User error: " + e.getMessage(), e);
                }
            });

        registerMethod("user/update",
            "Updates an existing user's information",
            List.of(
                Map.of("name", "user", "type", "object", "required", true, "description", "User object containing id, display_name, and active")
            ),
            params -> {
                try {
                    User user = objectMapper.convertValue(params[0], User.class);
                    boolean success = userRegistry.update(user);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("User error: " + e.getMessage(), e);
                }
            });

        registerMethod("user/activate",
            "Activates a user account",
            List.of(
                Map.of("name", "id", "type", "number", "required", true, "description", "The user's ID")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long id = ((Number) paramArray[0]).longValue();
                    boolean success = userRegistry.activate(id);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("User error: " + e.getMessage(), e);
                }
            });

        registerMethod("user/deactivate",
            "Deactivates a user account",
            List.of(
                Map.of("name", "id", "type", "number", "required", true, "description", "The user's ID")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long id = ((Number) paramArray[0]).longValue();
                    boolean success = userRegistry.deactivate(id);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("User error: " + e.getMessage(), e);
                }
            });

        registerMethod("user/findById",
            "Retrieves a user by their ID",
            List.of(
                Map.of("name", "id", "type", "number", "required", true, "description", "The user's ID")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long id = ((Number) paramArray[0]).longValue();
                    return userRegistry.findById(id);
                } catch (Exception e) {
                    throw new RuntimeException("User error: " + e.getMessage(), e);
                }
            });

        registerMethod("user/findBy",
            "Retrieves a paginated list of users with optional filtering and sorting",
            List.of(
                Map.of("name", "paginator", "type", "object", "required", true, "description", "Pagination options")
            ),
            params -> {
                try {
                    Paginator paginator = objectMapper.convertValue(params[0], Paginator.class);
                    return userRegistry.findBy(paginator);
                } catch (Exception e) {
                    throw new RuntimeException("User error: " + e.getMessage(), e);
                }
            });

        registerMethod("user/setAttribute",
            "Sets an attribute for a user",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID"),
                Map.of("name", "name", "type", "string", "required", true, "description", "The attribute name"),
                Map.of("name", "value", "type", "string", "required", true, "description", "The attribute value")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    String name = paramArray[1].toString();
                    String value = paramArray[2].toString();
                    boolean success = userRegistry.setAttribute(userId, name, value);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("User error: " + e.getMessage(), e);
                }
            });

        registerMethod("user/getAttribute",
            "Retrieves an attribute value for a user",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID"),
                Map.of("name", "name", "type", "string", "required", true, "description", "The attribute name")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    String name = paramArray[1].toString();
                    return userRegistry.getAttribute(userId, name);
                } catch (Exception e) {
                    throw new RuntimeException("User error: " + e.getMessage(), e);
                }
            });

        registerMethod("audit/log",
            "Logs an audit event",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", false, "description", "The user ID associated with the event (null for system events)"),
                Map.of("name", "type", "type", "string", "required", true, "description", "The type of audit event (e.g., LOGIN, LOGOUT, SYSTEM)"),
                Map.of("name", "payload", "type", "string", "required", true, "description", "The payload data for the audit event")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = paramArray[0] != null ? ((Number) paramArray[0]).longValue() : null;
                    String typeCode = paramArray[1].toString();
                    String payload = paramArray[2].toString();
                    Long id = databaseManager.auditLog(userId, typeCode, payload);
                    return Map.of("id", id);
                } catch (Exception e) {
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
            });

        registerMethod("audit/list",
            "Retrieves a paginated list of audit log entries with optional filtering and sorting",
            List.of(
                Map.of("name", "paginator", "type", "object", "required", true, "description", "Pagination options with optional filters for type_code and user_id")
            ),
            params -> {
                try {
                    Paginator paginator = objectMapper.convertValue(params[0], Paginator.class);
                    return databaseManager.auditLogList(paginator);
                } catch (Exception e) {
                    throw new RuntimeException("Database error: " + e.getMessage(), e);
                }
            });

        registerMethod("auth/registerType",
            "Registers a new credential type or updates an existing one",
            List.of(
                Map.of("name", "code", "type", "string", "required", true, "description", "The unique code for the credential type"),
                Map.of("name", "only_one_per_user_id", "type", "boolean", "required", true, "description", "If true, only one valid credential can exist per user for this type"),
                Map.of("name", "only_one_security_principal", "type", "boolean", "required", true, "description", "If true, only one valid credential can exist per security principal for this type")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    String code = paramArray[0].toString();
                    boolean onlyOnePerUserId = Boolean.parseBoolean(paramArray[1].toString());
                    boolean onlyOneSecurityPrincipal = Boolean.parseBoolean(paramArray[2].toString());
                    authenticationRegistry.registerType(code, onlyOnePerUserId, onlyOneSecurityPrincipal);
                    return Map.of("success", true);
                } catch (Exception e) {
                    throw new RuntimeException("Auth error: " + e.getMessage(), e);
                }
            });

        registerMethod("auth/create",
            "Creates a new credential for a user",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID"),
                Map.of("name", "credential_type_code", "type", "string", "required", true, "description", "The credential type code"),
                Map.of("name", "valid_from", "type", "string/number", "required", true, "description", "Timestamp when the credential becomes valid (ISO format or epoch milliseconds)"),
                Map.of("name", "valid_until", "type", "string/number", "required", true, "description", "Timestamp when the credential expires (ISO format or epoch milliseconds)"),
                Map.of("name", "security_principal", "type", "string", "required", true, "description", "The security principal (e.g., username, token, API key)"),
                Map.of("name", "security_credentials", "type", "string", "required", false, "description", "The security credentials (e.g., hashed password, null for token-based auth)")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    String credentialTypeCode = paramArray[1].toString();
                    Timestamp validFrom = parseTimestamp(paramArray[2]);
                    Timestamp validUntil = parseTimestamp(paramArray[3]);
                    String securityPrincipal = paramArray[4].toString();
                    String securityCredentials =
                        paramArray.length > 5 && paramArray[5] != null ? paramArray[5].toString() : null;
                    Long id =
                        authenticationRegistry.create(
                            userId,
                            credentialTypeCode,
                            validFrom,
                            validUntil,
                            securityPrincipal,
                            securityCredentials);
                    return Map.of("id", id);
                } catch (Exception e) {
                    throw new RuntimeException("Auth error: " + e.getMessage(), e);
                }
            });

        registerMethod("auth/expireOne",
            "Expires a specific credential for a user and credential type",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID"),
                Map.of("name", "credential_type_code", "type", "string", "required", true, "description", "The credential type code"),
                Map.of("name", "security_principal", "type", "string", "required", true, "description", "The security principal to expire")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    String credentialTypeCode = paramArray[1].toString();
                    String securityPrincipal = paramArray[2].toString();
                    boolean success = authenticationRegistry.expireOne(userId, credentialTypeCode, securityPrincipal);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("Auth error: " + e.getMessage(), e);
                }
            });

        registerMethod("auth/expireAll",
            "Expires all valid credentials for a user and credential type",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID"),
                Map.of("name", "credential_type_code", "type", "string", "required", true, "description", "The credential type code")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    String credentialTypeCode = paramArray[1].toString();
                    boolean success = authenticationRegistry.expireAll(userId, credentialTypeCode);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("Auth error: " + e.getMessage(), e);
                }
            });

        registerMethod("auth/verify",
            "Verifies a credential and returns the user ID if valid and not expired",
            List.of(
                Map.of("name", "credential_type_code", "type", "string", "required", true, "description", "The credential type code"),
                Map.of("name", "security_principal", "type", "string", "required", true, "description", "The security principal"),
                Map.of("name", "security_credentials", "type", "string", "required", false, "description", "The security credentials to verify (null for credential types without credentials)")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    String credentialTypeCode = paramArray[0].toString();
                    String securityPrincipal = paramArray[1].toString();
                    String securityCredentials =
                        paramArray.length > 2 && paramArray[2] != null ? paramArray[2].toString() : null;
                    Long userId =
                        authenticationRegistry.verify(credentialTypeCode, securityPrincipal, securityCredentials);
                    return userId != null ? Map.of("user_id", userId) : null;
                } catch (Exception e) {
                    throw new RuntimeException("Auth error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/createRole",
            "Creates a new role in the system",
            List.of(
                Map.of("name", "role", "type", "object", "required", true, "description", "Role object containing code and name")
            ),
            params -> {
                try {
                    Role role = objectMapper.convertValue(params[0], Role.class);
                    Long id = authorizationRegistry.create(role);
                    return Map.of("id", id);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/updateRole",
            "Updates an existing role's information",
            List.of(
                Map.of("name", "role", "type", "object", "required", true, "description", "Role object containing id, code, and name")
            ),
            params -> {
                try {
                    Role role = objectMapper.convertValue(params[0], Role.class);
                    boolean success = authorizationRegistry.update(role);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/authorize",
            "Authorizes a user to have a specific role for a specified time period",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID"),
                Map.of("name", "role_id", "type", "number", "required", true, "description", "The role's ID"),
                Map.of("name", "valid_from", "type", "string/number", "required", true, "description", "Timestamp when the authorization becomes valid (ISO format or epoch milliseconds)"),
                Map.of("name", "valid_until", "type", "string/number", "required", true, "description", "Timestamp when the authorization expires (ISO format or epoch milliseconds)")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    Long roleId = ((Number) paramArray[1]).longValue();
                    Timestamp validFrom = parseTimestamp(paramArray[2]);
                    Timestamp validUntil = parseTimestamp(paramArray[3]);
                    Long id = authorizationRegistry.authorize(userId, roleId, validFrom, validUntil);
                    return Map.of("id", id);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/deauthorize",
            "Removes all authorizations for a specific user and role",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID"),
                Map.of("name", "role_id", "type", "number", "required", true, "description", "The role's ID")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    Long roleId = ((Number) paramArray[1]).longValue();
                    boolean success = authorizationRegistry.deauthorize(userId, roleId);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/isUserInRole",
            "Checks if a user currently has a specific role",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID"),
                Map.of("name", "role_id", "type", "number", "required", true, "description", "The role's ID")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    Long roleId = ((Number) paramArray[1]).longValue();
                    boolean result = authorizationRegistry.isUserInRole(userId, roleId);
                    return Map.of("result", result);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/isUserInAnyRoles",
            "Checks if a user currently has any of the specified roles",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID"),
                Map.of("name", "role_ids", "type", "array", "required", true, "description", "An array of role IDs to check")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    List<?> roleIdsList = (List<?>) paramArray[1];
                    List<Long> roleIds = new ArrayList<>();
                    for (Object roleId : roleIdsList) {
                        roleIds.add(((Number) roleId).longValue());
                    }
                    boolean result = authorizationRegistry.isUserInAnyRoles(userId, roleIds);
                    return Map.of("result", result);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/isUserInAllRoles",
            "Checks if a user currently has all of the specified roles",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID"),
                Map.of("name", "role_ids", "type", "array", "required", true, "description", "An array of role IDs to check")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    List<?> roleIdsList = (List<?>) paramArray[1];
                    List<Long> roleIds = new ArrayList<>();
                    for (Object roleId : roleIdsList) {
                        roleIds.add(((Number) roleId).longValue());
                    }
                    boolean result = authorizationRegistry.isUserInAllRoles(userId, roleIds);
                    return Map.of("result", result);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/addFunctionToRole",
            "Links a function to a role",
            List.of(
                Map.of("name", "role_id", "type", "number", "required", true, "description", "The role's ID"),
                Map.of("name", "function_id", "type", "number", "required", true, "description", "The function's ID")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long roleId = ((Number) paramArray[0]).longValue();
                    Long functionId = ((Number) paramArray[1]).longValue();
                    Long id = authorizationRegistry.addFunctionToRole(roleId, functionId);
                    return Map.of("id", id);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/removeFunctionFromRole",
            "Unlinks a function from a role",
            List.of(
                Map.of("name", "role_id", "type", "number", "required", true, "description", "The role's ID"),
                Map.of("name", "function_id", "type", "number", "required", true, "description", "The function's ID")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long roleId = ((Number) paramArray[0]).longValue();
                    Long functionId = ((Number) paramArray[1]).longValue();
                    boolean success = authorizationRegistry.removeFunctionFromRole(roleId, functionId);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/getFunctionsByRole",
            "Retrieves all functions linked to a role",
            List.of(
                Map.of("name", "role_id", "type", "number", "required", true, "description", "The role's ID")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long roleId = ((Number) paramArray[0]).longValue();
                    return authorizationRegistry.getFunctionsByRole(roleId);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("authorization/getMenuTree",
            "Retrieves the menu tree for a user based on their authorized functions",
            List.of(
                Map.of("name", "user_id", "type", "number", "required", true, "description", "The user's ID")
            ),
            params -> {
                try {
                    Object[] paramArray = (Object[]) params;
                    Long userId = ((Number) paramArray[0]).longValue();
                    return authorizationRegistry.getMenuTree(userId);
                } catch (Exception e) {
                    throw new RuntimeException("Authorization error: " + e.getMessage(), e);
                }
            });

        registerMethod("generic/createFunction",
            "Creates a new function in the system",
            List.of(
                Map.of("name", "function", "type", "object", "required", true, "description", "Function object containing name and optionally url")
            ),
            params -> {
                try {
                    Function function = objectMapper.convertValue(params[0], Function.class);
                    Long id = genericRegistry.createFunction(function);
                    return Map.of("id", id);
                } catch (Exception e) {
                    throw new RuntimeException("Generic error: " + e.getMessage(), e);
                }
            });

        registerMethod("generic/updateFunction",
            "Updates an existing function",
            List.of(
                Map.of("name", "function", "type", "object", "required", true, "description", "Function object containing id, name, and optionally url")
            ),
            params -> {
                try {
                    Function function = objectMapper.convertValue(params[0], Function.class);
                    boolean success = genericRegistry.updateFunction(function);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("Generic error: " + e.getMessage(), e);
                }
            });

        registerMethod("generic/createMenu",
            "Creates a new menu entry",
            List.of(
                Map.of("name", "menu", "type", "object", "required", true, "description", "Menu object containing function_id and optionally parent_id")
            ),
            params -> {
                try {
                    Menu menu = objectMapper.convertValue(params[0], Menu.class);
                    Long id = genericRegistry.createMenu(menu);
                    return Map.of("id", id);
                } catch (Exception e) {
                    throw new RuntimeException("Generic error: " + e.getMessage(), e);
                }
            });

        registerMethod("generic/updateMenu",
            "Updates an existing menu entry",
            List.of(
                Map.of("name", "menu", "type", "object", "required", true, "description", "Menu object containing id, function_id, and optionally parent_id")
            ),
            params -> {
                try {
                    Menu menu = objectMapper.convertValue(params[0], Menu.class);
                    boolean success = genericRegistry.updateMenu(menu);
                    return Map.of("success", success);
                } catch (Exception e) {
                    throw new RuntimeException("Generic error: " + e.getMessage(), e);
                }
            });

        registerMethod("generic/getMenuTree",
            "Retrieves the complete menu tree",
            List.of(),
            params -> {
                try {
                    return genericRegistry.getMenuTree();
                } catch (Exception e) {
                    throw new RuntimeException("Generic error: " + e.getMessage(), e);
                }
            });
    }

    private Timestamp parseTimestamp(Object obj) {
        if (obj == null) {
            return new Timestamp(System.currentTimeMillis());
        }
        if (obj instanceof Timestamp) {
            return (Timestamp) obj;
        }
        if (obj instanceof String) {
            try {
                return Timestamp.valueOf((String) obj);
            } catch (IllegalArgumentException e) {
                try {
                    return new Timestamp(Long.parseLong((String) obj));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Invalid timestamp format: " + obj);
                }
            }
        }
        if (obj instanceof Number) {
            return new Timestamp(((Number) obj).longValue());
        }
        throw new IllegalArgumentException("Invalid timestamp type: " + obj.getClass().getName());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            byte[] data = new byte[msg.readableBytes()];
            msg.readBytes(data);

            MsgPackRequest request = unpackRequest(data);
            MsgPackResponse response = handleRequest(request);
            byte[] responseData = packResponse(response);

            ByteBuf responseBuf = ctx.alloc().buffer(responseData.length);
            responseBuf.writeBytes(responseData);
            ctx.writeAndFlush(responseBuf).addListener(future -> ctx.close());

        } catch (Exception e) {
            e.printStackTrace();
            ctx.close();
        }
    }

    private MsgPackRequest unpackRequest(byte[] data) throws Exception {
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(new ByteArrayInputStream(data));
        try {
            int arraySize = unpacker.unpackArrayHeader();
            String jsonrpc = unpacker.unpackString();
            String method = unpacker.unpackString();
            Object params = unpackValue(unpacker);
            int id = unpacker.unpackInt();

            Object[] paramsArray;
            if (params instanceof Object[]) {
                paramsArray = (Object[]) params;
            } else {
                paramsArray = new Object[]{};
            }

            return new MsgPackRequest(jsonrpc, method, paramsArray, id);
        } finally {
            unpacker.close();
        }
    }

    private Object unpackValue(MessageUnpacker unpacker) throws Exception {
        ValueType valueType = unpacker.getNextFormat().getValueType();
        if (valueType == ValueType.NIL) {
            unpacker.unpackNil();
            return null;
        } else if (valueType == ValueType.BOOLEAN) {
            return unpacker.unpackBoolean();
        } else if (valueType == ValueType.INTEGER) {
            return unpacker.unpackLong();
        } else if (valueType == ValueType.FLOAT) {
            return unpacker.unpackDouble();
        } else if (valueType == ValueType.STRING) {
            return unpacker.unpackString();
        } else if (valueType == ValueType.BINARY) {
            int binSize = unpacker.unpackBinaryHeader();
            byte[] binData = new byte[binSize];
            unpacker.readPayload(binData);
            return binData;
        } else if (valueType == ValueType.ARRAY) {
            int arraySize = unpacker.unpackArrayHeader();
            Object[] array = new Object[arraySize];
            for (int i = 0; i < arraySize; i++) {
                array[i] = unpackValue(unpacker);
            }
            return array;
        } else if (valueType == ValueType.MAP) {
            int mapSize = unpacker.unpackMapHeader();
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                String key = unpacker.unpackString();
                Object value = unpackValue(unpacker);
                map.put(key, value);
            }
            return map;
        } else {
            throw new IllegalArgumentException("Unsupported MessagePack format: " + valueType);
        }
    }

    private byte[] packResponse(MsgPackResponse response) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MessagePacker packer = MessagePack.newDefaultPacker(out);
        try {
            packer.packArrayHeader(4);
            packer.packString(response.getJsonrpc());
            packValue(packer, response.getResult());
            if (response.getError() != null) {
                packer.packArrayHeader(2);
                packer.packInt(response.getError().getCode());
                packer.packString(response.getError().getMessage());
            } else {
                packer.packNil();
            }
            packer.packInt(response.getId());
            packer.flush();
            return out.toByteArray();
        } finally {
            packer.close();
        }
    }

    private void packValue(MessagePacker packer, Object value) throws Exception {
        if (value == null) {
            packer.packNil();
        } else if (value instanceof Boolean) {
            packer.packBoolean((Boolean) value);
        } else if (value instanceof Integer) {
            packer.packInt((Integer) value);
        } else if (value instanceof Long) {
            packer.packLong((Long) value);
        } else if (value instanceof Float) {
            packer.packFloat((Float) value);
        } else if (value instanceof Double) {
            packer.packDouble((Double) value);
        } else if (value instanceof String) {
            packer.packString((String) value);
        } else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            packer.packArrayHeader(array.length);
            for (Object item : array) {
                packValue(packer, item);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            packer.packArrayHeader(list.size());
            for (Object item : list) {
                packValue(packer, item);
            }
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            packer.packMapHeader(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                packer.packString(entry.getKey().toString());
                packValue(packer, entry.getValue());
            }
        } else {
            try {
                Map<?, ?> map = objectMapper.convertValue(value, Map.class);
                packer.packMapHeader(map.size());
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    packer.packString(entry.getKey().toString());
                    packValue(packer, entry.getValue());
                }
            } catch (Exception e) {
                packer.packString(value.toString());
            }
        }
    }

    private MsgPackResponse handleRequest(MsgPackRequest request) {
        java.util.function.Function<Object[], Object> method = methods.get(request.getMethod());
        if (method == null) {
            return MsgPackResponse.error(request.getId(), -32601, "Method not found");
        }

        try {
            Object result = method.apply(request.getParams());
            return MsgPackResponse.success(request.getId(), result);
        } catch (Exception e) {
            return MsgPackResponse.error(request.getId(), -32603, "Internal error: " + e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public static class MsgPackRequest {
        private final String jsonrpc;
        private final String method;
        private final Object[] params;
        private final int id;

        public MsgPackRequest(String jsonrpc, String method, Object[] params, int id) {
            this.jsonrpc = jsonrpc;
            this.method = method;
            this.params = params;
            this.id = id;
        }

        public String getJsonrpc() {
            return jsonrpc;
        }

        public String getMethod() {
            return method;
        }

        public Object[] getParams() {
            return params;
        }

        public int getId() {
            return id;
        }
    }

    public static class MsgPackResponse {
        private final String jsonrpc = "2.0";
        private final Object result;
        private final MsgPackError error;
        private final int id;

        public MsgPackResponse(Object result, MsgPackError error, int id) {
            this.result = result;
            this.error = error;
            this.id = id;
        }

        public static MsgPackResponse success(int id, Object result) {
            return new MsgPackResponse(result, null, id);
        }

        public static MsgPackResponse error(int id, int code, String message) {
            return new MsgPackResponse(null, new MsgPackError(code, message), id);
        }

        public String getJsonrpc() {
            return jsonrpc;
        }

        public Object getResult() {
            return result;
        }

        public MsgPackError getError() {
            return error;
        }

        public int getId() {
            return id;
        }
    }

    public static class MsgPackError {
        private final int code;
        private final String message;

        public MsgPackError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
