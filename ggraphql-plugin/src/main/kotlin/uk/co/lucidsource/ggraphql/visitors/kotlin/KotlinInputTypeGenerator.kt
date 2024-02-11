package uk.co.lucidsource.ggraphql.visitors.kotlin

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import graphql.language.InputObjectTypeDefinition
import uk.co.lucidsource.ggraphql.api.filtering.FilterGroupOperator
import uk.co.lucidsource.ggraphql.api.filtering.FilterOperator
import uk.co.lucidsource.ggraphql.api.filtering.ast.Expression
import uk.co.lucidsource.ggraphql.api.filtering.ast.FilterGroupCompoundExpression
import uk.co.lucidsource.ggraphql.api.filtering.expression.FilterExpression
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getAppliedFilterFieldNameForTypeAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getAppliedFilterForTypeAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isFilterForTypeAspectApplied
import uk.co.lucidsource.ggraphql.util.GraphQLTypeUtil
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitor
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

class KotlinInputTypeGenerator(
    private val typeResolver: KotlinTypeResolver
) : SDLNodeVisitor {
    override fun visitInputType(
        inputObjectTypeDefinition: InputObjectTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        val properties = inputObjectTypeDefinition.inputValueDefinitions
            .map {
                PropertySpec.builder(it.name, typeResolver.getKotlinTypeForModel(it.type))
                    .initializer(CodeBlock.of(it.name)).build()
            }

        val parameters = inputObjectTypeDefinition.inputValueDefinitions
            .map {
                ParameterSpec.builder(it.name, typeResolver.getKotlinTypeForModel(it.type))
                    .addAnnotation(AnnotationSpec.builder(JsonProperty::class).addMember("%S", it.name).build())
                    .defaultValue(if (GraphQLTypeUtil.isNullType(it.type)) CodeBlock.of("null") else null)
                    .build()
            }

        val inputTypeBuilder = TypeSpec.classBuilder(inputObjectTypeDefinition.name)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameters(parameters)
                    .addAnnotation(JsonCreator::class)
                    .build()
            )
            .addProperties(properties)

        if (inputObjectTypeDefinition.isFilterForTypeAspectApplied()) {
            applyFilterAstGenerate(inputTypeBuilder, inputObjectTypeDefinition)
        }

        context.typeSpecs += FileSpec.get(typeResolver.getModelPackageName(), inputTypeBuilder.build())
    }

    private fun applyFilterAstGenerate(inputTypeBuilder: TypeSpec.Builder, inputObject: InputObjectTypeDefinition) {
        val filterAspectType = inputObject.getAppliedFilterForTypeAspect()
        if (filterAspectType == GraphQLTypeAspects.FilterAspectType.EXPRESSION) {
            inputTypeBuilder.addSuperinterface(FilterExpression::class.java)
            inputTypeBuilder.addFunction(buildAstMethodForExpression())
        } else if (filterAspectType == GraphQLTypeAspects.FilterAspectType.OBJECT_CRITERIA) {
            inputTypeBuilder.addSuperinterface(FilterExpression::class.java)
            inputTypeBuilder.addFunction(buildAstMethodForObjectCriteria(inputObject))
        } else if (filterAspectType == GraphQLTypeAspects.FilterAspectType.FIELD_CRITERIA) {
            inputTypeBuilder.addSuperinterface(FilterExpression::class.java)
            inputTypeBuilder.addFunction(buildAstMethodForFieldCriteria(inputObject))
        }
    }

    private fun buildAstMethodForExpression(): FunSpec {
        val funSpecBuilder = FunSpec.builder("ast")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Expression::class.java)

        val codeBlocks = FilterGroupOperator.entries.map {
            CodeBlock.of(
                "val %L = %T(%T.%L, %L?.map { it.ast() }?.filter { it != Expression.EMPTY_EXPRESSION } ?: emptyList())\n",
                it.fieldName,
                FilterGroupCompoundExpression::class.java,
                FilterGroupOperator::class.java,
                it.name,
                it.fieldName
            )
        } + CodeBlock.of(
            """
              val filters = listOf(%L).filter{ it.expressions.isNotEmpty() }
              if(filters.isEmpty()) {
                  return Expression.EMPTY_EXPRESSION
              }
              return %T(%T.ALL, filters)
            """,
            FilterGroupOperator.entries.joinToString(",") { it.fieldName },
            FilterGroupCompoundExpression::class.java,
            FilterGroupOperator::class.java,
        )

        codeBlocks.forEach {
            funSpecBuilder.addCode(it)
        }

        return funSpecBuilder.build()
    }

    private fun buildAstMethodForObjectCriteria(inputObject: InputObjectTypeDefinition): FunSpec {
        val codeBlock = """
                val fieldExpressions = listOf(%L).filterNotNull().map { it.ast() }.filter { it != Expression.EMPTY_EXPRESSION }
                if(fieldExpressions.isEmpty()) {
                    return Expression.EMPTY_EXPRESSION
                }
                return %T(%T.ALL, fieldExpressions)
            """.trimIndent()

        val funSpecBuilder = FunSpec.builder("ast")
            .addModifiers(KModifier.OVERRIDE)
            .addCode(
                CodeBlock.of(
                    codeBlock,
                    inputObject.inputValueDefinitions.map { it.name }.joinToString(","),
                    FilterGroupCompoundExpression::class.java,
                    FilterGroupOperator::class.java
                )
            )
            .returns(Expression::class.java)

        return funSpecBuilder.build()
    }

    private fun buildAstMethodForFieldCriteria(inputObject: InputObjectTypeDefinition): FunSpec {
        val funSpecBuilder = FunSpec.builder("ast")
            .addModifiers(KModifier.OVERRIDE)
            .returns(Expression::class.java)

        val codeBlocks = inputObject.inputValueDefinitions
            .map {
                val fieldName = inputObject.getAppliedFilterFieldNameForTypeAspect()
                    ?: throw IllegalArgumentException("No filter field name specified for: ${it.name}")

                val filterOperator = FilterOperator.fromFieldName(it.name)
                    ?: throw IllegalArgumentException("Unexpected filter operator for: ${it.name}")

                CodeBlock.of(
                    """
                    if (`%L` != null) {
                        return %T(field = %S, operator = %T.%L, %L = `%L`)
                    }
            """,
                    it.name,
                    filterOperator.expressionType,
                    fieldName,
                    FilterOperator::class.java,
                    filterOperator.name,
                    if (filterOperator.isCompound) "values" else "value",
                    it.name,
                )
            } + CodeBlock.of("return %T.EMPTY_EXPRESSION", Expression::class.java)

        codeBlocks.forEach {
            funSpecBuilder.addCode(it)
        }

        return funSpecBuilder.build()
    }
}