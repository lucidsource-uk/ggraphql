package uk.co.lucidsource.ggraphql.api.pagination

data class PaginatedResult<T, P>(
    val nodes: List<T>,
    val total: Int,
    val pageNumber: Int,
    val previousCursor: P? = null,
    val nextCursor: P? = null
)