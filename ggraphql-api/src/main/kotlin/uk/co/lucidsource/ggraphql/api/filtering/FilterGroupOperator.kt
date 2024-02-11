package uk.co.lucidsource.ggraphql.api.filtering

import uk.co.lucidsource.ggraphql.api.filtering.ast.Expression
import uk.co.lucidsource.ggraphql.api.filtering.ast.FilterGroupCompoundExpression

enum class FilterGroupOperator(val fieldName: String) {
    ALL("all"),
    NOT("not"),
    ANY("any");

    val expressionType: Class<out Expression> = FilterGroupCompoundExpression::class.java
}
