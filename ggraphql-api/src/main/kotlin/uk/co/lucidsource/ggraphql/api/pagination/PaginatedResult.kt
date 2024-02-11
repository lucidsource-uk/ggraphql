package uk.co.lucidsource.ggraphql.api.pagination

data class PaginatedResult<T>(
    val nodes: List<T>,
    val total: Int,
    val pageNumber: Int,
    val previousCursor: String? = null,
    val nextCursor: String? = null
)