package co.uk.lucidsource.filtrate.api.ast

import co.uk.lucidsource.filtrate.api.FilterOperator

class FilterUnaryExpression<T>(
    val field: String,
    val operator: FilterOperator,
    val value: T
) : Expression {
    override fun <V> accept(visitor: ExpressionVisitor<V>): V {
        return visitor.visit(this)
    }
}