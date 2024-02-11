package uk.co.lucidsource.ggraphql.api.filtering.ast

interface Expression {
    companion object {
        val EMPTY_EXPRESSION: Expression = object : Expression {
            override fun <T> accept(visitor: ExpressionVisitor<T>): T {
                return visitor.visit(this)
            }
        }
    }

    fun <T> accept(visitor: ExpressionVisitor<T>): T
}
