package co.uk.lucidsource.filtrate.api.expression

import co.uk.lucidsource.filtrate.api.ast.Expression

/**
 * This method is used to represent the filter expression in form of an Abstract Syntax Tree (AST)
 *
 * @return Expression that represents the AST of the filter expression
 */
interface FilterExpression {
    fun ast(): Expression
}
