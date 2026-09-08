package com.keshu.mobile.domain.usecase

import com.keshu.mobile.data.repository.AcademicRepository
import com.keshu.mobile.data.repository.ImportSummary
import com.keshu.mobile.data.source.AcademicDataset
import com.keshu.mobile.data.source.AcademicImportData

class ImportAcademicDataUseCase(
    private val repository: AcademicRepository,
) {
    suspend operator fun invoke(data: AcademicImportData, dataset: AcademicDataset): ImportSummary {
        return repository.importData(data, dataset)
    }
}
