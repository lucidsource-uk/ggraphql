package uk.co.lucidsource.ggraphql.api.filtering.ast

interface ExpressionVisitor<T> {
    fun visit(expression: Expression): T
}
