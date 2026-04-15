package uk.co.lucidsource.ggraphql.transformers.schema

import graphql.language.FieldDefinition
import graphql.language.InputObjectTypeDefinition
import graphql.language.InterfaceTypeDefinition
import graphql.language.ObjectTypeDefinition
import uk.co.lucidsource.ggraphql.plugin.AnnotationMapper
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformer
import uk.co.lucidsource.ggraphql.transformers.SDLNodeTransformerContext
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.applyAnnotationAspects

/**
 * Transformer that processes custom GraphQL directives and applies annotation aspects
 * to SDL nodes for code generation.
 *
 * This transformer handles object types, interface types, input types, and their field
 * definitions, extracting directives that have configured mappings and storing annotation
 * metadata using the GraphQLTypeAspects utility.
 *
 * @property annotationMapper The mapper for converting GraphQL directives to annotation metadata
 */
class AnnotationDirectiveTransformer(
    private val annotationMapper: AnnotationMapper
) : SDLNodeTransformer {

    override fun transformObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ): ObjectTypeDefinition {
        val aspects = annotationMapper.processDirectives(objectTypeDefinition)
        val processedFields = processFieldDefinitions(objectTypeDefinition.fieldDefinitions)
        
        if (aspects.isEmpty() && processedFields == objectTypeDefinition.fieldDefinitions) {
            return objectTypeDefinition
        }
        
        return objectTypeDefinition.transform { builder ->
            if (aspects.isNotEmpty()) {
                builder.applyAnnotationAspects(aspects)
            }
            builder.fieldDefinitions(processedFields)
        }
    }

    override fun transformInterfaceType(
        interfaceTypeDefinition: InterfaceTypeDefinition,
        context: SDLNodeTransformerContext
    ): InterfaceTypeDefinition {
        val aspects = annotationMapper.processDirectives(interfaceTypeDefinition)
        val processedFields = processFieldDefinitions(interfaceTypeDefinition.fieldDefinitions)
        
        if (aspects.isEmpty() && processedFields == interfaceTypeDefinition.fieldDefinitions) {
            return interfaceTypeDefinition
        }
        
        return interfaceTypeDefinition.transform { builder ->
            if (aspects.isNotEmpty()) {
                builder.applyAnnotationAspects(aspects)
            }
            builder.definitions(processedFields)
        }
    }

    override fun transformInputType(
        inputObjectTypeDefinition: InputObjectTypeDefinition,
        context: SDLNodeTransformerContext
    ): InputObjectTypeDefinition {
        val aspects = annotationMapper.processDirectives(inputObjectTypeDefinition)
        val processedInputValues = inputObjectTypeDefinition.inputValueDefinitions.map { inputValue ->
            val fieldAspects = annotationMapper.processDirectives(inputValue)
            if (fieldAspects.isEmpty()) {
                inputValue
            } else {
                inputValue.transform { builder ->
                    builder.applyAnnotationAspects(fieldAspects)
                }
            }
        }
        
        if (aspects.isEmpty() && processedInputValues == inputObjectTypeDefinition.inputValueDefinitions) {
            return inputObjectTypeDefinition
        }
        
        return inputObjectTypeDefinition.transform { builder ->
            if (aspects.isNotEmpty()) {
                builder.applyAnnotationAspects(aspects)
            }
            builder.inputValueDefinitions(processedInputValues)
        }
    }

    /**
     * Processes field definitions to apply annotation aspects from custom directives.
     *
     * @param fieldDefinitions The list of field definitions to process
     * @return The processed list of field definitions with annotation aspects applied
     */
    private fun processFieldDefinitions(
        fieldDefinitions: List<FieldDefinition>
    ): List<FieldDefinition> {
        return fieldDefinitions.map { field ->
            val aspects = annotationMapper.processDirectives(field)
            if (aspects.isEmpty()) {
                field
            } else {
                field.transform { builder ->
                    builder.applyAnnotationAspects(aspects)
                }
            }
        }
    }
}
