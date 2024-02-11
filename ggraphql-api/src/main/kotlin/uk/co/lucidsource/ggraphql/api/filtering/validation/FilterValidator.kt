package uk.co.lucidsource.ggraphql.api.filtering.validation

interface FilterValidator {
    fun validate(): String?
}