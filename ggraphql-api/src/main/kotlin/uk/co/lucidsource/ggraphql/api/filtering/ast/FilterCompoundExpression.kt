package uk.co.lucidsource.ggraphql.api.filtering.ast

import uk.co.lucidsource.ggraphql.api.filtering.FilterOperator

class FilterCompoundExpression<T>(
    val field: String,
    val operator: FilterOperator,
    val values: List<T>
) : Expression {
    override fun <V> accept(visitor: ExpressionVisitor<V>): V {
        return visitor.visit(this)
    }
}