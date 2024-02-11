package uk.co.lucidsource

import org.junit.jupiter.api.Test
import uk.co.lucidsource.generated.models.CandidateFilter
import uk.co.lucidsource.generated.models.CandidateFilterCriteria
import uk.co.lucidsource.generated.models.CandidateIdFilterCriteria
import uk.co.lucidsource.generated.models.CandidateLastnameFilterCriteria
import uk.co.lucidsource.generated.models.CandidateTagsFilterCriteria
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
                CandidateFilterCriteria()
            )
        ).ast()

        assertEquals(Expression.EMPTY_EXPRESSION, ast)
    }

    @Test
    fun testFilterObjectEmptyFieldExpression() {
        val ast = CandidateFilter(
            any = listOf(
                CandidateFilterCriteria(
                    id = CandidateIdFilterCriteria()
                )
            )
        ).ast()

        assertEquals(Expression.EMPTY_EXPRESSION, ast)
    }

    @Test
    fun testSimplyQueryBuilder() {
        val ast = CandidateFilter(
            any = listOf(
                CandidateFilterCriteria(
                    id = CandidateIdFilterCriteria(eq = "test")
                )
            )
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
                ),
                CandidateFilterCriteria(
                    id = CandidateIdFilterCriteria(eq = "test2"),
                    tags = CandidateTagsFilterCriteria(`in` = listOf("tag1", "tag2"))
                )
            )
        ).ast()

        val parsed = ast.accept(SimpleVisitor())

        assertEquals(
            "ALL(ANY(ALL((id EQ test1), (lastname LIKE dogs.*)), ALL((id EQ test2), (tags IN [tag1, tag2]))))",
            parsed
        )
    }

    @Test
    fun testQueryMultipleFieldCriteria() {
        val ast = CandidateFilter(
            any = listOf(
                CandidateFilterCriteria(
                    id = CandidateIdFilterCriteria(eq = "test1"),
                    lastname = CandidateLastnameFilterCriteria(like = "dogs.*", eq = "test"),
                )
            )
        ).ast()

        val parsed = ast.accept(SimpleVisitor())

        assertEquals(
            "ALL(ANY(ALL((id EQ test1), (lastname LIKE dogs.*))))",
            parsed,
            "Only one filter criteria per field is supported"
        )
    }
}