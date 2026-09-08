package com.keshu.mobile.data.source

class AcademicDataSourceRegistry(
    factories: List<WebAcademicDataSourceFactory>,
    defaultSourceId: String,
) {
    private val factoriesById = factories.associateBy { it.descriptor.id }

    init {
        require(factories.isNotEmpty()) { "At least one academic data source is required" }
        require(factoriesById.size == factories.size) { "Academic data source IDs must be unique" }
        require(defaultSourceId in factoriesById) { "Unknown default academic data source: $defaultSourceId" }
    }

    val availableSources: List<AcademicDataSourceDescriptor> = factories.map { it.descriptor }
    val defaultFactory: WebAcademicDataSourceFactory = factoriesById.getValue(defaultSourceId)

    fun factory(sourceId: String): WebAcademicDataSourceFactory? = factoriesById[sourceId]
}
