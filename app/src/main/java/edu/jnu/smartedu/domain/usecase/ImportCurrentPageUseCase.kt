package edu.jnu.smartedu.domain.usecase

import edu.jnu.smartedu.data.parser.ImportPageType
import edu.jnu.smartedu.data.repository.AcademicRepository
import edu.jnu.smartedu.data.repository.ImportSummary

class ImportCurrentPageUseCase(private val repository: AcademicRepository) {
    suspend operator fun invoke(html: String, pageType: ImportPageType): ImportSummary {
        require(html.isNotBlank()) { "当前页面为空，无法导入" }
        return repository.importCurrentPage(html, pageType)
    }
}
