package uk.co.lucidsource.ggraphql.api.serde

interface Deserializer {
    fun <O> deserialize(input: Any, clazz: Class<O>): O
}