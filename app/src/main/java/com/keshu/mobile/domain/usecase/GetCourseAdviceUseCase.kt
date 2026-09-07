package com.keshu.mobile.domain.usecase

import com.keshu.mobile.data.repository.AdvisorRepository
import com.keshu.mobile.domain.model.CourseAdvice

class GetCourseAdviceUseCase(private val repository: AdvisorRepository) {
    suspend operator fun invoke(apiKey: String): CourseAdvice {
        require(apiKey.isNotBlank()) { "请输入 DeepSeek API Key" }
        return repository.requestAdvice(apiKey)
    }
}
