package uk.co.lucidsource.ggraphql.transformers.schema

import graphql.language.ArrayValue
import graphql.language.EnumValue
import graphql.language.FieldDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InputValueDefinition
import graphql.language.ListType
import graphql.language.NonNullType
import graphql.language.ObjectTypeDefinition
import graphql.language.TypeName
import uk.co.lucidsource.ggraphql.api.filtering.FilterGroupOperator
import uk.co.lucidsource.ggraphql.api.filtering.FilterOperator
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformer
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyFilterFieldNameForTypeAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyFilterForTypeAspect
import uk.co.lucidsource.ggraphql.util.GraphQLTypeNameResolver
import uk.co.lucidsource.ggraphql.util.GraphQLTypeUtil

/**
 * Takes a GraphQL schema object types and expands the use of 'filter' annotations into
 * input types to build a filter query.
 */
class FilterDirectiveTransformer : SDLNodeTransformer {
    companion object {
        const val FILTER_DIRECTIVE = "filter"
        const val FILTER_DIRECTIVE_OPERATORS = "operators"
    }

    override fun transformObjectType(
        objectTypeDefinition: ObjectTypeDefinition, context: SDLNodeTransformerContext
    ): ObjectTypeDefinition {
        if (!shouldProcess(objectTypeDefinition)) {
            return objectTypeDefinition
        }

        // Generates filter criteria for each field in the form:
        // { operator: FieldType, otherOperator: FieldType }
        val filterFieldInputTypes = getFilterOperators(objectTypeDefinition)
            .map { fieldOperators ->
                val fieldName = fieldOperators.key.name

                fieldName to InputObjectTypeDefinition.newInputObjectDefinition()
                    .name(
                        GraphQLTypeNameResolver.getFilterFieldCriteriaTypeDefName(
                            objectTypeDefinition, fieldOperators.key
                        )
                    ).inputValueDefinitions(fieldOperators.value.map {
                        InputValueDefinition.newInputValueDefinition().name(it.fieldName).type(
                            if (it.isCompound) ListType(GraphQLTypeUtil.unwrapType(fieldOperators.key.type))
                            else GraphQLTypeUtil.unwrapType(fieldOperators.key.type)
                        ).build()
                    })
                    .applyFilterForTypeAspect(GraphQLTypeAspects.FilterAspectType.FIELD_CRITERIA)
                    .applyFilterFieldNameForTypeAspect(fieldName)
                    .build()
            }.toMap()

        // Generates filter criteria for all fields in the form
        // { field: FieldFilterCriteria, otherField: OtherFieldFilterCriteria }
        val filterCriteriaObjectTypeDef = InputObjectTypeDefinition.newInputObjectDefinition()
            .name(GraphQLTypeNameResolver.getFilterCriteriaTypeDefName(objectTypeDefinition))
            .inputValueDefinitions(filterFieldInputTypes.map {
                InputValueDefinition.newInputValueDefinition().name(it.key).type(TypeName(it.value.name)).build()
            })
            .applyFilterForTypeAspect(GraphQLTypeAspects.FilterAspectType.OBJECT_CRITERIA)
            .build()

        // Generates a filter in the form of { any: [FilterCriteria], not:[FilterCriteria] }
        val filterCriteriaObjectType =
            ListType(NonNullType(TypeName(GraphQLTypeNameResolver.getFilterCriteriaTypeDefName(objectTypeDefinition))))

        val filterObjectTypeDef = InputObjectTypeDefinition.newInputObjectDefinition()
            .name(GraphQLTypeNameResolver.getFilterTypeDefName(objectTypeDefinition)).inputValueDefinitions(
                FilterGroupOperator.entries.map {
                    InputValueDefinition.newInputValueDefinition()
                        .name(it.fieldName)
                        .type(filterCriteriaObjectType)
                        .build()
                }
            )
            .applyFilterForTypeAspect(GraphQLTypeAspects.FilterAspectType.EXPRESSION)
            .build()

        (filterFieldInputTypes.values + filterObjectTypeDef + filterCriteriaObjectTypeDef).forEach {
            context.newTypes.putIfAbsent(it.name, it)
        }

        return objectTypeDefinition
    }

    private fun getFilterOperators(typeDef: ObjectTypeDefinition): Map<FieldDefinition, List<FilterOperator>> {
        return typeDef.fieldDefinitions.filter { it.hasDirective(FILTER_DIRECTIVE) }
            .associate { field ->
                val filterDirective =
                    field.getDirectives(FILTER_DIRECTIVE)
                        .first()
                val arguments =
                    filterDirective.getArgument(FILTER_DIRECTIVE_OPERATORS).value as? ArrayValue
                val operators = (arguments?.values as? List<*>)?.filterIsInstance<EnumValue>()?.map {
                    FilterOperator.valueOf(it.name)
                }
                    ?: throw IllegalArgumentException("Unexpected type [${arguments.toString()}] for filter operators on field [${field.name}] of type [${typeDef.name}].")

                if (operators.isEmpty()) {
                    throw IllegalArgumentException("Filtered field [${field.name}] for type [${typeDef.name}] must supply valid filter operators.")
                }

                field to operators
            }
    }

    private fun shouldProcess(typeDef: ObjectTypeDefinition): Boolean {
        return typeDef.fieldDefinitions.any { it.hasDirective(FILTER_DIRECTIVE) }
    }
}