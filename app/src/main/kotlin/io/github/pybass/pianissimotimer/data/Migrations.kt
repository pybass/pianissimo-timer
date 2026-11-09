package io.github.pybass.pianissimotimer.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// A migration step: the document of one version as the document of the next. A step works on the JSON tree only,
// never on the Kotlin classes: they describe the current version and will move on, the step must keep working.
internal typealias Migration = (JsonObject) -> JsonObject

// MIGRATIONS[v - 1] brings a document of version v to version v + 1 (docs/data.md "Changing the format"). The runner
// sets `version`, so a step changes only the sections it needs to. No step touches `state`: it is dropped, with its
// bells, on any version change (docs/spec.md 6.3).
internal val MIGRATIONS: List<Migration> = listOf()

// `tree`, a document of version `from`, as a document of version `to`: each step in turn, then its version.
internal fun migrate(tree: JsonObject, from: Int, to: Int = FORMAT_VERSION, steps: List<Migration> = MIGRATIONS) =
    (from until to).fold(tree) { t, v -> JsonObject(steps[v - 1](t) + ("version" to JsonPrimitive(v + 1))) }
