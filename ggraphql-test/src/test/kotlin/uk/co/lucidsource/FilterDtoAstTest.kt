package uk.co.lucidsource

import org.junit.jupiter.api.Test
import uk.co.lucidsource.generated.CandidateFilter
import uk.co.lucidsource.generated.CandidateFilterCriteria
import uk.co.lucidsource.generated.CandidateIdFilterCriteria
import uk.co.lucidsource.generated.CandidateLastnameFilterCriteria
import uk.co.lucidsource.generated.CandidateTagsFilterCriteria
import uk.co.lucidsource.ggraphql.api.filtering.ast.Expression
import uk.co.lucidsource.ggraphql.api.filtering.ast.ExpressionVisitor
import uk.co.lucidsource.ggraphql.api.filtering.ast.FilterCompoundExpression
import uk.co.lucidsource.ggraphql.api.filtering.ast.FilterGroupCompoundExpression
import uk.co.lucidsource.ggraphql.api.filtering.ast.FilterUnaryExpression
import kotlin.test.assertEquals

class FilterDtoAstTest {
    class SimpleVisitor : ExpressionVisitor<String> {
        override fun visit(expression: Expression): String {
            when (expression) {
                is FilterGroupCompoundExpression -> {
                    return expression.operator.name + "(" + expression.expressions.map { it.accept(this) }
                        .joinToString(", ") + ")"
                }

                is FilterCompoundExpression<*> -> {
                    return "(" + expression.field + " " + expression.operator.name + " " + expression.values.toString() + ")"
                }

                is FilterUnaryExpression<*> -> {
                    return "(" + expression.field + " " + expression.operator.name + " " + expression.value.toString() + ")"
                }

                else -> throw IllegalArgumentException("Unknown expression type ${expression}")
            }
        }
    }

    @Test
    fun testFilterEmptyExpression() {
        val ast = CandidateFilter(any = listOf(), not = null, all = null).ast()
        assertEquals(Expression.EMPTY_EXPRESSION, ast)
    }

    @Test
    fun testFilterObjectEmptyExpression() {
        val ast = CandidateFilter(
            any = listOf(
                CandidateFilterCriteria(
                    id = null,
                    lastname = null,
                    tags = null
                )
            ), not = null, all = null
        ).ast()

        assertEquals(Expression.EMPTY_EXPRESSION, ast)
    }

    @Test
    fun testFilterObjectEmptyFieldExpression() {
        val ast = CandidateFilter(
            any = listOf(
                CandidateFilterCriteria(
                    id = CandidateIdFilterCriteria(eq = null),
                    lastname = null,
                    tags = null
                )
            ), not = null, all = null
        ).ast()

        assertEquals(Expression.EMPTY_EXPRESSION, ast)
    }

    @Test
    fun testSimplyQueryBuilder() {
        val ast = CandidateFilter(
            any = listOf(
                CandidateFilterCriteria(
                    id = CandidateIdFilterCriteria(eq = "test"),
                    lastname = null,
                    tags = null
                )
            ), not = null, all = null
        ).ast()

        val parsed = ast.accept(SimpleVisitor())

        assertEquals("ALL(ANY(ALL((id EQ test))))", parsed)
    }

    @Test
    fun testComplexQueryBuilder() {
        val ast = CandidateFilter(
            any = listOf(
                CandidateFilterCriteria(
                    id = CandidateIdFilterCriteria(eq = "test1"),
                    lastname = CandidateLastnameFilterCriteria(like = "dogs.*"),
                    tags = null
                ),
                CandidateFilterCriteria(
                    id = CandidateIdFilterCriteria(eq = "test2"),
                    lastname = null,
                    tags = CandidateTagsFilterCriteria(`in` = listOf("tag1", "tag2"))
                )
            ), not = null, all = null
        ).ast()

        val parsed = ast.accept(SimpleVisitor())

        assertEquals(
            "ALL(ANY(ALL((id EQ test1), (lastname LIKE dogs.*)), ALL((id EQ test2), (tags IN [tag1, tag2]))))",
            parsed
        )
    }
}