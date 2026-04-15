package uk.co.lucidsource.ggraphql.visitors.kotlin

import graphql.language.ObjectTypeDefinition
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.getResolverAspectResolverName
import uk.co.lucidsource.ggraphql.util.GraphQLTypeAspects.isResolverOnlyType
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitor
import uk.co.lucidsource.ggraphql.visitors.SDLNodeVisitorContext

/**
 * Visitor that identifies resolver-only types before other visitors run.
 * This allows other visitors to know which types are resolver-only during generation.
 */
class ResolverOnlyTypeDetector : SDLNodeVisitor {
    override fun visitObjectType(
        objectTypeDefinition: ObjectTypeDefinition,
        context: SDLNodeVisitorContext
    ) {
        if (objectTypeDefinition.isResolverOnlyType()) {
            context.resolverOnlyTypes.add(objectTypeDefinition.name)
        }
    }
}