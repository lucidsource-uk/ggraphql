package uk.co.lucidsource.ggraphql.api.filtering

import uk.co.lucidsource.ggraphql.api.filtering.ast.Expression
import uk.co.lucidsource.ggraphql.api.filtering.ast.FilterCompoundExpression
import uk.co.lucidsource.ggraphql.api.filtering.ast.FilterUnaryExpression

enum class FilterOperator(val fieldName: String, val isCompound: Boolean) {
    LIKE("like", false),
    EQ("eq", false),
    GT("gt", false),
    GT_EQ("gtEq", false),
    LT("lt", false),
    LT_EQ("ltEq", false),
    IN("in", true);

    companion object {
        fun fromFieldName(fieldName: String): FilterOperator? {
            return entries.firstOrNull { it.fieldName == fieldName }
        }
    }

    val expressionType: Class<out Expression> =
        if (isCompound) FilterCompoundExpression::class.java else FilterUnaryExpression::class.java
}
