package uk.co.lucidsource.ggraphql.api.filtering.expression

import uk.co.lucidsource.ggraphql.api.filtering.ast.Expression

/**
 * This method is used to represent the filter expression in form of an Abstract Syntax Tree (AST)
 *
 * @return Expression that represents the AST of the filter expression
 */
interface FilterExpression {
    fun ast(): Expression
}
