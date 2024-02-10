package uk.co.lucidsource.ggraphql.model

enum class FilterOperator(val fieldName: String, val isCompound: Boolean = false) {
    LIKE("like"), EQ("eq"), GT("gt"), GT_EQ("gtEq"), LT("lt"), LT_EQ("ltEq"), IN("in", true);
}