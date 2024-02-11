package uk.co.lucidsource.ggraphql.api.filtering.ast

import uk.co.lucidsource.ggraphql.api.filtering.FilterGroupOperator

class FilterGroupCompoundExpression(
    val operator: FilterGroupOperator,
    val expressions: List<Expression>
) : Expression {
    override fun <V> accept(visitor: ExpressionVisitor<V>): V {
        return visitor.visit(this)
    }
}
