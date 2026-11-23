package ch.orellbuehler.acolyte.contract

import ch.orellbuehler.acolyte.api.Endpoint
import ch.orellbuehler.acolyte.api.EndpointGroup
import ch.orellbuehler.acolyte.api.Need
import ch.orellbuehler.acolyte.api.NoContent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

internal class SpecChecker(
    specFile: File,
) {
    val spec: JsonObject = Json.parseToJsonElement(specFile.readText()).jsonObject
    private val paths = spec.getValue("paths").jsonObject
    private val schemas = spec["components"]?.jsonObject?.get("schemas")?.jsonObject ?: JsonObject(emptyMap())

    val groups: List<EndpointGroup> = EndpointGroup::class.sealedSubclasses.mapNotNull { it.objectInstance }

    val groupsWithoutObject: List<String> =
        EndpointGroup::class.sealedSubclasses.filter { it.objectInstance == null }.map { it.simpleName.orEmpty() }

    val endpoints: List<Endpoint<*, *>> = groups.flatMap { it.endpoints }

    val notes = mutableListOf<String>()

    fun operation(
        method: String,
        path: String,
    ): JsonObject? = paths[path]?.jsonObject?.get(method.lowercase())?.jsonObject

    fun expectedNeed(op: JsonObject): String {
        val security = op["security"]
        if (security is JsonArray && security.isEmpty()) return Need.PUBLIC.wire
        return op.string("x-holyarrs-permission").orEmpty()
    }

    fun check(endpoint: Endpoint<*, *>): List<String> {
        val problems = mutableListOf<String>()
        val key = endpoint.key
        if (isPlugin(endpoint.path)) problems += "$key: plugin routes are out of scope for the app"
        val op = operation(endpoint.method.value, endpoint.path)
        if (op == null) {
            val similar = similarPath(endpoint.path)
            problems += "$key: not in api/openapi.json" + (similar?.let { " (the spec has $it)" } ?: "")
            return problems
        }
        if (op["deprecated"]?.jsonPrimitive?.booleanOrNull == true) problems += "$key: deprecated in the spec"
        val need = expectedNeed(op)
        if (endpoint.need.wire != need) problems += "$key: need is ${endpoint.need.wire}, the spec says $need"
        val part = op.string("x-holyarrs-part").orEmpty()
        if (endpoint.part.wire != part) problems += "$key: part is ${endpoint.part.wire}, the spec says $part"
        problems += checkParameters(endpoint, op)
        problems += checkRequest(endpoint, op)
        problems += checkResponse(endpoint, op)
        return problems
    }

    fun responseSchema(op: JsonObject): JsonObject? {
        val responses = op["responses"]?.jsonObject ?: return null
        val success = responses.entries.filter { it.key.startsWith("2") }.sortedBy { it.key }
        return success.firstNotNullOfOrNull { jsonSchema(it.value.jsonObject) }
    }

    fun isTyped(schema: JsonObject?): Boolean {
        val resolved = schema?.let { resolve(it) } ?: return false
        return resolved.keys.any { it in setOf("properties", "items", "additionalProperties", "enum", "oneOf", "anyOf", "allOf") } ||
            types(resolved).any { it != "object" }
    }

    fun requiredQuery(op: JsonObject): List<String> =
        parameters(op).filter { it.string("in") == "query" && it["required"]?.jsonPrimitive?.booleanOrNull == true }.map {
            it.string("name").orEmpty()
        }

    private fun checkParameters(
        endpoint: Endpoint<*, *>,
        op: JsonObject,
    ): List<String> {
        val problems = mutableListOf<String>()
        val params = parameters(op)
        val specPath = params.filter { it.string("in") == "path" }.mapNotNull { it.string("name") }.toSet()
        val specQuery = params.filter { it.string("in") == "query" }.mapNotNull { it.string("name") }.toSet()
        if (specPath != endpoint.pathParams.toSet()) {
            problems += "${endpoint.key}: path parameters ${endpoint.pathParams} do not match the spec's ${specPath.toList()}"
        }
        for (name in endpoint.query) {
            if (name !in specQuery) {
                problems += "${endpoint.key}: query parameter '$name' is not in the spec (it has ${specQuery.toList()})"
            }
        }
        for (name in requiredQuery(op)) {
            if (name !in endpoint.query) problems += "${endpoint.key}: the spec requires query parameter '$name'"
        }
        return problems
    }

    private fun checkRequest(
        endpoint: Endpoint<*, *>,
        op: JsonObject,
    ): List<String> {
        val body = op["requestBody"]?.jsonObject
        val schema = body?.let { jsonSchema(it) }
        val serializer = endpoint.body
        val required = body?.get("required")?.jsonPrimitive?.booleanOrNull == true
        if (serializer == null) {
            return if (required) listOf("${endpoint.key}: the spec requires a request body") else emptyList()
        }
        if (body == null) return listOf("${endpoint.key}: sends a body but the spec takes none")
        if (schema == null || !isTyped(schema)) {
            notes += "${endpoint.key}: request body is untyped in the spec"
            return emptyList()
        }
        val problems = mutableListOf<String>()
        compare(serializer.descriptor, schema, "${endpoint.key} body", Direction.REQUEST, problems)
        return problems
    }

    private fun checkResponse(
        endpoint: Endpoint<*, *>,
        op: JsonObject,
    ): List<String> {
        if (endpoint.response === NoContent) return emptyList()
        val responses = op["responses"]?.jsonObject ?: return listOf("${endpoint.key}: the spec lists no responses")
        val success = responses.keys.filter { it.startsWith("2") }
        if (success.isNotEmpty() && success.all { it == "204" }) {
            return listOf("${endpoint.key}: the spec answers 204 without a body; use NoContent")
        }
        val schema = responseSchema(op)
        if (schema == null || !isTyped(schema)) {
            notes += "${endpoint.key}: response is untyped in the spec; the live contract test covers it"
            return emptyList()
        }
        val problems = mutableListOf<String>()
        compare(endpoint.response.descriptor, schema, "${endpoint.key} response", Direction.RESPONSE, problems)
        return problems
    }

    enum class Direction { REQUEST, RESPONSE }

    private fun compare(
        descriptor: SerialDescriptor,
        rawSchema: JsonObject,
        where: String,
        direction: Direction,
        problems: MutableList<String>,
        depth: Int = 0,
    ) {
        if (depth > 12) return
        val schema = resolve(rawSchema)
        if (schema.containsKey("oneOf") || schema.containsKey("anyOf")) return
        val ours = kindOf(descriptor) ?: return
        val theirs = types(schema).filter { it != "null" }
        if (theirs.isEmpty()) return
        val compatible =
            when (ours) {
                "number" -> "number" in theirs || "integer" in theirs
                "integer" -> "integer" in theirs
                else -> ours in theirs
            }
        if (!compatible) {
            problems += "$where: the model has $ours, the spec has ${theirs.joinToString("|")}"
            return
        }
        when (ours) {
            "array" ->
                schema["items"]?.jsonObject?.let {
                    compare(
                        descriptor.getElementDescriptor(0),
                        it,
                        "$where[]",
                        direction,
                        problems,
                        depth + 1,
                    )
                }
            "string" -> if (descriptor.kind == SerialKind.ENUM) compareEnum(descriptor, schema, where, direction, problems)
            "object" ->
                if (descriptor.kind == StructureKind.MAP) {
                    (schema["additionalProperties"] as? JsonObject)?.let {
                        compare(descriptor.getElementDescriptor(1), it, "$where{}", direction, problems, depth + 1)
                    }
                } else {
                    compareObject(descriptor, schema, where, direction, problems, depth)
                }
        }
    }

    private fun compareObject(
        descriptor: SerialDescriptor,
        schema: JsonObject,
        where: String,
        direction: Direction,
        problems: MutableList<String>,
        depth: Int,
    ) {
        val properties = schema["properties"]?.jsonObject ?: return
        val required =
            schema["required"]
                ?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?.toSet()
                .orEmpty()
        val open = schema["additionalProperties"]?.let { it is JsonObject || (it as? JsonPrimitive)?.booleanOrNull == true } ?: false
        val names = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }.toSet()
        for (i in 0 until descriptor.elementsCount) {
            val name = descriptor.getElementName(i)
            val element = descriptor.getElementDescriptor(i)
            val optional = descriptor.isElementOptional(i) || element.isNullable
            val property = properties[name]?.jsonObject
            if (property == null) {
                when {
                    direction == Direction.RESPONSE && !optional ->
                        problems +=
                            "$where.$name: required by the model but not in the spec; give it a default"
                    direction == Direction.REQUEST && !open ->
                        problems += "$where.$name: sent by the model but not in the spec"
                    else -> notes += "$where.$name: not in the spec"
                }
                continue
            }
            if (direction == Direction.RESPONSE && !optional && name !in required) {
                problems += "$where.$name: required by the model but optional in the spec; give it a default"
            }
            compare(element, property, "$where.$name", direction, problems, depth + 1)
        }
        if (direction == Direction.REQUEST) {
            for (name in required - names) problems += "$where.$name: required by the spec but missing in the model"
            for (i in 0 until descriptor.elementsCount) {
                val name = descriptor.getElementName(i)
                if (name in required && descriptor.getElementDescriptor(i).isNullable) {
                    problems += "$where.$name: required by the spec but nullable in the model"
                }
            }
        }
    }

    private fun compareEnum(
        descriptor: SerialDescriptor,
        schema: JsonObject,
        where: String,
        direction: Direction,
        problems: MutableList<String>,
    ) {
        val allowed = schema["enum"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet() ?: return
        val ours = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }.toSet()
        val unknown = ours - allowed
        if (unknown.isNotEmpty() && direction == Direction.REQUEST) problems += "$where: values $unknown are not in the spec's $allowed"
        if (unknown.isNotEmpty() && direction == Direction.RESPONSE) notes += "$where: values $unknown never come from the spec"
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun kindOf(descriptor: SerialDescriptor): String? {
        if (descriptor.serialName.startsWith("kotlinx.serialization.json.Json")) return null
        return when (descriptor.kind) {
            PrimitiveKind.STRING, PrimitiveKind.CHAR, SerialKind.ENUM -> "string"
            PrimitiveKind.BOOLEAN -> "boolean"
            PrimitiveKind.INT, PrimitiveKind.LONG, PrimitiveKind.SHORT, PrimitiveKind.BYTE -> "integer"
            PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> "number"
            StructureKind.LIST -> "array"
            StructureKind.MAP, StructureKind.CLASS, StructureKind.OBJECT -> "object"
            is PolymorphicKind, SerialKind.CONTEXTUAL -> null
        }
    }

    private fun types(schema: JsonObject): List<String> =
        when (val type = schema["type"]) {
            is JsonPrimitive -> listOfNotNull(type.contentOrNull) + if (schema.boolean("nullable")) listOf("null") else emptyList()
            is JsonArray -> type.mapNotNull { it.jsonPrimitive.contentOrNull }
            else -> if (schema.containsKey("properties")) listOf("object") else emptyList()
        }

    fun resolve(schema: JsonObject): JsonObject {
        var current = schema
        repeat(10) {
            val ref = current.string("\$ref") ?: return@repeat
            current = schemas[ref.substringAfterLast('/')]?.jsonObject ?: return current
        }
        val allOf = current["allOf"]?.jsonArray ?: return current
        val properties = mutableMapOf<String, JsonElement>()
        val required = mutableListOf<JsonElement>()
        for (part in allOf) {
            val resolved = resolve(part.jsonObject)
            resolved["properties"]?.jsonObject?.let { properties.putAll(it) }
            resolved["required"]?.jsonArray?.let { required.addAll(it) }
        }
        return JsonObject(
            mapOf("type" to JsonPrimitive("object"), "properties" to JsonObject(properties), "required" to JsonArray(required)),
        )
    }

    private fun jsonSchema(container: JsonObject): JsonObject? =
        container["content"]?.jsonObject?.let { content ->
            (
                content["application/json"] ?: content.entries
                    .firstOrNull {
                        it.key.contains(
                            "json",
                        )
                    }?.value
            )?.jsonObject?.get("schema")?.jsonObject
        }

    private fun parameters(op: JsonObject): List<JsonObject> = op["parameters"]?.jsonArray?.map { resolve(it.jsonObject) }.orEmpty()

    private fun similarPath(path: String): String? {
        val shape = path.replace(Regex("\\{[^}]+}"), "{}")
        return paths.keys.firstOrNull { it.replace(Regex("\\{[^}]+}"), "{}") == shape }
    }

    private fun JsonObject.string(name: String): String? = (this[name] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.boolean(name: String): Boolean = (this[name] as? JsonPrimitive)?.booleanOrNull == true

    companion object {
        fun isPlugin(path: String): Boolean = path.startsWith("/api/v1/plugins") || path.startsWith("/api/v1/plugin-")
    }
}
