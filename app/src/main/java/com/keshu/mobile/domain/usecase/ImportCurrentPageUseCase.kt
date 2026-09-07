package com.keshu.mobile.domain.usecase

import com.keshu.mobile.data.parser.ImportPageType
import com.keshu.mobile.data.repository.AcademicRepository
import com.keshu.mobile.data.repository.ImportSummary

class ImportCurrentPageUseCase(private val repository: AcademicRepository) {
    suspend operator fun invoke(html: String, pageType: ImportPageType): ImportSummary {
        require(html.isNotBlank()) { "当前页面为空，无法导入" }
        return repository.importCurrentPage(html, pageType)
    }
}
